package googlecalxplugin;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import util.exc.ErrorHandler;
import util.ui.EnhancedPanelBuilder;
import util.ui.Localizer;

import com.google.api.services.calendar.model.ColorDefinition;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormSpecs;

import devplugin.SettingsTab;

/**
 * UI layer for the settings of the plugin.
 */
public class GoogleCalXPreferences implements SettingsTab, ActionListener {

	private final Component parent;
	private final GoogleCalXPlugin plugin;
	private final CalendarAccess calendarAccess;
	private final Localizer localizer;
	private final Icon icon;
	private JTextField fieldCalendarId;
	private CalendarTargetField fieldCalendarTarget;
	private JCheckBox fieldShowCalendarId;
	private JTextField fieldNotificationTime;
	private JTextField fieldNotificationTitle;
	private JTextField fieldNotificationBody;
	private JComboBox<NotificationTypes> fieldNotificationType;
	private JButton pickNotificationColor;
	private JButton pickCalendarTarget;
	private JButton clearCredentials;
	private NotificationColor notificationColor;

	/**
	 * Creates the preferences view (plugin.settings tab).
	 * @param comp the parent component (frame), must not be <code>null</code>.
	 * @param aPlugin the plugin holding the plugin.settings, must not be <code>null</code>.
	 * @param anAccess the calendar accessor, must not be <code>null</code>.
	 * @param aLocalizer the localizer for messages, must not be <code>null</code>.
	 * @param anIcon an icon; may be <code>null</code>
	 */
	public GoogleCalXPreferences(final Component comp, final GoogleCalXPlugin aPlugin, final CalendarAccess anAccess, final Localizer aLocalizer, final Icon anIcon) {
		parent = comp;
		plugin = aPlugin;
		calendarAccess = anAccess;
		localizer = aLocalizer;
		icon = anIcon;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JPanel createSettingsPanel() {
		final EnhancedPanelBuilder pb = new EnhancedPanelBuilder(
				FormSpecs.RELATED_GAP_COLSPEC.encode() + ","
						+ FormSpecs.PREF_COLSPEC.encode() + ","
						+ FormSpecs.RELATED_GAP_COLSPEC.encode()
						+ ",default:grow,"
						+ FormSpecs.RELATED_GAP_COLSPEC.encode() + ","
						+ FormSpecs.PREF_COLSPEC.encode());
		final CellConstraints cc = new CellConstraints();
		
		fieldCalendarId = new JTextField(plugin.settings.getCalendarId());
		fieldCalendarId.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(final KeyEvent e) {
				handleCalendarTarget();
			}
		});
		pb.addRow();
		pb.addLabel(localizer.msg(GoogleCalXPlugin.MSG_CALENDAR_ID, "Calendar ID (eMail)") + ':', cc.xy(2, pb.getRow()));
		pb.add(fieldCalendarId, cc.xyw(4, pb.getRow(), pb.getColumnCount() - 3));
		CalendarTarget target = plugin.settings.getCalendarTarget();
		if (target == null) {
			final String defaultID = plugin.settings.getCalendarId();
			if (defaultID != null) {
				target = new CalendarTarget(defaultID, defaultID);
			} else {
				target = new CalendarTarget("", "");
			}
		}
		fieldCalendarTarget = new CalendarTargetField(target);
		pickCalendarTarget = new JButton(localizer.msg(GoogleCalXPlugin.MSG_SELECT, "select"));
		pickCalendarTarget.addActionListener(this);
		JPanel temp = new JPanel(new FlowLayout(FlowLayout.LEFT));
		temp.add(fieldCalendarTarget);
		temp.add(pickCalendarTarget);
		pb.addRow();
		pb.addLabel(localizer.msg(GoogleCalXPlugin.MSG_CALENDAR_TARGET, "Export target") + ':', cc.xy(2, pb.getRow()));
		pb.add(temp, cc.xyw(4, pb.getRow(), pb.getColumnCount() - 3));
		
