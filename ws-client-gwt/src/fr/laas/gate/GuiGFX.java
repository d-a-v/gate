
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


package fr.laas.gate;

// http://code.google.com/p/gwt-graphics/wiki/Manual

import java.util.HashMap;
import java.util.Map.Entry;

import org.vaadin.gwtgraphics.client.DrawingArea;
import org.vaadin.gwtgraphics.client.shape.Ellipse;
import org.vaadin.gwtgraphics.client.shape.Path;
import org.vaadin.gwtgraphics.client.shape.Rectangle;
import org.vaadin.gwtgraphics.client.VectorObject;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;


class GuiGFX extends GuiPanel
{	
	private static final int pixelWidth = 2;
	private static final int arrowDivider = 3;
	private static final float arrowAlpha = 0.1f;
	
	protected final class Gfx
	{
		public VectorObject gfx;
		public float x1, y1, x2, y2;
		public int attr;
		
		public Gfx (final VectorObject gfx, final float x1, final float y1, final float x2, final float y2, final int attr)
		{
			this.gfx = gfx;
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
			this.attr = attr;
		}
	}
	
	protected final DrawingArea				area;
	protected final HashMap<String, Gfx>	gfx = new HashMap<String, Gfx>();
	
	public GuiGFX (final IntfObject parent, final String name)
	{
		super(parent, name);
		area = new DrawingArea(0, 0);
		add(area);
	}
		
	public static String help ()
	{
		return
					   "# \tadd <name> <gfxtype*> <args..>\tadd a graphics object"
			+ Gate.endl + "# \tdel <name>\t\t\tdelete the named object"
			+ Gate.endl + "# \tcolor <name> <color*>\t\tchange color"
			+ Gate.endl + "# \tbcolor <name> <color*>\t\tchange border color"
			+ Gate.endl + "# \t\t* gfxtype and args are:   (all coordinates are in %)"
			+ Gate.endl + "# \t\t  - circle    - x y radius"
			+ Gate.endl + "# \t\t  - ellipse   - x y xradius yradius"
			+ Gate.endl + "# \t\t  - rectangle - x1 y1 x2 y2"
			+ Gate.endl + "# \t\t  - line      - x1 y1 x2 y2"
			+ Gate.endl + "# \t\t  - arrow     - x1 y1 x2 y2"
			+ Gate.endl + "# \t\t* color is english/#rgb/#rrggbb like red/#f00/#ff0000"
			;
	}
	
	public boolean update (final Words words) throws WordsException
	{
		Boolean isFirst;
		
		if (words == null)
			return true;
		
		if  (words.checkNextAndForward("del"))
		{
			final String name = words.getString(Gate.cmdlineName);
			if (gfx.remove(name) == null)
				return Gate.getW().error(words, -1, Gate.cmdlineNotFound);
			Gate.getW().uiNeedUpdate(this);
		}
		
		else if (words.checkNextAndForward("add"))
		{
			final String name = words.getString(Gate.cmdlineName);
			Gfx g =  null;
			if (gfx.get(name) != null)
				return Gate.getW().error(words, -1, Gate.errorNameAlreadyExists);
			
			final String type = words.getString(Gate.cmdlineUndefinedShape);
			final float x1 = words.getPosFloat(Gate.cmdlineCenterX);
			final float y1 = words.getPosFloat(Gate.cmdlineCenterY);
			if (type.equals("circle"))
			{
				final float radius = words.getPosFloat(Gate.cmdlineRadius);
				gfx.put(name, g = new Gfx(new Ellipse(0,0,0,0), x1, y1, radius, radius, 0));
			}
			else if (type.equals("ellipse"))
			{
				final float radiusx = words.getPosFloat(Gate.cmdlineRadius);
				final float radiusy = words.getPosFloat(Gate.cmdlineRadius);
				gfx.put(name, g = new Gfx(new Ellipse(0,0,0,0), x1, y1, radiusx, radiusy, 0));
			}
			else if ((isFirst = type.equals("line")) || type.equals("arrow"))
			{
				final float nextx = words.getPosFloat(Gate.cmdlineCenterX);
				final float nexty = words.getPosFloat(Gate.cmdlineCenterY);
				gfx.put(name, g = new Gfx(new Path(0,0), x1, y1, nextx, nexty, isFirst? 0: 1));
			}
			else if (type.equals("rectangle"))
			{
				final float nextx = words.getPosFloat(Gate.cmdlineCenterX);
				final float nexty = words.getPosFloat(Gate.cmdlineCenterY);
				gfx.put(name, g = new Gfx(new Rectangle(0, 0, 0, 0), x1, y1, nextx, nexty, 0));
				((Rectangle)g.gfx).setFillOpacity(0);
				((Rectangle)g.gfx).setStrokeOpacity(1);
			}

			if (g != null)
				g.gfx.addClickHandler(new ClickHandler()
				{
					public void onClick (final ClickEvent event)
					{
						Gate.getW().send("'" + getName() + "' '" + name + "'");
					}
				});

			Gate.getW().uiNeedUpdate(this);
		}
		
		else if ((isFirst = words.checkNextAndForward("color")) || words.checkNextAndForward("bcolor"))
		{ 
			final String name = words.getString(Gate.cmdlineName);
			final Gfx g = gfx.get(name);
			if (g == null)
				return Gate.getW().error(words, -1, Gate.cmdlineNotFound);
			final String color = words.getString(Gate.cmdlineColor);
			if (isFirst) // fill color
			{
				if (g.gfx instanceof Ellipse)
					((Ellipse)g.gfx).setFillColor(color);
				else if (g.gfx instanceof Path)
				{
					((Path)g.gfx).setFillColor(color);
					((Path)g.gfx).setStrokeColor(color);					
				}
				else if (g.gfx instanceof Rectangle)
				{
					((Rectangle)g.gfx).setFillColor(color);
					((Rectangle)g.gfx).setFillOpacity(1);
					((Rectangle)g.gfx).setStrokeOpacity(1);

				}
			}
			else // border color
			{
				if (g.gfx instanceof Ellipse)
					((Ellipse)g.gfx).setStrokeColor(color);
				else if (g.gfx instanceof Path)
				{
					((Path)g.gfx).setFillColor(color);
					((Path)g.gfx).setStrokeColor(color);					
				}
				else if (g.gfx instanceof Rectangle)
					((Rectangle)g.gfx).setStrokeColor(color);
			}				
		}
		else
			return super.update(words);
		
		return true;
	}
		
