package da;

/** @author danis.tazeev@gmail.com */
public final class AttendanceDTO {
	private long id;
	private long employeeId;
	private long checkedIn;
	private long checkedOut;

	AttendanceDTO(long id, long employeeId, long checkedIn, long checkedOut) {
		if (checkedIn <= 0)
			throw new IllegalArgumentException("checkedIn must be greater than 0");
		if (checkedOut < 0)
			throw new IllegalArgumentException("checkedOut must be greater or equal to 0");
		if (checkedOut > 0 && checkedOut < checkedIn)
			throw new IllegalArgumentException("checkedOut must be greater or equal to checkedIn or be 0");
		this.id = id;
		this.employeeId = employeeId;
		this.checkedIn = checkedIn;
		this.checkedOut = checkedOut;
	}

	public long getId() { return id; }
	public long getEmployeeId() { return employeeId; }
	public long getCheckedIn() { return checkedIn; }
	/** @return {@code 0} if attendance has not been checked out yet */
	public long getCheckedOut() { return checkedOut; }
}
