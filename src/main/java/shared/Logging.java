package shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * This is the logging API that is more convenient than the 'standard' logging APIs.
 * @author danis.tazeev@gmail.com
 */
public final class Logging {
	private final Logger logger;

	public Logging(Class cls) {
		assert cls != null;
		logger = LoggerFactory.getLogger(cls);
	}

	/**
	 * This method concatenates the {@code msg} and the chain of messages of {@code err}. It is useful for concatenating
	 * a message with a {@code Throwable} and passing the result into {@link #info}. For example:
	 * <pre>
	 *     import static Logging.concat;
	 *     ...
	 *     Logging log = ...;
	 *     Throwable err = ...;
	 *     log.info(concat(err, "An error occurred. arg = %s"), arg);
	 * </pre>
	 */
	public static String concat(String msg, Throwable err) {
		StringBuilder sb = new StringBuilder();
		if (msg != null)
			sb.append(msg);
		while (err != null) {
			sb.append("\nCaused by: ").append(err);
			err = err.getCause();
		}
		return sb.toString();
	}

	public boolean infoEnabled() { return logger.isInfoEnabled(); }
	public void info(String msg) { logger.info(msg); }
	public void info(String fmt, Object... args) { logger.info(fmt, args); }

	///////////////////////////////////////////////////////////////////
	// Warnings are always enabled

	public void warn(String msg) { logger.warn(msg); }
	public void warn(String fmt, Object... args) { logger.warn(fmt, args); }
	/**
	 * Does not log the stack trace of the {@code err}. Only logs the
	 * chain of error messages from the {@code err} and its causes.
	 */
	public void warn(Throwable err, String msg) { logger.warn(concat(msg, err)); }
	/**
	 * Does not log the stack trace of the {@code err}. Only logs the
	 * chain of error messages from the {@code err} and its causes.
	 */
	public void warn(Throwable err, String fmt, Object... args) { logger.warn(concat(fmt, err), args); }

	///////////////////////////////////////////////////////////////////
	// Errors are always enabled

	public void error(String msg) { logger.error(msg); }
	public void error(String fmt, Object... args) { logger.error(fmt, args); }
	/** @return {@code err} as is */
	public <E extends Throwable> E error(E err, String msg) { logger.error(msg, err); return err; }
	/** @return {@code err} as is */
	public <E extends Throwable> E error(E err, String fmt, Object... args) {
		args = Arrays.copyOf(args, args.length + 1);
		args[args.length - 1] = err;
		logger.error(fmt, args);
		return err;
	}
}
