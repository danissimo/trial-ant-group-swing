package da;

import shared.EnvironmentError;
import shared.FailureException;
import shared.Logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/** @author danis.tazeev@gmail.com */
public final class DbInitializer implements AutoCloseable {
	private static final Logging log = new Logging(DbInitializer.class);
	private final Connection conn;

	private RuntimeException failure(SQLException cause, String fmt, Object... args) {
		return log.error(new FailureException("DB failure"),
				Logging.concat((fmt == null ? "" : fmt + '\n')
						+ "SQL error code: " + cause.getErrorCode(), cause), args);
	}

	private DbInitializer() {
		try {
			// NOTE: Accessing DAO class causes the H2 JDBC driver to get registered
			conn = DriverManager.getConnection(DAO.JDBC_URL);
		} catch (SQLException ex) {
			throw failure(ex, "Failed to obtain a connection to the DB");
		}
	}

	/** @return {@code true} if the DB schema is created and prefilled, i.e. create.sql was executed */
	private boolean isInitialized() {
		// the next line fails if the EMPLOYEE table does not exist
		try (Statement stmt = conn.prepareStatement("select * from employee where false")) {
			return true;
		} catch (SQLException ex) {
			if (ex.getErrorCode() == 42102) // table not found
				return false;
			throw failure(ex, "Unexpected error: err code = " + ex.getErrorCode());
		}
	}

	/** Runs the given SQL, which is meant to be loaded from create.sql */
	private void initialize(String sql) {
		log.info("initialize(...)");
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.execute();
			log.info("initialize = success");
		} catch (SQLException ex) {
			throw failure(ex, "initialize = fail");
		}
	}

	@Override
	public void close() {
		DAO.close(conn);
	}

	/** @throws Throwable in a case of a permanent (unrecoverable) failure */
	public static void initialize() {
		try (DbInitializer dao = new DbInitializer()) {
			if (!dao.isInitialized()) {
				final String path = "create.sql";
				StringBuilder sb = new StringBuilder(8192); // 16 KiB
				try (InputStream is = DbInitializer.class.getResourceAsStream(path)) {
					if (is == null)
						throw log.error(new EnvironmentError("Broken package"), "Failed to load resource: '{}'", path);
					BufferedReader r = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
					r.lines().forEachOrdered(sb::append);
				} catch (IOException neverHappens) {}
				dao.initialize(sb.toString());
			}
		}
	}
}
