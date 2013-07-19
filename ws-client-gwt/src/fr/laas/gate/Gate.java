
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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;


public class Gate implements EntryPoint
{
	public static final String		appName = "gate";
	public static final String		protocolVersion = "20130326";
	
	public static final String		endl = "\r\n";
	public static final Style.Unit	TabLayoutPanelUnit = Unit.PX; 
	public static final double		TabLayoutPanelSize = 25;
	
	public static final String		errorNoCommand = "unknown command";
	public static final String		errorBadCommand = "what?";
	public static final String		errorBadAttribute = "unknown attribute";
	public static final String		errorNoMoreArguments = "no more arguments allowed";
	public static final String		errorNameAlreadyExists = "already exists";
	public static final String		errorInvalidType = "object type not supported";
	public static final String		errorInternal = "internal error :(";
	public static final String		errorCannotDoThat = "can't do that";

	public static final String		cmdlineArgument = "argument needed";
	public static final String		cmdlineEmpty = "object name, command or 'help' needed";
	public static final String		cmdlineCommand = "command needed";
	public static final String		cmdlineParent = "parent needed";
	public static final String		cmdlineObjectType = "object type needed";
	public static final String		cmdlineObjectName = "object name needed";
	public static final String		cmdlineWidth = "width needed";
	public static final String		cmdlineHeight = "height needed";
	public static final String		cmdlineCenterX = "center X needed";
	public static final String		cmdlineCenterY = "center Y needed";
	public static final String		cmdlineRadius = "radius needed";
	public static final String		cmdlineCoordinate = "coordinate needed (ex: 123 or 123p)";
	public static final String		cmdlineTitle = "title needed";
	public static final String		cmdlineAlreadySet = "already set";
	public static final String		cmdlineNotFound = "not found";
	public static final String		cmdlineOutOfRange = "out of range";
	public static final String		cmdlinePositive = "must be strictly positive";
	public static final String		cmdlineUndefinedAxis = "undefined X-axis";
	public static final String		cmdlineUndefinedShape = "undefined shape";
	public static final String		cmdlineName = "name needed";
	public static final String		cmdlineText = "text needed";
	public static final String		cmdlineGroup = "group name needed";
	public static final String		cmdlineColor = "color needed";
	
	public static final int			defaultGap = 5;
	public static final int			refreshDelay = 100; // ms
	
	public static final int			ppiMin = 50;
	public static final int			ppiMax = 700;

	private	 static boolean			frozen = false;
	private static Gate						w;
		
	public static float				ppi, ppiV, ppiH;
	public static boolean				horizontal;
	public static float				ppmm;
	
			
	private final HashMap<String, IntfObject>	names = new HashMap<String, IntfObject>();
	private final HashMap<String, IntfHelper>	helpers = new HashMap<String, IntfHelper>();
	private       GuiRoot						guiRoot = null;
	private       IntfObject					uiRefreshNeeded = null;
	
	///////////////////////////////////////////////////////

	public static final Gate getW ()
	{
		return w;
	}
	
	public static final boolean isFrozen ()
	{
		return frozen;
	}
		
	///////////////////////////////////////////////////////

	// this is the simplest way I found for being able to simply connect new classes that you will thankfully create
	public void populateHelpers ()
	{
		helpers.put("panel", new IntfHelper() 
		{
			public IntfObject	starter (final IntfObject p, final String n) { return new GuiPanel(p, n); }
			public String		help	() { return GuiPanel.help(); }
		});
		
		helpers.put("button", new IntfHelper() 
		{
			public IntfObject	starter (final IntfObject p, final String n) { return new GuiButton(p, n); }
			public String		help	() { return GuiButton.help(); }
		});

		helpers.put("radio", new IntfHelper() 
		{
			public IntfObject	starter (final IntfObject p, final String n) { return new GuiRadio(p, n); }
			public String		help	() { return GuiRadio.help(); }
		});

		helpers.put("checkbox", new IntfHelper() 
		{
			public IntfObject	starter (final IntfObject p, final String n) { return new GuiCheckBox(p, n); }
			public String		help	() { return GuiCheckBox.help(); }
		});

		helpers.put("plot", new IntfHelper() 
		{
			public IntfObject	starter (final IntfObject p, final String n) { return GuiPlot.apiLoaded()? new GuiPlot(p, n): null; }
			public String		help	() { return GuiPlot.help(); }
		});

		helpers.put("text", new IntfHelper() 
		{
			public IntfObject	starter (final IntfObject p, final String n) { return new GuiText(p, n); }
			public String		help	() { return GuiText.help(); }
		});

		helpers.put("textzone", new IntfHelper() 
		{
			public IntfObject	starter (final IntfObject p, final String n) { return new GuiTextZone(p, n); }
			public String		help	() { return GuiTextZone.help(); }
		});

		helpers.put("decorator", new IntfHelper() 
		{
			public IntfObject	starter (final IntfObject p, final String n) { return new GuiDecorator(p, n); }
			public String		help	() { return GuiDecorator.help(); }
		});

		helpers.put("gfx", new IntfHelper() 
		{
			public IntfObject	starter (final IntfObject p, final String n) { return new GuiGFX(p, n); }
			public String		help	() { return GuiGFX.help(); }
		});

		helpers.put("list", new IntfHelper() 
		{
			public IntfObject	starter (final IntfObject p, final String n) { return new GuiList(p, n); }
			public String		help	() { return GuiList.help(); }
		});

		helpers.put("sliderbar", new IntfHelper() 
		{
			public IntfObject	starter (final IntfObject p, final String n) { return new GuiSliderBar(p, n); }
			public String		help	() { return GuiSliderBar.help(); }
		});

		helpers.put("image", new IntfHelper() 
		{
			public IntfObject	starter (final IntfObject p, final String n) { return new GuiImage(p, n); }
			public String		help	() { return GuiImage.help(); }
		});

	}
	
