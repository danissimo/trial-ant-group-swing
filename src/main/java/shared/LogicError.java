package shared;

/**
 * This error means that an error in the program logic detected.
 * @author danis.tazeev@gmail.com
 */
public final class LogicError extends Error {
	public LogicError(String msg) { super(msg); }
	public LogicError(String msg, Throwable cause) { super(msg, cause); }
}
