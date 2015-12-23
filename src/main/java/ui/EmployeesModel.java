package ui;

import da.EmployeeDTO;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/** @author danis.tazeev@gmail.com */
final class EmployeesModel {
	private final EventListenerList listeners = new EventListenerList();
	private final ChangeEvent theChangeEvent = new ChangeEvent(this);
	private EmployeeDTO[] employees;

	boolean hasEmployeesSet() {
		return employees != null;
	}

	void setEmployees(EmployeeDTO[] employees) {
		if (employees == null)
			throw new IllegalArgumentException("employees = null");
		if (this.employees != null)
			throw new IllegalStateException("employees has already been set. It is allowed to set employees only once");
		this.employees = employees;
		fireStateChange();
	}

	EmployeeDTO[] getEmployees() {
		if (!hasEmployeesSet())
			throw new IllegalStateException("employees has not been set yet");
		return employees;
	}

	void addChangeListener(ChangeListener l) {
		if (l == null)
			throw new IllegalArgumentException("l = null");
		listeners.add(ChangeListener.class, l);
	}

	void removeChangeListener(ChangeListener l) {
		if (l == null)
			return;
		listeners.remove(ChangeListener.class, l);
	}

	private void fireStateChange() {
		Object[] lx = listeners.getListenerList();
		assert lx != null;
		for (int i = lx.length - 1; i > 0; i -= 2) {
			assert lx[i - 1] == ChangeListener.class;
			((ChangeListener)lx[i]).stateChanged(theChangeEvent);
		}
	}
}
