
/**
 *
 * (Apache License 2.0)
 * 
 * A widget that allows the user to select a value within a range of possible
 * values using a sliding bar that responds to mouse events.
 *
 * You need to set the three images, knobNormal, knobDisabled, and knobSliding
 *
 * Original code from {http://code.google.com/p/google-web-toolkit-incubator/wiki/SliderBar}
 *
 * Modifications:
 * Stripped out all key handlers, css, and abstract images.
 * Upgraded to use ChangeHandlers
 *
 * 
 * <h3>Example usage with UIBinder</h3>
 * <code>
 *      <p><ui:UiBinder ... xmlns:m="urn:import:mypackage"></p>
 *  <p><ui:with field="res" type="MyResources" /></p>
 *  <p>...</p>
 *      <p><m:SliderBar
 *              ui:field="mySlider"
 *              knobNormal="{res.mySliderNormalImageResource}"
 *              knobDisabled="{res.mySliderDisabledImageResource}"
 *              knobSliding="{res.mySliderSlidingImageResource}"/>
 *  </p>
 * </code>
 *
 * @author Google (original)  https://code.google.com/p/google-web-toolkit-incubator/wiki/SliderBar
 * @author Craig Mitchell     http://www.mail-archive.com/google-web-toolkit@googlegroups.com/msg56751.html
 * @since 07/01/2011
 *
 * adapted to GATE by David Gauchard
 */

package fr.laas.gate;

import java.util.ArrayList;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FocusPanel;

public class SliderBar extends FocusPanel
{
	/**
	 * The change listeners.
	 */
	private ArrayList<ChangeHandler> changeHandlers = null;

	/**
	 * The current value.
	 */
	private double curValue = 0;

	/**
	 * The knob that slides across the line.
	 */
	private Button knobImage;

	/**
	 * The know image resources
	 */

	/**
	 * The line that the knob moves over.
	 */
	private Element lineElement;
	private Element knobElement;

	/**
	 * The offset between the edge (or Top) of the shell and the line.
	 */
	private int lineOffset = 0;

	/**
	 * The maximum slider value.
	 */
	private double maxValue = 1;

	/**
	 * The minimum slider value.
	 */
	private double minValue = 0;
	private double lastEventValue = minValue - 1;

	/**
	 * A bit indicating whether or not we send mouse-down change-value events in callbacks
	 */
	private boolean dragEvents = false;
	private boolean mouseIsDragging = false;

	/**
	 * The size of the increments between knob positions.
	 */
	private double stepSize = 1;
	
	private boolean horizontal = true;

	/**
	 * Constructor
	 */
	public SliderBar ()
	{
		super();

		// Create the outer shell
		DOM.setStyleAttribute(getElement(), "position",	"relative");

		// Create the line
		lineElement = DOM.createDiv();
		DOM.appendChild(getElement(), lineElement);
		DOM.setStyleAttribute(lineElement, "position", "absolute");
		DOM.setStyleAttribute(lineElement, "border", "1px solid");
		DOM.setStyleAttribute(lineElement, "overflow", "hidden");

		sinkEvents(Event.ONMOUSEDOWN | Event.ONMOUSEUP | Event.ONMOUSEWHEEL);
		mouseIsDragging = false;
		dragEvents = false;
		
		knobImage = new Button();
		knobElement = knobImage.getElement();
		DOM.appendChild(getElement(), knobElement);
		DOM.setStyleAttribute(knobElement, "position", "absolute");
		DOM.setStyleAttribute(knobElement, "cursor", "pointer");
		
		horizontal = (getOffsetWidth() >= getOffsetHeight());
		setupVerticalHorizontal();
	}
	