		fieldShowCalendarId = new JCheckBox();
		fieldShowCalendarId.setSelected(plugin.settings.getShowCalendarId());
		pb.addRow();
		pb.addLabel(localizer.msg(GoogleCalXPlugin.MSG_SHOW_CALENDAR_ID, "Show ID in export action") + ':', cc.xy(2, pb.getRow()));
		pb.add(fieldShowCalendarId, cc.xyw(4, pb.getRow(), pb.getColumnCount() - 3));

		fieldNotificationTitle = new JTextField(plugin.settings.getNotificationTitle());
		pb.addRow();
		pb.addLabel(localizer.msg(GoogleCalXPlugin.MSG_NOTIFICATION_TITLE, "Notification title") + ':', cc.xy(2, pb.getRow()));
		pb.add(fieldNotificationTitle, cc.xyw(4, pb.getRow(), pb.getColumnCount() - 3));

		fieldNotificationBody = new JTextField(plugin.settings.getNotificationBody());
		pb.addRow();
		pb.addLabel(localizer.msg(GoogleCalXPlugin.MSG_NOTIFICATION_BODY, "Notification body") + ':', cc.xy(2, pb.getRow()));
		pb.add(fieldNotificationBody, cc.xyw(4, pb.getRow(), pb.getColumnCount() - 3));

		fieldNotificationType = new JComboBox<NotificationTypes>(NotificationTypes.values());
		fieldNotificationType.setSelectedItem(plugin.settings.getNotificationType());
		fieldNotificationType.addActionListener(this);
		pb.addRow();
		pb.addLabel(localizer.msg(GoogleCalXPlugin.MSG_NOTIFICATION_TYPE, "Notification type") + ':', cc.xy(2, pb.getRow()));
		pb.add(fieldNotificationType, cc.xyw(4, pb.getRow(), pb.getColumnCount() - 3));

		fieldNotificationTime = new JTextField(plugin.settings.getNotificationTime());
		pb.addRow();
		pb.addLabel(localizer.msg(GoogleCalXPlugin.MSG_NOTIFICATION_TIME, "Notification time (minutes)") + ':', cc.xy(2, pb.getRow()));
		pb.add(fieldNotificationTime, cc.xyw(4, pb.getRow(), pb.getColumnCount() - 3));
		
		pickNotificationColor = new JButton(localizer.msg(GoogleCalXPlugin.MSG_SELECT, "select"));
		pickNotificationColor.addActionListener(this);
		pb.addRow();
		pb.addLabel(localizer.msg(GoogleCalXPlugin.MSG_NOTIFICATION_COLOR, "Notification color") + ':', cc.xy(2, pb.getRow()));
		notificationColor = new NotificationColor(localizer.msg(GoogleCalXPlugin.MSG_EXAMPLE, "example"), plugin.settings.getNotificationColor());
		temp = new JPanel(new FlowLayout(FlowLayout.LEFT));
		temp.add(notificationColor);
		temp.add(pickNotificationColor);
		pb.add(temp, cc.xyw(4, pb.getRow(), pb.getColumnCount() - 3));

		pb.addRow();
		pb.add(new JSeparator(), cc.xyw(4, pb.getRow(), pb.getColumnCount() - 3));

		clearCredentials = new JButton(localizer.msg(GoogleCalXPlugin.MSG_CLEAR_CREDENTIALS, "clear"));
		clearCredentials.addActionListener(this);
		pb.addRow();
		pb.addLabel(localizer.msg(GoogleCalXPlugin.MSG_CREDENTIALS, "Calender credentials") + ':', cc.xy(2, pb.getRow()));
		pb.add(clearCredentials, cc.xyw(4, pb.getRow(), pb.getColumnCount() - 3));

		handleDependencies();
		handleCalendarTarget();

