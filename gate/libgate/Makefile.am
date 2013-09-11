ACLOCAL_AMFLAGS = -I m4

bin_PROGRAMS = gate-zrawc
gate_zrawc_SOURCES = gate-zrawc.c
gate_zrawc_LDADD = -lz

webgwt.c webgwt.h: $(shell echo ../../gwt/src/fr/laas/gate/*.java)
	$(MAKE) -f $(MAKEFILE_LIST) gate-zrawc
	cd ../../gwt; ant build
	cd ../../gwt/war/gate && wget http://www.laas.fr/favicon.ico; true
	here=$(shell pwd); cd ../../gwt/war/gate && $${here}/gate-zrawc $${here}/webgwt bin `find -type f`

lib_LTLIBRARIES = libgate.la
# (dirty) /usr/include/libgate.h instead of /usr/include/gate/libgate.h
gatedir=$(pkgincludedir)/..
gate_HEADERS=libgate.h

libgate_la_SOURCES = fifo.c fifo.h gatestr.c make-cb-list.h malloc_e.c malloc_e.h wsserver.c webgwt.h webgwt.c
BUILT_SOURCES = webgwt.h webgwt.c
#no nodist_libgate_la_SOURCES = webgwt.h webgwt.c
#no CLEANFILES = webgwt.h webgwt.c

pkgconfigdir = $(libdir)/pkgconfig
pkgconfig_DATA = libgate.pc
