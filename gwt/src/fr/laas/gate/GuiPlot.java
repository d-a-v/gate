
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

import java.util.HashMap;
import java.util.List;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.visualizations.corechart.LineChart;
import com.google.gwt.visualization.client.visualizations.corechart.Options;


class GuiPlot implements IntfObject
{
	private static final float 			datasetXRangeMaxDefault = 10;
	
	private LineChart					chart;
	private DataTable					data;
	private Options						options;
	private float						datasetXStart;
	private float						datasetXStep;
	private int						plotRowHighest;
	private HashMap<String, Integer>	titles;
	private NumberFormat				numberFormat;
	private float						datasetXRangeMax;
	
	// internal gwt2.5 compiler error if declared inside 
	// the block where it is used. try again later.
	float								newDatasetXStep;

	static boolean visualizationLoaded = false;
	static boolean visualizationLoading = false;
	static public boolean apiLoaded ()
	{
		if (visualizationLoaded) 
			return true;
		
	    if (!visualizationLoading)
	    {
	    	visualizationLoading = true;

	    	Gate.getW().addLocker();
	    	Gate.debug("loading visu api");

	        VisualizationUtils.loadVisualizationApi(
	    		new Runnable()
	    		{
	    			public void run ()
	    			{
	    				Gate.debug("visu api loaded");
	    				visualizationLoaded = true;
	    				Gate.getW().gotLocker();
	    			}
	    		}, LineChart.PACKAGE);
	    }

	    return false;
	}

	
	///////////////////////////////////////////////////////
	// IntfObject implementation

	String				name			= null;
	IntfObject			parent			= null;
	Place				place			= null;
		
	public String			getName		()	{ return name; }
	public IntfObject		getGOParent	()	{ return parent; }
	public Widget			getWidget	()	{ return chart; }
	public Place			getPlace	()	{ return place; }
	public List<IntfObject>	getSons		()	{ return null; }
	
	void resetData ()
	{
		data = DataTable.create();
		data.addColumn(ColumnType.STRING, "X-axis");
	    options = Options.create();
	    datasetXStart = 0;
	    datasetXStep = 0;
		titles = new HashMap<String, Integer>();
		datasetXRangeMax = datasetXRangeMaxDefault;
	}
	
	public GuiPlot (IntfObject parent, String name)
	{
		this.name = name;
		this.parent = parent;
		place = new Place(this);
	
		resetData();
		chart = new LineChart(data, options);
				
		parent.addSon(this, name);
	}
	
	public boolean redraw ()
	{
		chart.draw(data, options);
		return true;
	}
	
	public static String help ()
	{
		return
					   "# \treset\t\t\t\tclear everything in plotter"
			+ Gate.endl + "# \tdataset <name>\t\t\tadd new named dataset"
			+ Gate.endl + "# \txstep <xstep>\t\t\tsetup X origin and step"
			+ Gate.endl + "# \tdatavalues <xstart> <yvalue>+\tadd new Y values"
			+ Gate.endl + "# \txrangemax <value-number>\tmaximum X range (default " + datasetXRangeMaxDefault + ")"
			;
	}
	
	public boolean update (Words words) throws WordsException
	{ 
		if (words == null)
			return true;

		if (words.checkNextAndForward("reset"))
		{
			resetData();
			Gate.getW().uiNeedUpdate(this);
		}
		
		else if (words.checkNextAndForward("xrangemax"))
		{
			datasetXRangeMax = words.getPosInt("dataset maximum X range");
		}

		else if (words.checkNextAndForward("dataset"))
		{
			String title = words.getString("dataset title");
			data.addColumn(ColumnType.NUMBER, title);
			titles.put(title, data.getNumberOfColumns() - 1);
		}

		else if (words.checkNextAndForward("xstep"))
		{
			newDatasetXStep = words.getPosFloat("dataset X step");
			if (datasetXStep != 0)
				return Gate.getW().error(words, -2, Gate.cmdlineAlreadySet);
			if (newDatasetXStep <= 0)
				return Gate.getW().error(words, -2, Gate.cmdlinePositive);
			datasetXStep = newDatasetXStep;

			// setup numberFormat
			long decimal = Math.round(0.5 - Math.log10(datasetXStep));
			String format = "0."; 
			for (long i = 0; i < decimal; i++)
				format += '0';
			numberFormat = NumberFormat.getFormat(format);
		}

		else if (words.checkNextAndForward("datavalues"))
		{
			if (datasetXStep == 0)
				return Gate.getW().error(words, -1, Gate.cmdlineUndefinedAxis);

			String datasetName = words.getString("dataset name");
			Integer datasetIndex = titles.get(datasetName);
			if (datasetIndex == null)
				return Gate.getW().error(words, -1, Gate.cmdlineNotFound);
				
			float datasetX = words.getRelFloat("dataset X start");

			if (data.getNumberOfRows() == 0)
				datasetXStart = datasetX;
				
			int plotRowUser = (int)(((datasetX - datasetXStart) / datasetXStep) + 0.5);
				
			{
				int plotNumberOfRows = data.getNumberOfRows();
				int plotLackOfRow = plotRowUser - plotNumberOfRows;
				if (plotNumberOfRows == 0 || plotLackOfRow > 0)
				{
					// add X labels
					data.addRows(plotLackOfRow);
					for (int i = plotNumberOfRows; i < plotRowUser; i++)
					{
						data.setValue(i, 0, numberFormat.format((i * datasetXStep) + datasetXStart));
					}
					plotRowHighest = plotRowUser - 1;
				}
				else if (plotLackOfRow < 0)
				{
					return Gate.getW().error(words, -1, Gate.cmdlineOutOfRange);
					//data.insertRows(0, plotLackOfRow);
					//datasetXStart += plotLackOfRow * datasetXStep;
				}
			}

			while (words.checkNextIsRelFloat())
			{					
				if (plotRowUser > plotRowHighest)
				{
					plotRowHighest = plotRowUser;

					// add X label
					data.addRow();
	                data.setValue(plotRowUser, 0, numberFormat.format((plotRowUser * datasetXStep) + datasetXStart));
				}
				data.setValue(plotRowUser++, datasetIndex, words.getRelFloat("value"));

				while (data.getNumberOfRows() > datasetXRangeMax)
				{
					if ((plotRowHighest - plotRowUser) > (plotRowUser - 0))
					{
						// remove last
						data.removeRow(--plotRowHighest);
					}
					else
					{
						// remove first
						data.removeRow(0);
						datasetXStart += datasetXStep;
						plotRowHighest--;
					}
				}
			}
				
			Gate.getW().uiNeedUpdate(this);
		}
		
		else 
			return false;
		
		return true;
	}
		
	public void	setSonTitle (IntfObject son, String title)
	{
		// this widget do not have sons
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
	
	///////////////////////////////////////////////////////

} // class GuiPlot
