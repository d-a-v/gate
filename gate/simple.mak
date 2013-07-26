
GATE	= gate/gate
LIB	= libgate/libgate.a
TEST	= test/fifo-test test/tetris

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
GWT	= ../gwt/war/gate/index.html
ARFLAGS	= rcs
LIBS	= -Llibgate -Wl,-Bstatic -lwebsockets -llockdev -Wl,-Bdynamic -lssl


ifeq ($(shell test -r /usr/include/lockdev.h && echo ok),ok)
$(shell mkdir -p temp && echo "#define HAVE_LIBLOCKDEV 1" > temp/config.h)
else
$(shell mkdir -p temp && echo "#undef HAVE_LIBLOCKDEV" > temp/config.h)
$(warning notice: lockdev is not installed (pkg liblockdev-dev))
endif

###############

help:
	@echo ""
	@echo "build rules: (all = $(ALL))"
	@grep "^[a-z/]*:" simple.mak | sed -e "s,^,\t,g" -e "s,:.*,,g"
	@echo ""

gate/gate: gate/gate.o gate/gatetcp.o gate/gatefd.o gate/gateser.o libgate/libgate.a
test/tetris: test/tetris.o libgate/libgate.a
test/fifo-test: test/fifo-test.o libgate/fifo.o libgate/malloc_e.o
libgate/wsserver.c: libgate/webgwt.h

libgate/libgate.a: libgate/wsserver.o libgate/fifo.o libgate/malloc_e.o libgate/gatestr.o libgate/webgwt.o
	$(AR) $(ARFLAGS) $@ $^
	
$(GWT): $(shell echo ../gwt/src/fr/laas/gate/*.java)
	cd ../gwt; ant build

../gwt/war/gate/favicon.ico:
	cd ../gwt/war/gate && wget http://www.laas.fr/favicon.ico

libgate/webgwt.h libgate/webgwt.c: ../gwt/war/gate/index.html ../gwt/war/gate/favicon.ico
	cd libgate; here=`pwd`; cd ../../gwt/war/gate && libwebsockets-rawc $${here}/webgwt bin `find -type f` favicon.ico

.SECONDARY: $(BIN:%=%.o)

.SUFFIXES:

%.o: %.c
	$(CC) $(CFLAGS) -c $< -MD -MF $(@:%.o=%.d) -o $@

%.o: %.cc
	$(CXX) $(CXXFLAGS) $(CFLAGS) -c $< -MD -MF $(@:%.o=%.d) -o $@

%: %.o
	$(CXX) $^ -o $@ $(LDFLAGS) $(LIBS)

clean:
	rm -f $(BIN) */*.o */*.d libgate/webgwt.[ch]
	rm -rf temp

-include */*.d
