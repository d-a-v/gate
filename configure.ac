#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.

AC_PREREQ([2.68])
AC_INIT([gate], [0.9.20130715], [gauchard@laas.fr])

AM_INIT_AUTOMAKE([-Wall -Werror foreign])

AC_CONFIG_MACRO_DIR([m4])
AC_CONFIG_SRCDIR([gate/libgate])
AC_CONFIG_SRCDIR([gate/gate])
AC_CONFIG_SRCDIR([gate/test])
AC_CONFIG_HEADERS([gate/config.h])

LT_INIT(disable-shared)

# Checks for programs.
AC_PROG_CXX
AC_PROG_CC

# Checks for libraries.
AC_CHECK_LIB([lockdev], [dev_lock])
AC_CHECK_LIB([ssl], [SSL_set_fd])
AC_CHECK_LIB([websockets], [libwebsocket_create_context])

# Checks for tools
AC_CHECK_PROG(RAWC_CHECK,libwebsockets-rawc,yes)
if test x"$RAWC_CHECK" != x"yes" ; then
	AC_MSG_ERROR([Please install libwebsockets-embed-dev before building.])
fi
AC_CHECK_PROG(WGET_CHECK,wget,yes)
if test x"$WGET_CHECK" != x"yes" ; then
	AC_MSG_ERROR([Please install wget before building.])
fi

# Checks for header files.
AC_CHECK_HEADERS([fcntl.h netinet/in.h stdlib.h string.h sys/file.h sys/socket.h termios.h unistd.h])

# Checks for typedefs, structures, and compiler characteristics.
AC_HEADER_STDBOOL
AC_TYPE_OFF_T
AC_TYPE_SIZE_T
AC_TYPE_SSIZE_T

# Checks for library functions.
AC_FUNC_MALLOC
AC_FUNC_REALLOC
AC_CHECK_FUNCS([atexit memmove memset socket strerror strncasecmp])

if test -n "$GCC" -o -n "$GXX"; then
AC_MSG_CHECKING([whether to disable -Wall -Wextra -Werror])
AC_ARG_ENABLE(angrygcc,
		AC_HELP_STRING(
			[--disable-angrygcc],
			[disable -Wall -Wextra -Werror if using gcc/g++]),
		AC_MSG_RESULT(yes),
		[
		  if test -n "$GCC" -o -n "$GXX"; then
			AC_MSG_RESULT(adding -Wall -Wextra -Werror)
		  fi
		  if test -n "$GCC"; then
			CFLAGS="$CFLAGS -Wall -Wextra -Werror"
		  fi
		  if test -n "$GXX"; then
			CPPFLAGS="$CPPFLAGS -Wall -Wextra -Werror"
		  fi
		]
	     )
fi

# we want static linking
#LDFLAGS="$LDFLAGS -all-static"

AC_CONFIG_FILES([Makefile gate/Makefile gate/libgate/Makefile gate/libgate/libgate.pc gate/gate/Makefile gate/test/Makefile])
AC_OUTPUT
