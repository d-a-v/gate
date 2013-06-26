
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



#ifndef __LIBGATE_H
#define __LIBGATE_H

#ifdef __cplusplus
extern "C"
{
#endif

#include <poll.h>

typedef struct gate_str_s
{
	char* str;
	size_t alloked;
} gate_str_t;

#define		GATE_WS_PROTOCOL	"gate"
#define		GATE_PERROR_HEADER	"gate: "
#define		GATE_DEFAULT_PORT_WS	7681
#define		GATE_STR_ALLOC_SIZE	128
#define		GATE_STR_INIT		{ NULL, 0 }
#define		GATE_TALKING_PROTOCOL	"# gate talking - protocol version 20130326"
#define		GATE_TALKING		"# gate talking"

#define 	GATE_LOGLEVEL		1

#define		LOG(lvl, x...)		do { if (lvl <= GATE_LOGLEVEL) fprintf(stderr, x); } while (0)

void		gate_set_port		(int port);
void		gate_set_interface	(const char* interface);

int		gate_init		(const char* protocol_name);
int		gate_start		(void);
void		gate_stop		(void);

int		gate_add_pollfd		(int fd, int events);
void		gate_del_pollfd		(int fd);

// returns <0 if error, 0 if timeouted, >0 if user's fds have to be checked
int		gate_poll_ms		(int timeout);

struct pollfd*	gate_get_pollfd		(int fd);

// may be NULL, user must call free on result
char*		gate_receive		(void);

gate_str_t*	gate_str_init		(gate_str_t* str);
void		gate_str_grow		(gate_str_t* str, size_t desired_size);
void		gate_str_free		(gate_str_t* str);

const char*	gate_send		(const char* output);
const char*	gate_psend		(const char* s, ...) __attribute__((format(printf, 1, 2)));

int		gate_talking		(const char* recv);
int		gate_talking_protocol	(const char* recv);

#ifdef __cplusplus
}
#endif

#endif // __LIBGATE_H
