
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
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <termios.h>
#include <unistd.h>
#include <sys/file.h>

#include "config.h"

#if HAVE_LIBLOCKDEV
#include <lockdev.h>
#endif
                            
#include "libgate.h"
#include "gatefd.h"
#include "gateser.h"
#include "fifo.h"

#define DEVICE_NAMELEN	256

#define IDX_SPEED	0
#define IDX_TYPE	1
#define IDX_DEVICE	2
#define IDXNUMBER	3

static fifo_t params [IDXNUMBER] = { NULL, NULL, NULL };
static fifo_iterator_t it_speed, it_type, it_device;
static const char* value_speed;
static const char* value_type;
static const char* value_device;

static int lock_before_open (const char* dev)
{
#ifdef HAVE_LIBLOCKDEV
	if (dev_lock(dev) != 0)
	{
		lwsl_notice("serial device '%s' already locked\n", dev);
		return -1;
	}
#else
	(void)dev;
#endif
	
	return 0;
}

static int lock_after_open (const char* dev, int fd)
{
	if (flock(fd, LOCK_EX | LOCK_NB) != 0)
	{
		lwsl_notice("serial device '%s' already locked (%s)\n", dev, strerror(errno));
		close(fd);
		return -1;
	}
	
	return 0;
}

void gate_close_unlock (const char* dev, int fd)
{
#ifdef HAVE_LIBLOCKDEV
	dev_unlock(dev, getpid());
#endif
	
	if (flock(fd, LOCK_UN) != 0)
		lwsl_notice("error unlocking serial device '%s': %s\n", dev, strerror(errno));
}

static void clean (void)
{
	int idx;
	for (idx = 0; idx < IDXNUMBER; idx++)
		if (params[idx])
			fifo_destroy(&params[idx]);
}

static void init_params (void)
{
	int idx;
	for (idx = 0; idx < IDXNUMBER; idx++)
		params[idx] = fifo_create();
}

static void atexit_serial (void)
{
	fprintf(stderr, "cleaning serial...\n");
	clean();
}

int gate_serial_parse (const char* serial_descr)
{
	const char* current = serial_descr;
	const char* next = serial_descr;
	int idx = 0;
	
	static int atexit_serial_registered = 0;
	if (!atexit_serial_registered)
		atexit(atexit_serial);
	
	init_params();
	
	do
	{
		while (*next && *next != ',' && *next != '#')
			next++;
		
		if (*next == ',' || *next == '#')
		{
			if (next - current > 0)
				fifo_adds(params[idx], current, next - current, NULL);
			if (*next == '#')
				idx++;
			current = ++next;
		}
	} while (*next && idx < IDXNUMBER);
	
	if (*next || idx != IDXNUMBER - 1)
	{
		fprintf(stderr, "error in serial argument format '%s'\n", serial_descr);
		clean();
		return 1;
	}

	if (!*next && next - current > 0)
		fifo_adds(params[idx], current, next - current, NULL);
	
#if 0
	for (idx = 0; idx < IDXNUMBER; idx++)
	{
		fifo_iterator_t it;
		const char* val;
		fprintf(stderr, "%i:\n", idx);
		for (it = fifo_begin(params[idx]); (val = fifo_next(&it)); )
			fprintf(stderr, "	'%s'\n", val);
		fprintf(stderr, "\n");
	}
#endif
	
	it_speed = it_type = it_device = NULL;
	
	return 0;
}



// -1: error / 0: ok / 1: ok,last
// will update value_device value_type value_speed
static int gate_serial_list_next (void)
{
	if (!it_device)
	{
		it_device = fifo_begin(params[IDX_DEVICE]);
		if (!it_type)
		{
			it_type = fifo_begin(params[IDX_TYPE]);
			if (!it_speed)
				it_speed = fifo_begin(params[IDX_SPEED]);
			value_speed = fifo_next(&it_speed);
		}
		value_type = fifo_next(&it_type);
	}
	value_device = fifo_next(&it_device);
	
	if (!value_device || !value_type || !value_speed || strlen(value_type) != 3)
	{
		fprintf(stderr, "error in serial argument value\n");
		return -1;
	}
	
	return 0;
}
	
