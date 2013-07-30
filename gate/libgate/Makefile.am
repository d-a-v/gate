ACLOCAL_AMFLAGS = -I m4

webgwt.h webgwt.c: $(shell echo ../../gwt/src/fr/laas/gate/*.java)
	cd ../../gwt; ant build
	cd ../../gwt/war/gate && wget http://www.cnrs.fr/favicon.ico; true
	here=$(shell pwd); cd ../../gwt/war/gate && libwebsockets-rawc $${here}/webgwt bin `find -type f`

lib_LTLIBRARIES = libgate.la
# (dirty) /usr/include/libgate.h instead of /usr/include/gate/libgate.h
gatedir=$(pkgincludedir)/..
gate_HEADERS=libgate.h

libgate_la_SOURCES = fifo.c fifo.h gatestr.c make-cb-list.h malloc_e.c malloc_e.h wsserver.c webgwt.h webgwt.c
#no nodist_libgate_la_SOURCES = webgwt.h webgwt.c
BUILT_SOURCES = webgwt.h webgwt.c
#no CLEANFILES = webgwt.h webgwt.c

pkgconfigdir = $(libdir)/pkgconfig
pkgconfig_DATA = libgate.pc
