package googlecalxplugin;

import javax.swing.JTextField;

/**
 * Text field that stores a {@link CalendarTarget}.
 */
public class CalendarTargetField extends JTextField {

	private static final long serialVersionUID = 1L;
	
	private CalendarTarget target;

	public CalendarTargetField(final CalendarTarget aTarget) {
		super(aTarget.getLabel(), 40);
		setEnabled(false);
	}
	
	public void setCalendarTarget(final CalendarTarget aTarget) {
		setText(aTarget.getLabel());
		target = aTarget;
	}

	/**
	 * Returns the calendar target of the edit field.
	 * @return the calendar target of the edit field.
	 */
	public CalendarTarget getCalendarTarget() {
		return target;
	}
	
}
