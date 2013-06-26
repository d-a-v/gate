
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
#include <getopt.h>
#include <string.h>

#include "libgate.h"
#include "gatetcp.h"
#include "gatefd.h"
#include "gateser.h"
#include "malloc_e.h"

#define VERSION			"20130222"
#define GATE_PROTOCOL_NAME	"gate"
#define GATE_AUTH_FROM_PEER	"OK gate"

#define PORT_TCP		(GATE_DEFAULT_PORT_WS + 1)


const char*	default_serial = "115200,57600,38400,19200,9600,4800,2400#8n1,7n2#ttyS0,ttyS1,ttyS2,ttyUSB0,ttyUSB1,ttyUSB2,ttyACM0,ttyACM1,ttyACM2,rfcomm0,rfcomm1,rfcomm2";


static struct option options[] = {
	{ "help",	no_argument,		NULL, 'h' },
	{ "ws",		required_argument,	NULL, 'w' },
	{ "tcp",	required_argument,	NULL, 't' },
	{ "serial",	optional_argument,	NULL, 's' },
	{ "daemon",	no_argument,		NULL, 'd' },
	{ NULL, 0, 0, 0 }
};



void help (const char* arg0, int exitcode)
{
	fprintf(stderr,
		"%s v%s - i/o multiplexer for websocket (tcp, serial)\n"
		"options are:\n"
		"	-w|--ws=<n>	websocket port (default %i)\n"
		"	-t|--tcp=<n>	tcp port (default %i)\n"
		"	-d|--daemon	daemonize, no console interaction\n"
		"	-s		use serial devices with default parameter (*1)\n"
		"	-h|--help\n"
		"	--serial=<devs>	serial devices (*1)\n"
		"\n"
		" (*1) optional serial parameter format is 'baud1[,baud2...]#8n1[,7e2...]#ttyUSB0[,ttyACM1...]'\n"
		"      default serial parameter is '%s'\n"
		"\n"
	       , arg0, VERSION, GATE_DEFAULT_PORT_WS, PORT_TCP, default_serial);
	exit(exitcode);
}
	       
int main (int argc, char *argv[])
{
	char* from_gui;
	const char* serial = NULL;
	int port_tcp = PORT_TCP;
	int port_ws = GATE_DEFAULT_PORT_WS;
	int daemon = 0;
	int tcp = 0; // tcp server socket

	while (1) 
	{
		int n = getopt_long(argc, argv, "hw:t:s;d", options, NULL);
		if (n < 0)
			break;
		switch (n)
		{
		case 'w':
			port_ws = atoi(optarg);
			break;
		case 't':
			port_tcp = atoi(optarg);
			break;
		case 's':
			serial = optarg? optarg: default_serial;
			break;
		case 'd':
			daemon = 1;
			break;
		case 'h':
			help(argv[0], EXIT_SUCCESS);
		}
	}

	if (gate_init(GATE_PROTOCOL_NAME) != 0)
		exit(1);
	
printf("port=%i\n",port_ws);
	gate_set_port(port_ws);

	if (gate_start() != 0)
		exit(EXIT_FAILURE);
		
	if (!daemon)
	{
		gate_fd_add(0, FDF_READONLY);
		gate_fd_add(1, FDF_WRITEONLY);
	}
		
if (serial) fprintf(stderr, "serial=%s\n", serial);
	if (serial)
		gate_serial_parse(serial);

	gate_add_pollfd(tcp = tcp_server(port_tcp), POLLIN);

	while (1)
	{
		if (serial)
			gate_serial_scan();

		int n = gate_poll_ms(serial? 1000: -1);

		if (n < 0)
			exit(EXIT_FAILURE);
		
		if (n > 0)
		{
			// check incoming local tcp connection
			if (gate_get_pollfd(tcp)->revents & POLLIN)
				gate_fd_add(tcp_server_welcome_client(tcp), 0);
			
			// process our file descriptors
			for (n = 0; n < gate_fd_count(); n++)
			{
				short revents = gate_get_pollfd(gate_fd(n)->fd)->revents;
				
				if (   ((revents & POLLIN) && gate_fd_receive(n) != 0)
				    || ((revents & POLLOUT) && gate_fd_process_send(n) != 0))
				{
					gate_fd_close(n--);
				}
			}
		}
		
		// dispatch tcp/serial to gui+tcp/serial
		for (n = 0; n < gate_fd_count(); n++)
		{
			fd_t* fd = gate_fd(n);
			while (!fifo_is_empty(fd->from_peer))
			{
				int m;
				
				char* from_peer = fifo_getdel(fd->from_peer);
				
				// peer needs to ack gate protocol ?
				if (fd->flags & FDF_RQGATEAUTH)
				{
					if (strncasecmp(from_peer, GATE_AUTH_FROM_PEER, sizeof GATE_AUTH_FROM_PEER - 1) == 0)
					{
						fd->flags &= ~FDF_RQGATEAUTH;

						// need to tell peer to send gui init ?
						if (fd->flags & FDF_NEEDTRIGGER)
						{
							fd->flags &= ~FDF_NEEDTRIGGER;
							gate_fd_send(n, GATE_PROTOCOL_NAME);
						}
					}
					else
					{
						fprintf(stderr, "'%s' is not gate auth, closing\n", from_peer);
						gate_fd_close(n--);
						break;
					}
					
					// no need to dispatch
					free_e(&from_peer);
					continue;
				}
				
				// dispatch
				for (m = 0; m < gate_fd_count(); m++)
					// do not echo to ourselves
					// do not echo from 0 to 1 either
					if (m != n && !(gate_fd(n)->fd == 0 && gate_fd(m)->fd == 1))
						gate_fd_send2(m, "#to-websocket: ", from_peer);
				gate_send(from_peer);
				free_e(&from_peer);
			}
		}

		// dispatch gui to tcp/serial
		while ((from_gui = gate_receive()))
		{
			for (n = 0; n < gate_fd_count(); n++)
				gate_fd_send(n, from_gui);
			free_e(&from_gui);
		}

	}

	while (gate_fd_count())
		gate_fd_close(0);
	gate_stop();

	return 0;
}
