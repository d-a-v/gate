
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

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdarg.h>

#include <libwebsockets.h>

#include "libgate.h"
#include "malloc_e.h"
#include "fifo.h"
#include "data.h"

#include "make-cb-list.h"

struct per_session_data_empty
{
};

/* list of supported protocols and callbacks */

enum protocols_e
{
	protocol_http = 0,
	protocol_gate = 1,

	protocol_endoflist = 2,
};

static struct libwebsocket_protocols protocols [protocol_endoflist + 1];

static	struct libwebsocket_context*		context = NULL;
static	struct pollfd*				pollfds = NULL;
static	int*					fd_lookup = NULL;
static	int					count_pollfds = 0;
static	int					max_poll_elements = 0;
static	fifo_t					ws_output = NULL;
static	fifo_t					ws_input = NULL;
static	struct lws_context_creation_info	info;
static	gate_str_t				psend_line = GATE_STR_INIT;
static	gate_str_t				output_buffer_lws = GATE_STR_INIT;

unsigned long (*gate_serve_external_file) (const char* name, const unsigned char** data);

int gate_add_pollfd (int fd, int event)
{
	if (count_pollfds >= max_poll_elements)
	{
		lwsl_err("ADD_POLL_FD: too many sockets to track\n");
		exit(EXIT_FAILURE);
	}

	// add stdin to pollfds
	pollfds[count_pollfds].fd = fd;
	pollfds[count_pollfds].events = event;
	pollfds[count_pollfds].revents = 0;
	return (fd_lookup[fd] = count_pollfds++);
}

void gate_del_pollfd (int fd)
{
	if (--count_pollfds)
	{
		int m = fd_lookup[fd];
		/* have the last guy take up the vacant slot */
		pollfds[m] = pollfds[count_pollfds];
		fd_lookup[pollfds[count_pollfds].fd] = m;
	}
}

void print_unhandled_reason (const char* from, int reason)
{
	int i;

	(void)from;

	for (i = 0; reasons[i].what; i++)
		if (reasons[i].reason == reason)
		{
			lwsl_debug("unhandled reason %i in %s: %s\n", reason, from, reasons[i].what);
			return;
		}
	lwsl_debug("unhandled reason %i in %s (no description)\n", reason, from);
}

static int callback_http (
		struct libwebsocket_context * context,
		struct libwebsocket *wsi,
		enum libwebsocket_callback_reasons reason, void *user,
							   void *in, size_t len)
{
	(void)user;
	int fd = (int)(long)in;
	
	switch (reason)
	{
	
	case LWS_CALLBACK_HTTP:
	{
		static const unsigned char t404 [] = "nothing here";
		static const binware_s w404 = { ".html", sizeof(t404), t404 };
		binware_s user;

		const binware_s* bin;
		const char* ext;
		const char* content_type;
		char* qmark;
		int ret;
		
		lwsl_notice("HTTP URI: '%s'\n", (char *)in);
		
		// cut at first '?' from end
		for (qmark = ((char*)in)+strlen(in); qmark != in && *qmark != '?'; qmark--);
		if (*qmark == '?')
			*qmark = 0;

		// default URL or search
		if (strcmp(in, "/") == 0)
			in = "/index.html";
		for (bin = &binware_bin[0]; bin->name; bin++)
			if (strcmp(in, &bin->name[1]) == 0)
				break;
		if (!bin->name)
		{
			if (gate_serve_external_file && (user.size = gate_serve_external_file(user.name = in, &user.data)))
				bin = &user;
			else
				// ... or 404		
				bin = &w404;
		}

		// locate extension and set content-type
		for (ext = &bin->name[strlen(bin->name)]; ext != bin->name && *ext != '.'; ext--);
		if (strcasecmp(ext, ".html") == 0)
			content_type = "text/html";
		else if (strcasecmp(ext, ".css") == 0)
			content_type = "text/css";
		else if (strcasecmp(ext, ".ico") == 0)
			content_type = "image/vnd.microsoft.icon";
		else if (strcasecmp(ext, ".js") == 0)
			content_type = "text/javascript";
		else if (strcasecmp(ext, ".svg") == 0)
			content_type = "image/svg+xml";
		else
			content_type = "";
		
		lwsl_notice("serving '%s'\n", bin->name);
		ret = libwebsockets_serve_http_bin(context, wsi, bin->data, bin->size, content_type);
		if (ret)
		{
			if (ret < 0)
				lwsl_err("Failed to send HTTP file '%s'\n", (char*)in);
			// ret > 0 indicates already finished
			return -1; // close
		}
		return 0; // further automatic libwebsockets processing needed
	}
	
	case LWS_CALLBACK_HTTP_FILE_COMPLETION:
		break;
	
	case LWS_CALLBACK_FILTER_NETWORK_CONNECTION:
		/* if we returned non-zero from here, we kill the connection */
		break;

	case LWS_CALLBACK_ADD_POLL_FD:
		if (gate_add_pollfd(fd, (int)(long)len) != 0)
			return -1;
		break;

	case LWS_CALLBACK_DEL_POLL_FD:
		gate_del_pollfd(fd);
		break;

	case LWS_CALLBACK_SET_MODE_POLL_FD:
		pollfds[fd_lookup[fd]].events |= (int)(long)len;
		break;

	case LWS_CALLBACK_CLEAR_MODE_POLL_FD:
		pollfds[fd_lookup[fd]].events &= ~(int)(long)len;
		break;

	default:
		print_unhandled_reason("cb_http", reason);
		break;

	}
	
	return 0;
}

