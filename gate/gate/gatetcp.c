
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
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
              
#include "libgate.h"
#include "gatetcp.h"

#ifndef AF_INET6
#define AF_INET6	AF_INET
#define sockaddr_in6	sockaddr_in
#define sin6_family	sin_family
#define sin6_port	sin_port
#define sin6_addr	sin_addr.s_addr
#define	in6addr_any	INADDR_ANY
#warning IPV6 not enabled in target?
#endif

int tcp_server (int port)
{
	int sock;

	sock = socket(AF_INET6, SOCK_STREAM, 0);
	if (sock == -1)
	{
		perror(GATE_PERROR_HEADER "tcp socket");
		exit(EXIT_FAILURE);
	}
	
#ifndef WIN32
	int opt = 1;
	if (setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)) == -1)
		perror(GATE_PERROR_HEADER "tcp setsockopt(SO_REUSEADDR,1)");
#endif
	
	struct sockaddr_in6 server;
	server.sin6_family = AF_INET6;
	server.sin6_port = htons(port);
	server.sin6_addr = in6addr_any;
	server.sin6_flowinfo = 0;
	server.sin6_scope_id = 0; // if_nametoindex(which interface for default gw);
	if (bind(sock, (struct sockaddr *)&server, sizeof(server)) == -1)
	{
		perror(GATE_PERROR_HEADER "tcp bind");
		exit(EXIT_FAILURE);
	}
	
	if (listen(sock, 1) == -1)
	{
		perror(GATE_PERROR_HEADER "tcp listen");
		exit(EXIT_FAILURE);
	}
	
	return sock;
}

int tcp_server_welcome_client (int sock)
{
	int cli;
	struct sockaddr_in client;
	socklen_t addr_len;
	
	addr_len = sizeof(client);
	if ((cli = accept(sock, (struct sockaddr*)&client, &addr_len)) == -1)
	{
		perror(GATE_PERROR_HEADER "tcp accept");
		exit(EXIT_FAILURE);
	}

	return cli;
}
	