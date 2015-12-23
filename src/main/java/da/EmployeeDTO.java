package da;

import static shared.Assert.filledAndTrimmed;
import static shared.Assert.nullOrQuoted;

/** @author danis.tazeev@gmail.com */
public final class EmployeeDTO {
	private final long id;
	private final String name;

	EmployeeDTO(long id, String name) {
		if (!filledAndTrimmed(name))
			throw new IllegalArgumentException("name = " + nullOrQuoted(name));
		this.id = id;
		this.name = name;
	}

	public long getId() { return id; }
	public String getName() { return name; }
}
