package shared;

/**
 * This error is thrown if an error in the environment that prevents the application from running detected.
 * @author danis.tazeev@gmail.com
 */
public final class EnvironmentError extends Error {
	public EnvironmentError(String msg) { super(msg); }
	public EnvironmentError(String msg, Throwable cause) { super(msg, cause); }
}