	///////////////////////////////////////////////////////

	float round (float x, int rounder)
	{
		return 1f * (int)(x * rounder) / rounder;
	}
	
	///////////////////////////////////////////////////////

	public static final String chop (final String input)
	{
		if (input == null)
			return null;
		
		int first = 0, last = input.length();
		
		while (first < last)
		{
			final char e = input.charAt(first);
			if (e != 10 && e != 13 && e != 8 && e != 32)
				break;
			first++;
		}
		while (first < last)
		{
			final char e = input.charAt(--last);
			if (e != 10 && e != 13 && e != 8 && e != 32)
				break;
		}
		return input.substring(first, last + 1);
	}

	///////////////////////////////////////////////////////

	public boolean error (final Words words, final int relativePosition, final String error)
	{
		if (words != null)
		{
			int i;
			int size;
			final int idx_error = words.getCurrentIdx() + relativePosition;
			String line;

			line = "# error: ";
			for (i = 0; i < idx_error; i++)
				line += words.showDetachedWord(i) + ' ';
			size = line.length();
			for (; i < words.getLength(); i++)
				line += words.showDetachedWord(i) + ' ';
			send(line);
			line = "#";
			for (i = 1; i < size; i++)
				line += ' ';
			line += "^ ";
			if (error != null && error.length() > 0)
				line += error;
			send(line);
		}
		else
			send("# error: " + error);
		return false;
	}

	///////////////////////////////////////////////////////
	// base parser

	private boolean parse (final String line) throws WordsException
	{
		final Words words = new Words(line);
		final String name = words.getString(cmdlineEmpty);

		if (name.equals("help"))
			return help(words);
		
		if (name.equals("info"))
			return info(words);
		
		if (name.equals("reset"))
		{
			uiRefreshNeeded = null;
			tryMeInvoked = false;
			calibrateInvoked = false;
			
			//names.clear();
			//RootLayoutPanel.get().remove(guiRoot);
			clear(guiRoot);
			guiRoot = null;
			
			createRoot();
			remoteLoginWindowSetup();
			return true;			
		}
		
		if (name.equals("refresh"))
		{
			//debug("refresh from cmdrefresh");
			uiRefresh(uiRefreshNeeded);
			uiRefreshNeeded = null;
			return true;
		}
		
		if (name.equals("redraw"))
		{
			//debug("refresh from cmdredraw");
			uiRefresh(guiRoot.getDisplayedSon());
			uiRefreshNeeded = null;
			return true;
		}
		
		final String command = words.getString(cmdlineCommand);
		final IntfObject obj = names.get(name);

		if (command.equals("add"))
			return obj == null? add(name, words, line): error(words, -2, errorNameAlreadyExists);
		
		if (obj == null)
			return error(words, -2, cmdlineNotFound);

		if (command.equals("update"))
			return userUpdate(obj, words);
		
		if (command.equals("clear"))
		{
			if (obj.getGOParent() == null)
				return error(words, -1, errorCannotDoThat);
			clear(obj);
			return true;
		}
	
		return error(words, -1, errorBadCommand);
	}
	
	private final void parseProtect (final String one)
	{
		try
		{
			final String chopped = chop(one);
			if (chopped.length() > 0)
				parse(chopped);
		}
		catch (final WordsException e)
		{
			error(e.getWords(), 0, e.getMessage());
		}
	}

	private int				waitForAPI = 0;
	private LinkedList<String> 	delayed = new LinkedList<String>();

	private final void parseMulti (final String multi)
	{
		if (multi != null)
			for (final String one: multi.split(";"))
				if (waitForAPI > 0)
					delayed.addLast(one);
				else
					parseProtect(one);
	}
	
	public final void addLocker ()
	{
		++waitForAPI;
	}
	
	public final void gotLocker ()
	{
		if ((--waitForAPI) == 0)
			while (waitForAPI == 0 && delayed.size() > 0)
				parseProtect(delayed.removeFirst());
	}
	
	///////////////////////////////////////////////////////

	void clear (final IntfObject o)
	{
		frozen = true;
		uiNeedUpdate(o.getGOParent());

		clearRec(o);
		
		// clear from parent
		if (o.getGOParent() != null)
			o.getGOParent().getSons().remove(o);

		final Timer timer = new Timer()
		{
			public void run ()
			{
				frozen = false;
				uiRefresh(guiRoot);
			}
		};
		timer.schedule(refreshDelay + 1);
	}

	void clearRec (final IntfObject o)
	{						
		// clear from all place reference
		for (final Entry<String, IntfObject> e: names.entrySet())
			e.getValue().getPlace().relativeHasVanished(o);

		// clear sons
		if (o.getSons() != null)
		{
			for (final IntfObject toClear: o.getSons())
				clearRec(toClear);
			o.getSons().clear();
		}
		
		// clear from global naming
		names.remove(o.getName());
		
		// clear from GUI
		o.getWidget().removeFromParent();
	}
	
	///////////////////////////////////////////////////////
	