	public void setupVerticalHorizontal ()
	{
		if (horizontal)
		{
			DOM.setStyleAttribute(lineElement, "width", "80%");
			DOM.setStyleAttribute(lineElement, "height", "50%");
			DOM.setStyleAttribute(lineElement, "top", "25%");
			DOM.setStyleAttribute(lineElement, "left", "10%");
			
			DOM.setStyleAttribute(knobElement, "width", "10%");
			DOM.setStyleAttribute(knobElement, "height", "100%");
			DOM.setStyleAttribute(knobElement, "top", "0px");

		}
		else
		{
			DOM.setStyleAttribute(lineElement, "width", "50%");
			DOM.setStyleAttribute(lineElement, "height", "80%");
			DOM.setStyleAttribute(lineElement, "top", "10%");
			DOM.setStyleAttribute(lineElement, "left", "25%");

			DOM.setStyleAttribute(knobElement, "width", "100%");
			DOM.setStyleAttribute(knobElement, "height", "10%");
			DOM.setStyleAttribute(knobElement, "left", "0px");
		}
	}
	
	public void setBarColor (String color)
	{
		DOM.setStyleAttribute(lineElement, "backgroundColor", color);
	}
	
	public void setBarBorderColor (String color)
	{
		DOM.setStyleAttribute(lineElement, "borderColor", color);
	}

	public void setCursorColor (String color)
	{
		DOM.setStyleAttribute(knobImage.getElement(), "background", color);
	}
	
	public void setCursorBorderColor (String color)
	{
		DOM.setStyleAttribute(knobImage.getElement(), "borderColor", color);
	}
	
	/**
	 * Draw the knob where it is supposed to be relative to the line.
	 */
	private void drawKnob ()
	{
		// Abort if not attached
		if (!isAttached())
			return;

		if (horizontal)
		{
			// Move the knob to the correct position
			//Element knobElement = knobImage.getElement();
			int lineWidth = DOM.getElementPropertyInt(lineElement, "offsetWidth");
			int knobWidth = DOM.getElementPropertyInt(knobElement, "offsetWidth");
			int knobLeftOffset = (int) (lineOffset + (getKnobPercent() * lineWidth) - (knobWidth / 2));
			knobLeftOffset = Math.min(knobLeftOffset, lineOffset + lineWidth - (knobWidth / 2) - 1);
			DOM.setStyleAttribute(knobElement, "left", knobLeftOffset + "px");
		}
		else
		{	
			// Move the knob to the correct position
			//Element knobElement = knobImage.getElement();
			int lineHeight = DOM.getElementPropertyInt(lineElement, "offsetHeight");
			int knobHeight = DOM.getElementPropertyInt(knobElement, "offsetHeight");
			int knobTopOffset = (int) (lineOffset + (getKnobPercent() * lineHeight) - (knobHeight / 2));
			knobTopOffset = Math.min(knobTopOffset, lineOffset + lineHeight - (knobHeight / 2) - 1);
			DOM.setStyleAttribute(knobElement, "top", knobTopOffset + "px");
		}
	}

	/**
	 * Add a change listener to this SliderBar.
	 *
	 * @param listener the listener to add
	 */
	public void addChangeHandler (ChangeHandler listener)
	{
		if (changeHandlers == null)
			changeHandlers = new ArrayList<ChangeHandler>();
		changeHandlers.add(listener);
	}

	/**
	 * Return the current value.
	 *
	 * @return the current value
	 */
	public double getCurrentValue()
	{
		return curValue;
	}

	/**
	 * Return the max value.
	 *
	 * @return the max value
	 */
	public double getMaxValue ()
	{
		return maxValue;
	}

	/**
	 * Return the minimum value.
	 *
	 * @return the minimum value
	 */
	public double getMinValue ()
	{
		return minValue;
	}

	/**
	 * Return the step size.
	 *
	 * @return the step size
	 */
	public double getStepSize ()
	{
		return stepSize;
	}

	/**
	 * Return the total range between the minimum and maximum values.
	 *
	 * @return the total range
	 */
	public double getTotalRange ()
	{
		if (minValue > maxValue)
			return 0;
		else
			return maxValue - minValue;
	}

