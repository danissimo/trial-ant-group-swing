package shared;

/**
 * This exception is thrown when a temporary denial of service detected.
 * @author danis.tazeev@gmail.com
 */
public final class FailureException extends RuntimeException {
	public FailureException(String msg) { super(msg); }
	public FailureException(String msg, Throwable cause) { super(msg, cause); }
}
