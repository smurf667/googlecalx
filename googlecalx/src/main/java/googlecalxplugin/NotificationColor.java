package googlecalxplugin;

import java.awt.Color;
import java.awt.Component;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

/**
 * A label demonstrating a {@link CalendarColor}.
 */
public class NotificationColor extends JLabel {

	private static final long serialVersionUID = 1L;

	private CalendarColor color;

	/**
	 * Creates the label with the given text and color.
	 * @param text the text to show, must not be <code>null</code>.
	 * @param aColor the calendar color to use, must not be <code>null</code>.
	 */
	public NotificationColor(final String text, final CalendarColor aColor) {
		super(text);
		setOpaque(true);
		setColor(aColor);
	}

	/**
	 * Returns the calendar color used by the label.
	 * @return the calendar color used by the label, never <code>null</code>.
	 */
	public CalendarColor getColor() {
		return color;
	}

	/**
	 * Sets the foreground and background colors of the label based on the given
	 * calendar color.
	 * @param aColor the calendar color to use, may be <code>null</code> (no change in that case)
	 */
	public void setColor(final CalendarColor aColor) {
		if (aColor != null) {
			color = aColor;
			setBackground(color.getBackground());
			setForeground(color.getForeground());
		}
	}

	/**
	 * Shows a selection of all available calendar colors and lets the user pick one.
	 * @param parent the parent component (frame)
	 * @param title the title of the dialog
	 * @param colors a list of calendar colors, must not be <code>null</code>.
	 * @return the selected calendar color, or <code>null</code>.
	 */
	public static CalendarColor pickColor(final Component parent, final String title, final List<CalendarColor> colors) {
		final NotificationColor[] items = new NotificationColor[colors.size()];
		for (int i = 0; i < items.length; i++) {
			items[i] = new NotificationColor("", colors.get(i));
		}
		final JList<NotificationColor> list = new JList<NotificationColor>(items);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setSelectedIndex(0);
		list.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;
			@Override
			public Component getListCellRendererComponent(JList<?> model,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				final CalendarColor color = ((NotificationColor) value).getColor();
				final NotificationColor result = new NotificationColor(" example", color);
				if (isSelected) {
					result.setBorder(BorderFactory.createLineBorder(Color.WHITE));
				}
				if (cellHasFocus) {
					result.setBackground(result.getBackground().brighter());
				}
				return result;
			}
		});
		if (JOptionPane.showConfirmDialog(
				parent,
				new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
				title,
				JOptionPane.OK_CANCEL_OPTION)
				== 0) {
			return list.getSelectedValue().getColor();
		} else {
			return null;
		}
	}

}