	public String infoSize (final IntfObject o)
	{
		final Place p = o.getPlace();
		final Widget w = o.getWidget();
		String s = "# " + o.getName() + "  ui: "
				+ w.getOffsetWidth() + 'x'
				+ w.getOffsetHeight() + '+'
				+ w.getAbsoluteLeft() + '+'
				+ w.getAbsoluteTop() + "  px: "
				+ (int)p.c(Place.width).getPixel() + 'x'
				+ (int)p.c(Place.height).getPixel() + '+'
				+ (int)p.c(Place.x).getPixel() + '+'
				+ (int)p.c(Place.y).getPixel() + "  %: "
				+ (int)(p.c(Place.width).getPercent() * 100.0f) + 'x'
				+ (int)(p.c(Place.height).getPercent() * 100.0f) + '+'
				+ (int)(p.c(Place.x).getPercent() * 100.0f) + '+'
				+ (int)(p.c(Place.y).getPercent() * 100.0f) + "  ";
		for (int i = 0; i < 4; i++)
			if (p.c(i).isPercent())
				s += '%';
			else if (p.c(i).isPixel())
				s += 'p';
			else if (p.c(i).isStuckToBorder())
				s += 'b';
			else
				s += 'r';	
		s += "  gap: " + p.getGap() + "p";
		return s;
	}
	
	public String infoUI (final IntfObject o)
	{
		int px = o.getPlace().c(Place.width).getPixel();
		int py = o.getPlace().c(Place.height).getPixel();
		
		return (px == 0 || py == 0)?
				null:
				o.getName()
					+ " px: " + px + " " + py
					+ " in: " + round(px / ppi, 100) + " " + round(py / ppi, 100)
					+ " mm: " + round(px / ppmm, 10) + " " + round(py / ppmm, 10);
	}
	
	private boolean info (final Words words) throws WordsException
	{
		final String info = words.getString(cmdlineArgument);
		if (info.equals("size"))
			for (final Entry<String, IntfObject> e: names.entrySet())
				send(infoSize(e.getValue()));
		else if (info.equals("ui"))
			send(infoUI(guiRoot));
		else
			return error(words, -1, errorBadAttribute);
	
		return true;
	}
	
	///////////////////////////////////////////////////////

	private boolean help (final Words words) throws WordsException
	{
		if (!words.hasNext())
		{
			commonGeneralHelp();
			
			String types = "# object-types are: ";
			for (final Iterator<String> iterator = helpers.keySet().iterator(); iterator.hasNext(); )
				types += iterator.next() + ' ';
			send(types);
			
			commonHelp();

			return true;
		}
		
		final String type = words.getString("");
		
		final IntfHelper helper = helpers.get(type);
		if (helper == null)
			return error(words, -1, errorInvalidType);

		final String help = helper.help();
		if (help != null)
		{
			send("# object-type '" + type + "' attributes are:");
			send(help);
		}
		
		commonHelp();
		
		return true;
	}
	
	// called by objects
	public void commonHelp ()
	{
		send("# ");
		send("# common attributes are:");
		send("#\tw <w>\t\twidth (*1)");
		send("#\th <h>\t\theight (*1)");
		send("#\tx <x>\t\tX (*1)");
		send("#\ty <y>\t\tY (*1)");
		send("#\tabove <loc>\tplacement above location (*2)");
		send("#\tbelow <loc>\tplacement below location (*2)");
		send("#\trightof <loc>\tplacement right of location (*2)");
		send("#\tleftof <loc>\tplacement left of location (*2)");
		send("#\tfg <color>\tforeground color (*3)");
		send("#\tbg <color>\tbackground color (*3)");
		send("#\ttitle <t>\tupdate title (in parent, if applicable)");
		send("#\tkeepratio\tkeep aspect ratio (*4)");
		send("#\tfreeratio\tfree aspect ratio");
		send("#\tgap <px>\tplain objects border size (*5)");
		send("#\tfocus\t\tswitch visible tab to get focus");
		send("#");
		send("# (*1) 10 is 10% - 10p is 10 pixels - 10mm is 10 millimeters - 10in is 10 inches");
		send("#      for mm and inch units:");
		send("#      . browser calibration is needed");
		send("#      . values are max values - sizes are reduced if parent is too small");
		send("#      . see (*4 keepratio)");
		send("#      see (*2) for default placement");
		send("# (*2) location is an object name, keyword 'border', or keyword 'nothing'");
		send("#      default attachment is 'border'");
		send("# (*3) colors are like: black, orange, lightgray, #rgb and #rrggbb");
		send("# (*4) ratio is kept if both W and H units are %, mm or inch");
		send("# (*5) gap size is in pixel,");
		send("#      default is " + defaultGap + " for plain object, and 0 for containers");
		send("#");
	}
	
	public void commonGeneralHelp ()
	{
		send("#");
		send("# This is " + appName + " - protocol version " + protocolVersion);
		send("#");
		send("# general help:");
		send("#\t'name' add 'parent-name' <object-type> [<attributes>]");
		send("#\t'name' update <attributes>");
		send("#\t'name' clear");
		send("#\tinfo size|ui");
		send("#\treset");
		send("#\trefresh");
		send("#\tredraw");
		send("#\thelp [<object-type>]");
		send("#\t");
		send("#\texample: ouch! add sandbox button x 50 y 50 h 50 w 20mm bg blue text \"hit me!\"");
		send("#\t creates a button named 'ouch!' inside its parent named 'sandbox'");
		send("#\t then redraw needs to be triggered (with command refresh)");
		send("#\t");
	}

	///////////////////////////////////////////////////////
	