static int callback_gate (
		struct libwebsocket_context * context,
		struct libwebsocket *wsi,
		enum libwebsocket_callback_reasons reason, void *user,
							   void *in, size_t len)
{
	(void)user;
	(void)context;
	
	switch (reason)
	{
	case LWS_CALLBACK_SERVER_WRITEABLE:
	{
		char* to_send = fifo_getdel(ws_output);
		if (to_send)
		{
			int offset;
			unsigned int len = strlen(to_send);

			if (len > (typeof(len))0xffff)
			{
				lwsl_err("line too long for websocket (%l > %l) in wsserver.c:\n('%s')\n", len, (typeof(len))0xffff, to_send);
				exit(1);
			}

			if (output_buffer_lws.alloked < 4 + len + LWS_SEND_BUFFER_PRE_PADDING + LWS_SEND_BUFFER_POST_PADDING)
				gate_str_grow(&output_buffer_lws, len + LWS_SEND_BUFFER_PRE_PADDING + LWS_SEND_BUFFER_POST_PADDING);

			output_buffer_lws.str[LWS_SEND_BUFFER_PRE_PADDING + 0] = 0x81; // text frame(0x01), (first and) last frame(0x80)
			if (len < 126)
			{
				output_buffer_lws.str[LWS_SEND_BUFFER_PRE_PADDING + 1] = len;
				offset = 2;
			}
			else
			{
				output_buffer_lws.str[LWS_SEND_BUFFER_PRE_PADDING + 1] = 126; // size is in the next 2 bytes (network byte order)
				output_buffer_lws.str[LWS_SEND_BUFFER_PRE_PADDING + 2] = len >> 8;
				output_buffer_lws.str[LWS_SEND_BUFFER_PRE_PADDING + 3] = len & 0xff;
				offset = 4;
			}

			memcpy((char*)&output_buffer_lws.str[LWS_SEND_BUFFER_PRE_PADDING + offset], to_send, len);
			free_e(&to_send);

			libwebsocket_write(wsi, (unsigned char*)&output_buffer_lws.str[LWS_SEND_BUFFER_PRE_PADDING], len + offset, LWS_WRITE_HTTP);
		}
		break;
	}

	case LWS_CALLBACK_RECEIVE:
	{
		// assume in[len]==0
		assert(!((char*)in)[len]);

		fifo_add(ws_input, (char*)in);

		break;
	}

	default:
		print_unhandled_reason("cb_gate", reason);
		break;
	}

	return 0;
}

void gate_set_port (int port)
{
	info.port = port;
}

void gate_set_interface	(const char* interface)
{
	info.iface = interface;
}

