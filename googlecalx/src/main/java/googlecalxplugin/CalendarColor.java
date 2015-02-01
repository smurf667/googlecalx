package googlecalxplugin;

import java.awt.Color;
import java.util.StringTokenizer;

import com.google.api.services.calendar.model.ColorDefinition;

/**
 * A calendar color. Consists of an ID, foreground and background color.
 */
public class CalendarColor implements Comparable<CalendarColor> {
	
	private String id;
	private Color background;
	private Color foreground;
	
	/**
	 * Creates a color based on the representation returned by {@link #toString()}.
	 * @param serialForm the serial form of the color, must not be <code>null</code>.
	 */
	public CalendarColor(final String serialForm) {
		final StringTokenizer tok = new StringTokenizer(serialForm, "/");
		id = tok.nextToken();
		foreground = toColor(tok.nextToken());
		background = toColor(tok.nextToken());
	}

	/**
	 * Creates a color based on the color definition supplied by the calendar.
	 * @param anId the ID of the color, must not be <code>null</code>
	 * @param def the color definition, must not be <code>null</code>
	 */
	public CalendarColor(final String anId, final ColorDefinition def) {
		id = anId;
		foreground = toColor(def.getForeground());
		background = toColor(def.getBackground());
	}
	
	private Color toColor(final String str) {
		if (str == null) {
			return Color.WHITE;
		} else {
			try {
				if (str.charAt(0) == '#') {
					return new Color(Integer.parseInt(str.substring(1), 16));
				} else {
					return new Color(Integer.parseInt(str, 16));
				}
			} catch (NumberFormatException e) {
				return Color.RED;
			}
		}
	}

	/**
	 * Returns a String representation of this color in the form <i>id/rgb-hex-foreground/rgb-hex-background</i>.
	 * @return a string representation, never <code>null</code>
	 */
	public String toString() {
		final StringBuilder sb = new StringBuilder(48);
		sb.append(id)
			.append('/').append(Integer.toHexString(foreground.getRGB()&0xffffff))
			.append('/').append(Integer.toHexString(background.getRGB()&0xffffff));
		return sb.toString();
	}

	/**
	 * Returns the ID of the calendar color.
	 * @return the ID of the calendar color.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the background of the calendar color.
	 * @return the background of the calendar color.
	 */
	public Color getBackground() {
		return background;
	}
	
	/**
	 * Returns the foreground of the calendar color.
	 * @return the foreground of the calendar color.
	 */
	public Color getForeground() {
		return foreground;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(final CalendarColor other) {
		return id.compareTo(other.id);
	}

}