	/**
	 * @param words words[0] is the name words[2+] are parameters 
	 */
	private boolean add (final String name, final Words words, final String wholeCommand) throws WordsException
	{
		final String parentName = words.getString(cmdlineParent);
		final IntfObject parent = names.get(parentName);
		if (parent == null)
			return error(words, -1, cmdlineNotFound);
		
		final String type = words.getString(cmdlineObjectType);
		final IntfHelper helper = helpers.get(type);
		if (helper == null)
			return error(words, -1, errorInvalidType);
		
		final IntfObject newObject = helper.starter(parent, name);
		if (newObject == null)
		{
			delayed.addLast(wholeCommand);
			return true;
		}
		
		names.put(name, newObject);
		userUpdate(newObject, words);
		
		return true; 
	}
	
		
	///////////////////////////////////////////////////////
	
	public IntfObject getPrimaryAncestor (IntfObject obj)
	{
		IntfObject leaf = obj;
		IntfObject son = null;
		while (obj.getGOParent() != null)
		{
			son = obj;
			obj = obj.getGOParent();
		}
		if (obj != guiRoot)
		{
			if (obj.getName().equals("root"))
			{
				debug("is object " + leaf.getName() + " dying?");
			}
			else
			{
				alert("internal error in getPrimaryAncestor()");
				debug(son.getName() + "'s parent is " + obj.getName() + " - guiRoot is " + guiRoot.getName());
			}
			return null;
		}
		return son;
	}	
	
	///////////////////////////////////////////////////////

	private boolean needSendInfoUI = false;
	
	public void sendInfoUI ()
	{
		needSendInfoUI = true;
	}
	
	
	// called on external events like resize, or user command 'refresh'/'redraw'
	public void uiRefresh (final IntfObject obj)
	{
		parseMulti("remote_pvalue update text 'ppi=" + ppi + " h=" + horizontal + "'");
		
		//debug("refresh " + (obj==null? "null": obj.getName()));
		if (obj != null && !uiRefreshRec(obj, 0) && !frozen)
		{
			// refresh too soon, try again in a sec
			debug("setting timer for later refresh");

			final Timer timer = new Timer()
			{
				public void run ()
				{
					//debug("refresh from timer ui="+obj.getName());
					uiRefresh(obj);
				}
			};
			timer.schedule(refreshDelay);
		}
		else if (needSendInfoUI)
		{
			String info = infoUI(guiRoot);
			if (info != null)
			{
				send("# " + info);
				needSendInfoUI = false;
			}
		}
	}
	
	public boolean uiRefreshRec (final IntfObject obj, final int sub)
	{
		if (frozen)
			return false;
		
		boolean updated = true;
		
		final List<IntfObject> sons = obj.getSons();

		if (obj.getGOParent() != null)
			if (!obj.getPlace().uiRefreshInsideParent())
				updated = false;
		
		if (!obj.redraw())
			updated = false;
		
		// recursive call on sons
		if (sons != null)
			for (final IntfObject son: sons)
				//debug("rec " + obj.getName() + " -> " + son.getName());
				if (!uiRefreshRec(son, sub + 1))
					updated = false;
		
		return updated;
	}
	
	///////////////////////////////////////////////////////
	// style

	private final static float minf (final float a, final float b)
	{
		return b < a? b: a;
	}

	public boolean setFontSize (final IntfObject obj, final int textLength)
	{
		final float h = obj.getWidget().getOffsetHeight() * 0.66f;
		final float w = obj.getWidget().getOffsetWidth() / -minf(-textLength, -1) * 1.2f;
		if (h == 0 || w == 0)
		{
			if (isInsideVisibleTab(obj))
			{
				//debug("null size in " + obj.getName() + " AND we are visible");
				return false; // then we will called back in a short while
			}
			//debug("null size in " + obj.getName() + " but we are NOT visible");
			return true; // do nothing, we will be called back once tab get visible
		}

		// ???.context2d.measureText()
		obj.getWidget().getElement().getStyle().setFontSize(-minf(-5, -minf(h, w)), Style.Unit.PX);
		
		//debug("font resized in " + obj.getName());

		return true;
	}

	public void setColor (final IntfObject obj, final String color)
	{
		obj.getWidget().getElement().getStyle().setProperty("color", color);
		setBorderColor(obj, color);
	}

	public void setBackgroundColor (final IntfObject obj, final String color)
	{
		obj.getWidget().getElement().getStyle().setProperty("background", color);
	}

	public void setBorderColor (final IntfObject obj, final String color)
	{
		obj.getWidget().getElement().getStyle().setProperty("borderColor", color);
	}
		
	public boolean isInsideVisibleTab (IntfObject obj)
	{
		IntfObject son = getPrimaryAncestor(obj);
		//debug("IsInsideVisibleTab: " + obj.getName() + "'s parent is " + (son==null? "not found": son.getName()));
		if (son == null)
			return false;
		return guiRoot.getWidgetIndex(son.getWidget()) == guiRoot.getSelectedIndex();
	}
	
	///////////////////////////////////////////////////////

	private boolean userUpdatePlace (final IntfObject obj, final Words words, final String userEntry, final String what, final int placeIndex) throws WordsException
	{
		if (words.checkNextAndForward(userEntry))
		{
			final Words.CoordinateElement coordinate = words.getCoordinate(what);
			obj.getPlace().c(placeIndex).setPlace(coordinate.getValue(), coordinate.getType());
			return true;
		}
		return false;
	}
	
