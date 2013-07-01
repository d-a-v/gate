
/**************************************************************
 *
 * - CeCILL-B license
 * - (bsd-like, check http://www.cecill.info/faq.en.html#bsd)
 * 
 * Copyright CNRS
 * Contributors:
 * David Gauchard <gauchard@laas.fr>	2013-01-01
 * 
 * This software is a computer program whose purpose is to
 * provide a "Graphical Access To Exterior" (GATE).  The goal
 * is to provide a generic GUI, within a javascript web
 * browser, through a TCP network using the websocket protocol. 
 * Plain text protocol (simple human readable graphic commands)
 * translators to websockets protocol are also provided to
 * connect user applications to the browser via a C library or
 * simple TCP server.
 * 
 * This software is governed by the CeCILL-B license under
 * French law and abiding by the rules of distribution of free
 * software.  You can use, modify and/ or redistribute the
 * software under the terms of the CeCILL-B license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * 
 * As a counterpart to the access to the source code and rights
 * to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the
 * software's author, the holder of the economic rights, and
 * the successive licensors have only limited liability.
 * 
 * In this respect, the user's attention is drawn to the risks
 * associated with loading, using, modifying and/or developing
 * or reproducing the software by the user in light of its
 * specific status of free software, that may mean that it is
 * complicated to manipulate, and that also therefore means
 * that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge.  Users are
 * therefore encouraged to load and test the software's
 * suitability as regards their requirements in conditions
 * enabling the security of their systems and/or data to be
 * ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 * 
 * The fact that you are presently reading this means that you
 * have had knowledge of the CeCILL-B license and that you
 * accept its terms.
 * 
 *************************************************************/


#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <string.h>
#include <errno.h>
#include <stdlib.h>

#include <lockdev.h>

#include "gatefd.h"
#include "libgate.h"
#include "malloc_e.h"

static fd_t	fds[FD_SIZE];
static int	fdlen = 0;
static int	atexit_fdclose_registered = 0;

static void atexit_fdclose (void);

static int is_comment (const char* str)
{
	while (*str && (*str == ' ' || *str == '\t'))
		str++;
	return *str == '#';
}

int gate_fd_count (void)
{
	return fdlen;
}

fd_t* gate_fd (int fdindex)
{
	return &fds[fdindex];
}

// return fdindex (or -1)
int gate_fd_add (int newfd, long flags)
{
	if (fdlen == FD_SIZE)
	{
		fprintf(stderr, "error: cannot handle more than %i peers.\n", FD_SIZE);
		return -1;
	}

	fd_t* fd = &fds[fdlen];

	fd->fd = newfd;
	fd->to_peer = fifo_create();
	fd->from_peer = fifo_create();
	gate_str_init(&fd->current_from_peer);
	fd->current_from_peer.str[0] = 0;
	fd->dev[0] = 0;
	fd->flags = flags;

	if (fcntl(newfd, F_SETFL, O_NONBLOCK) != 0)
	{
		perror(GATE_PERROR_HEADER "fcntl(NONBLOCK)");
		return -1;
	}

	gate_add_pollfd(newfd, POLLIN);
	
	if (!atexit_fdclose_registered)
	{
		atexit_fdclose_registered = 1;
		atexit(atexit_fdclose);
	}
	
	return fdlen++;
}