	public boolean redraw ()
	{
		int w, h; 
		
		area.clear();
		area.setWidth(w = getPlace().c(Place.width).getPixel());
		area.setHeight(h = getPlace().c(Place.height).getPixel());
		
		if (w == 0 || h == 0)
			return false;
		
		for (final Entry<String, Gfx> it: gfx.entrySet())
		{
			final Gfx g = it.getValue();
			
			final int x = (int)(g.x1 * w / 100.0);
			final int y = (int)(g.y1 * h / 100.0);
			final int x2 = (int)(g.x2 * w / 100.0);
			final int y2 = (int)(g.y2 * h / 100.0);
			if (g.gfx instanceof Ellipse)
			{
				final Ellipse e = (Ellipse)g.gfx;
				e.setStrokeWidth(pixelWidth);
				e.setX(x);
				e.setY(y);
				e.setRadiusX(x2);
				e.setRadiusY(y2);
				area.add(e);
			}
			else if (g.gfx instanceof Path)
			{
				final Path p = (Path)g.gfx;
				p.setStrokeWidth(pixelWidth);
				while (p.getStepCount() > 0)
					p.removeStep(0);
				if (g.attr == 0) // line
				{
					p.moveTo(x, y);
					p.lineTo(x2, y2);
				}
				else // arrow
				{
					final float a = arrowAlpha;
					final int ax = (int)(a * x + (1.0 - a) * x2);
					final int ay = (int)(a * y + (1.0 - a) * y2);
					final int dirx = (int)(a / arrowDivider * (y2 - y));
					final int diry = (int)(a / arrowDivider * (x - x2));
					p.moveTo(x, y);
					p.lineTo(ax, ay);
					p.lineTo(ax + dirx, ay + diry);
					p.lineTo(x2, y2);
					p.lineTo(ax - dirx, ay - diry);
					p.lineTo(ax, ay);
				}
				area.add(p);
			}
			else if (g.gfx instanceof Rectangle)
			{
				final Rectangle r = (Rectangle)g.gfx;
				r.setStrokeWidth(pixelWidth);
				r.setX(x);
				r.setY(y);
				r.setWidth(x2 - x);
				r.setHeight(y2 - y);
				area.add(r);
			}
			//XXX else bad bad bad ??				
		}
		
		return true;
	}

	public void	setSonTitle (final IntfObject son, final String title)
	{
		// this widget do not have son or title
	}
	
	public boolean addSon (final IntfObject son, final String name)
	{
		// this widget is not a container
		return false;
	}	
	
	public boolean setSonPosition (final IntfObject son)
	{
		return true;
	}
	
} // class GuiGFX