	private boolean userUpdateRelativePlace (final IntfObject obj, final Words words, final String userEntry, final String what, final int relativeIndex) throws WordsException
	{
		if (words.checkNextAndForward(userEntry))
		{
			IntfObject relative;
			String relativeName = words.getString(what);
			if (relativeName.equals("border"))
				obj.getPlace().setStickToBorder(relativeIndex, true);
			else if (relativeName.equals("nothing"))
				obj.getPlace().setStickToBorder(relativeIndex, false);
			else if ((relative = names.get(relativeName)) == null)
				throw new WordsException(words.rewind(1), cmdlineObjectName);
			else
				obj.getPlace().setRelative(relativeIndex, relative);
			return true;
		}
		return false;
	}
	
	public void uiNeedUpdate (final IntfObject obj)
	{
		//not now: obj.getPlace().uiRefresh();
		if (uiRefreshNeeded == null)
			uiRefreshNeeded = obj;
		else if (uiRefreshNeeded != obj)
			uiRefreshNeeded = guiRoot;
	}

	public boolean userUpdate (final IntfObject obj, final Words words) throws WordsException
	{
		if (words == null)
			return true;
		
		while (words.hasNext())
			// check object's update() .. or check common update 
			if (!obj.update(words) && !update(obj, words))
				return error(words, 0, Gate.errorBadAttribute);
		
		return true;
	}

	// called back by objects for their common attributes
	public boolean update (final IntfObject obj, final Words words) throws WordsException
	{
		if (words == null || !words.hasNext())
			return true;
		
		// check generic attributes
			
		if (words.checkNextAndForward("title"))
			obj.getGOParent().setSonTitle(obj, words.getString(cmdlineTitle));
		
		else if (words.checkNextAndForward("keepratio"))
		{
			obj.getPlace().setKeepRatio(true);
			uiNeedUpdate(obj);
		}

		else if (words.checkNextAndForward("freeratio"))
		{
			obj.getPlace().setKeepRatio(false);
			uiNeedUpdate(obj);
		}

		else if (userUpdatePlace(obj, words, "w", cmdlineWidth, Place.width)
			   || userUpdatePlace(obj, words, "h", cmdlineHeight, Place.height)
			   || userUpdatePlace(obj, words, "x", cmdlineCenterX, Place.x)
			   || userUpdatePlace(obj, words, "y", cmdlineCenterY, Place.y)
			   || userUpdateRelativePlace(obj, words, "rightof", cmdlineObjectName, Place.rightOf)
			   || userUpdateRelativePlace(obj, words, "leftof", cmdlineObjectName, Place.leftOf)
			   || userUpdateRelativePlace(obj, words, "above", cmdlineObjectName, Place.above)
			   || userUpdateRelativePlace(obj, words, "below", cmdlineObjectName, Place.below))
		{
			uiNeedUpdate(obj);
		}
		
		else if (words.checkNextAndForward("fg"))
			setColor(obj, words.getString(Gate.cmdlineText));

		else if (words.checkNextAndForward("bg"))
			setBackgroundColor(obj, words.getString(Gate.cmdlineText));
		
		else if (words.checkNextAndForward("gap"))
			//XXX string constant
			obj.getPlace().setGap(words.getPosInt("gap size"));

		else if (words.checkNextAndForward("focus"))
			guiRoot.focus(getPrimaryAncestor(obj));

		else
			return false;		

		return true;
	}
	
	///////////////////////////////////////////////////////

	//XXX move me
	private List<WebSocketClient> remoteHistory = new ArrayList<WebSocketClient>();
	private WebSocketClientCallback wscb = new WSCallback();
	private String remoteValue = "";
	private String remotePassword = "";
	private boolean remoteDeleteNow = false;
	private boolean tryMeInvoked = false;
	private boolean calibrateInvoked = false;
	private int gfxDemoX = 1;
	private Storage storage = null;
	
	class WSCallback implements WebSocketClientCallback
    {
        public void onConnect (final WebSocketClient ws)
        {
        	debug("gui connected to server");
        	send("# " + appName + " talking - protocol version " + protocolVersion);
        	parseMulti(ws.getIntfObject().getName() + " update bg green");
		    // change order in history
		    remoteLoginHistoryBackup();
        }
        
        public void onClose (final WebSocketClient ws)
        {
        	//debug("onClose");
        	//alert("Peer has vanished");
        	parseMulti(ws.getIntfObject().getName() + " update bg " + (ws.isLiving()? "orange": "lightgray"));
        	if (ws.isLiving())
        	{
        		debug("arming timer for reconnect to " + ws.getServer());
        		final Timer timer = new Timer()
        		{
        			public void run ()
        			{
        				if (ws.isLiving() && !ws.isConnected())
        				{
            				debug("trying now to reconnect to " + ws.getServer());
        					ws.reconnect();
        				}
        			}
        		};
        		timer.schedule(1000); // 1000 = 1 sec
        	}
        	
		    // change order in history
		    remoteLoginHistoryBackup();
        }
        
        public void onError (final WebSocketClient ws)
        {
        	stopConnection(ws);
        	alert("Connection has crashed (" + ws.getServer() + ")");
        }
        
        public void onMessage (final WebSocketClient ws, final String message)
        {
        	debug("received: '" + message + "'");
        	parseMulti(message);
        }
    }

    void remoteLoginHistoryUpdate ()
    {
        int i;
    	String w = "rh clear;rh add remote_history panel scrollable;";
		
		i = 0;
    	for (WebSocketClient h: remoteHistory)
		{
		    h.setIntfObject(null);
			w += "rhb" + i + " add rh button text '" + h.getServer() + "' h 10mm leftof nothing w 70 gap 2 bg ";
			if (h.isConnected())
				w += "green";
			else if (h.isLiving())
				w += "orange";
			else
				w += "lightgray";
			if (i > 0)
				w += " below rhb" + (i - 1);
			w += ";rhr" + i + " add rh button h 10mm gap 4 leftof nothing w 20 bg red text X fg white rightof rhb" + i;
			if (i > 0)
				w += " below rhb" + (i - 1);
			w += ";";
			i++;
		}
		parseMulti(w);

		i = 0;
    	for (WebSocketClient h: remoteHistory)
		    h.setIntfObject(names.get("rhb" + i++));

    	remoteLoginHistoryBackup();
    }
    
