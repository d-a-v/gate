
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

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;


class GuiRoot extends TabLayoutPanel implements IntfObject
{
	String				name			= null;
	Gate					w				= null;		// caller
	Place				place			= null;
	List<IntfObject>	sons			= null;
	
	String				activeName		= null;

	public String			getName		()	{ return name; }
	public IntfObject		getGOParent	()	{ return null; }
	public Widget			getWidget	()	{ return this; }
	public Place			getPlace	()	{ return place; }
	public List<IntfObject>	getSons		()	{ return sons; }

	public GuiRoot (Gate caller, IntfObject parent, String name)
	{
		super(Gate.TabLayoutPanelSize, Gate.TabLayoutPanelUnit);
		RootLayoutPanel.get().add(this);
		this.name = name;
		w = caller;
		place = new Place(this);
		place.setGap(0);
		sons = new ArrayList<IntfObject>();
		
		addSelectionHandler(new SelectionHandler<Integer>()
		{
			public void onSelection(SelectionEvent<Integer> event)
			{
				// things need to be redrawed (today: GuiText's text needs to be resized at least the first time they are displayed)
				//W.debug("refresh from tab select " + event.getSelectedItem() + " (" + sons.get(event.getSelectedItem()).getName() + ")");
				Gate.getW().uiRefresh(getDisplayedSon());
			}
		});
	}
	
	public IntfObject getDisplayedSon ()
	{
		return sons.get(getSelectedIndex());
	}
	
	public void back ()
	{
		if (activeName == null)
			return;
		for (IntfObject son: sons)
			if (son.getName().equals(activeName))
			{
				focus(son);
				return;
			}
	}
	
	public void focus (IntfObject o)
	{
		//W.debug("focus on " + o.getName());
		if (o != null)
		{
			activeName = getDisplayedSon().getName();
			selectTab(o.getWidget());
		}
	}
	
	public void onResize ()
	{
		//W.debug("refresh from guiroot on resize");
		w.onResize_PPIfromHV();
		w.sendInfoUI();
		w.uiRefresh(getDisplayedSon());
	}
	
	public boolean setSonPosition (IntfObject son)
	{
		return false;
	}
	
	public void	setSonTitle (IntfObject son, String title)
	{
		setTabText(getWidgetIndex(son.getWidget()), title);
	}
	
	public boolean addSon (IntfObject son, String name)
	{
		final int count = getWidgetCount();
		
		//??? son.getPlace().setGap(0);
		if (count == 0)
		{
			sons.add(son);
			add(son.getWidget(), name);
		}
		else
		{
			sons.add(count - 1, son);
			insert(son.getWidget(), name, count - 1);
		}
		return true; 
	}
		
	public void delSon (IntfObject son)
	{
		sons.remove(son);
	}	
	
	public boolean update (Words words)
	{
		// never called by user
		return false;
	}
	
	public boolean redraw ()
	{
		return true;
	}

}
