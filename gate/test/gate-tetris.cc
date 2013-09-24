
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


 
#include <string>
#include <sstream>
#include <iostream>
#include <list>
#include <queue>

#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include <libgate.h>

using namespace std;


#define SX 8
#define SY 15

#define TX (100.0/(SX))
#define TY (100.0/(SY))

// milliseconds
#define DELAY_SLOW	500
#define DELAY_GREYING	50


int delay;

class dot_c
{
public:
	string name;
	string color;
	int x, y;
	bool busy;
	bool current;
	
	dot_c(): x(-1), y(-1), busy(false), current(false) { }
};

dot_c tetris [SX][SY];

void send (const char* s)
{
	fprintf(stderr, "-> %s\n", gate_send(s));
}

void send (const string& s)
{
	send(s.c_str());
}

void create_dot (int x, int y)
{
	dot_c& t = tetris[x][y];
	if (t.busy)
		fprintf(stderr, "-> %s\n",
		gate_psend("%s add tetris button bg %s w %g h %g x %g y %g gap 0 disable",
			   t.name.c_str(),
			   t.color.c_str(),
			   TX, TY,
			   (t.x + 0.5) * TX, (t.y + 0.5) * TY));
}

void recreate ()
{
	fprintf(stderr, "-> %s\n",
	gate_psend("game clear;"
		   "game add root panel title Tetris;"
		   "whole add game panel x 50 w %imm h %imm above nothing keepratio;"
		   "left add whole button below nothing w 25 h 10mm leftof nothing bg orange text '<' gap 0;"
		   "turn add whole button below nothing w 25 h 10mm rightof left bg yellow text O gap 0;"
		   "down add whole button below nothing w 25 h 10mm rightof turn bg #640 text V gap 0;"
		   "right add whole button below nothing h 10mm rightof down bg orange text '>' gap 0;"
		   "decorator add whole decorator above left;"
		   "tetris add decorator panel focus;",
		   SX * 7, SY * 7
		  ));

	for (int x = 0; x < SX; x++)
		for (int y = 0; y < SY; y++)
			create_dot(x, y);

	send("refresh");
}

const char* color ()
{
	const char* colors [] =
	{
		"#f00",
		"#0f0",
		"#00f",
		"#ff0",
		"#f0f",
		"#0ff",
	};
	return colors[random() % (sizeof colors / sizeof colors[0])];
}

typedef list<dot_c*> object;
int idx = 0;


enum x
{a,b,c};

const struct
{
	const char* name;
	int len;
} objects [] =
{
	{ "bar", 4 },
	{ "L", 4 },
	{ "T", 4 },
	{ "square", 4 },
	{ "V", 3 },
};

object* create_random ()
{
	object* o = new object;
	const char* col = color();
	int type = random() % (sizeof(objects) / sizeof(objects[0]));

	int x = SX / 2;
	for (int i = 0; i < objects[type].len; i++)
	{
		int dx = 0;
		int dy = 0;
		string name;
		
		name = objects[type].name;

		switch (type)
		{
		case 0: // bar
			dx = i - 2;
			break;
		
		case 1: // L
			switch (i)
			{
			case 0: dx = -1; dy = 1; break;
			case 1: dx = -1; dy = 0; break;
			case 2: dx = 0; dy = 0; break;
			case 3: dx = 1; dy = 0; break;
			}
			break;

		case 2: // T
			switch (i)
			{
			case 0: dx = -1; dy = 0; break;
			case 1: dx = 0; dy = 0; break;
			case 2: dx = 0; dy = 1; break;
			case 3: dx = 1; dy = 0; break;
			}
			break;

		case 3: // square
			switch (i)
			{
			case 0: dx = -1; dy = 0; break;
			case 1: dx = -1; dy = 1; break;
			case 2: dx = 0; dy = 0; break;
			case 3: dx = 0; dy = 1; break;
			}
			break;

		case 4: // V
			switch (i)
			{
			case 0: dx = -1; dy = 0; break;
			case 1: dx = 0; dy = 1; break;
			case 2: dx = 1; dy = 0; break;
			}
			break;
		
		}

		dot_c* d = &tetris[x + dx][dy];

		if (d->busy)
		{
			// XPLODE
			delete o;
			return NULL;
		}
	
		d->busy = true;
		d->current = true;
		d->color = col;
		
		stringstream krotte;
		krotte << name << (++idx);
		
		d->name = krotte.str();
		o->push_back(d);
	}
		
