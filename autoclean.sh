make clean distclean 2> /dev/null
rm -rf \
	*~ */*~ */*/*~ \
	m4 aclocal.m4 autom4te.cache config \
	config config.* configure \
	install* \
	libtool ltmain.sh \
	`find . -name Makefile.in -o -name Makefile` \
	gate/config.h.in \
	missing depcomp stamp-h1 \
	gate/libgate/webgwt.[ch] \
	gate/libgate/*.a \
	gwt/.project \
	gwt/gwt-unitCache \
	gwt/war/WEB-INF \
	bin-gate bin-tetris \
	.classpath .project
 