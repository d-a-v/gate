
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

import java.util.List;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

class GuiText extends TextBox implements IntfObject
{
	private String text;
	
	///////////////////////////////////////////////////////
	// IntfObject implementation

	String				name			= null;
	IntfObject			parent			= null;
	Place				place			= null;
		
	public String			getName		()	{ return name; }
	public IntfObject		getGOParent	()	{ return parent; }
	public Widget			getWidget	()	{ return this; }
	public Place			getPlace	()	{ return place; }
	public List<IntfObject>	getSons		()	{ return null; }

	
	public GuiText (final IntfObject parent, final String name)
	{
		setStyleName("");
		this.name = name;
		this.parent = parent;
		place = new Place(this);

		text = "";
	
		/*
		addValueChangeHandler(new ValueChangeHandler<String>()
		{
			public void onValueChange (final ValueChangeEvent<String> event)
			{
				text = getText();
				redraw(); // fontsize
				W.getW().send("'" + getName() + "' '" + text + "'");
		    }
		});
		*/
		
	    addKeyDownHandler(new KeyDownHandler()
	    {
	    	public void onKeyDown (KeyDownEvent event)
	    	{
	    		if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
	    		{
	    			text = getText();
    				Gate.getW().send("'" + getName() + "' '" + text + "'");
    				cancelKey();
	    		}
	    		redraw(); // fontsize
	        }
	    });

	    parent.addSon(this, name);
	}
	

		
	public static String help ()
	{
		return
					   "#\ttext <t>\tchange text"
			+ Gate.endl + "#\tleft\talignment (default)"
			+ Gate.endl + "#\tright\talignment"
			+ Gate.endl + "#\tcenter\talignment"
			+ Gate.endl + "#\tjustify\talignment"
			+ Gate.endl + "#\tdisable"
			+ Gate.endl + "#\tenable"
			;
	}
	
	public boolean update (final Words words) throws WordsException
	{
		if (words == null)
			return true;
		
		if (words.checkNextAndForward("text"))
		{
			setText(text = words.getString(Gate.cmdlineText));
			redraw();
		}
		else if (words.checkNextAndForward("enable"))
			setEnabled(true);
		else if (words.checkNextAndForward("disable"))
			setEnabled(false);
		else if (words.checkNextAndForward("left"))
			setAlignment(TextAlignment.LEFT);
		else if (words.checkNextAndForward("right"))
			setAlignment(TextAlignment.RIGHT);
		else if (words.checkNextAndForward("center"))
			setAlignment(TextAlignment.CENTER);
		else if (words.checkNextAndForward("justify"))
			setAlignment(TextAlignment.JUSTIFY);
		else 
			return false;
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
	
	public void delSon (IntfObject son)
	{
		// this widget is not a container
	}	
	
	public boolean setSonPosition (final IntfObject son)
	{
		// this widget do not have sons
		return false;
	}
	
	
	public boolean redraw ()
	{
		return Gate.getW().setFontSize(this, getText().length());
	}

} // class GuiButton
