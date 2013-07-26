
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
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.regexp.shared.MatchResult;


class Words
{
	public static final String regexpPosInt = "[0-9]+"; 
	public static final String regexpRelInt = "[+-]?[0-9]+"; 
	public static final String regexpPosFloat = "[0-9]+(\\.[0-9]+)?([eE][+-]?[0-9]+)?"; 
	public static final String regexpRelFloat = "[+-]?" + regexpPosFloat; 
	public static final String regexpPixel = "[0-9\\.]+p"; 
	public static final String regexpMm = "[0-9\\.]+mm";
	public static final String regexpInch = "[0-9\\.]+in"; 
	//public static final String wordSeparator = "[^a-zA-Z0-9_:\\-\\.\\+#]+";

	public class CoordinateElement
	{
		private float value;
		private Place.Type type;
		
		public float		getValue ()	{ return value; }
		public Place.Type	getType ()	{ return type; }
		
		public CoordinateElement (float value, Place.Type type)
		{
			this.value = value;
			this.type = type;
		}
	}

	private List<String> array;
	private int currentIdx;
	
	public Words (String line)
	{
		// http://stackoverflow.com/questions/366202/regex-for-splitting-a-string-using-space-when-not-surrounded-by-single-or-double
		// http://stackoverflow.com/questions/6323024/gwt-2-1-regex-class-to-parse-freetext
		// + rework
		array = new ArrayList<String>();
		RegExp regex = RegExp.compile("([^\\s\"']+)|\"([^\"]*)\"|'([^']*)'", "g");
		for (MatchResult result = regex.exec(line); result != null; result = regex.exec(line))
			for (int i = 1; i < result.getGroupCount(); i++)
				if (result.getGroup(i) != null)
				{
					array.add(result.getGroup(i));
					break;
				}
		currentIdx = 0;
	}
	
	public String toString ()
	{
		String value = "";
		for (String w: array)
			value += "'" + w + "' ";
		return value;
	}
	
	public int getCurrentIdx ()
	{
		return currentIdx;
	}
	
	public int getLength ()
	{
		return array.size();
	}
	
	public String showDetachedWord (int idx)
	{
		return array.get(idx); 
	}
	
	public Words rewind (int howMuch)
	{
		currentIdx -= howMuch;
		return this;
	}
	
	
	
	public boolean hasNext ()
	{
		return currentIdx < array.size();
	}
	
	public boolean checkNextAndForward (String next)
	{
		boolean check;
		check = (hasNext() && array.get(currentIdx).equals(next));
		if (check)
				currentIdx++;
		return check;
	}
	
	public String checkSubNextAndForward (String subNext)
	{
		String ret;
		if (hasNext() && (ret = array.get(currentIdx)).substring(0, subNext.length()).equals(subNext))
		{
			currentIdx++;
			return ret;
		}
		return null;
	}
	
	public boolean checkNextIsPosInt ()
	{
		return (hasNext() && array.get(currentIdx).matches(regexpPosInt));
	}

	public boolean checkNextIsRelInt ()
	{
		return (hasNext() && array.get(currentIdx).matches(regexpRelInt));
	}

	public boolean checkNextIsPosFloat ()
	{
		return (hasNext() && array.get(currentIdx).matches(regexpPosFloat));
	}

	public boolean checkNextIsRelFloat ()
	{
		return (hasNext() && array.get(currentIdx).matches(regexpRelFloat));
	}

	public String getString (String error) throws WordsException
	{
		// clean for potential / known problems
		return getRawString(error).replace(";", "");
	}
	
	public String getRawString (String error) throws WordsException
	{
		if (!hasNext() /*|| array.get(currentIdx).length() == 0*/)
			throw new WordsException(this, error);
		return array.get(currentIdx++);
	}
	
	public float getRelFloat (String error) throws WordsException
	{
		String word = getString(error);
		if (word.matches(regexpRelFloat))
			return new Float(word).floatValue();
		throw new WordsException(this, error);
	}

	public float getPosFloat (String error) throws WordsException
	{
		String word = getString(error);
		if (word.matches(regexpPosFloat))
			return new Float(word).floatValue();
		throw new WordsException(this, error);
	}

	public int getRelInt (String error) throws WordsException
	{
		String word = getString(error);
		if (word.matches(regexpRelInt))
			return new Integer(word).intValue();
		throw new WordsException(this, error);
	}

	public int getPosInt (String error) throws WordsException
	{
		String word = getString(error);
		if (word.matches(regexpPosInt))
			return new Integer(word).intValue();
		throw new WordsException(this, error);
	}

	public CoordinateElement getCoordinate (String error) throws WordsException 
	{
		String word = getString(error); 
		if (word.matches(regexpPixel))
			return new CoordinateElement(new Float(word.substring(0, word.length() - 1)).floatValue(), Place.Type.PIXEL);
		if (word.matches(regexpMm))
			return new CoordinateElement(new Float(word.substring(0, word.length() - 2)).floatValue(), Place.Type.MM);
		if (word.matches(regexpInch))
			return new CoordinateElement(new Float(word.substring(0, word.length() - 2)).floatValue(), Place.Type.INCH);
		if (word.matches(regexpPosFloat))
			return new CoordinateElement(new Float(word).floatValue() / 100.0f, Place.Type.PERCENT);
		rewind(1);
		throw new WordsException(this, Gate.cmdlineCoordinate);
	}
}
