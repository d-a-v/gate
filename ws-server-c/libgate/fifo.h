
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



#ifndef __FIFO_H
#define __FIFO_H

typedef struct fifo_s* fifo_t;
typedef struct fifo_iterator_s* fifo_iterator_t;

#define fifo_add(fifo,str)	fifo_add2(fifo,str,NULL)
#define fifo_next(it)		fifo_next2(it,NULL)

fifo_t		fifo_create	(void);
char*		fifo_getdel	(fifo_t fifo);
void		fifo_destroy	(fifo_t* fifo);
int		fifo_is_empty	(const fifo_t fifo);

// going through elements
fifo_iterator_t	fifo_begin	(const fifo_t fifo);
const char*	fifo_next2	(fifo_iterator_t* iterator, fifo_iterator_t* on_element);

// add helpers
fifo_t		fifo_add2	(fifo_t fifo, const char* str1, const char* str2);
// fifo_adds: input str1 may not be 0-terminated
fifo_t		fifo_adds	(fifo_t fifo, const char* str1, size_t len1, const char* str2);

#endif // __FIFO_H
