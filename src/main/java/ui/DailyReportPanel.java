package ui;

import da.AttendanceDAO;
import da.AttendanceDTO;
import da.EmployeeDTO;
import shared.Logging;
import shared.LogicError;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import static ui.Main.DAY_MILLIS;
import static ui.Main.toLocalMidnight;

/** @author danis.tazeev@gmail.com */
final class DailyReportPanel extends ActionContent {
	private final EmployeesModel employeesModel;
	private final CustomTableModel tableModel;
	private final JSpinner dateChooser;
	private final JButton report = new JButton("Отчёт");
	private final JButton back = new JButton("Назад");
	private SwingWorker worker;

	DailyReportPanel(final EmployeesModel employeesModel) {
		super(new BorderLayout());
		if (employeesModel == null)
			throw new IllegalArgumentException("employeesModel = null");
		this.employeesModel = employeesModel;

		tableModel = new CustomTableModel(employeesModel);
		JTable table = new JTable(tableModel);
		table.setDefaultRenderer(EmployeeDTO.class, new EmployeeRenderer());
		table.setDefaultRenderer(Date.class, new DateRenderer());

		Date midnight = new Date(toLocalMidnight(System.currentTimeMillis()));
		SpinnerDateModel spinnerModel = new SpinnerDateModel(midnight, null, midnight, Calendar.DAY_OF_MONTH);
		dateChooser = new JSpinner(spinnerModel);
		dateChooser.setEditor(new JSpinner.DateEditor(dateChooser, "dd.MM.yy"));

		report.addActionListener(e -> launchReportFetcher());
		back.addActionListener(e -> {
			resetView();
			fireActionPerformed(e);
		});

		JPanel controls = new JPanel();
		controls.add(dateChooser);
		controls.add(report);
		controls.add(back);

		add(controls, BorderLayout.PAGE_START);
		add(new JScrollPane(table));
	}

	private void resetView() {
		if (worker != null)
			worker.cancel(false);
		tableModel.setData(null);
		showReportFetcherConfiguringControlsAreReady();
	}

	private void showReportFetcherConfiguringControlsAreWaiting() {
		tableModel.setData(null);
		dateChooser.setEnabled(false);
		report.setEnabled(false);
		back.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}