    void removeConnectionItem (WebSocketClient ws, int index)
    {
    	stopConnection(ws);
    	remoteHistory.remove(index);
    }
    
    void removeConnectionItem (int index)
    {
	    removeConnectionItem(remoteHistory.get(index), index);
    }
        
    void addConnectionItem (String h, boolean first)
    {
    	int index = 0;
    	
		if (!h.substring(0, 5).equals("ws://"))
			h = "ws://" + h;

	    for (final WebSocketClient ws: remoteHistory)
	    {
            if (ws.getServer().equals(h))
                removeConnectionItem(ws, index);
            ++index;
	    }
	    
	    if (first)
        	remoteHistory.add(0, new WebSocketClient(wscb, h, appName));
        else
        	remoteHistory.add(new WebSocketClient(wscb, h, appName));
    }

	void remoteLoginWindowSetup ()
	{
		final String init = 
                  "remote_tab add root panel title Connect;"
                + "remote_playground add remote_tab decorator x 50 w 150mm h 100mm above nothing;"
                + "remote_login add remote_playground panel;"
                + "remote_title add remote_login button disable w 60 h 20 above nothing leftof nothing text 'Remote connect' fg white bg black;"
                + "remote_tryme add remote_login button text 'try me !' h 20 w 20 above nothing rightof remote_title above nothing fg white bg lightgrey;"
                + "remote_cal add remote_login button text 'cal' h 20 above nothing rightof remote_tryme above nothing fg white bg red;"
                + "remote_value add remote_login text w 70 h 18 above nothing rightof nothing below remote_title text '" + remoteValue + "';"
                + "remote_text add remote_login button h 20 above nothing leftof remote_value below remote_title text host:port disable;"
                + "remote_pvalue add remote_login text w 70 h 18 above nothing rightof nothing below remote_text;"
                + "remote_ptext add remote_login button h 20 above nothing leftof remote_pvalue below remote_text text password disable;"
                + "remote_history add remote_login decorator below remote_ptext;"
                + "rh add remote_history panel scrollable;"
                + "refresh;"
                ;
		
		parseMulti(init);

		remoteHistory.clear();
		if (storage != null)
		{
			for (final String h: storage.getItem("Whistory").split(" "))
				addConnectionItem(h, false);
			remoteLoginHistoryUpdate();
		}
	}
	
	private void remoteLoginHistoryBackup ()
	{
		String value = new String();
		for (final WebSocketClient h: remoteHistory)
			if (h.isConnected())
				value += h.getServer() + " ";
		for (final WebSocketClient h: remoteHistory)
			if (!h.isConnected() && h.isLiving())
				value += h.getServer() + " ";
		for (final WebSocketClient h: remoteHistory)
			if (!h.isConnected() && !h.isLiving())
				value += h.getServer() + " ";
		storage.setItem("Whistory", value);
	}
	
	private void sandboxUpdateLog (final String newText)
	{
		GuiTextZone z = (GuiTextZone)names.get("remote_tryme_log");
		if (z != null)
			z.moreText(newText + "\n");
	}
	
	private void logAndExecute (String demo)
	{
		sandboxUpdateLog(demo.replace(';', '\n'));
		parseMulti(demo);
	}
	
	private void logAction (Words w, boolean action)
	{
		logAction(w.toString(), action);
	}
	
	private void logAction (String s, boolean action)
	{
		sandboxUpdateLog("received: " + s.replace("\n",  "\nreceived: ") + (action? " (demo action follows)": ""));
	}
	
	private void restartConnection (WebSocketClient h)
	{
	    if (!h.isLiving())
	    {
    		parseMulti(h.getIntfObject().getName() + " update bg orange");
    		h.reconnect();
	    }
	}
	
	private void stopConnection (WebSocketClient h)
	{
		if (h.isLiving())
		{
			parseMulti(h.getIntfObject().getName() + " update bg lightgray");
	    	h.close();
	    }
	}
	
	private void newConnection (String h)
	{
		if (h != null) //XXX check syntax, empty string etc
		{
			addConnectionItem(h, true);
			remoteLoginHistoryUpdate();
		    restartConnection(remoteHistory.get(0));
		}
	}
	
	private void removeConnection (int index)
	{
		removeConnectionItem(index);
    	remoteLoginHistoryUpdate();
	}
	
	/////////////////////////////////////////////////////////

