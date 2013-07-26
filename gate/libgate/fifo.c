
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



#include <string.h>

#include "malloc_e.h"
#include "fifo.h"

typedef struct fifo_iterator_s
{
	char*			str;
	struct fifo_iterator_s*	prev;
}* fifo_iterator_t;

typedef struct fifo_s
{
	fifo_iterator_t		first, last;
}* fifo_t;

int fifo_is_empty (const fifo_t fifo)
{
	return !fifo->last;
}

fifo_t fifo_create (void)
{
	fifo_t fifo = (fifo_t)malloc_e(sizeof(struct fifo_s), "fifo head");
	fifo->first = fifo->last = NULL;
	return fifo;
}

fifo_t fifo_add2 (fifo_t fifo, const char* str1, const char* str2)
{
	return fifo_adds(fifo, str1, strlen(str1), str2);
}

// str1 is not necessarily 0-terminated
fifo_t fifo_adds (fifo_t fifo, const char* str1, size_t len1, const char* str2)
{
	char* copy = (char*)malloc_e(len1 + (str2?strlen(str2):0) + 1, "string copy");
	fifo_iterator_t iterator = (fifo_iterator_t)malloc_e(sizeof(struct fifo_iterator_s), "fifo iterator");
	// str1 is not necessarily 0-terminated
	iterator->str = strncpy(copy, str1, len1);
	copy[len1] = 0;
	if (str2)
		strcpy(copy + len1, str2);
	iterator->prev = NULL;
	if (fifo->first)
		fifo->first->prev = iterator;
	fifo->first = iterator;
	if (!fifo->last)
		fifo->last = iterator;
	return fifo;
}

char* fifo_getdel (fifo_t fifo)
{
	if (!fifo->last)
		return NULL;
	char* ret = fifo->last->str;
	fifo_iterator_t todelete = fifo->last;
	if (!(fifo->last = fifo->last->prev))
		fifo->first = NULL;
	free_e(&todelete);
	return ret;
}

void fifo_destroy (fifo_t* fifo)
{
	char* todel;
	while ((todel = fifo_getdel(*fifo)))
		free_e(&todel);
	free_e(fifo);
}





fifo_iterator_t fifo_begin (const fifo_t fifo)
{
	return fifo->last;
}

const char* fifo_next2 (fifo_iterator_t* iterator, fifo_iterator_t* on_element)
{
	if (!*iterator)
		return NULL;
	const char* ret = (*iterator)->str;
	if (on_element)
		*on_element = *iterator;
	*iterator = (*iterator)->prev;
	return ret;
}
