package googlecalxplugin;

/**
 * Represents a calendar that a program can be exported to.
 * This is a pair of calendar ID and label.
 *
 */
public class CalendarTarget {
	
	private static final char SEP = '|';
	
	private final String id;
	private final String label;
	
	public CalendarTarget(final String anID, final String aLabel) {
		id = anID;
		label = aLabel;
	}
	
	public String getId() {
		return id;
	}
	
	public String getLabel() {
		return label;
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return label;
	}

	/**
	 * Returns a String version of the target.
	 * @return a String version of the target, never <code>null</code>.
	 */
	public String toStringRep() {
		return id+SEP+label;
	}

	public static CalendarTarget parse(final String str) {
		if (str != null && str.length() > 0) {
			final int cut = str.indexOf(SEP);
			if (cut <= 0) {
				throw new IllegalStateException("cannot parse "+str);
			}
			return new CalendarTarget(str.substring(0, cut), str.substring(cut+1));
		}
		return null;
	}
	
}