	return o;
}

object* create_object (object* o)
{
	if (o)
	{
		for (object::iterator i = o->begin(); i != o->end(); i++)
			create_dot((*i)->x, (*i)->y);
		send("refresh");
	}
	return o;
}

void drop_object (object** o)
{
	for (object::iterator i = (*o)->begin(); i != (*o)->end(); i++)
		(*i)->current = false;
	delete *o;
	*o = NULL;
}

object* next_object ()
{
	return create_object(create_random());
}

enum move_result_e
{
	MOVE_OK,
	MOVE_WALL,
	MOVE_DOWN,
};

class id_c { public: string name, color; id_c(string n, string c) { name = n; color = c; } };

move_result_e move_object (object* o, int dx, int dy)
{
	// check collision
	for (object::iterator i = o->begin(); i != o->end(); i++)
	{
		const dot_c* d = *i;
		if (d->y + dy >= SY)
			return MOVE_DOWN;

		if (d->x + dx < 0 || d->x + dx >= SX)
			return MOVE_WALL;

		const dot_c* td = &tetris[d->x + dx][d->y + dy];
		if (td->busy && !td->current)
			return dy? MOVE_DOWN: MOVE_WALL;
	}
	
	// move
	list<id_c> ids;
	for (object::iterator i = o->begin(); i != o->end(); i++)
	{
		dot_c* d = *i;
		dot_c* nd = &tetris[d->x + dx][d->y + dy];
		d->busy = false;
		d->current = false;
		ids.push_back(id_c(d->name, d->color));
		*i = nd;
		
		fprintf(stderr, "-> %s\n",
		gate_psend("%s update x %g y %g",
			d->name.c_str(),
			(nd->x + 0.5) * TX,
			(nd->y + 0.5) * TY));
	}

	for (object::iterator i = o->begin(); i != o->end(); i++)
	{
		dot_c* d = *i;
		const id_c& id = ids.front();
		d->busy = d->current = true;
		d->name = id.name;
		d->color = id.color;
		ids.pop_front();
	}
	
	send("refresh");

	return MOVE_OK;
}

move_result_e turn_object (object* o, bool right)
{
	// check collision
	int sign = right? +1: -1;
	
	// middle coordinate
	object::iterator mid = o->begin();
	for (int i = o->size() / 2; i > 0; i--)
		mid++;
	const dot_c* midot = *mid;
	for (object::iterator i = o->begin(); i != o->end(); i++)
	{
		const dot_c* d = *i;
		int nx = midot->x + sign * (midot->y - d->y);
		int ny = midot->y - sign * (midot->x - d->x);
		if (nx < 0 || nx >= SX || ny < 0 || ny >= SY)
			return MOVE_WALL;
		const dot_c* td = &tetris[nx][ny];
		if (td->busy && !td->current)
			return MOVE_WALL;
	}
	
	// move

	list<id_c> ids;
	for (object::iterator i = o->begin(); i != o->end(); i++)
	{
		dot_c* d = *i;
		dot_c* nd = &tetris[midot->x + sign * (midot->y - d->y)][midot->y - sign * (midot->x - d->x)];
		d->busy = false;
		d->current = false;
		ids.push_back(id_c(d->name, d->color));
		*i = nd;
		
		fprintf(stderr, "-> %s\n",
		gate_psend("%s update x %g y %g",
			d->name.c_str(),
			(nd->x + 0.5) * TX,
			(nd->y + 0.5) * TY));
	}

	for (object::iterator i = o->begin(); i != o->end(); i++)
	{
		dot_c* d = *i;
		const id_c& id = ids.front();
		d->busy = d->current = true;
		d->name = id.name;
		d->color = id.color;
		ids.pop_front();
	}
	
	send("refresh");

	return MOVE_OK;
}