	/**
	 * Listen for events that will move the knob.
	 *
	 * @param event the event that occurred
	 */
	@Override
	public void onBrowserEvent (Event event)
	{
		super.onBrowserEvent(event);

		switch (DOM.eventGetType(event))
		{

		case Event.ONMOUSEWHEEL:
			int velocityY = DOM.eventGetMouseWheelVelocityY(event);
			DOM.eventPreventDefault(event);
			if (velocityY > 0)
				shiftRight(1);
			else
				shiftLeft(1);
			break;

		case Event.ONMOUSEDOWN:
			sinkEvents(Event.ONMOUSEMOVE);
			unsinkEvents(Event.ONMOUSEWHEEL);
			DOM.setCapture(getElement());
			DOM.eventPreventDefault(event);
			slideKnob(event);
			break;

		case Event.ONMOUSEUP:
			sinkEvents(Event.ONMOUSEWHEEL);
			unsinkEvents(Event.ONMOUSEMOVE);
			mouseIsDragging = false;
			DOM.releaseCapture(getElement());
			slideKnob(event);
			break;

		case Event.ONMOUSEMOVE:
			mouseIsDragging = true;
			slideKnob(event);
			break;
		}
	}

	
	/**
	 * This method is called when the dimensions of the parent element change.
	 * Subclasses should override this method as needed.
	 *
	 * @param width the new client width of the element
	 * @param height the new client height of the element
	 */
	public void onResize (int width, int height)
	{
		boolean newHorizontal = width >= height;
		
		if (newHorizontal != horizontal)
		{
			horizontal = newHorizontal;
			setupVerticalHorizontal();
		}

		if (horizontal)
		{
			// Center the line in the shell
			int lineWidth = DOM.getElementPropertyInt(lineElement, "offsetWidth");
			lineOffset = (width / 2) - (lineWidth / 2);
			DOM.setStyleAttribute(lineElement, "left", lineOffset +	"px");

		}
		else
		{
			// Center the line in the shell
			int lineHeight = DOM.getElementPropertyInt(lineElement, "offsetHeight");
			lineOffset = (height / 2) - (lineHeight / 2);
			DOM.setStyleAttribute(lineElement, "top", lineOffset +	"px");
		}
		
		// Draw the other components
		drawKnob();
	}

	/**
	 * Remove a change listener from this SliderBar.
	 *
	 * @param listener the listener to remove
	 */
	public void removeChangeHandler (ChangeHandler listener)
	{
		if (changeHandlers != null)
			changeHandlers.remove(listener);
	}

	/**
	 * Set the current value and fire the onValueChange event.
	 *
	 * @param curValue the current value
	 */
	public void setCurrentValue (double curValue)
	{
		setCurrentValue(curValue, true);
	}

	/**
	 * Set the current value and optionally fire the onValueChange event.
	 *
	 * @param curValue the current value
	 * @param fireEvent fire the onValue change event if true
	 */
	public void setCurrentValue (double curValue, boolean fireEvent)
	{
		// Confine the value to the range
		this.curValue = Math.max(minValue, Math.min(maxValue, curValue));
		double remainder = (this.curValue - minValue) % stepSize;
		this.curValue -= remainder;

		// Go to next step if more than halfway there
		if ((remainder > (stepSize / 2)) && ((this.curValue + stepSize) <=	maxValue))
			this.curValue += stepSize;

		// Redraw the knob
		drawKnob();
		
		if (fireEvent)
		{
			if (lastEventValue == this.curValue || (mouseIsDragging && !dragEvents))
				fireEvent = false;
			else
				lastEventValue = this.curValue;
		}

		// Fire the onValueChange event
		if (fireEvent && changeHandlers != null)
			for (ChangeHandler changeHandler: changeHandlers)
				changeHandler.onChange(new ChangeValueEvent(this.curValue));
	}

	/**
	 * Sets whether this widget is enabled.
	 *
	 * @param enabled true to enable the widget, false to disable it
	 */
	public void setEnabled (boolean enabled)
	{
		if (enabled)
		{
			mouseIsDragging = false;
			sinkEvents(Event.ONMOUSEDOWN | Event.ONMOUSEUP | Event.ONMOUSEWHEEL);
		}
		else
			unsinkEvents(Event.ONMOUSEDOWN | Event.ONMOUSEUP | Event.ONMOUSEWHEEL | Event.ONMOUSEMOVE);
	}