int gate_init (const char* protocol_name)
{
	context = NULL;
	memset(&info, 0, sizeof info);
	info.port = GATE_DEFAULT_PORT_WS;
	info.iface = NULL;
	info.protocols = protocols;

	protocols[protocol_http].name = "http-only";
	protocols[protocol_http].callback = callback_http;
	protocols[protocol_http].per_session_data_size = sizeof(struct per_session_data_empty);
	protocols[protocol_http].rx_buffer_size = 0;

	protocols[protocol_gate].name = protocol_name? protocol_name: GATE_WS_PROTOCOL;
	protocols[protocol_gate].callback = callback_gate;
	protocols[protocol_gate].per_session_data_size = sizeof(struct per_session_data_empty);
	protocols[protocol_gate].rx_buffer_size = 0;

	protocols[protocol_endoflist].callback = NULL;

	count_pollfds = 0;

	max_poll_elements = getdtablesize();
	pollfds = malloc_e(max_poll_elements * sizeof(struct pollfd), "pollfds");
	fd_lookup = malloc_e(max_poll_elements * sizeof(int), "fdlookups");

	ws_output = fifo_create();
	ws_input = fifo_create();
	
	gate_str_init(&psend_line);
	
	gate_serve_external_file = NULL;

	lws_set_log_level(0, NULL);

	return 0;
}


struct pollfd* gate_get_pollfd (int fd)
{
	return &pollfds[fd_lookup[fd]];
}

const char* gate_send (const char* output)
{
	fifo_add(ws_output, output);
	return output;
}

int gate_start (void)
{
	context = libwebsocket_create_context(&info);
	if (!context)
	{
		lwsl_err("libwebsocket init failed\n");
		return -1;
	}
	
	atexit(gate_stop);

	return 0;
}

// returns <0 if error, 0 if timeouted, >0 if user's fds have to be checked
int gate_poll_ms (int timeout_ms)
{
	int i, ret, wsret;
			
	do
	{
		if (!fifo_is_empty(ws_output))
			libwebsocket_callback_on_writable_all_protocol(&protocols[protocol_gate]);

		lwsl_notice("call poll(%ifds, timeout=%ims)\n", count_pollfds, timeout_ms);

		ret = wsret = poll(pollfds, count_pollfds, timeout_ms);

		lwsl_notice("poll ret = %i\n", ret);

		if (ret < 0)
		{
			perror(GATE_PERROR_HEADER "poll");
			return ret;
		}

		if (ret)
			for (i = 0; i < count_pollfds; i++)
				if (pollfds[i].revents)
				{
					lwsl_notice("poll: fd=%i POLLIN=%i POLLOUT=%i\n", pollfds[i].fd, !!(pollfds[i].revents & POLLIN), !!(pollfds[i].revents & POLLOUT));
					if (libwebsocket_service_fd(context, &pollfds[i]) < 0)
						return -1;
					if (!pollfds[i].revents)
						ret--;
				}

		// loop if
		// - timeout infinite and (no error, nothing to read)
		// - timeout not infinite and websocket internals but (no error, nothing to read)
				
	} while (((timeout_ms < 0 || wsret > 0) && ret == 0) && fifo_is_empty(ws_input));

	for (i = 0; i < count_pollfds; i++)
		if (pollfds[i].revents)
			lwsl_notice("finally poll: fd=%i POLLIN=%i POLLOUT=%i\n", pollfds[i].fd, !!(pollfds[i].revents & POLLIN), !!(pollfds[i].revents & POLLOUT));

	return ret;
}

char* gate_receive (void)
{
	return fifo_getdel(ws_input);
}

const char* gate_psend (const char* s, ...)
{
	va_list ap;
	int too_short = 0;
	
	do
	{
		va_start(ap, s);

		psend_line.str[psend_line.alloked - 1] = 255;
		vsnprintf(psend_line.str, psend_line.alloked, s, ap);
		too_short = psend_line.str[psend_line.alloked - 1] == 0;

		va_end(ap);
		
		if (too_short)
			gate_str_grow(&psend_line, 1);

	} while (too_short);

	return gate_send(psend_line.str);
}

int gate_talking (const char* recv)
{
	return strncmp(recv, GATE_TALKING, sizeof(GATE_TALKING) - 1)? -1: 0;
}

int gate_talking_protocol (const char* recv)
{
	return strncmp(recv, GATE_TALKING_PROTOCOL, sizeof(GATE_TALKING_PROTOCOL) - 1)? -1: 0;
}

void gate_stop (void)
{
	lwsl_notice("cleaning ws...\n");
	
	if (context)
		libwebsocket_context_destroy(context);
	context = NULL;
	fifo_destroy(&ws_output);
	fifo_destroy(&ws_input);
	free_e(&pollfds);
	free_e(&fd_lookup);
	gate_str_free(&psend_line);
	gate_str_free(&output_buffer_lws);
}

void gate_lws_setloglevel (int loglevel)
{
	lws_set_log_level(loglevel, NULL);
}
