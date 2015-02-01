package googlecalxplugin;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.WeakHashMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import util.exc.ErrorHandler;
import util.ui.Localizer;
import devplugin.ActionMenu;
import devplugin.Plugin;
import devplugin.PluginInfo;
import devplugin.Program;
import devplugin.SettingsTab;
import devplugin.ThemeIcon;
import devplugin.Version;

/**
 * TV-Browser plugin for exporting programs to Google Calendar.
 */
public class GoogleCalXPlugin extends Plugin {

	private static Version VERSION;// = new Version(1,0);

	protected static final String MSG_PLUGIN_NAME = "pluginName";
	protected static final String MSG_PLUGIN_DESCRIPTION = "description";
	protected static final String MSG_EXPORT_TEXT = "exportText";
	protected static final String MSG_CALENDAR_ID = "calendarId";
	protected static final String MSG_USE_DEFAULTS = "useDefaults";
	protected static final String MSG_NO_CALENDAR = "noCalendar";
	protected static final String MSG_NOTIFICATION_TIME = "notificationTime";
	protected static final String MSG_NOTIFICATION_TYPE = "notificationType";
	protected static final String MSG_SETTINGS = "settings";
	protected static final String MSG_ERROR = "svcError";
	protected static final String MSG_SUCCESS = "success";
	protected static final String MSG_EXPORT_OK = "exportComplete";
	protected static final String MSG_SET_CALENDAR = "setCalendar";
	protected static final String MSG_CONFIGURE = "configure";
	protected static final String MSG_SELECT = "select";
	protected static final String MSG_EXAMPLE = "example";
	protected static final String MSG_NOTIFICATION_COLOR = "notificationColor";
	protected static final String MSG_CHOOSE_COLOR = "chooseColor";
	
	private static final Localizer localizer = Localizer.getLocalizerFor(GoogleCalXPlugin.class);
	private static PluginInfo pluginInfo;
	protected static final String PLUGIN_ID = "googlecalx";
	
	private final Map<Program, ExportAction> actionsCache;
	private final ImageIcon exportIcon;
	protected GoogleCalXSettings settings;
	protected CalendarAccess calendarAccess;

	/**
	 * Returns the version of the plugin. Read by TV-Browser with some reflection magic.
	 * @return the version of the plugin.
	 */
	public static Version getVersion() {
		if (VERSION == null) {
			final Properties props = new Properties();
			try {
				props.load(GoogleCalXPlugin.class.getResourceAsStream("/META-INF/maven/de.engehausen/googlecalx/pom.properties"));
				final String pomVersion = props.getProperty("version");
				final StringTokenizer tok = new StringTokenizer(pomVersion, ".");
				final int major = Integer.parseInt(tok.nextToken());
				String suffix = tok.nextToken();
				final int cut = suffix.indexOf('-');
				if (cut > 0) {
					suffix = suffix.substring(0, cut);
				}
				final int minor = Integer.parseInt(suffix);
				final boolean stable = !pomVersion.contains("SNAPSHOT");
				VERSION = new Version(major, minor, stable);
			} catch (IOException e) {
				VERSION = new Version(0, 0, false);
			}
		}
		return VERSION;
	}

	/**
	 * Creates the plugin.
	 */
	public GoogleCalXPlugin() {
		super();
		actionsCache = new WeakHashMap<Program, ExportAction>();
		exportIcon = createImageIcon("apps", "office-calendar");
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public PluginInfo getInfo() {
		if (pluginInfo == null) {
			pluginInfo = new PluginInfo(
					GoogleCalXPlugin.class, 
					localizer.msg(MSG_PLUGIN_NAME, "Google calendar export"),
					localizer.msg(MSG_PLUGIN_DESCRIPTION, "Exports a program into your Google calendar."), 
					"Jan Engehausen, smurf667@gmail.com");
		}
		return pluginInfo;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ThemeIcon getMarkIconFromTheme() {
		return new ThemeIcon("apps", "office-calendar");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionMenu getContextMenuActions(final Program program) {
		Action action = actionsCache.get(program);
		if (action == null) {
			final String calendarId = settings.getCalendarId();
			if (calendarId != null && calendarId.contains("@")) {
				final ExportAction export = new ExportAction(program, calendarId, this);
				export.putValue(Action.SMALL_ICON, exportIcon);
				actionsCache.put(program, export);
				action = export;
			} else {
				action = new AbstractAction(localizer.msg(MSG_NO_CALENDAR, "Export to Google Calendar...")) {
					private static final long serialVersionUID = 666L;
					@Override
					public void actionPerformed(final ActionEvent e) {
						JOptionPane.showMessageDialog(getParentFrame(), localizer.msg(MSG_SET_CALENDAR, "Please set your calendar in the settings"), localizer.msg(MSG_CONFIGURE, "Please configure"), JOptionPane.INFORMATION_MESSAGE);
					}
				};
			}
		}
		return new ActionMenu(action);
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public SettingsTab getSettingsTab() {
		return new GoogleCalXPreferences(getParentFrame(), settings, calendarAccess, localizer, createImageIcon("apps", "office-calendar", 16));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Properties storeSettings() {
		return settings.storeSettings();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void loadSettings(final Properties properties) {
		settings = new GoogleCalXSettings(properties);
		try {
			calendarAccess = new CalendarAccess(
					settings,
					getPluginManager().getTvBrowserSettings().getTvBrowserUserHome(),
					null);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Action to export a program.
	 */
	private static class ExportAction extends AbstractAction {
		
		private static final long serialVersionUID = 1L;

		private final Program program;
		private final GoogleCalXPlugin plugin;
		
		protected ExportAction(final Program prog, final String calId, final GoogleCalXPlugin parent) {
			super(localizer.msg(MSG_EXPORT_TEXT, "Export to {0}", calId));
			program = prog;
			plugin = parent;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (plugin.calendarAccess != null) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						try {
							plugin.calendarAccess.addEvent(
									plugin.calendarAccess.createEvent(program)
									);
							program.mark(plugin);
							JOptionPane.showMessageDialog(plugin.getParentFrame(), localizer.msg(MSG_EXPORT_OK, "Export to calendar was successful."), localizer.msg(MSG_SUCCESS, "Success!"), JOptionPane.INFORMATION_MESSAGE);
						} catch (IOException ex) {
							ErrorHandler.handle(localizer.msg(MSG_ERROR, "Service call error"), ex);
						}
					}
				});
			}
		}
		
	}
}
