package da;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** @author danis.tazeev@gmail.com */
public final class EmployeeDAO extends DAO {
	/** @return Never {@code null} */
	public EmployeeDTO[] selectAllEmployeesOrderedByName() {
		ResultSet rs = null;
		try {
			PreparedStatement stmt = prepareStatement("select id, name from employee order by name");
			rs = stmt.executeQuery();

			List<EmployeeDTO> result = new ArrayList<>(100);
			while (rs.next())
				result.add(new EmployeeDTO(rs.getLong(1), rs.getString(2)));
			return result.toArray(new EmployeeDTO[result.size()]);
		} catch (SQLException ex) {
			throw failure(ex, "selectAllEmployeesOrderedByName = fail");
		} finally {
			close(rs);
		}
	}
}
