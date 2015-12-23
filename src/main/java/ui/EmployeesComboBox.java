package ui;

import da.EmployeeDTO;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/** @author danis.tazeev@gmail.com */
final class EmployeesComboBox extends JComboBox<EmployeeDTO> {
	EmployeesComboBox(EmployeesModel employeesModel) {
		super(new CustomComboBoxModel(employeesModel));
		if (employeesModel == null)
			throw new IllegalArgumentException("employeesModel = null");
		setMaximumRowCount(25);
		setRenderer(new EmployeeRenderer());
	}

	EmployeeDTO getSelectedEmployee() {
		return (EmployeeDTO)getSelectedItem();
	}

	private static final class CustomComboBoxModel
			extends AbstractListModel<EmployeeDTO>
			implements ComboBoxModel<EmployeeDTO> {
		private final EmployeesModel employeesModel;
		private Object selectedItem;

		private CustomComboBoxModel(final EmployeesModel employeesModel) {
			assert employeesModel != null;
			this.employeesModel = employeesModel;
			if (!employeesModel.hasEmployeesSet()) {
				employeesModel.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent e) {
						employeesModel.removeChangeListener(this);
						EmployeeDTO[] employees = employeesModel.getEmployees();
						if (employees.length > 0) {
							fireIntervalAdded(CustomComboBoxModel.this, 0, employees.length - 1);
							setSelectedItem(employees[0]);
						}
					}
				});
			}
		}

		@Override
		public int getSize() {
			return !employeesModel.hasEmployeesSet() ? 0 : employeesModel.getEmployees().length;
		}

		@Override
		public EmployeeDTO getElementAt(int index) {
			if (index < 0 || index >= getSize())
				throw new IllegalArgumentException("index = null");
			return employeesModel.getEmployees()[index];
		}

		@Override
		public void setSelectedItem(Object item) {
			if (selectedItem != null && !selectedItem.equals(item) || selectedItem == null && item != null) {
				selectedItem = item;
				fireContentsChanged(this, -1, -1);
			}
		}

		@Override
		public Object getSelectedItem() {
			return selectedItem;
		}
	}

	private static final class EmployeeRenderer extends DefaultListCellRenderer {
		@Override
		public Component getListCellRendererComponent(
				JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			assert c == this;
			setText(value == null ? "" : ((EmployeeDTO)value).getName());
			return this;
		}
	}
}