	private void showReportFetcherConfiguringControlsAreReady() {
		dateChooser.setEnabled(true);
		report.setEnabled(true);
		back.setCursor(null);
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	private void launchReportFetcher() {
		showReportFetcherConfiguringControlsAreWaiting();

		// DO NOT move the next line into doInBackground() since doInBackground() runs in a thread other than EDT
		final long chosenMidnight = ((Date)dateChooser.getValue()).getTime();
		worker = new SwingWorker<Map<Long, AttendanceDTO[]>, Void>() {
			@Override
			protected Map<Long, AttendanceDTO[]> doInBackground() throws Exception {
				Thread.sleep(Main.getRandomDelay());
				try (AttendanceDAO dao = new AttendanceDAO()) {
					return dao.selectAllEmployeesAttendanceBetween(chosenMidnight, chosenMidnight + DAY_MILLIS);
				}
			}

			@Override
			protected void done() {
				if (worker == this) {
					try {
						if (!isCancelled())
							showReport(get());
					} catch (Throwable err) {
						Main.reportFailureAndTerminate(err);
					} finally {
						worker = null;
					}
				}
			}
		};
		worker.execute();
	}

	private void showReport(Map<Long, AttendanceDTO[]> attendance) {
		tableModel.setData(attendance);
		if (employeesModel.hasEmployeesSet())
			showReportFetcherConfiguringControlsAreReady();
		else {
			employeesModel.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					employeesModel.removeChangeListener(this);
					showReportFetcherConfiguringControlsAreReady();
				}
			});
		}
	}

	private static final class CustomTableModel extends FixedColumnNamesTableModel {
		private final EmployeesModel employeesModel;
		private Map<Long, AttendanceDTO[]> data;

		/**
		 * Each 32-bit value represents two numbers:
		 * <ul>
		 *     <li>The highest 22 bits is the index of an {@link EmployeeDTO} in the
		 *     {@link EmployeesModel#getEmployees() EmployeeDTO[]} array.</li>
		 *     <li>The lowest 10 bits is the index in {@code AttendanceDTO[]} array found in the {@link #data} map.
		 *     If the map does not contain a mapping for the {@code employeeId} of the {@code EmployeeDTO} from the
		 *     highest 22-bit index, then the lowest 10 bits are ignored.</li>
		 * </ul>
		 */
		private int[] index;

		private CustomTableModel(final EmployeesModel employeesModel) {
			super(new String[] { "Сотрудник", "Пришёл", "Ушёл" });
			assert employeesModel != null;
			this.employeesModel = employeesModel;
			if (!employeesModel.hasEmployeesSet()) {
				employeesModel.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent e) {
						employeesModel.removeChangeListener(this);
						if (data != null) {
							buildIndex();
							fireTableDataChanged();
						}
					}
				});
			}
		}

		@Override
		public Class<?> getColumnClass(int col) {
			return col == 0 ? EmployeeDTO.class : Date.class;
		}

		private void setData(Map<Long, AttendanceDTO[]> data) {
			Object oldData = this.data;
			this.data = data;
			if (data == null && oldData != null) {
				index = null; // clear index
				fireTableDataChanged();
			} else if (data != null && employeesModel.hasEmployeesSet()) {
				buildIndex();
				fireTableDataChanged();
			}
		}

		private void buildIndex() {
			assert data != null && employeesModel.hasEmployeesSet();

			EmployeeDTO[] employees = employeesModel.getEmployees();
			if (employees.length > 0x3FFFFF) {
				Main.reportFailureAndTerminate(new Logging(DailyReportPanel.class).error(
						new LogicError("Broken invariant"),
						"The length of EmployeeDTO[] is greater than 2^22"));
			}

			int dataRowCount = 0;
			for (AttendanceDTO[] x : data.values())
				dataRowCount += x.length;
			int indexLength = employees.length - data.size() + dataRowCount;
			index = new int[indexLength];

			for (int i = 0, j = 0; i < employees.length; i++) {
				EmployeeDTO emp = employees[i];
				AttendanceDTO[] attx = data.get(emp.getId());
				if (attx == null)
					index[j++] = i << 10;
				else {
					if (attx.length > 0x3FF) {
						Main.reportFailureAndTerminate(new Logging(DailyReportPanel.class).error(
								new LogicError("Broken invariant"),
								"The length of AttendanceDTO[] is greater than 2^10"));
					}
					int emp_idx = i << 10;
					for (int att_idx = 0; att_idx < attx.length; att_idx++)
						index[j++] = emp_idx | att_idx & 0x3FF;
				}
			}
		}

		@Override
		public int getRowCount() {
			return data == null || !employeesModel.hasEmployeesSet() ? 0 : index.length;
		}

		@Override
		public Object getValueAt(int row, int col) {
			int tuple = index[row];
			EmployeeDTO employee = employeesModel.getEmployees()[tuple >> 10];
			if (col == 0)
				return employee;

			AttendanceDTO[] attx = data.get(employee.getId());
			if (attx == null)
				return null;

			AttendanceDTO attendance = attx[tuple & 0x3FF];
			long ts = col == 1 ? attendance.getCheckedIn() : attendance.getCheckedOut();
			return ts == 0 ? null : new Date(ts);
		}
	}

	private static final class EmployeeRenderer extends DefaultTableCellRenderer {
		@Override
		protected void setValue(Object value) {
			setText(((EmployeeDTO)value).getName());
		}
	}

	private final class DateRenderer extends CustomDateRenderer {
		private final DateFormat df = new SimpleDateFormat("HH:mm");

		@Override
		protected void setValue(Object value) {
			if (value != null) {
				Date d = (Date)value;
				long chosenMidnight = ((Date)dateChooser.getValue()).getTime();
				if (d.getTime() < chosenMidnight)
					super.setValue(value);
				else
					setText(df.format(d));
			} else
				setText("");
		}
	}
}