		return pb.getPanel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Icon getIcon() {
		return icon;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTitle() {
		return localizer.msg(GoogleCalXPlugin.MSG_PLUGIN_NAME, "Google Calendar export");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void saveSettings() {
		plugin.settings.setCalendarId(fieldCalendarId.getText());
		plugin.settings.setCalendarTarget(fieldCalendarTarget.getCalendarTarget());
		plugin.settings.setShowCalendarId(fieldShowCalendarId.isSelected());
		plugin.settings.setNotificationTitle(fieldNotificationTitle.getText());
		plugin.settings.setNotificationBody(fieldNotificationBody.getText());
		plugin.settings.setNotificationType((NotificationTypes) fieldNotificationType.getSelectedItem());
		plugin.settings.setNotificationTime(fieldNotificationTime.getText());
		plugin.settings.setNotificationColor(notificationColor.getColor());
		plugin.resetActionsCache();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent e) {
		final Object source = e.getSource();
		if (source == fieldNotificationType) {
			handleDependencies();
		} else if (source == clearCredentials) {
			if (JOptionPane.showConfirmDialog(parent, localizer.msg(GoogleCalXPlugin.MSG_R_U_SURE, "Are you sure?"), localizer.msg(GoogleCalXPlugin.MSG_CLEAR_CREDENTIALS, "clear"), JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {
				calendarAccess.deleteCredentials();
			}
		} else if (source == pickCalendarTarget) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					try {
						final List<CalendarTarget> targets = calendarAccess.getCalendarTargets();
						if (targets.size() > 0) {
							final CalendarTarget[] possibleValues = targets.toArray(new CalendarTarget[targets.size()]);
							final Object selectedValue = JOptionPane.showInputDialog(
									parent,
									localizer.msg(GoogleCalXPlugin.MSG_CHOOSE_CALENDAR, "Choose calendar"),
									localizer.msg(GoogleCalXPlugin.MSG_SELECT, "select"),
									JOptionPane.INFORMATION_MESSAGE,
									null,
									possibleValues, possibleValues[0]);
							if (selectedValue != null) {
								fieldCalendarTarget.setCalendarTarget((CalendarTarget) selectedValue);
							}
						}
					} catch (IOException ex) {
						ErrorHandler.handle(localizer.msg(GoogleCalXPlugin.MSG_ERROR, "Service call error"), ex);
					}
				}
			});
		} else if (source == pickNotificationColor) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					try {
						final List<CalendarColor> colors = new ArrayList<CalendarColor>();
						for (Map.Entry<String, ColorDefinition> entry : calendarAccess.getCalendarColors()) {
							colors.add(new CalendarColor(entry.getKey(), entry.getValue()));
						}
						Collections.sort(colors);
						notificationColor.setColor(NotificationColor.pickColor(parent, localizer.msg(GoogleCalXPlugin.MSG_CHOOSE_COLOR, "Choose color"), colors));
					} catch (IOException ex) {
						ErrorHandler.handle(localizer.msg(GoogleCalXPlugin.MSG_ERROR, "Service call error"), ex);
					}
				}
			});
		}
	}

	/**
	 * Enables or disables components dependent on whether custom notifications should be used.
	 */
	protected void handleDependencies() {
		final Object selected = fieldNotificationType.getSelectedItem();
		final boolean flag = 
				!(NotificationTypes.none.equals(selected) ||
				NotificationTypes.defaults.equals(selected));
		fieldNotificationTime.setEnabled(flag);
		pickNotificationColor.setEnabled(flag);
		notificationColor.setEnabled(flag);
	}

	/**
	 * Enables/disables the calendar target choose based on the
	 * validity of the main calendar ID.
	 */
	protected void handleCalendarTarget() {
		final String text = fieldCalendarId.getText();
		final int l = (text!=null)?text.length()-1:0;
		final boolean flag;
		if (l > 0) {
			final int idx = text.indexOf('@');
			flag = idx > 0 && idx < l;
		} else {
			flag = false;
		}
		pickCalendarTarget.setEnabled(flag);
		if (flag) {
			// if enabled, check that the color picker is enabled too
			handleDependencies();
		} else {
			// if calendarId is not good, do disable color picker
			pickNotificationColor.setEnabled(false);
		}
	}

}