	private boolean localProcess (final String text)
	{
		//XXX replace string/int constants by final statics
		String name;
		boolean parsed = true;
		boolean cal_remote = false, cal_slider = false, cal_ppi = false;
		
		try
		{
			/////////////////////////////////////////////////////////

			final Words w = new Words(text);
			if (w.checkNextAndForward("remote_value"))
				newConnection(w.getString("remote address"));

			else if (w.checkNextAndForward("remote_pvalue"))
				remotePassword = w.getString("remote password");

			else if ((name = w.checkSubNextAndForward("rhb")) != null)
			{
			    WebSocketClient h = remoteHistory.get(new Integer(name.substring(3)).intValue());
			    if (h.isLiving())
			    	stopConnection(h);
			    else
			    	restartConnection(h);
			}

			else if ((name = w.checkSubNextAndForward("rhr")) != null)
				removeConnection(new Integer(name.substring(3)).intValue());

			/////////////////////////////////////////////////////////
			
			else if (w.checkNextAndForward("remote_tryme"))
			{
				if (!tryMeInvoked)
				{
					tryMeInvoked = true;
					parseMulti(  "remote_tryme_panel add root panel title 'sandbox' bg black fg white;"
							   + "sandbox add remote_tryme_panel panel w 50 bg white rightof nothing gap 5;"
							   + "remote_tryme_log add remote_tryme_panel textzone h 80 above nothing leftof sandbox bg lightgray disable fg black;" 
							   + "remote_tryme_input add remote_tryme_panel text bg lightgray leftof sandbox below nothing rightof nothing w 43 h 12 text help;"
							   + "remote_tryme_info add remote_tryme_panel text text 'play:' leftof remote_tryme_input below nothing h 12 fg lightgray bg black disable;"
							   + "remote_tryme_demo1 add remote_tryme_panel button text 'demo' below remote_tryme_log above remote_tryme_input rightof nothing leftof sandbox w 8 gap 2;"
							   + "remote_tryme_panel update focus;"
							  );
					sandboxUpdateLog("The white panel on the right side is called 'sandbox'.\n"
						           + "try \"sandbox update bg yellow\"\n"
						           + "\n");						       
				}
			}
			else if (w.checkNextAndForward("remote_tryme_input"))
			{				
				final String sentence = w.getRawString("command");
				sandboxUpdateLog(sentence);
				parseMulti(sentence);
				
				// must be last to get focus on input zone
				parseMulti("remote_tryme_input update text ''");
			}
			else if (w.checkNextAndForward("remote_tryme_demo1"))
			{
				logAndExecute( 
						  "t1 clear;t2 clear;t3 clear;"
						+ "t1 add sandbox decorator h 70 above nothing;"
						+ "t11 add t1 panel;"
						+ "t2 add sandbox panel below t1 leftof nothing w 25 bg gray;"
						+ "t3 add sandbox panel below t1 rightof t2;"
						+ "bclear add t11 button  w 30 h 15 x 30 below nothing leftof nothing text clear;"
						+ "bquit  add t11 button  w 30 h 15 x 70 below nothing rightof nothing text quit;"
						+ "btitle add t11 button  w 200p h 15 x 50 above nothing text temp disable;"
						+ "rlarge add t2 radio above nothing              h 50 group c1 text large fg red;"
						+ "rsmall add t2 radio above nothing below rlarge h 25 group c1 text small fg blue;"
						+ "dec add t11 decorator below btitle above bclear;"
						+ "zz add dec plot dataset last1mn dataset last5mn dataset last15mn dataset cpu;"
						+ "zz update xstep 1 xrangemax 30;"
						+ "text add t2 text below rsmall text data;"
						+ "remote_tryme_g add t3 gfx;"
						+ "remote_tryme_g update add c circle 15 15 15 enable c;"
						+ "remote_tryme_g update add d circle 85 15 15 enable d;"
						+ "remote_tryme_g update add e circle 85 85 15 enable e;"
						+ "remote_tryme_g update add l1 arrow 30 15 70 15 enable l1;"
						+ "remote_tryme_g update add l2 arrow 25.6 25.6 74.4 74.4;"
						+ "remote_tryme_g update color l2 red;"
						+ "remote_tryme_g update color l1 blue;"
						+ "remote_tryme_g update bcolor c yellow;"
						+ "remote_tryme_g update color d green;"
						+ "remote_tryme_g update color e purple;"
						+ "remote_tryme_g update keepratio;"
						+ "refresh"
						);
			}
			else if (w.checkNextAndForward("remote_tryme_g"))
			{
				logAction(w, true);
				if (w.checkNextAndForward("d"))
					logAndExecute("remote_tryme_g update freeratio;redraw");
				else if (w.checkNextAndForward("e"))
					logAndExecute("remote_tryme_g update keepratio;redraw");
				else if (w.checkNextAndForward("c"))
					logAndExecute("zz update datavalues last1mn " + gfxDemoX + " " + (5 * Math.cos(gfxDemoX++ / 4.0)) + ";redraw");
				
				else
					sandboxUpdateLog("(no action)");
			}

			/////////////////////////////////////////////////////////

			else if (  (cal_remote = w.checkNextAndForward("remote_cal"))
			         || (cal_slider = w.checkNextAndForward("cal_slider"))
			         || (cal_ppi = w.checkNextAndForward("cal_ppi"))
			         )
			{
				if (cal_remote && !calibrateInvoked)
				{
					calibrateInvoked = true;
					
					parseMulti(  "Calibration add root panel;"
                               + "cal_rulers add Calibration panel leftof nothing w 30;"
                               + "cal_slider add Calibration sliderbar min " + ppiMin + " max " + ppiMax + " step 1 dragupdate set " + ppi + " below nothing rightof cal_rulers h 15 cc red fg lightgray;"
                               + "cal_screen add Calibration panel above cal_slider rightof cal_rulers;"
                               + "cal_ppi add cal_screen text h 10 right above nothing leftof nothing w 15;"
                               + "cal_ppmm add cal_screen text h 10 right above nothing leftof nothing below cal_ppi w 15 disable;"
                               + "cal_back add cal_screen button text 'ok!' bg black fg red h 20 w 20 rightof nothing above nothing;"
                               + "cal_in add cal_screen text h 10 above nothing rightof cal_ppi leftof cal_back disable;"
                               + "cal_mm add cal_screen text h 10 above nothing rightof cal_ppmm below cal_in leftof cal_back disable;"
                               + "cal_gfx add cal_screen gfx gap 0 w 100 leftof nothing below cal_mm keepratio;"
                               + "cal_gfx update add screen rectangle 0 0 100 100 color screen blue;"
                               + "cal_gfx update add diag arrow 10 90 80 20 color diag white;"
                               + "cal_gfx update add horz arrow 10 90 90 90 color horz white;"
                               + "cal_gfx update add vert arrow 10 90 10 10 color vert white;"
                               + "cal_but1in add cal_rulers button disable gap 0 fg white bg black text '1\"' above nothing h 1in w 25 leftof nothing;"
                               + "cal_but10in add cal_rulers button disable gap 0 fg white bg black text '5\"' above nothing h 5in w 25 leftof nothing rightof cal_but1in;"
                               + "cal_but30mm add cal_rulers button disable gap 0 fg white bg black text '30mm' above nothing h 30mm w 25 leftof nothing rightof cal_but10in;"
                               + "cal_but300mm add cal_rulers button disable gap 0 fg white bg black text '150mm' above nothing h 150mm w 25 rightof nothing;"
                               + "Calibration update focus;"
							  );
				}
				
				if (cal_slider || cal_ppi)
				{
                        ppi = w.getPosInt("ppi");
                        if (ppi < ppiMin)
                                ppi = ppiMin;
                        if (ppi > ppiMax)
                                ppi = ppiMax;
                }
				ppmm = ppi / 25.4f;
					
				IntfObject gfx = names.get("cal_gfx");
				if (gfx != null)
				{
					int xp = gfx.getPlace().c(Place.width).getPixel();
					int yp = gfx.getPlace().c(Place.height).getPixel();
					float xin = round(xp / ppi, 100);
					float xmm = round(xp / ppmm, 10);
					int big = xp * xp + yp * yp;
					float diagin = round((float)Math.sqrt(big / ppi / ppi), 100);
					float diagmm = round((float)Math.sqrt(big / ppmm / ppmm), 10);
					float ppmmi = round(ppmm, 100);

					parseMulti("cal_ppi update text " + ppi + ";"
							 + "cal_ppmm update text " + ppmmi + ";"
							 + (cal_ppi? "cal_slider update set " + ppi + ";": "")
							 + "cal_in update text 'ppi W/H:" + xin + " D:" + diagin + "';"
							 + "cal_mm update text 'ppmm W/H:" + xmm + " D:" + diagmm + "';"
							  );

					uiRefresh(names.get("Calibration"));
				}
				
			}

			else if (w.checkNextAndForward("cal_back"))
			{
				if (horizontal)
				{
					storage.setItem("ppiH", "" + ppi);
					ppiH = ppi;
				}
				else
				{
					storage.setItem("ppiV", "" + ppi);
					ppiV = ppi;
				}
					
			    guiRoot.back();
			    clear(names.get("Calibration"));
                calibrateInvoked = false;
            }

			/////////////////////////////////////////////////////////

			else
				parsed = false;
			
		} catch (final WordsException e)
		{
			error(e.getWords(), -1, e.getMessage());
		}
			
		return parsed;
	}

