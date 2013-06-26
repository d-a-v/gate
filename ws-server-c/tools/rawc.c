
/**************************************************************
 *
 * - CeCILL-B license
 * - (bsd-like, check http://www.cecill.info/faq.en.html#bsd)
 * 
 * Copyright CNRS
 * Contributors:
 * David Gauchard <gauchard@laas.fr>	2013-01-01
 * 
 * This software is a computer program whose purpose is to
 * provide a "Graphical Access To Exterior" (GATE).  The goal
 * is to provide a generic GUI, within a javascript web
 * browser, through a TCP network using the websocket protocol. 
 * Plain text protocol (simple human readable graphic commands)
 * translators to websockets protocol are also provided to
 * connect user applications to the browser via a C library or
 * simple TCP server.
 * 
 * This software is governed by the CeCILL-B license under
 * French law and abiding by the rules of distribution of free
 * software.  You can use, modify and/ or redistribute the
 * software under the terms of the CeCILL-B license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * 
 * As a counterpart to the access to the source code and rights
 * to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the
 * software's author, the holder of the economic rights, and
 * the successive licensors have only limited liability.
 * 
 * In this respect, the user's attention is drawn to the risks
 * associated with loading, using, modifying and/or developing
 * or reproducing the software by the user in light of its
 * specific status of free software, that may mean that it is
 * complicated to manipulate, and that also therefore means
 * that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge.  Users are
 * therefore encouraged to load and test the software's
 * suitability as regards their requirements in conditions
 * enabling the security of their systems and/or data to be
 * ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 * 
 * The fact that you are presently reading this means that you
 * have had knowledge of the CeCILL-B license and that you
 * accept its terms.
 * 
 *************************************************************/



#include "stdio.h"
#include "stdlib.h"
#include "string.h"
#include "ctype.h"
#include "errno.h"
#include "sys/stat.h"

#define LEN	1024
#define BIGLEN	32768

#if defined(__CYGWIN32) || defined(_WIN32)
#define snprintf(a,b,c...) sprintf(a,c)
#endif

void help (char* name);
void build_defname (char* defname, char* filename);
void build_defnamelen (char* defnamelen, char* filename);
int main (int argc, char* argv[]);

void help (char* name)
{
	printf("Syntax1: %s <file-name> <struct-name> <filename> [<filename>...]\n", name);
	printf("Syntax2: %s <file-name>\n", name);
}

void build_defname (char* defname, char* filename)
{
	int i;
	char c;
	strcpy(defname, filename);
	i = 0;
	do
	{
		switch (c = filename[i])
		{
			case '.':
			case '-':
			case '/':
				c = '_';
				break;
		}
		defname[i++] = c;
	} while (c);
}
		
void build_defnamelen (char* defnamelen, char* filename)
{
	build_defname(defnamelen, filename);
	strcat(defnamelen, "_LEN");
}
		
char fcname[LEN], fhname[LEN], temp[LEN], decl[BIGLEN];

