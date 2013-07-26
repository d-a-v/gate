ACLOCAL_AMFLAGS = -I m4

../../gwt/war/gate/index.html: $(shell echo ../../gwt/src/fr/laas/gate/*.java)
	cd ../../gwt; ant build

../../gwt/war/gate/favicon.ico:
	cd ../../gwt/war/gate && wget http://www.laas.fr/favicon.ico

webgwt.h webgwt.c: ../../gwt/war/gate/index.html ../../gwt/war/gate/favicon.ico
	here=$(shell pwd); cd ../../gwt/war/gate && libwebsockets-rawc $${here}/webgwt bin `find -type f` favicon.ico

lib_LTLIBRARIES = libgate.la
# (dirty) /usr/include/libgate.h instead of /usr/include/gate/libgate.h
gatedir=$(pkgincludedir)/..
gate_HEADERS=libgate.h

libgate_la_SOURCES = fifo.c fifo.h gatestr.c make-cb-list.h malloc_e.c malloc_e.h wsserver.c
nodist_libgate_la_SOURCES = webgwt.h webgwt.c
BUILT_SOURCES = webgwt.h webgwt.c
CLEANFILES = webgwt.h webgwt.c

pkgconfigdir = $(libdir)/pkgconfig
pkgconfig_DATA = libgate.pc