	/**
	 * Set the max value.
	 *
	 * @param maxValue the current value
	 */
	public void setMaxValue (double maxValue)
	{
		this.maxValue = maxValue;
		resetCurrentValue();
	}

	/**
	 * Set the minimum value.
	 *
	 * @param minValue the current value
	 */
	public void setMinValue (double minValue)
	{
		this.minValue = minValue;
		resetCurrentValue();
	}

	/**
	 * Set the step size.
	 *
	 * @param stepSize the current value
	 */
	public void setStepSize (double stepSize)
	{
		this.stepSize = stepSize;
		resetCurrentValue();
	}
	
	public void setDragChanges (boolean update)
	{
		dragEvents = update;
	}
	
	/**
	 * Shift to the left (smaller value).
	 *
	 * @param numSteps the number of steps to shift
	 */
	public void shiftLeft (int numSteps)
	{
		setCurrentValue(getCurrentValue() - numSteps * stepSize);
	}

	/**
	 * Shift to the right (greater value).
	 *
	 * @param numSteps the number of steps to shift
	 */
	public void shiftRight (int numSteps)
	{
		setCurrentValue(getCurrentValue() + numSteps * stepSize);
	}

	/**
	 * Get the percentage of the knob's position relative to the size of
the line.
	 * The return value will be between 0.0 and 1.0.
	 *
	 * @return the current percent complete
	 */
	protected double getKnobPercent ()
	{
		// If we have no range
		if (maxValue <= minValue)
			return 0;

		// Calculate the relative progress
		double percent = (curValue - minValue) / (maxValue - minValue);
		return Math.max(0.0, Math.min(1.0, percent));
	}

	/**
	 * This method is called immediately after a widget becomes attached to the
	 * browser's document.
	 */
	@Override
	protected void onLoad ()
	{
		// Reset the position attribute of the parent element
		DOM.setStyleAttribute(getElement(), "position",	"relative");
	}

	@Override
	protected void onUnload ()
	{
	}

	/**
	 * Highlight this widget.
	 */
/*
	private void highlight ()
	{
		String styleName = getStylePrimaryName();
		DOM.setElementProperty(getElement(), "className", styleName + "" + styleName + "-focused");
	}
*/
	/**
	 * Reset the progress to constrain the progress to the current range and
	 * redraw the knob as needed.
	 */
	private void resetCurrentValue ()
	{
		lastEventValue = minValue - 1;
		setCurrentValue(getCurrentValue());
	}

	/**
	 * Slide the knob to a new location.
	 *
	 * @param event the mouse event
	 */
	private void slideKnob (Event event)
	{
		if (horizontal)
		{
			int x = DOM.eventGetClientX(event);
			if (x > 0)
			{
				int lineWidth = DOM.getElementPropertyInt(lineElement, "offsetWidth");
				int lineLeft = DOM.getAbsoluteLeft(lineElement);
				double percent = (double) (x - lineLeft) / lineWidth * 1.0;
				setCurrentValue(getTotalRange() * percent + minValue, true);
			}
		}
		else
		{
			int y = DOM.eventGetClientY(event);
			if (y > 0)
			{
				int lineHeight = DOM.getElementPropertyInt(lineElement, "offsetHeight");
				int lineTop = DOM.getAbsoluteTop(lineElement);
				double percent = (double) (y - lineTop) / lineHeight * 1.0;
				setCurrentValue(getTotalRange() * percent + minValue, true);
			}
		}
	}

	/**
	 * Unhighlight this widget.
	 */
/*
	private void unhighlight ()
	{
		DOM.setElementProperty(getElement(), "className", getStylePrimaryName());
	}
*/
	class ChangeValueEvent extends ChangeEvent
	{
		public double value;
		public ChangeValueEvent (double value)
		{
			this.value = value;
		}
	}
	
	class ChangeValueHandler implements ChangeHandler
	{
		public void onChange (ChangeEvent event)
		{
			onValueChange((ChangeValueEvent)event);
		}
		
		public void onValueChange (ChangeValueEvent event)
		{
			// to be overridden
		}
	}
	
}

