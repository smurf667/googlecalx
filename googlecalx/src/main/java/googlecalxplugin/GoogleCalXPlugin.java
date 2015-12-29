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
import devplugin.Marker;
import devplugin.Plugin;
import devplugin.PluginInfo;
import devplugin.Program;
import devplugin.ProgramReceiveTarget;
import devplugin.SettingsTab;
import devplugin.ThemeIcon;
import devplugin.Version;

/**
 * TV-Browser plugin for exporting programs to Google Calendar.
 */
public class GoogleCalXPlugin extends Plugin {

	private static Version VERSION;

	protected static final String MSG_PLUGIN_NAME = "pluginName";
	protected static final String MSG_PLUGIN_DESCRIPTION = "description";
	protected static final String MSG_EXPORT_TEXT = "exportText";
	protected static final String MSG_CALENDAR_ID = "calendarId";
	protected static final String MSG_CALENDAR_TARGET = "exportTarget";
	protected static final String MSG_SHOW_CALENDAR_ID = "showCalendarId";
	protected static final String MSG_SHOW_EXPORT_SUCCESS = "showExportSuccess";
	protected static final String MSG_NO_CALENDAR = "noCalendar";
	protected static final String MSG_NOTIFICATION_TIME = "notificationTime";
	protected static final String MSG_NOTIFICATION_TYPE = "notificationType";
	protected static final String MSG_NOTIFICATION_TITLE = "notificationTitle";
	protected static final String MSG_NOTIFICATION_BODY = "notificationBody";
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
	protected static final String MSG_CHOOSE_CALENDAR = "chooseCalendar";
	protected static final String MSG_UNMARK = "unmark";
	protected static final String MSG_CLEAR_CREDENTIALS = "clearCreds";
	protected static final String MSG_CREDENTIALS = "creds";
	protected static final String MSG_R_U_SURE = "sure";
	
	private static final Localizer localizer = Localizer.getLocalizerFor(GoogleCalXPlugin.class);
	private static PluginInfo pluginInfo;
	protected static final String PLUGIN_ID = "googlecalx";
	
	private final Map<Program, ExportAction> actionsCache;
	private final ImageIcon exportIcon;
	private final ProgramReceiveTarget exportReceiveTarget;
	protected GoogleCalXSettings settings;
	protected CalendarAccess calendarAccess;

	/**
	 * Returns the version of the plugin. Read by TV-Browser with some reflection magic.
	 * @return the version of the plugin.
	 */
	public static Version getVersion() {
		if (VERSION == null) {
			final Properties props = new Properties();
			int[] vs = { 0, 0, 0 };
			boolean stable = false;
			try {
				props.load(GoogleCalXPlugin.class.getResourceAsStream("/META-INF/maven/de.engehausen/googlecalx/pom.properties"));
				final String pomVersion = props.getProperty("version");
				stable = !pomVersion.contains("SNAPSHOT");
				final StringTokenizer tok = new StringTokenizer(pomVersion, ".");
				int i = 0;
				while (i < 3 && tok.hasMoreTokens()) {
					final String str = tok.nextToken();
					final int cut = str.indexOf('-');
					if (cut > 0) {
						vs[i++] = Integer.parseInt(str.substring(0, cut));
						break;
					} else {
						vs[i++] = Integer.parseInt(str);
					}
				}
			} catch (IOException e) {
				stable = false;
			}
			VERSION = new Version(vs[0], vs[1], vs[2], stable);
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
		exportReceiveTarget = new ProgramReceiveTarget(this, localizer.msg(MSG_PLUGIN_NAME, "Google calendar export"), "googleCalX");
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
					"Jan Engehausen, smurf667@gmail.com",
					"Apache License, Version 2.0",
					"https://code.google.com/p/googlecalx/wiki/SettingsHowTo");
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
		if (isMarkedByPlugin(program)) {
			final GoogleCalXPlugin plugin = this;
			return new ActionMenu(new AbstractAction(localizer.msg(MSG_UNMARK, "Unmark Google Calendar export"), exportIcon) {
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(final ActionEvent e) {
					program.unmark(plugin);
				}
			});
		} else if (program.getUniqueID() != null) { // circumvent issue in 3.4.2 - see http://hilfe.tvbrowser.org/viewtopic.php?f=42&t=17003&p=113370
			Action action = actionsCache.get(program);
			if (action == null) {
				final String calendarId = settings.getCalendarId();
				if (calendarId != null && calendarId.indexOf('@') > 0) {
					final ExportAction export = new ExportAction(program, this);
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
		} else {
			return null;
		}
	}

	/**
	 * Clears the actions cache. Can be invoked e.g. when
	 * preferences change.
	 */
	protected void resetActionsCache() {
		actionsCache.clear();
	}

	/**
	 * Checks whether the program was marked by this plugin.
	 * @param program the program to check, must not be <code>null</code>.
	 * @return <code>true</code> if the program was marked by the plugin, <code>false</code> otherwise.
	 */
	protected boolean isMarkedByPlugin(final Program program) {
		final Marker[] markers = program.getMarkerArr();
		if (markers != null) {
			final String id = getId();
			for (Marker marker : markers) {
				if (id.equals(marker.getId())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SettingsTab getSettingsTab() {
		return new GoogleCalXPreferences(getParentFrame(), this, calendarAccess, localizer, createImageIcon("apps", "office-calendar", 16));
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
	 * {@inheritDoc}
	 */
	public boolean canReceiveProgramsWithTarget() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean receivePrograms(final Program[] programArr, final ProgramReceiveTarget receiveTarget) {
		if (exportReceiveTarget.equals(receiveTarget)) {
			if (programArr != null) {
				for (Program program : programArr) {
					try {
						calendarAccess.addEvent(
							calendarAccess.createEvent(program)
						);
						program.mark(this);
					} catch (IOException ex) {
						ErrorHandler.handle(localizer.msg(MSG_ERROR, "Service call error"), ex);
					}
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public ProgramReceiveTarget[] getProgramReceiveTargets() {
		return new ProgramReceiveTarget[] { exportReceiveTarget };
	}

	/**
	 * Action to export a program.
	 */
	private static class ExportAction extends AbstractAction {
		
		private static final long serialVersionUID = 1L;

		private final Program program;
		private final GoogleCalXPlugin plugin;
		
		protected ExportAction(final Program prog, final GoogleCalXPlugin parent) {
			super(getLabel(parent.settings));
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
							if (plugin.settings.getShowExportSuccess()) {
								JOptionPane.showMessageDialog(
									plugin.getParentFrame(),
									localizer.msg(MSG_EXPORT_OK, "Export to calendar was successful."),
									localizer.msg(MSG_SUCCESS, "Success!"),
									JOptionPane.INFORMATION_MESSAGE);
							}
						} catch (IOException ex) {
							ErrorHandler.handle(localizer.msg(MSG_ERROR, "Service call error"), ex);
						}
					}
				});
			}
		}

		/**
		 * Returns the text for the export action.
		 * @param settings the settings of the plugin, must not be <code>null</code>.
		 * @return the display label for the export action.
		 */
		private static String getLabel(final GoogleCalXSettings settings) {
			if (settings.getShowCalendarId()) {
				String label = null;
				final CalendarTarget target = settings.getCalendarTarget();
				if (target != null) {
					label = target.getLabel();
				}
				if (label == null || label.isEmpty()) {
					label = settings.getCalendarId();
				}
				return localizer.msg(MSG_EXPORT_TEXT, "Export to {0}", label);
			}
			return localizer.msg(MSG_NO_CALENDAR, "Export to Google Calendar...");
		}
	}
}
