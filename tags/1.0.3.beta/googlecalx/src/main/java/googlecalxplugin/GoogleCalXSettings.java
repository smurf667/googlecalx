package googlecalxplugin;

import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import devplugin.Program;
import devplugin.ProgramFieldType;
import util.settings.PropertyBasedSettings;

/**
 * Settings accessor helper.
 */
public class GoogleCalXSettings extends PropertyBasedSettings {
	
	private static final String PROP_CALENDAR_ID = "calendarId";
	private static final String PROP_SHOW_CALENDAR_ID = "idInContextMenu";
	private static final String PROP_NOTIFICATION_TIME = "ntime";
	private static final String PROP_NOTIFICATION_TYPE = "ntype";
	private static final String PROP_NOTIFICATION_COLOR = "ncolor";
	private static final String PROP_NOTIFICATION_TITLE = "ntitle";
	private static final String PROP_NOTIFICATION_BODY = "nbody";
	
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
	 * Indicates whether or not to show the calendar ID in the
	 * context menu export action text.
	 * @return <code>true</code> if the calendar ID is to be shown.
	 */
	public boolean getShowCalendarId() {
		return Boolean.parseBoolean(get(PROP_SHOW_CALENDAR_ID, Boolean.toString(true)));
	}

	/**
	 * Sets whether or not to show the calendar ID in the
	 * context menu export action text.
	 * @param flag <code>true</code> to show the calendar ID in the action text.
	 */
	public void setShowCalendarId(final boolean flag) {
		set(PROP_SHOW_CALENDAR_ID, Boolean.toString(flag));
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
	 * Returns the notification title. May include place holders, see {@link #setNotificationTitle(String)}.
	 * @return the notification title, never <code>null</code>
	 */
	public String getNotificationTitle() {
		return get(PROP_NOTIFICATION_TITLE, "{channel.name}: {title}");
	}

	/**
	 * Sets the notification title. May use place holders in curly
	 * braces; these are accessed as bean properties of the {@link Program}.
	 * Special case: Text-based {@link ProgramFieldType} fields - these
	 * are no bean properties and can be directly accessed using the type name.
	 * @param title the title to set, must not be <code>null</code>
	 */
	public void setNotificationTitle(final String title) {
		set(PROP_NOTIFICATION_TITLE, title);
	}

	/**
	 * Returns the notification body. May include place holders, see {@link #setNotificationTitle(String)}.
	 * Recommended values: <code>{shortInfo}</code> or <code>{description}</code>.
	 * @return the notification body, never <code>null</code>
	 */
	public String getNotificationBody() {
		return get(PROP_NOTIFICATION_BODY, "{shortInfo}");
	}

	/**
	 * Sets the notification title. May use place holders in curly
	 * braces; these are accessed as bean properties of the {@link Program}.
	 * Special case: Text-based {@link ProgramFieldType} fields - these
	 * are no bean properties and can be directly accessed using the type name.
	 * @param body the body to set, must not be <code>null</code>
	 */
	public void setNotificationBody(final String body) {
		set(PROP_NOTIFICATION_BODY, body);
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