	///////////////////////////////////////////////////////
	
	/**
	 * Send to all peers public method
	 * @param text
	 */
	
	public final void send (final String text)
	{	
		if (!localProcess(text))
		{
			for (final WebSocketClient h: remoteHistory)
				if (h.isConnected())
					h.send(text);
			if (tryMeInvoked)
				logAction(text, false);	
			debug(text);
		}
	}

	private static native void jsconsole (String msg)
	/*-{
		setTimeout(function() { throw new Error(msg); }, 0);
	}-*/; 
	
	public static final void alert (final String text)
	{
			jsconsole(text);
			Window.alert(text);
	}

	public static final void debug (final String text)
	{
		jsconsole("DEBUG: " + text);
	}

	///////////////////////////////////////////////////////

	private void createRoot ()
	{
		final String name = "root";
		names.put(name, guiRoot = new GuiRoot(this, null, name));
	}
	
	public void onResize_PPIfromHV ()
	{
		Widget r = RootLayoutPanel.get();
		if (r.getOffsetWidth() > r.getOffsetHeight())
		{
			horizontal = true;
			ppi = ppiH;
		}
		else
		{
			horizontal = false;
			ppi = ppiV;
		}
        ppmm = ppi / 25.4f;
		debug("root: x=" + r.getOffsetWidth() + " y=" + r.getOffsetHeight());		
	}

	public void onModuleLoad ()
	{
		w = this;
		createRoot();
		populateHelpers();

        ppiH = ppiV = PPI.getPPI();
		storage = Storage.getLocalStorageIfSupported();
		if (storage != null)
		{
        	String ppiStr = storage.getItem("ppiH");
			if (ppiStr != null) try
			{
				int storedPPI = new Integer(ppiStr).intValue();
				if (storedPPI >= ppiMin && storedPPI <= ppiMax)
        	        ppiH = ppiV = storedPPI;
			} catch (NumberFormatException e) { }

			ppiStr = storage.getItem("ppiV");
			if (ppiStr != null) try
			{
				int storedPPI = new Integer(ppiStr).intValue();
				if (storedPPI >= ppiMin && storedPPI <= ppiMax)
        	        ppiV = storedPPI;
			} catch (NumberFormatException e) { }

        }
		onResize_PPIfromHV();

		remoteLoginWindowSetup();

		String server = Window.Location.getParameter("server");
		if (server != null && !server.equals("undefined"))
				newConnection(server);
	}
}
