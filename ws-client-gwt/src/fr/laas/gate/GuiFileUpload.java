
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
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;


class GuiFileUpload extends VerticalPanel implements IntfObject
{	
	///////////////////////////////////////////////////////
	// IntfObject implementation

	String				name			= null;
	IntfObject			parent			= null;
	Place				place			= null;
	TextBox				t				= null;
	FileUpload			f				= null;
		
	public String			getName		()	{ return name; }
	public IntfObject		getGOParent	()	{ return parent; }
	public Widget			getWidget	()	{ return this; }
	public Place			getPlace	()	{ return place; }
	public List<IntfObject>	getSons		()	{ return null; }

	
	public GuiFileUpload (IntfObject parent, String name)
	{
		super();
		setStyleName("");
		
		this.name = name;
		this.parent = parent;
		HorizontalPanel h1 = new HorizontalPanel();
		HorizontalPanel h2 = new HorizontalPanel();
		t = new TextBox();
		t.setName(this.name + "Text");
		f = new FileUpload() {
			public void onLoad() {
				setName("Parcourir");
			}
		};
		h1.add(t);
		h1.add(f);
		t.setText(f.getFilename());
		h2.add(new Button("Submit", new ClickHandler() {
			public void onClick(ClickEvent event) {
				String fileName = f.getFilename();
				t.setText(fileName);
				Gate.getW().send("'" + getName() + "' '" + fileName + "'");
				//submit();
			}
		}));
		this.add(h1);
		this.add(h2);

		place = new Place(this);
		// we are container, gap will be useful by inside objects
		place.setGap(0);
			
		parent.addSon(this, name);
	}

	public boolean setSonPosition (IntfObject son)
	{
		/*setWidgetPosition(
				son.getWidget(), 
				(int)(son.getPlace().c(Place.x).getPixel() + son.getPlace().getGap()), 
				(int)(son.getPlace().c(Place.y).getPixel() + son.getPlace().getGap()));
		return true;*/
		return false;
	}
		
	public static String help ()
	{
		return
					   "# \tgroup <t>\tset group name"
			+ Gate.endl + "# \ttext <t>\tchange text in button"
			;
	}
	
	public boolean update (Words words) throws WordsException
	{
		if (words == null)
			return true;
		
		/*if (words.checkNextAndForward("group"))
			setName(words.getString(Gate.cmdlineGroup));
		else if (words.checkNextAndForward("text"))
			setText(text = words.getString(Gate.cmdlineText));
		else
			return false;*/
		return true;
	}
		
	public void	setSonTitle (IntfObject son, String title)
	{
		// this widget do not have title
	}
	
	public boolean addSon (IntfObject son, String name)
	{
		/*sons.add(son);
		add(son.getWidget());
		return true;*/
		return false;
	}	
	
	public void delSon (IntfObject son)
	{
		//sons.remove(son);
	}
	
	public boolean redraw ()
	{
		return true;
	}


	///////////////////////////////////////////////////////

//	public void onClick (Widget sender)
//	{
//		W.getW().send(name);
//	}
	

} // class GuiButton
