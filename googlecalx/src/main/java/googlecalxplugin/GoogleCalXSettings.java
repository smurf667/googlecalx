package googlecalxplugin;

import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import util.settings.PropertyBasedSettings;

/**
 * Settings accessor helper.
 */
public class GoogleCalXSettings extends PropertyBasedSettings {
	
	private static final String PROP_CALENDAR_ID = "calendarId";
	private static final String PROP_USE_DEFAULTS = "defaults";
	private static final String PROP_NOTIFICATION_TIME = "ntime";
	private static final String PROP_NOTIFICATION_TYPE = "ntype";
	private static final String PROP_NOTIFICATION_COLOR = "ncolor";
	
	private final Map<String, CalendarColor> colorCache = new WeakHashMap<String, CalendarColor>();

	/**
	 * Creates the settings based on properties.
	 * @param properties the properties backing the settings, must not be <code>null</code>.
	 */
	public GoogleCalXSettings(final Properties properties) {
		super(properties);
	}

	/**
	 * Returns the calendar ID.
	 * @return the calendar ID, may be <code>null</codeY.
	 */
	public String getCalendarId() {
		return get(PROP_CALENDAR_ID);
	}

	/**
	 * Sets the calendar ID.
	 * @param id the calendar ID to use.
	 */
	public void setCalendarId(final String id) {
		set(PROP_CALENDAR_ID, id);
	}

	/**
	 * Indicates whether or not to use the calendar defaults
	 * when creating events.
	 * @return <code>true</code> if defaults are to be used
	 */
	public boolean getUseDefaults() {
		boolean result = Boolean.parseBoolean(get(PROP_USE_DEFAULTS, Boolean.toString(true)));
		return result;
	}

	/**
	 * Sets whether or not to use defaults for event creation in the calendar.
	 * @param flag <code>true</code> to use defaults.
	 */
	public void setUseDefaults(final boolean flag) {
		set(PROP_USE_DEFAULTS, Boolean.toString(flag));
	}

	/**
	 * Returns the type of notification to use for event creation in the calendar.
	 * @return the type of notification to use for event creation.
	 */
	public NotificationTypes getNotificationType() {
		return Enum.valueOf(NotificationTypes.class, get(PROP_NOTIFICATION_TYPE, NotificationTypes.email.name()));
	}

	/**
	 * Sets the type of notification to use for event creation in the calendar.
	 * @param type the type of notification to use for event creation in the calendar.
	 */
	public void setNotificationType(final NotificationTypes type) {
		if (type != null) {
			set(PROP_NOTIFICATION_TYPE, type.name());
		}
	}

	/**
	 * Returns the time in minutes to use for reminders in the calendar.
	 * @return the time in minutes to use for reminders in the calendar.
	 */
	public String getNotificationTime() {
		return get(PROP_NOTIFICATION_TIME, "10");
	}

	/**
	 * Sets the time in minutes to use for reminders in the calendar.
	 * @param time the time in minutes to use for reminders in the calendar.
	 */
	public void setNotificationTime(final String time) {
		try {
			Integer.parseInt(time);
			set(PROP_NOTIFICATION_TIME, time);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the calendar color to use for reminders.
	 * @return the calendar color to use for reminders.
	 */
	public CalendarColor getNotificationColor() {
		final String serial = get(PROP_NOTIFICATION_COLOR, "0/0/ffffff");
		CalendarColor color = colorCache.get(serial);
		if (color == null) {
			color = new CalendarColor(serial);
			colorCache.put(serial, color);
		}
		return color;
	}

	/**
	 * Sets the calendar color to use for reminders.
	 * @param color the calendar color to use for reminders.
	 */
	public void setNotificationColor(final CalendarColor color) {
		set(PROP_NOTIFICATION_COLOR, color.toString());
	}

}
