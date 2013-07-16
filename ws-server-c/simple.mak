
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
CFLAGS	+= -Ilibgate -Itools
GWT	= ../ws-client-gwt/war/gate/index.html
ARFLAGS	= rcs
LIBS	= -Llibgate -Wl,-Bstatic -lwebsockets -llockdev -Wl,-Bdynamic -lssl

ifeq ($(shell test -r /usr/include/lockdev.h && echo ok),ok)
CFLAGS	+= -DHAVE_LOCKDEV=1
else
$(warning notice: lockdev is not installed (pkg liblockdev-dev))
endif

###############

help:
	@echo ""
	@echo "build rules: (all = $(ALL))"
	@grep "^[a-z/]*:" Makefile | sed -e "s,^,\t,g" -e "s,:.*,,g"
	@echo ""

gate/gate: gate/gate.o gate/gatetcp.o gate/gatefd.o gate/gateser.o libgate/libgate.a
test/tetris: test/tetris.o libgate/libgate.a
test/fifo-test: test/fifo-test.o libgate/fifo.o libgate/malloc_e.o
libgate/wsserver.c: tools/data.h

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
	
cleandata:
	rm -f tools/data.[ch]

distclean mrproper: clean cleandata
	rm -f {*/,}*~
	#cd $(WS); ./CLEAN
	rm -rf libwebsockets-org

-include */*.d

###### gwt

$(GWT): $(shell echo ../ws-client-gwt/src/fr/laas/gate/*.java)
	cd ../ws-client-gwt; ant build
