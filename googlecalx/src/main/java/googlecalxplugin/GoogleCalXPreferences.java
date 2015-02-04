package googlecalxplugin;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
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
	private final GoogleCalXSettings settings;
	private final CalendarAccess calendarAccess;
	private final Localizer localizer;
	private final Icon icon;
	private JTextField fieldCalendarId;
	private JTextField fieldNotificationTime;
	private JTextField fieldNotificationTitle;
	private JTextField fieldNotificationBody;
	private JComboBox<NotificationTypes> fieldNotificationType;
	private JButton pickNotificationColor;
	private NotificationColor notificationColor;

	/**
	 * Creates the preferences view (settings tab).
	 * @param comp the parent component (frame), must not be <code>null</code>.
	 * @param aSettings the plugin settings, must not be <code>null</code>.
	 * @param anAccess the calendar accessor, must not be <code>null</code>.
	 * @param aLocalizer the localizer for messages, must not be <code>null</code>.
	 * @param anIcon an icon; may be <code>null</code>
	 */
	public GoogleCalXPreferences(final Component comp, final GoogleCalXSettings aSettings, final CalendarAccess anAccess, final Localizer aLocalizer, final Icon anIcon) {
		parent = comp;
		settings = aSettings;
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

		fieldCalendarId = new JTextField(settings.getCalendarId());
		pb.addRow();
		pb.addLabel(localizer.msg(GoogleCalXPlugin.MSG_CALENDAR_ID, "Calendar ID (eMail)") + ':', cc.xy(2, pb.getRow()));
		pb.add(fieldCalendarId, cc.xyw(4, pb.getRow(), pb.getColumnCount() - 3));

		fieldNotificationTitle = new JTextField(settings.getNotificationTitle());
		pb.addRow();
		pb.addLabel(localizer.msg(GoogleCalXPlugin.MSG_NOTIFICATION_TITLE, "Notification title") + ':', cc.xy(2, pb.getRow()));
		pb.add(fieldNotificationTitle, cc.xyw(4, pb.getRow(), pb.getColumnCount() - 3));

		fieldNotificationBody = new JTextField(settings.getNotificationBody());
		pb.addRow();
		pb.addLabel(localizer.msg(GoogleCalXPlugin.MSG_NOTIFICATION_BODY, "Notification body") + ':', cc.xy(2, pb.getRow()));
		pb.add(fieldNotificationBody, cc.xyw(4, pb.getRow(), pb.getColumnCount() - 3));

		fieldNotificationType = new JComboBox<NotificationTypes>(NotificationTypes.values());
		fieldNotificationType.setSelectedItem(settings.getNotificationType());
		fieldNotificationType.addActionListener(this);
		pb.addRow();
		pb.addLabel(localizer.msg(GoogleCalXPlugin.MSG_NOTIFICATION_TYPE, "Notification type") + ':', cc.xy(2, pb.getRow()));
		pb.add(fieldNotificationType, cc.xyw(4, pb.getRow(), pb.getColumnCount() - 3));

		fieldNotificationTime = new JTextField(settings.getNotificationTime());
		pb.addRow();
		pb.addLabel(localizer.msg(GoogleCalXPlugin.MSG_NOTIFICATION_TIME, "Notification time (minutes)") + ':', cc.xy(2, pb.getRow()));
		pb.add(fieldNotificationTime, cc.xyw(4, pb.getRow(), pb.getColumnCount() - 3));
		
		pickNotificationColor = new JButton(localizer.msg(GoogleCalXPlugin.MSG_SELECT, "select"));
		pickNotificationColor.addActionListener(this);
		pb.addRow();
		pb.addLabel(localizer.msg(GoogleCalXPlugin.MSG_NOTIFICATION_COLOR, "Notification color") + ':', cc.xy(2, pb.getRow()));
		notificationColor = new NotificationColor(localizer.msg(GoogleCalXPlugin.MSG_EXAMPLE, "example"), settings.getNotificationColor());
		final JPanel temp = new JPanel(new FlowLayout(FlowLayout.LEFT));
		temp.add(notificationColor);
		temp.add(pickNotificationColor);
		pb.add(temp, cc.xyw(4, pb.getRow(), pb.getColumnCount() - 3));

		handleDependencies();

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
		settings.setCalendarId(fieldCalendarId.getText());
		settings.setNotificationTitle(fieldNotificationTitle.getText());
		settings.setNotificationBody(fieldNotificationBody.getText());
		settings.setNotificationType((NotificationTypes) fieldNotificationType.getSelectedItem());
		settings.setNotificationTime(fieldNotificationTime.getText());
		settings.setNotificationColor(notificationColor.getColor());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent e) {
		final Object source = e.getSource();
		if (source == fieldNotificationType) {
			handleDependencies();
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

}
