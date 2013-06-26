
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

#include "fifo.h"

#define BLOCK 1000
#define DISPLAY	0xfffff

int main (void)
{
	long long put = 0, get = 0;
	char str[32];
	fifo_t fifo = fifo_create();
	
	while (1)
	{
		int i;
		int block = random() % BLOCK;
		
		for (i = 0; i < block; i++)
		{
			if ((put & DISPLAY) == DISPLAY)
			{
				printf("%lli... (size %lli)\r", put, put - get);
				fflush(stdout);
			}

			sprintf(str, "%lli", put++);
			fifo_add(fifo, str);
		}

		block = random() % BLOCK;
		for (i = 0; i < block; i++)
		{
			char* out = fifo_getdel(fifo);
			if (!out)
				break;
			long long test = atoll(out);
			
			if (test != get)
			{
				fprintf(stderr, "error, getting %lli (%s), should be %lli\n", test, out, get);
				exit(1);
			}
			get++;
			
			free(out);
		}
	}
	
	return 0;
}
