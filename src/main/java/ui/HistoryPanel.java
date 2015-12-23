package ui;

import da.AttendanceDAO;
import da.AttendanceDTO;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Calendar;
import java.util.Date;

import static ui.Main.DAY_MILLIS;
import static ui.Main.toLocalMidnight;

/** @author danis.tazeev@gmail.com */
final class HistoryPanel extends ActionContent {
	private final EmployeesModel employeesModel;
	private final CustomTableModel tableModel;
	private final EmployeesComboBox employees;
	private final JSpinner fromDateChooser;
	private final JSpinner toDateChooser;
	private final JButton history = new JButton("История");
	private final JButton back = new JButton("Назад");
	private SwingWorker worker;

	HistoryPanel(final EmployeesModel employeesModel) {
		super(new BorderLayout());
		if (employeesModel == null)
			throw new IllegalArgumentException("employeesModel = null");
		this.employeesModel = employeesModel;

		tableModel = new CustomTableModel(employeesModel);
		JTable table = new JTable(tableModel);
		table.setDefaultRenderer(Date.class, new CustomDateRenderer());

		Date tonightMidnight = new Date(toLocalMidnight(System.currentTimeMillis()));
		Date tomorrowMidnight = new Date(tonightMidnight.getTime() + DAY_MILLIS);

		final SpinnerDateModel fromSpinnerModel = new SpinnerDateModel(tonightMidnight, null, tonightMidnight, Calendar.DAY_OF_MONTH);
		final SpinnerDateModel toSpinnerModel = new SpinnerDateModel(tomorrowMidnight, null, tomorrowMidnight, Calendar.DAY_OF_MONTH);
		fromSpinnerModel.addChangeListener(e -> {
			long fromMidnight = fromSpinnerModel.getDate().getTime();
			long toMidnight = toSpinnerModel.getDate().getTime();
			if (toMidnight < fromMidnight + DAY_MILLIS)
				toSpinnerModel.setValue(new Date(fromMidnight + DAY_MILLIS));
		});
		toSpinnerModel.addChangeListener(e -> {
			long fromMidnight = fromSpinnerModel.getDate().getTime();
			long toMidnight = toSpinnerModel.getDate().getTime();
			if (fromMidnight > toMidnight - DAY_MILLIS)
				fromSpinnerModel.setValue(new Date(toMidnight - DAY_MILLIS));
		});

		fromDateChooser = new JSpinner(fromSpinnerModel);
		fromDateChooser.setEditor(new JSpinner.DateEditor(fromDateChooser, "dd.MM.yy"));
		toDateChooser = new JSpinner(toSpinnerModel);
		toDateChooser.setEditor(new JSpinner.DateEditor(toDateChooser, "dd.MM.yy"));

		history.addActionListener(e -> launchHistoryFetcher());
		back.addActionListener(e -> {
			resetView();
			fireActionPerformed(e);
		});

		employees = new EmployeesComboBox(employeesModel);
		employees.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				MainFrame.packAndCenterFrameOf(HistoryPanel.this);
			}
		});

		if (!employeesModel.hasEmployeesSet()) {
			showEmployeesDependentControlsAreWaiting();
			employeesModel.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					employeesModel.removeChangeListener(this);
					showEmployeesDependentControlsAreReady();
				}
			});
		}

		JPanel controls = new JPanel();
		controls.add(employees);
		controls.add(fromDateChooser);
		controls.add(toDateChooser);
		controls.add(history);
		controls.add(back);

		add(controls, BorderLayout.PAGE_START);
		add(new JScrollPane(table));
	}

	private void resetView() {
		if (worker != null)
			worker.cancel(false);
		tableModel.setData(null);
		showHistoryFetcherConfiguringControlsAreReady();
		if (!employeesModel.hasEmployeesSet())
			showEmployeesDependentControlsAreWaiting();
	}

	private void showEmployeesDependentControlsAreWaiting() {
		employees.setEnabled(false);
		history.setEnabled(false);
		employees.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		history.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}

	private void showEmployeesDependentControlsAreReady() {
		employees.setEnabled(true);
		history.setEnabled(true);
		employees.setCursor(null);
		history.setCursor(null);
	}

	private void showHistoryFetcherConfiguringControlsAreWaiting() {
		tableModel.setData(null);
		employees.setEnabled(false);
		fromDateChooser.setEnabled(false);
		toDateChooser.setEnabled(false);
		history.setEnabled(false);
		back.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}

	private void showHistoryFetcherConfiguringControlsAreReady() {
		employees.setEnabled(true);
		fromDateChooser.setEnabled(true);
		toDateChooser.setEnabled(true);
		history.setEnabled(true);
		back.setCursor(null);
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	private void launchHistoryFetcher() {
		showHistoryFetcherConfiguringControlsAreWaiting();

		final long employeeId = employees.getSelectedEmployee().getId();
		final long fromMidnight = ((Date)fromDateChooser.getValue()).getTime();
		final long toMidnight = ((Date)toDateChooser.getValue()).getTime();

		worker = new SwingWorker<AttendanceDTO[], Void>() {
			@Override
			protected AttendanceDTO[] doInBackground() throws Exception {
				Thread.sleep(Main.getRandomDelay());
				try (AttendanceDAO dao = new AttendanceDAO()) {
					return dao.selectEmployeeAttendanceBetween(employeeId, fromMidnight, toMidnight);
				}
			}

			@Override
			protected void done() {
				if (worker == this) {
					try {
						if (!isCancelled())
							showHistory(get());
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

	private void showHistory(AttendanceDTO[] attendance) {
		tableModel.setData(attendance);
		showHistoryFetcherConfiguringControlsAreReady();
	}

	private static final class CustomTableModel extends FixedColumnNamesTableModel {
		private final EmployeesModel employeesModel;
		private AttendanceDTO[] data;

		private CustomTableModel(EmployeesModel employeesModel) {
			super(new String[] { "Пришёл", "Ушёл" });
			assert employeesModel != null;
			this.employeesModel = employeesModel;
			if (!employeesModel.hasEmployeesSet()) {
				employeesModel.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent e) {
						employeesModel.removeChangeListener(this);
						if (data != null)
							fireTableDataChanged();
					}
				});
			}
		}

		@Override
		public Class<?> getColumnClass(int col) {
			return Date.class;
		}

		private void setData(AttendanceDTO[] data) {
			Object oldData = this.data;
			this.data = data;
			if (data == null && oldData != null || data != null && employeesModel.hasEmployeesSet())
				fireTableDataChanged();
		}

		@Override
		public int getRowCount() {
			return data == null || !employeesModel.hasEmployeesSet() ? 0 : data.length;
		}

		@Override
		public Object getValueAt(int row, int col) {
			AttendanceDTO attendance = data[row];
			long ts = col == 0 ? attendance.getCheckedIn() : attendance.getCheckedOut();
			return ts == 0 ? null : new Date(ts);
		}
	}
}
