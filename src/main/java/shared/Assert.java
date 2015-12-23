package shared;

/** @author danis.tazeev@gmail.com */
public final class Assert {
	private Assert() {}

	public static boolean filledAndTrimmed(String s) {
		return s != null && s.length() > 0 && s.length() == s.trim().length();
	}

	public static String nullOrQuoted(String s) {
		return s == null ? null : String.format("'%s'", s);
	}
}
