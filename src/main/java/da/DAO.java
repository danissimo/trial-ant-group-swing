package da;

import org.h2.Driver;
import shared.EnvironmentError;
import shared.FailureException;
import shared.Logging;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static shared.Assert.nullOrQuoted;

/**
 * Descendants of this class are not thread-safe.
 * <b>WARN:</b> Descendants cannot run concurrently since this class (and
 * the descendants transitively) uses a single and the only DB connection.
 * @author danis.tazeev@gmail.com
 */
public abstract class DAO implements AutoCloseable {
//	static final String JDBC_URL = "jdbc:h2:./ant-group-trial/db;DB_CLOSE_DELAY=0;MULTI_THREADED=FALSE;MVCC=TRUE;MV_STORE=TRUE";
	static final String JDBC_URL = "jdbc:h2:./database/trial";

	private static final Connection CONN;
	static {
		try {
			DriverManager.registerDriver(new Driver());
			CONN = DriverManager.getConnection(JDBC_URL);
		} catch (SQLException ex) {
			// NOTE: Since H2 runs in the process the exception is thrown:
			// - if the DB cannot be created in the working directory
			// - the DB is already in use
			throw new Logging(DAO.class).error(
					new EnvironmentError("DB failure"),
					concat("Failed to obtain a connection", ex));
		}
	}
	private static boolean connectionIsFree = true;

	private final Map<String, PreparedStatement> stmts = new HashMap<>();
	private boolean autoCommit = true;
	private boolean closed;
	private String lastSql;

	DAO() {
		// ensures that only one DAO at a time is active (instantiated and not closed yet) and uses the single connection
		synchronized (CONN) {
			boolean interrupted = false;
			while (!connectionIsFree) {
				try {
					CONN.wait();
				} catch (InterruptedException save) { // do not loose the interruption flag
					interrupted = true;
				}
			}
			connectionIsFree = false;
			if (interrupted)
				Thread.currentThread().interrupt(); // re-raise the flag
		}
		setAutoCommit(true);
	}

	private void throwIfClosed() {
		if (closed)
			throw new IllegalStateException("DAO closed");
	}

	void disableAutoCommit() {
		throwIfClosed();
		setAutoCommit(false);
	}

	private void setAutoCommit(boolean autoCommit) {
		if (closed || this.autoCommit == autoCommit)
			return;
		try {
			CONN.setAutoCommit(autoCommit);
			this.autoCommit = autoCommit;
		} catch (SQLException ex) {
			throw error(new FailureException("DB failure"), concat("Failed to set auto-commit: " + autoCommit, ex));
		}
	}

	void commitAndEnableAutoCommit() {
		throwIfClosed();
		if (!autoCommit) {
			try {
				CONN.commit();
			} catch (SQLException ex) {
				throw failure(ex, "Failed to commit");
			} finally {
				setAutoCommit(true);
			}
		}
	}

	void rollbackAndEnableAutoCommit() {
		throwIfClosed();
		if (!autoCommit) {
			try {
				CONN.rollback();
			} catch (SQLException ex) {
				warn(ex, "Failed to rollback");
			} finally {
				setAutoCommit(true);
			}
		}
	}

	PreparedStatement prepareStatement(String sql) {
		throwIfClosed();
		String s;
		if ((s = sql) == null || (sql = sql.trim()).length() <= 0)
			throw new IllegalArgumentException("sql = " + nullOrQuoted(s));
		lastSql = sql;
		try {
			PreparedStatement stmt = stmts.get(sql);
			if (stmt == null)
				stmts.put(sql, stmt = CONN.prepareStatement(sql));
			return stmt;
		} catch (SQLException ex) {
			throw failure(ex, "Failed to prepare statement");
		}
	}

	static void close(AutoCloseable v) {
		if (v != null) {
			try {
				v.close();
			} catch (Exception ignore) {}
		}
	}

	@Override
	public void close() {
		if (closed)
			return;
		rollbackAndEnableAutoCommit();
		stmts.values().forEach(DAO::close);
		stmts.clear();
		closed = true;
		synchronized (CONN) {
			connectionIsFree = true;
			CONN.notify();
		}
	}

	/**
	 * Closes the single connection.
	 * <b>No descending DAOs are aligible for use after this method is invoked.
	 * Trying to use them will fail.</b>
	 */
	public static void terminate() { close(CONN); }

	///////////////////////////////////////////////////////////////////
	// Logging
	private final Logging log = new Logging(getClass());

	static String concat(String msg, Throwable err) { return Logging.concat(msg, err); }

	boolean infoEnabled() { return log.infoEnabled(); }
	void info(String msg) { log.info(msg); }
	void info(String fmt, Object... args) { log.info(fmt, args); }

	void warn(String msg) { log.warn(msg); }
	void warn(String fmt, Object... args) { log.warn(fmt, args); }
	void warn(Throwable err, String msg) { log.warn(err, msg); }
	void warn(Throwable err, String fmt, Object... args) { log.warn(err, fmt, args); }

	void error(String msg) { log.error(msg); }
	void error(String fmt, Object... args) { log.error(fmt, args); }
	<E extends Throwable> E error(E err, String msg) { return log.error(err, msg); }
	<E extends Throwable> E error(E err, String fmt, Object... args) { return log.error(err, fmt, args); }

	FailureException failure(SQLException cause, String fmt, Object... args) {
		rollbackAndEnableAutoCommit();
		return error(new FailureException("DB Failure"),
				concat((fmt == null ? "" : fmt + '\n')
						+ "SQL error code: " + cause.getErrorCode()
						+ "\nLast SQL: " + lastSql, cause), args);
	}
}
