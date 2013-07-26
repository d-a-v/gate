./autoclean.sh && ./autogen.sh && make -f debian/rules source && debuild "$@"
