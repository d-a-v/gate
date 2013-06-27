
GATE	= gate/gate
LIB	= libgate/libgate.a
TEST	= test/fifo-test test/tetris
RAWC	= tools/rawc

BIN	= $(GATE) $(LIB) $(TEST) $(RAWC)
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
CFLAGS	+= -I$(WS)/lib -Ilibgate -Itools
WS	= libwebsockets
LIBWS	= $(WS)/lib/$(WS).a
WSH	= $(WS)/lib/$(WS).h
GWT	= ../ws-client-gwt/war/gate/index.html
ARFLAGS	= rcs
LIBS	= -Llibgate -lgate $(WS)/lib/$(WS).a -Wl,-Bstatic -llockdev -Wl,-Bdynamic -lssl

###############


help:
	@echo ""
	@echo "build rules: (all = $(ALL))"
	@grep "^[a-z/]*:" Makefile | sed -e "s,^,\t,g" -e "s,:.*,,g"
	@echo ""

gate/gate: gate/gate.o gate/gatetcp.o gate/gatefd.o gate/gateser.o libgate/libgate.a $(LIBWS)
test/tetris: test/tetris.o libgate/libgate.a
libgate/wsserver.c: $(WSH) tools/data.h

libgate/libgate.a: libgate/wsserver.o libgate/fifo.o libgate/malloc_e.o libgate/gatestr.o tools/data.o
	$(AR) $(ARFLAGS) $@ $^
	
tools/data.h tools/data.c: $(GWT) $(RAWC)
	(cd tools; ./binifier)

# special comp rule for rawc which has no depends
tools/rawc: tools/rawc.o
	$(CC) $^ -o $@ $(LDFLAGS)
	
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
	cd $(WS); make clean; true
	
cleandata:
	rm -f tools/data.[ch]

distclean mrproper: clean cleandata
	rm -f {*/,}*~
	cd $(WS); ./CLEAN
	rm -rf libwebsockets-org

-include */*.d

###### gwt

$(GWT): $(shell echo ../ws-client-gwt/src/fr/laas/gate/*.java)
	cd ../ws-client-gwt; ant build

###### websockets

$(LIBWS):
	(cd $(WS) && cmake -G "Unix Makefiles" && make)

$(WS)-org/lib/$(WS).h:
	git clone git://git.libwebsockets.org/$(WS) $(WS)-org

comparews: $(WS)-org/lib/$(WS).h
	(cd $(WS)-org; git pull)
	diff -dEbwBur $(WS) $(WS)-org; true
