/**************************************************************
 *
 * - CeCILL-B license
 * - (bsd-like, check http://www.cecill.info/faq.en.html#bsd)
 * 
 * Copyright CNRS
 * Contributors:
 * David Gauchard <gauchard@laas.fr>	2013-09-01
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

// TODO
// check gwt-mobile to get rid of timer
// RESIZE bug

import org.vaadin.gwtgraphics.client.DrawingArea;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;


class GuiSliderTouch extends GuiPanel
{	
	private static final float defaultCursorW = 5.0f;

	private final static int 	cBAR = 0;
	private final static int 	cBARBORDER = 1;
	private final static int 	cCURSOR = 2;
	private final static int 	cCURSORBORDER = 3;
	String colors[][] = new String [][]
	{
			{ "color bar", "lightgrey" },	// bar
			{ "bcolor bar", "lightgrey" },	// bar border
			{ "color cursor", "grey" },		// cursor
			{ "bcolor cursor", "grey" },	// cursor border
	};
	
	private float cursorW = -1;
	private float barW = -1;
	private float cursorL = -1;
	private float cursorR = -1;
	private GuiGFX gfxPanel = null;
	private DrawingArea gfx = null;
	private HandlerRegistration mouseHandler = null;
	private String pending = "";
	
	private float min = 0;
	private float max = 100;
	private float step = 1;
	private float value = max;
	private boolean dragUpdate = false;
	private boolean horizontal = true;
	
	
	// THIS startBuggyTouchTimer WILL BE USELESS WHEN TOUCH-EVENTS ARE AVAILABLE IN GWT
	private int lastScroll;
	private void startBuggyTouchTimer ()
	{
		if (Gate.onTouchScreen())
		{
			final Timer timer = new Timer()
			{
				public void run ()
				{
					int scroll = horizontal? getElement().getScrollLeft():
						                     getElement().getScrollTop();
					if (scroll != lastScroll)
						dragToRelX(1.0f - (1.0f * scroll / (horizontal? (gfx.getWidth()  - getOffsetWidth()): 
							                                            (gfx.getHeight() - getOffsetHeight()))),
							       true);
					if (getOffsetWidth() > 0 || getOffsetHeight() > 0)
						startBuggyTouchTimer();
				}
			};
			timer.schedule(500); // 1000 = 1 sec
		}
	}

	public GuiSliderTouch (IntfObject parent, String name)
	{	
		super(parent, name);
		Gate.getW().uiNeedUpdate(this);
	}
	
	public static String help ()
	{
		return
			              "# \tmin <n>\t"
			+ Gate.endl + "#\tmax <n>\t"
			+ Gate.endl + "#\tstep <n>\t"
			+ Gate.endl + "#\tset <n>\t"
			+ Gate.endl + "#\tdragupdate\tprovide drag updates"
			+ Gate.endl + "#\tdragignore\tignore drag updates"
			+ Gate.endl + "#\tcc <color>\tcursor color"
			+ Gate.endl + "#\tcb <color>\tcursor border color"
			+ Gate.endl + "#\tbc <color>\tbar border color"
			+ Gate.endl + "#\tcw\tcursor width (%)"
			;
	}
	
	private boolean doPending ()
	{
		if (pending.length() > 0 && gfx != null)
		{
			// we can be recursively called back if pending is not empty
			// so it must be emptied before calling the parser
			String copy = pending;
			pending = "";
			Gate.getW().parseMulti(copy);
		}
		return pending.length() == 0;
	}
	
	public boolean update (Words words) throws WordsException
	{		
		// no, we are not a panel:
		//if (super.update(words))
		//	return true;

		boolean ret = true;

		if (words.checkNextAndForward("cw"))
			setupGfx(words.getPosFloat("cursor width"), name + "# clear;");
		else if (words.checkNextAndForward("fg")) // override
			setColor(cBAR, words.getString("bar color"));
		else if (words.checkNextAndForward("bc"))
			setColor(cBARBORDER, words.getString("cursor border color"));
		else if (words.checkNextAndForward("cc")) 
			setColor(cCURSOR, words.getString("cursor color"));
		else if (words.checkNextAndForward("cb"))
			setColor(cCURSORBORDER, words.getString("cursor border color"));
		else if (words.checkNextAndForward("min"))
			min = words.getRelFloat("sliderbar min value");
		else if (words.checkNextAndForward("max"))
			max = words.getRelFloat("sliderbar max value");
		else if (words.checkNextAndForward("step"))
			step = words.getRelFloat("sliderbar step value");
		else if (words.checkNextAndForward("set"))
			setTo(words.getRelFloat("sliderbar value"));
		else if (words.checkNextAndForward("dragupdate"))
			dragUpdate = true;
		else if (words.checkNextAndForward("dragignore"))
			dragUpdate = false;
		else
			ret = false;
		
		doPending();
		
		return ret;
	}
	
	private String getColorCmd (int idx)
	{
		return colors[idx][0] + " " + colors[idx][1];
	}

	private String getColorWholeCmd (int idx)
	{
		return ";" + name + "# update " + getColorCmd(idx);
	}

	private void setColor (int idx, String color)
	{
		colors[idx][1] = color;
		pending += getColorWholeCmd(idx);
	}
	
	private void setTo (float newValue)
	{
		if (gfx == null)
			pending += ";" + name + " update set " + newValue;
		else
			dragToRelX((newValue - min) * step / (max - min), false);
	}
	
	private void dragTo (float x)
	{
		// getOffsetWidth(): visible window size
		// gfxPanel.getAbsoluteLeft(): left scroll position
		// gfx.getWidth(): total width (~barW)
		
		float relX;
		if (horizontal)
			relX = 1.0f * (x + gfxPanel.getAbsoluteLeft()) / getOffsetWidth();
		else
			relX = 1.0f * (x + gfxPanel.getAbsoluteTop()) / getOffsetHeight();
		
		// relX is in [0..1]
		// enlarge so that 0 is (0%-cursorW/2) and 1 is (100%+cursorW/2)
		relX = (relX - 0.5f) * (1 + cursorW / 100f) + 0.5f;
		// and truncate to [0..1]
		if (relX < 0)
			relX = 0;
		else if (relX > 1.0f)
			relX = 1.0f;
		
		dragToRelX(relX, false);
	}
	
	private void dragToRelX (float relX, boolean show)
	{
		if (relX > 1.0f) relX = 1.0f;
		if (relX <   0f) relX =   0f;
		
		if (horizontal)
		{
			float scroll = (1.0f - relX) * (gfx.getWidth() - getOffsetWidth());
			getElement().setScrollLeft(lastScroll = (int)scroll);
		}
		else
		{
			float scroll = (1.0f - relX) * (gfx.getHeight() - getOffsetHeight());
			getElement().setScrollTop(lastScroll = (int)scroll);
		}
		
		float newValue = (int)(relX * (max - min) / step + 0.5f) * step + min;
		if (newValue != value)
		{
			value = newValue;
			if (show || dragUpdate)
				sendValue();
		}
	}
	
	private void setupDrag ()
	{
		if (mouseHandler != null)
			return;
		mouseHandler = gfx.addMouseMoveHandler(new MouseMoveHandler()
		{
			public void onMouseMove (MouseMoveEvent event)
			{
				dragTo(horizontal? event.getX() - getAbsoluteLeft(): event.getY() - getAbsoluteTop());
			}
		});
	}
	
	private void removeDrag ()
	{
		if (mouseHandler != null)
		{
			mouseHandler.removeHandler();
			mouseHandler = null;
		}
	}
	
	private float sentValue = -100;
	private void sendValue ()
	{
		if (sentValue != value)
		{
			Gate.getW().send("'" + name + "' '" + value + "'");
			sentValue = value;
		}
	}
	
	private void setupGfx (float cursorW, String preamble)
	{
		horizontal = getOffsetWidth() > getOffsetHeight();
		
		this.cursorW = cursorW;
		barW = 200.0f - cursorW;
		cursorL = 100.0f * (100 - cursorW) / barW;
		cursorR = 100.0f *  100            / barW;
		String cmd = preamble + name + "# add " + name + " gfx ";
		if (horizontal)
			cmd += "w " + barW + " "
				 + "add bar rectangle 0 40 100 60 "
				 + "add cursor rectangle " + cursorL + " 10 " + cursorR + " 90 ";
		else
			cmd += "h " + barW + " "
			     + "add bar rectangle 40 0 60 100 "
				 + "add cursor rectangle 10 " + cursorR + " 90 " + cursorL + " ";
		
		for (int i = 0; i < colors.length; i++)
			cmd += getColorCmd(i) + " ";

		Gate.getW().parseMulti(cmd);
				
		gfxPanel = ((GuiGFX)Gate.getO(name + "#")); 
		gfx = gfxPanel.getDrawingArea(); 
		
		gfx.addMouseDownHandler(new MouseDownHandler()
		{
			public void onMouseDown (MouseDownEvent event)
			{
				dragTo(horizontal? event.getX() - getAbsoluteLeft(): event.getY() - getAbsoluteTop());
				setupDrag();
			}
		});
		
		gfx.addMouseUpHandler(new MouseUpHandler()
		{
			public void onMouseUp (MouseUpEvent event)
			{
				removeDrag();
				if (!dragUpdate)
					sendValue();
			}
		});

		gfx.addMouseWheelHandler(new MouseWheelHandler()
		{
			public void onMouseWheel (MouseWheelEvent event)
			{
				float shift = step * Math.signum(event.getDeltaY());
				if (horizontal)
					setTo(value - shift);
				else
					setTo(value + shift);
				sendValue();
			}
		});
		
		gfx.addMouseOutHandler(new MouseOutHandler()
		{
			public void onMouseOut (MouseOutEvent event)
			{
				sendValue();
			}
		});
		
		// touch handler will not even compile (gwt-2.5.1)
//		addAttachHandler(new TouchStartHandler()
//		{
//			public void onTouchStart(TouchStartEvent event) {
//				Gate.debug("touchstart " + event);
//			}
//		});

		// https://code.google.com/p/google-web-toolkit/issues/detail?id=3983
		// trying to get button state when coming back for whether to drag or not
//		gfx.addMouseOverHandler(new MouseOverHandler()
//		{
//			public void onMouseOver (MouseOverEvent event)
//			{
//				Gate.debug("button=" + event.getNativeButton());
//			}
//		});
	}
	
	public boolean redraw ()
	{
		super.redraw();
		if (cursorW < 0) 
		{
			// first call
			setScrollable();
			if (!Gate.onTouchScreen())
				hideScrollBars();
			setupGfx(defaultCursorW, "");
			startBuggyTouchTimer();
			return false;
		}
Gate.debug("rescroll " + value);
setTo(value); // rescroll
		return doPending();
	}

}; // GuiSliderTouch
