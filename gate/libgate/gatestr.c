
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

#include <libgate.h>

#include "malloc_e.h"

#define GATE_STR_ALLOC_SIZE	128

gate_str_t* gate_str_init (gate_str_t* str, size_t size)
{
	if (size)
	{
		str->str = malloc_e(size, "string allocation");
		str->alloked = size;
	}
	else
	{
		str->str = NULL;
		str->alloked = 0;
	}
	return str;
}

void gate_str_realloc (gate_str_t* str, size_t size)
{
	size_t new_alloked = ((size_t)((size + GATE_STR_ALLOC_SIZE - 1) / GATE_STR_ALLOC_SIZE)) * GATE_STR_ALLOC_SIZE;
	if (new_alloked < str->alloked)
		return;
	char* new_str = realloc(str->str, new_alloked);
	if (new_str == NULL)
	{
		lwsl_err("cannot realloc from %lu to %lu bytes...\n", (long)str->alloked, (long)new_alloked);
		exit(EXIT_FAILURE);
	}
	str->alloked = new_alloked;
	str->str = new_str;
}

void gate_str_free (gate_str_t* str)
{
	if (str->alloked)
		free_e(&str->str);
}
