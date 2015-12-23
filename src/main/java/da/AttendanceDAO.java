package da;

import shared.LogicError;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** @author danis.tazeev@gmail.com */
public final class AttendanceDAO extends DAO {
	public boolean hasNotCheckedOutYet(long employeeId) {
		ResultSet rs = null;
		try {
			PreparedStatement stmt = prepareStatement(
					"select 1 from attendance where employee_id = ? and checked_out is null");
			stmt.setLong(1, employeeId);
			rs = stmt.executeQuery();
			return rs.next();
		} catch (SQLException ex) {
			throw failure(ex, "hasNotCheckedOutYet({}) = fail", employeeId);
		} finally {
			close(rs);
		}
	}

	public void checkIn(long employeeId, long timestamp) {
		info("checkIn({}, ...)", employeeId);
		try {
			PreparedStatement stmt = prepareStatement("insert into attendance(employee_id, checked_in) values(?, ?)");
			stmt.setLong(1, employeeId);
			stmt.setTimestamp(2, new Timestamp(timestamp));
			stmt.executeUpdate();
			info("checkIn = success");
		} catch (SQLException ex) {
			throw failure(ex, "checkIn = fail");
		}
	}

	public void checkOut(long employeeId, long timestamp) {
		info("checkOut({}, ...)", employeeId);
		try {
			disableAutoCommit();
			PreparedStatement stmt = prepareStatement(
					"update attendance set checked_out = ? where employee_id = ? and checked_out is null");
			stmt.setTimestamp(1, new Timestamp(timestamp));
			stmt.setLong(2, employeeId);
			int n = stmt.executeUpdate();
			if (n != 1) {
				rollbackAndEnableAutoCommit();
				throw error(new LogicError("Service contract violated"),
						"checkOut = fail: checkIn/checkOut contract violated:"
						+ " each single checkIn() MUST be followed by a single checkOut()");
			}
			commitAndEnableAutoCommit();
			info("checkOut = success");
		} catch (SQLException ex) {
			rollbackAndEnableAutoCommit();
			// err code = 90053: scalar subquery contains more than one row
			throw failure(ex, "checkOut = fail");
		}
	}
	
	/**
	 * @param from the timestamp of the beginning of the interval (inclusive) 
	 * @param to the timestamp of the end of the interval (exclusive)
	 * @return Never {@code null}
	 */
	public AttendanceDTO[] selectEmployeeAttendanceBetween(long employeeId, long from, long to) {
		if (to <= from)
			throw new IllegalArgumentException("to must be greater than from");
		ResultSet rs = null;
		try {
			PreparedStatement stmt = prepareStatement(
					"select id, employee_id, checked_in, checked_out"
					+ " from attendance"
					+ " where employee_id = ? and checked_in < ? and (checked_out > ? or checked_out is null)"
					+ " order by id");
			stmt.setLong(1, employeeId);
			stmt.setTimestamp(2, new Timestamp(to));
			stmt.setTimestamp(3, new Timestamp(from));
			rs = stmt.executeQuery();

			List<AttendanceDTO> result = new ArrayList<>();
			while (rs.next()) {
				Timestamp ts_checkedOut = rs.getTimestamp(4);
				long millis_checkedOut = ts_checkedOut == null ? 0 : ts_checkedOut.getTime();
				result.add(new AttendanceDTO(rs.getLong(1), rs.getLong(2),
						rs.getTimestamp(3).getTime(), millis_checkedOut));
			}
			return result.toArray(new AttendanceDTO[result.size()]);
		} catch (SQLException ex) {
			throw failure(ex, "selectEmployeeAttendanceBetween({}, ...) = fail", employeeId);
		} finally {
			close(rs);
		}
	}

	/**
	 * @return A {@code Map} of {@code emplyeeId} to his/her attendance during the given period.
	 * If an employee didn't attend the given period the {@code Map} does not contains a mapping
	 * for that employee. Never {@code null}
	 */
	public Map<Long, AttendanceDTO[]> selectAllEmployeesAttendanceBetween(long from, long to) {
		if (to <= from)
			throw new IllegalArgumentException("to must be greater than from");
		ResultSet rs = null;
		try {
			PreparedStatement stmt = prepareStatement(
					"select employee_id, id, checked_in, checked_out"
					+ " from attendance"
					+ " where checked_in < ? and (checked_out > ? or checked_out is null)"
					+ " order by employee_id, id");
			stmt.setTimestamp(1, new Timestamp(to));
			stmt.setTimestamp(2, new Timestamp(from));
			rs = stmt.executeQuery();

			Map<Long, AttendanceDTO[]> result = new HashMap<>();
			if (!rs.next())
				return result;

			List<AttendanceDTO> attendance = new ArrayList<>();
			long prevEmployeeId = rs.getLong(1);
			do {
				long employeeId = rs.getLong(1);
				if (prevEmployeeId != employeeId) {
					result.put(prevEmployeeId, attendance.toArray(new AttendanceDTO[attendance.size()]));
					attendance.clear();
					prevEmployeeId = employeeId;
				}
				Timestamp ts_checkedOut = rs.getTimestamp(4);
				long millis_checkedOut = ts_checkedOut == null ? 0 : ts_checkedOut.getTime();
				attendance.add(new AttendanceDTO(rs.getLong(2), employeeId,
						rs.getTimestamp(3).getTime(), millis_checkedOut));
			} while (rs.next());
			result.put(prevEmployeeId, attendance.toArray(new AttendanceDTO[attendance.size()]));
			return result;
		} catch (SQLException ex) {
			throw failure(ex, "selectAllEmployeesAttendanceBetween(...) = fail");
		} finally {
			close(rs);
		}
	}
}