// 0: ok, otherwise remove fd
int gate_fd_receive (int fdindex)
{
	int len;
	fd_t* fd = &fds[fdindex];
	
	do
	{
		int pre = strlen(fd->current_from_peer.str);
		
		#define REASONABLE 256
		if (fd->current_from_peer.alloked - pre < REASONABLE)
			gate_str_grow(&fd->current_from_peer, REASONABLE);
		
		len = read(fd->fd, fd->current_from_peer.str + pre, fd->current_from_peer.alloked - pre);
		lwsl_info("%i has been read(fd=%i) (%li asked)\n", len, fd->fd, (long)(fd->current_from_peer.alloked - pre));
		if (len > 0) { int i; lwsl_debug("read:('"); for (i = 0; i < len; i++) lwsl_debug("%c", *(fd->current_from_peer.str + pre + i)); lwsl_debug("')\n"); }
		
		if (len == 0 || (len == -1 && errno != EAGAIN && errno != EWOULDBLOCK))
		{
			perror(GATE_PERROR_HEADER "read");
			
			if (fd->fd == 0)
				exit(0);
			
			return 1; // to be closed
		}
			
		if (len > 0)
		{
			char* start;
			char* end;

			fd->current_from_peer.str[len += pre] = 0;

			lwsl_info("processing %i more chars, total='%s'\n", len, fd->current_from_peer.str);

			// chop and store
			start = end = fd->current_from_peer.str;
			while (1)
			{
				if (!*end) // no endline received
				{
					lwsl_info("updating '%s' for fifo(fd=%i dev='%s')\n", start, fd->fd, fd->dev? fd->dev: "");
					memmove(fd->current_from_peer.str, start, end - start + 1);
					break;
				}
				if (*end == '\n' || *end == '\r')
				{
					lwsl_info("adding '%s' on fifo(fd=%i dev='%s')\n", start, fd->fd, fd->dev? fd->dev: "");
					*end = 0;
					if (end != start)
						fifo_add(fd->from_peer, start);
					start = end + 1;
				}
				end++;
			}
		}
	} while (len > 0);

	return 0;
}

void gate_fd_send2 (int fdindex, const char* to_send1, const char* to_send2)
{
	fd_t* fd = &fds[fdindex];
	if ((!(fd->flags & FDF_NOCOMMENT) || !is_comment(to_send1)) && !(fd->flags & FDF_READONLY))
	{
		fifo_add2(fd->to_peer, to_send1, to_send2);
		gate_get_pollfd(fd->fd)->events |= POLLOUT;
	}
}

// 0: ok, otherwise remove fd
int gate_fd_process_send (int fdindex)
{
	fd_t* fd = &fds[fdindex];
	
	while (1)
	{
		// do we already have something to write in
		// current_to_peer otherwise is fifo empty ?
		if (   (!fd->current_to_peer || !fd->current_to_peer[0])
		    && (!(fd->current_to_peer = fifo_getdel(fd->to_peer)))
		   )
		{
			// no, no more interested in POLLOUT
			gate_get_pollfd(fd->fd)->events &= ~POLLOUT;
			break;
		}

		size_t len = strlen(fd->current_to_peer);
		ssize_t sent = 0;
		ssize_t offset = 0;
		
		// overwrite trailing \0
		fd->current_to_peer[len++] = '\n';
		do
		{
static int bad = 0;if (!bad){bad=1;fprintf(stderr, "(improve serial settings!)\n");}
			//sent = write(fd->fd, fd->current_to_peer + offset, len);
			sent = write(fd->fd, fd->current_to_peer + offset, len>1? 1: len);
			
			if (sent < 0)
			{
				perror(GATE_PERROR_HEADER "write");
				return 1; // error, remove fd
			}
			
			offset += sent;
			len -= sent;
			
		} while (sent > 0);
		
		if (len > 0)
		{
			// could not finish current line

			// shift data not sent
			memmove(fd->current_to_peer, fd->current_to_peer + offset, len);
			fd->current_to_peer[len - 1] = 0;
			
			// more to send, but not now, pollout kept
			break;
		}
		
		// current_line completely sent
		free_e(&fd->current_to_peer);
		fd->current_to_peer = NULL;
	}
	return 0;
}

void gate_fd_close (int fdindex)
{
	fd_t* fd = &fds[fdindex];
	gate_del_pollfd(fd->fd);
	close(fd->fd);
	fifo_destroy(&fd->from_peer);
	fifo_destroy(&fd->to_peer);
	gate_str_free(&fd->current_from_peer);
	
	if (fd->flags & FDF_NEEDUNLOCK)
		dev_unlock(fd->dev, getpid());

	memmove(fd, &fds[--fdlen], sizeof(fd_t));
}


// check if device is already opened (0 = opened)
int gate_fd_dev_is_opened (const char* dev)
{
	int i;
	for (i = 0; i < fdlen; i++)
		if (strcmp(dev, fds[i].dev) == 0)
			return 0;
	return 1;
}


static void atexit_fdclose (void)
{
	lwsl_info("cleaning fd...\n");
	while (fdlen)
		gate_fd_close(0);
}
