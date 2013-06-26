
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
import com.google.gwt.user.client.ui.Widget;


class GuiSliderBar extends SliderBar implements IntfObject
{
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

	
	public GuiSliderBar (IntfObject parent, String name) 
	{
		this.name = name;
		this.parent = parent;
		place = new Place(this);

		parent.addSon(this, name);
		
		addChangeHandler(new ChangeValueHandler()
		{
			public void onValueChange (ChangeValueEvent e)
			{
				Gate.getW().send("'" + getName() + "' " + e.value);
			}
		});
	}
		
	public static String help ()
	{
		return
					   "# \tmin <n>\t"
			+ Gate.endl + "# \tmax <n>\t"
			+ Gate.endl + "# \tstep <n>\t"
			+ Gate.endl + "# \tset <n>\t"
			+ Gate.endl + "# \tdragupdate\tprovide drag updates"
			+ Gate.endl + "# \tdragignore\tignore drag updates"
			+ Gate.endl + "# \tcc <color>\tcursor color"
			+ Gate.endl + "# \tcb <color>\tcursor border color"
			+ Gate.endl + "# \tbc <color>\tbar border color"
			;
	}
	
	public boolean update (Words words) throws WordsException
	{		
		if (words == null)
			return true;
		
		if (words.checkNextAndForward("fg")) // override global fg
			setBarColor(words.getString("bar color"));
		else if (words.checkNextAndForward("cc")) 
			setCursorColor(words.getString("cursor color"));
		else if (words.checkNextAndForward("cb"))
			setCursorBorderColor(words.getString("cursor border color"));
		else if (words.checkNextAndForward("bc"))
			setBarBorderColor(words.getString("bar border color"));
		else if (words.checkNextAndForward("min"))
			setMinValue(words.getPosInt("sliderbar min value"));
		else if (words.checkNextAndForward("max"))
			setMaxValue(words.getPosInt("sliderbar max value"));
		else if (words.checkNextAndForward("step"))
			setStepSize(words.getPosInt("sliderbar step value"));
		else if (words.checkNextAndForward("set"))
			setCurrentValue(words.getPosInt("sliderbar set value"));
		else if (words.checkNextAndForward("dragupdate"))
			setDragChanges(true);
		else if (words.checkNextAndForward("dragignore"))
			setDragChanges(false);
		else
			return false;		
		
		return true;
	}
		
	public void	setSonTitle (IntfObject son, String title)
	{
		// this widget do not have son or title
	}
	
	public boolean addSon (IntfObject son, String name)
	{
		// this widget is not a container
		return false;
	}	
	
	public void delSon (IntfObject son)
	{
		// this widget is not a container
	}	
	
	public boolean setSonPosition (IntfObject son)
	{
		// this widget do not have sons
		return false;
	}
	
	public boolean redraw ()
	{
		return true;
	}

} // class GuiButton