int main (int argc, char* argv[])
{
	FILE* f;
	FILE* fc;
	FILE* fh;
	char* filename;
	char* basehname;
	char* headername;
	off_t size, i;
	char* buf;
	int nf;
	
	if (argc < 2)
	{
		help(argv[0]);
		return 1;
	}
	
	filename = argv[1];
	
	for (basehname = &filename[strlen(filename) - 1]; basehname != filename && *basehname != '/'; basehname--);
	if (basehname != filename)
		basehname++;
	snprintf(fhname, LEN, "%s.h", filename);
	
	/*
	 * fopen failing in update means the file may not exist.
	 * Write file banner in that case.
	 */
	if ((fh = fopen(fhname, "r+")) == NULL)
	{
		if (errno != ENOENT)
		{
			perror(fhname);
			return 1;
		}
		if ((fh = fopen(fhname, "w")) == NULL)
		{
			perror(fhname);
			return 1;
		}
		fprintf(fh, "/* This file is automatically generated */\n"
			    "\n"
			    "#ifndef __BINWARE_H__\n"
			    "#define __BINWARE_H__\n"
			    "\n"
			    "typedef struct\n" "{\n"
			    "\tconst char*\t\tname;\n"
			    "\tunsigned long\t\tsize;\n"
			    "\tconst unsigned char*\tdata;\n"
			    "} binware_s;\n"
			    "\n");
	}
	else if (fseek(fh, 0, SEEK_END) == -1)
	{
		perror(fhname);
		return 1;
	}
	
	if (argc == 2)
	{
		fprintf(fh, "\n#endif // __BINWARE_H__\n\n");
		fclose(fh);
		return 0;
	}
	
	headername = argv[2];
	build_defname(temp, headername);
	fprintf(fh, "extern const binware_s binware_%s [];\n", temp);
	fclose(fh);

	decl[BIGLEN - 1] = 255;
	snprintf(decl, BIGLEN, "const binware_s binware_%s [] =\n{", temp);

	snprintf(fcname, LEN, "%s.c", filename);
	if ((fc = fopen(fcname, "r+")) == NULL)
	{
		if (errno != ENOENT)
		{
			perror(fcname);
			return 1;
		}
		if ((fc = fopen(fcname, "w")) == NULL)
		{
			perror(fcname);
			return 1;
		}
		fprintf(fc, "/* This file is automatically generated */\n"
			    "\n"
			    "#include \"%s.h\"\n"
			    "\n",
			    basehname);
	}
	else if (fseek(fc, 0, SEEK_END) == -1)
	{
		perror(fcname);
		return 1;
	}

	for (nf = 3; nf < argc; nf++)
	{
		int ret;
		int decl_only = 0;
		struct stat st;
		char defname[LEN];
		char defnamelen[LEN];
		char* binname = argv[nf];

#if 0
		/*
		   do not produce binaries if binname contains "./"
		*/
		char* guilty;
		if ((decl_only = !!(guilty = strstr(binname, "./"))))
			memmove(guilty, &guilty[2], strlen(guilty) - 1);
#endif

		fprintf(stderr, "processing '%s'\n", binname);
		build_defname(defname, binname);
		build_defnamelen(defnamelen, binname);

		if (!decl_only)
		{
	
			if (stat(binname, &st) == -1)
			{
				perror(binname);
				return 1;
			}
			size = st.st_size;

			if ((f = fopen(binname, "rb")) == NULL)
			{
				perror(binname);
				return 1;
			}

			if ((buf = (char*)malloc(size)) == NULL)
			{
				perror("malloc");
				fprintf(stderr, "Cannot allocate %i bytes for file %s\n", (int)size, binname);
				return 1;
			}
	
			if ((ret = fread(buf, size, 1, f)) != 1)
			{
				if (ferror(f))
					perror(binname);
				fprintf(stderr, "Could not fully read file %s\n", binname);
				return 1;
			}
	
			fclose(f);
	
			fprintf(fc, "#define %s %i\n", defnamelen, (int)size);
	
			fprintf(fc, "static const unsigned char %s[%s] =\n{\t", defname, defnamelen);
			for (i = 0; i < size; i++)
			{
				fprintf(fc, "0x%02x, ", (unsigned char)buf[i]);
				if ((i != (size - 1)) && !((i+1) % 16))
					fprintf(fc, "\n\t");
			}
			fprintf(fc, "\n};\n\n");
		
			free(buf);
		}
		
		if (snprintf(temp, LEN, "\t{ \"%s\", %s, %s },\n", binname, defnamelen, defname) == LEN)
		{
			fprintf(stderr, "please increase LEN\n");
			return 1;
		}

		decl[BIGLEN - 1] = 255;
		strncat(decl, temp, BIGLEN - 1 - strlen(decl));
		if (!decl[BIGLEN - 1])
		{
			fprintf(stderr, "please increase BIGLEN\n");
			return 1;
		}
	}
	fprintf(fc, "%s", decl);
	fprintf(fc, "\t{ /*NULL*/0L, 0, /*NULL*/0L }\n};\n\n");

	fclose(fc);

	return 0;
}
