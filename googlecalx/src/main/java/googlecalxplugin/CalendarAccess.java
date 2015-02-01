package googlecalxplugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.ZipInputStream;

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
import com.google.api.services.calendar.model.ColorDefinition;
import com.google.api.services.calendar.model.Colors;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Event.Reminders;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;

import de.engehausen.tvbrowser.CalAccess;
import devplugin.Program;

/**
 * Google Calendar access.
 */
public class CalendarAccess {

	private static final String STR_ENTER = "Enter";
	private Calendar client;
	private final GoogleCalXSettings settings;
	private final HttpTransport httpTransport;
	private final JsonFactory jsonFactory;
	private final FileDataStoreFactory fileDataStoreFactory;

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
	 * Inserts the given event into the calendar. The calendar ID supplied by
	 * {@link GoogleCalXSettings#getCalendarId()} is used.
	 * @param event the event to insert into the calendar, must not be <code>null</code>
	 * @return the inserted event
	 * @throws IOException in case of error
	 */
	public Event addEvent(final Event event) throws IOException {
		return getCalendar().events().insert(settings.getCalendarId(), event).execute();
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
		event.setSummary(program.getChannel().getName()+": "+program.getTitle());
		String desc = program.getShortInfo();
		if (desc == null) {
			desc = program.getDescription();
		}
		if (desc != null) {
			event.setDescription(desc);
		}
		if (!settings.getUseDefaults()) {
			final Reminders reminders = new Reminders();
			reminders.setUseDefault(Boolean.FALSE);
			final EventReminder r = new EventReminder();
			r.setMinutes(Integer.valueOf(settings.getNotificationTime()));
			r.setMethod(settings.getNotificationType().name());
			reminders.setOverrides(Collections.singletonList(r));
			event.setReminders(reminders);
			event.setColorId(settings.getNotificationColor().getId());
		}
		// TODO seems there's a time offset problem for me for UK data (GMT+1 vs. UTC?)
		final TimeZone timeZone = program.getChannel().getTimeZone();
		final devplugin.Date programDate = program.getDate();
		final java.util.Calendar jcal = java.util.Calendar.getInstance(timeZone);
		jcal.setTimeInMillis(0);
		jcal.set(java.util.Calendar.YEAR, programDate.getYear());
		jcal.set(java.util.Calendar.MONTH, programDate.getMonth()-1);
		jcal.set(java.util.Calendar.DAY_OF_MONTH, programDate.getDayOfMonth());
		jcal.set(java.util.Calendar.HOUR_OF_DAY, program.getHours());
		jcal.set(java.util.Calendar.MINUTE, program.getMinutes());
		final Date startDate = jcal.getTime();
		final Date endDate = new Date(startDate.getTime() + program.getLength()*60L*1000L);
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
			final ZipInputStream zis = new ZipInputStream(CalAccess.class.getResourceAsStream("/googlecalxplugin/json"));
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
					Collections.singleton(CalendarScopes.CALENDAR)).setDataStoreFactory(fileDataStoreFactory).build();
			final Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
			client = new Calendar.Builder(
					httpTransport, 
					jsonFactory, 
					credential).setApplicationName(GoogleCalXPlugin.PLUGIN_ID).build();
			if (client == null) {
				throw new IllegalStateException("Cannot build client");
			}
		}
		return client;
	}

}
