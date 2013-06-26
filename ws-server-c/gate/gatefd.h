
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


#ifndef __GATE_FD_H
#define __GATE_FD_H

#define DEVNAME_SIZE	256
#define FD_SIZE		10

#include <libgate.h>

#include "fifo.h"

#define FDF_NOCOMMENT	(1<<0)
#define FDF_RQGATEAUTH	(1<<1)
#define FDF_NEEDTRIGGER	(1<<2)
#define FDF_NEEDUNLOCK	(1<<3)
#define FDF_READONLY	(1<<4)
#define FDF_WRITEONLY	(1<<5)

typedef struct fd_s
{
	int		fd;
	char		dev [DEVNAME_SIZE];
	long		flags;

	fifo_t		to_peer;
	char*		current_to_peer;

	fifo_t		from_peer;
	gate_str_t	current_from_peer;
} fd_t;

//
// non blocking minilib
//

// return fdindex (or -1)
int		gate_fd_add		(int newfd, long flags);

int		gate_fd_count		(void);
fd_t*		gate_fd			(int fdindex);
void		gate_fd_close		(int fdindex);

// add new data to send
void		gate_fd_send2		(int fdindex, const char* to_send, const char* to_send2);
#define		gate_fd_send(a,b)	gate_fd_send2(a,b,NULL)

// receive and store data (when POLLIN)
// 0: ok, otherwise remove fd
int		gate_fd_receive		(int fdindex);

// process stored data through fd (when POLLOUT)
// 0: ok, otherwise remove fd
int		gate_fd_process_send	(int fdindex);

// check if device is already opened (0 = opened)
int		gate_fd_dev_is_opened	(const char* dev);

#endif // __GATE_FD_H
