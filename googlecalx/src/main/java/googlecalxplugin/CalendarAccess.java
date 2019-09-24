package googlecalxplugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import org.apache.commons.beanutils.BeanUtils;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.ColorDefinition;
import com.google.api.services.calendar.model.Colors;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Event.Reminders;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;

import devplugin.Program;
import devplugin.ProgramFieldType;

/**
 * Google Calendar access.
 */
public class CalendarAccess {

	private static final String STR_ENTER = "Enter";
	private static final Map<String, ProgramFieldType> STR2PFT;
	private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z_.]+?)\\}");
	
	private Calendar client;
	private final GoogleCalXSettings settings;
	private final HttpTransport httpTransport;
	private final JsonFactory jsonFactory;
	private final FileDataStoreFactory fileDataStoreFactory;

	static {
		final Map<String, ProgramFieldType> result = new HashMap<String, ProgramFieldType>();
		for (Field field : ProgramFieldType.class.getFields()) {
			try {
				final Object value = field.get(null);
				if (value  instanceof ProgramFieldType) {
					result.put(field.getName(), (ProgramFieldType) value);
				}
			} catch (IllegalAccessException e) {
				// ignore field
			}
		}
		STR2PFT = Collections.unmodifiableMap(result);
	}


	/**
	 * Creates the accessor.
	 * @param settings the settings to use, must not be <code>null</code>.
	 * @throws IOException in case of error
	 * @throws GeneralSecurityException in case of error
	 */
	public CalendarAccess(final GoogleCalXSettings settings) throws IOException, GeneralSecurityException {
		this(settings, System.getProperty("user.home"), null);
	}

	/**
	 * Creates the accessor.
	 * @param settings the settings to use, must not be <code>null</code>.
	 * @param directory directory for subfolder <code>.store</code> which will hold the authentication token, must not be <code>null</code>
	 * @param factory a JSON factory, may be <code>null</code>
	 * @throws IOException in case of error
	 * @throws GeneralSecurityException in case of error
	 */
	public CalendarAccess(final GoogleCalXSettings settings, final String directory, final JsonFactory factory) throws IOException, GeneralSecurityException {
		this.settings = settings;
		final File dataStoreDirectory = new File(directory, ".store/"+GoogleCalXPlugin.PLUGIN_ID);
		fileDataStoreFactory = new FileDataStoreFactory(dataStoreDirectory);
		jsonFactory = factory!=null?factory:JacksonFactory.getDefaultInstance();
		httpTransport = GoogleNetHttpTransport.newTrustedTransport();
	}
	
	/**
	 * Deletes all files from the data store directory.
	 */
	public void deleteCredentials() {
		final File directory = fileDataStoreFactory.getDataDirectory();
		boolean deleted = false;
		if (directory != null) {
			for (File file : directory.listFiles()) {
				if (file.isFile()) {
					deleted |= file.delete();
				}
			}
		}
		if (deleted) {
			client = null;
		}
	}

	/**
	 * Lists the calendars that can be used to add events to.
	 * @return a list with possible calendar export targets, never <code>null</code>
	 * @throws IOException in case of error
	 */
	public List<CalendarTarget> getCalendarTargets() throws IOException {
		final List<CalendarTarget> result = new ArrayList<CalendarTarget>();
		String pageToken = null;
		do {
			final CalendarList calendarList = getCalendar().calendarList().list().setPageToken(pageToken).execute();
			final List<CalendarListEntry> items = calendarList.getItems();
			for (CalendarListEntry calendarListEntry : items) {
				result.add(new CalendarTarget(calendarListEntry.getId(), calendarListEntry.getSummary()));
			}
			pageToken = calendarList.getNextPageToken();
		} while (pageToken != null);
		return result;
	}
	
	/**
	 * Inserts the given event into the calendar. The calendar ID supplied by
	 * {@link GoogleCalXSettings#getCalendarTarget()} is used if set, otherwise
	 * {@link GoogleCalXSettings#getCalendarId()} is used.
	 * @param event the event to insert into the calendar, must not be <code>null</code>
	 * @return the inserted event
	 * @throws IOException in case of error
	 */
	public Event addEvent(final Event event) throws IOException {
		final CalendarTarget target = settings.getCalendarTarget();
		String id = null;
		if (target != null) {
			id = target.getId();
		}
		if (id == null || id.length() == 0) {
			id = settings.getCalendarId();
		}
		return getCalendar().events().insert(id, event).execute();
	}

	/**
	 * Returns a set with the color definitions of the calendar.
	 * @return a set with the color definitions of the calendar.
	 * @throws IOException in case of error
	 */
	public Set<Map.Entry<String, ColorDefinition>> getCalendarColors() throws IOException {
		final Colors colors = getCalendar().colors().get().execute();
		return colors.getEvent().entrySet();
	}

	/**
	 * Creates an event out of the given program. The settings are used
	 * for the calendar ID and to determine wether to use the calendar
	 * defaults or the ones configured in the settings.
	 * @param program the program supplying the event information.
	 * @return an event that can be inserted via {@link #addEvent(Event)}
	 */
	public Event createEvent(final Program program) {
		final Event event = new Event();
		event.setSummary(resolve(program, settings.getNotificationTitle()));
		event.setDescription(resolve(program, settings.getNotificationBody()));
		final NotificationTypes notificationType = settings.getNotificationType();
		final Reminders reminders = new Reminders();
		if (!NotificationTypes.none.equals(notificationType)) {
			if (NotificationTypes.defaults.equals(notificationType)) {
				reminders.setUseDefault(Boolean.TRUE);
			} else {
				reminders.setUseDefault(Boolean.FALSE);
				final EventReminder r = new EventReminder();
				r.setMinutes(Integer.valueOf(settings.getNotificationTime()));
				r.setMethod(settings.getNotificationType().name());
				reminders.setOverrides(Collections.singletonList(r));
				event.setColorId(settings.getNotificationColor().getId());
			}
		} else {
			// no reminders at all
			reminders.setUseDefault(Boolean.FALSE);
		}
		event.setReminders(reminders);
		// seems there's a time offset problem for me for UK data (GMT+1 vs. UTC?) - should I use local time zone always?
		//final TimeZone timeZone = program.getChannel().getTimeZone();
		final TimeZone timeZone = TimeZone.getDefault();
		final devplugin.Date programDate = program.getDate();
		final java.util.Calendar jcal = java.util.Calendar.getInstance(timeZone);
		jcal.setTimeInMillis(0);
		jcal.set(java.util.Calendar.YEAR, programDate.getYear());
		jcal.set(java.util.Calendar.MONTH, programDate.getMonth()-1);
		jcal.set(java.util.Calendar.DAY_OF_MONTH, programDate.getDayOfMonth());
		jcal.set(java.util.Calendar.HOUR_OF_DAY, program.getHours());
		jcal.set(java.util.Calendar.MINUTE, program.getMinutes());
		final Date startDate = jcal.getTime();
		final long minutes = Math.max(1L, program.getLength());
		final Date endDate = new Date(startDate.getTime() + minutes*60L*1000L);
		final DateTime start = new DateTime(startDate, timeZone);
		event.setStart(new EventDateTime().setDateTime(start));
		final DateTime end = new DateTime(endDate, timeZone);
		event.setEnd(new EventDateTime().setDateTime(end));
		return event;
	}

	/**
	 * Returns the calendar client.
	 * @return the calendar client.
	 * @throws IOException in case of error
	 */
	protected Calendar getCalendar() throws IOException {
		if (client == null) {
			final ZipInputStream zis = new ZipInputStream(CalendarAccess.class.getResourceAsStream("/googlecalxplugin/json"));
			if (0xb0b61cc5L != zis.getNextEntry().getCrc()) {
				throw new IllegalStateException();
			}
			final GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
				jsonFactory, 
				new InputStreamReader(zis));
			if (clientSecrets.getDetails().getClientId().startsWith(STR_ENTER)
					|| clientSecrets.getDetails().getClientSecret().startsWith(STR_ENTER)) {
				throw new IllegalStateException("Client secrets unavailable or invalid");
			}
			
			final GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				httpTransport,
				jsonFactory,
				clientSecrets,
				Arrays.asList(CalendarScopes.CALENDAR_EVENTS, CalendarScopes.CALENDAR_READONLY)
				).setDataStoreFactory(fileDataStoreFactory).build();
			final Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
			client = new Calendar.Builder(
				httpTransport, 
				jsonFactory, 
				credential).setApplicationName(GoogleCalXPlugin.GOOGLE_APPLICATION_ID).build();
			if (client == null) {
				throw new IllegalStateException("Cannot build client");
			}
		}
		return client;
	}

	/**
	 * Returns a string with all placeholders of the given template resolved.
	 * @param program the program to access properties of, must not be <code>null</code>.
	 * @param template the template with placeholders in curly braces
	 * @return the resolved string
	 */
	protected String resolve(final Program program, final String template) {
		if (template.indexOf('{') < 0) {
			return template;
		}
		final StringBuilder sb = new StringBuilder(128);
		final Matcher matchPattern = PLACEHOLDER.matcher(template);

		int last = 0;
		while (matchPattern.find(last)) {
			final int next = matchPattern.end();
			if (next > last) {
				sb.append(template.substring(last, matchPattern.start()));
			}
			final String key = matchPattern.group(1);
			if (key.endsWith("_TYPE")) {
				final ProgramFieldType type = STR2PFT.get(key);
				if (type != null) {
					if (type.isRightFormat(ProgramFieldType.TEXT_FORMAT)) {
						final String str = program.getTextField(type);
						if (str != null) {
							sb.append(str);
						}
					} else if (type.isRightFormat(ProgramFieldType.INT_FORMAT)) {
						final String str = program.getIntFieldAsString(type);
						if (str != null) {
							sb.append(str);
						}
					} else {
						sb.append(key);
					}
				} else {
					sb.append(key);
				}
			} else {
				try {
					final String value = BeanUtils.getNestedProperty(program, key);
					if (value != null) {
						sb.append(value);
					}
				} catch (IllegalAccessException e) {
					sb.append(key);
				} catch (InvocationTargetException e) {
					sb.append(key);
				} catch (NoSuchMethodException e) {
					sb.append(key);
				}
			}
			last = next;
		}
		if (last < template.length()) {
			sb.append(template.substring(last));
		}
		return sb.toString();
	}

}