int gate_serial_scan (void)
{
	struct termios tio;
	char dev [DEVICE_NAMELEN];

	do
	{
		if (gate_serial_list_next() < 0)
			return -1;

		snprintf(dev, DEVICE_NAMELEN, "%s%s", *value_device == '/'? "": "/dev/", value_device);
		
		if (gate_fd_dev_is_opened(dev) == 0)
		{
			lwsl_notice("not trying to reopen '%s'\n", dev);
			continue;
		}

		lwsl_notice("try open serial device '%s' (%s/%s/%s)...\n", dev, value_speed, value_type, value_device);
		
		if (lock_before_open(dev) != 0)
			continue;

		int fd = open(dev, O_RDWR);
		if (fd == -1)
		{
			if (errno != ENOENT)
				perror(dev);
			continue;
		}
		
		if (lock_after_open(dev, fd) != 0)
			continue; // closed
		
		lwsl_notice("serial device '%s' opened, fd=%i - trying %s/%s\n", dev, fd, value_type, value_speed);
		
		if (tcgetattr(fd, &tio) == -1)
		{
			lwsl_notice("serial/tcgetattr: %s\n", strerror(errno));
			gate_close_unlock(dev, fd);
			continue;
		}

		tio.c_cflag = 0;
		
		switch (atoi(value_speed))
		{
		case 115200: tio.c_cflag |= B115200; break;
		case 57600: tio.c_cflag |= B57600; break;
		case 38400: tio.c_cflag |= B38400; break;
		case 19200: tio.c_cflag |= B19200; break;
		case 9600: tio.c_cflag |= B9600; break;
		case 4800: tio.c_cflag |= B4800; break;
		case 2400: tio.c_cflag |= B2400; break;
		default:
			fprintf(stderr, "invalid serial speed '%s'\n", value_speed);
			gate_close_unlock(dev, fd);
			continue;
		}
		
		switch (value_type[0] - '0')
		{
		case 8: tio.c_cflag |= CS8; break;
		case 7: tio.c_cflag |= CS7; break;
		case 6: tio.c_cflag |= CS6; break;
		case 5: tio.c_cflag |= CS5; break;
		default:
			fprintf(stderr, "invalid serial data size '%c' in '%s'\n", value_type[0], value_type);
			gate_close_unlock(dev, fd); 
			continue;
		}
		
		switch (tolower(value_type[1]))
		{
		case 's':
		case 'n': break;
		case 'e': tio.c_cflag |= PARENB; break;
		case 'o': tio.c_cflag |= PARENB | PARODD; break;
		default:
			fprintf(stderr, "invalid serial parity '%c' in '%s'\n", value_type[1], value_type);
			gate_close_unlock(dev, fd); 
			continue;
		}
		
		switch (value_type[2] - '0')
		{
		case 1: break;
		case 2: tio.c_cflag |= CSTOPB; break;
		default:
			fprintf(stderr, "invalid serial stop bit '%c' in '%s'\n", value_type[2], value_type);
			close(fd); 
			gate_close_unlock(dev, fd);
			continue;
		}
		
		tio.c_iflag = IGNPAR;
		tio.c_oflag = 0;
		tio.c_lflag = 0;   
		tio.c_cc[VMIN] = 1;
		tio.c_cc[VTIME] = 0;
		
		// empty the output queue
		if (tcflush(fd, TCIFLUSH) == -1)
		{
			perror(GATE_PERROR_HEADER "tcflush");
			close(fd);
			gate_close_unlock(dev, fd);
			continue;
		}

		// set new attributes
		if (tcsetattr(fd, TCSANOW, &tio) == -1)
		{
			perror(GATE_PERROR_HEADER "serial/tcsetattr");
			close(fd);
			gate_close_unlock(dev, fd);
			continue;
		}
		
		// serial devices need attention
		int fdindex = gate_fd_add(fd, FDF_NOCOMMENT | FDF_RQGATEAUTH | FDF_NEEDTRIGGER | FDF_NEEDUNLOCK);
		if (fdindex < 0)
		{
			close(fd);
			gate_close_unlock(dev, fd);
			continue;
		}
		strcpy(gate_fd(fdindex)->dev, dev);
		
		gate_fd_send(fdindex, "at");

	} while (it_device != NULL);

	return 0;
}
