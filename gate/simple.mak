
GATE	= gate/gate
LIB	= libgate/libgate.a libgate/gate-zrawc
TEST	= test/fifo-test test/gate-tetris

BIN	= $(GATE) $(LIB) $(TEST)
ALL	= help gate lib test

all: $(ALL)

gate:	$(GATE)
lib:	$(LIB)
test:	$(TEST)

###############

CFLAGS	+= -g
LDFLAGS += -g

###############

CFLAGS	+= -Wall -Wextra -Werror
CFLAGS	+= -Itemp -Ilibgate
ARFLAGS	= rcs
LIBS	= -Llibgate -lwebsockets -lssl

ifeq ($(shell test -r /usr/include/lockdev.h && echo ok),ok)
$(shell mkdir -p temp && echo "#define HAVE_LIBLOCKDEV 1" > temp/config.h)
LIBS	+= -llockdev
else
$(shell mkdir -p temp && echo "/* #undef HAVE_LIBLOCKDEV */" > temp/config.h)
$(warning notice: optional lockdev library is not installed (debpkg liblockdev1-dev))
endif

###############

help:
	@echo ""
	@echo "build rules: (all = $(ALL))"
	@grep "^[a-z/]*:" simple.mak | sed -e "s,^,\t,g" -e "s,:.*,,g"
	@echo ""

gate/gate: gate/gate.o gate/gatetcp.o gate/gatefd.o gate/gateser.o libgate/libgate.a
test/gate-tetris: test/gate-tetris.o libgate/libgate.a
test/fifo-test: test/fifo-test.o libgate/fifo.o libgate/malloc_e.o
libgate/wsserver.c: libgate/webgwt.h

libgate/gate-zrawc: libgate/gate-zrawc.o
	$(CXX) $^ -o $@ $(LDFLAGS) -lz

libgate/libgate.a: libgate/wsserver.o libgate/fifo.o libgate/malloc_e.o libgate/gatestr.o libgate/webgwt.o
	$(AR) $(ARFLAGS) $@ $^
	
libgate/webgwt.c libgate/webgwt.h: $(shell echo ../gwt/src/fr/laas/gate/*.java)
	$(MAKE) -f $(MAKEFILE_LIST) libgate/gate-zrawc
	cd ../gwt; ant build
	cd ../gwt/war/gate && wget http://www.laas.fr/favicon.ico; true
	cd libgate; here=`pwd`; cd ../../gwt/war/gate && $${here}/gate-zrawc $${here}/webgwt bin `find -type f -exec echo {} \;`


.SECONDARY: $(BIN:%=%.o)

.SUFFIXES:

%.o: %.c
	$(CC) $(CFLAGS) -c $< -MD -MF $(@:%.o=%.d) -o $@

%.o: %.cc
	$(CXX) $(CXXFLAGS) $(CFLAGS) -c $< -MD -MF $(@:%.o=%.d) -o $@

%: %.o
	$(CXX) $^ -o $@ $(LDFLAGS) $(LIBS)

clean:
	rm -f $(BIN) */*.o */*.d
	rm -rf temp

-include */*.d
