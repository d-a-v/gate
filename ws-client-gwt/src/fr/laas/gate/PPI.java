
// http://stackoverflow.com/questions/8456455/mobile-web-how-to-get-physical-pixel-size

package fr.laas.gate;

import com.google.gwt.core.client.JavaScriptObject;

public class PPI
{
	private final static class PPIImpl extends JavaScriptObject
	{
		protected PPIImpl()
		{
		}

		public static native float ppi ()
		/*-{
		 	// create an empty element
		 	var div = document.createElement("div");
		 	// give it an absolute size of one inch
		 	div.style.width="1in";
		 	// append it to the body
		 	var body = document.getElementsByTagName("body")[0];
		 	body.appendChild(div);
		 	// read the computed width
		 	var ppi = document.defaultView.getComputedStyle(div, null).getPropertyValue('width');
		 	// remove it again
		 	body.removeChild(div);
		 	// and return the value
		 	return parseFloat(ppi);
		}-*/;
	}
		
	public static float getPPI ()
	{
		return PPIImpl.ppi();
	}
}