int main ()
{
	int port = 1234;

	bool started = false;
	int greying = 0;
	int checking = false;
	int delline = -1;
	
	printf("try http://localhost:%i/?server=localhost:%i\n"
	       "or http://localhost:%i/?run\n",
	       port, port, port);
	
	for (int x = 0; x < SX; x++)
		for (int y = 0; y < SY; y++)
		{
			dot_c* d = &tetris[x][y];
			d->x = x;
			d->y = y;
		}
			
	object* current = NULL;

	gate_init(NULL);
	gate_set_port(port);
	gate_start();
	
	delay = DELAY_SLOW;
	
	while (1)
	{
		gate_poll_ms(delay);
		
		const char* gr = gate_receive();
		
		move_result_e m = MOVE_OK;
		
		if (gr)
		{
			fprintf(stderr, "r: '%s'\n", gr);
			
			if (gate_talking(gr) == 0)
			{
				recreate();
				started = true;
			}
				
			if (current)
			{
				if (strcmp(gr, "'left'") == 0)
					m = move_object(current, -1, 0);
				else if (strcmp(gr, "'right'") == 0)
					m = move_object(current, 1, 0);
				else if (strcmp(gr, "'down'") == 0)
					m = move_object(current, 0, +1);
				else if (strcmp(gr, "'turn'") == 0)
					m = turn_object(current, false);
			}
				
		}
		else
		{
			printf("loop\n");
		
			if (current)
				m = move_object(current, 0, +1);
		}
		
		if (!started)
			continue;
		
		
		switch (m)
		{
		case MOVE_DOWN:
			drop_object(&current);
			checking = true;
			break;
		default:
			break;
		}

		if (delline >= 0)
		{
			// clear line
		 	for (int x = 0; x < SX; x++)
		 	{
				if (tetris[x][delline].busy)
				{
					fprintf(stderr, "-> %s\n",
					gate_psend("%s clear", tetris[x][delline].name.c_str()));
					tetris[x][delline].busy = false;
				}
			}
			// shift above lines
			for (int y = delline; y > 0; y--)
			{
				for (int x = 0; x < SX; x++)
				{
					dot_c* od = &tetris[x][y - 1];
					if (od->busy)
					{
						dot_c* nd = &tetris[x][y];
						nd->name = od->name;
						nd->color = od->color;
						nd->busy = true;
						fprintf(stderr, "-> %s\n",
						gate_psend("%s update y %g",
							nd->name.c_str(),
							(nd->y + 0.5) * TY));

						od->busy = false;
					}
				}
			}
			send("refresh");
			delline = -1;
		}

		if (checking)
		{
			checking = false;
			for (int y = SY - 1; y >= 0; y--)
			{
				int x;
				for (x = 0; x < SX && tetris[x][y].busy; x++);
				if (x == SX)
				{
					for (x = 0; x < SX; x++)
						fprintf(stderr, "-> %s\n",
						gate_psend("%s update bg grey w %g h %g", tetris[x][y].name.c_str(), TX / 2, TY / 2));
					send("refresh");
					delline = y;
					checking = true;
					break;
				}
			}
		}
		else
		{
			if (current == NULL)
				current = next_object();
		
			if (greying == 0 && current == NULL)
			{
				// end of game
				greying = SY;
				delay = DELAY_GREYING;
			}
		}
		
		if (greying)
		{
			--greying;
			for (int x = 0; x < SX; x++)
				if (tetris[x][greying].busy)
					fprintf(stderr, "-> %s\n",
					gate_psend("%s update bg gray", tetris[x][greying].name.c_str()));
			if (greying != SY - 1)
				for (int x = 0; x < SX; x++)
					if (tetris[x][greying + 1].busy)
						fprintf(stderr, "-> %s\n",
						gate_psend("%s update w %g h %g", tetris[x][greying + 1].name.c_str(), TX / 2, TY / 2));
			send("redraw");
			fprintf(stderr,"greying=%i\n",greying);
			
			if (greying == 0)
			{
				delay = DELAY_SLOW;
				for (int x = 0; x < SX; x++)
					for (int y = 0; y < SY; y++)
					{
						dot_c* d = &tetris[x][y];
						d->busy = d->current = false;
					}
				fprintf(stderr, "-> %s\n",				                                                
				gate_psend("tetris clear;tetris add decorator panel;refresh"));
			}
		}
	}
	
	gate_stop();
	
	return 0;
}
