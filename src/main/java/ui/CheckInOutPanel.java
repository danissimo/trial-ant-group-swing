package ui;

import da.AttendanceDAO;
import shared.Logging;
import shared.LogicError;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/** @author danis.tazeev@gmail.com */
final class CheckInOutPanel extends ActionContent {
	static final String ACTION_COMMAND_CHECK_IN = "checkIn";
	static final String ACTION_COMMAND_CHECK_OUT = "checkOut";

	private final EmployeesComboBox employees;
	private final JButton checkIn = new JButton("Пришёл");
	private final JButton checkOut = new JButton("Ушёл");
	private SwingWorker<Boolean, Void> worker;

	CheckInOutPanel(EmployeesModel employeesModel) {
		if (employeesModel == null)
			throw new IllegalArgumentException("employeesModel = null");

		// NOTE: As soon as the EmployeesModel gets saturated the EmployeesComboBox emits an ActionEvent
		// NOTE: Also it resizes and notifies with a ComponentEvent
		// NOTE: It means there is no need to listen to a ChangeEvent from the EmployeesModel
		employees = new EmployeesComboBox(employeesModel);
		employees.addActionListener(e -> launchCheckWhatFetcher());
		employees.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				MainFrame.packAndCenterFrameOf(CheckInOutPanel.this);
			}
		});

		if (employeesModel.hasEmployeesSet()) {
			// this means the EmployeesComboBox has a selected employee
			launchCheckWhatFetcher();
		} else
			showWholeViewIsWaiting();

		checkIn.setActionCommand(ACTION_COMMAND_CHECK_IN);
		checkOut.setActionCommand(ACTION_COMMAND_CHECK_OUT);
		ActionListener l = e -> {
			launchCheckTimestampPersister(e.getActionCommand(),
					employees.getSelectedEmployee().getId(), System.currentTimeMillis());
			// notify external listeners; the external listener will switch this view to another one
			fireActionPerformed(e);
		};
		checkIn.addActionListener(l);
		checkOut.addActionListener(l);

		add(employees);
		add(checkIn);
		add(checkOut);
	}

	private void showWholeViewIsWaiting() {
		employees.setEnabled(false);
		checkIn.setEnabled(false);
		checkOut.setEnabled(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}

	private void showCheckWhatWaiting() {
		// Disable buttons and set the waiting cursor while the worker will be working.
		// But leave the combo box enabled and leave the cursor over it normal for the
		// case the user will select another employee while the worker is working.
		employees.setEnabled(true);
		checkIn.setEnabled(false);
		checkOut.setEnabled(false);
		employees.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}

	private void showCheckWhat(boolean enableCheckOut) {
		employees.setEnabled(true);
		(enableCheckOut ? checkOut : checkIn).setEnabled(true);
		employees.setCursor(null);
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	/** Invoke this method each time selection of the {@link #employees combo box} changes. */
	private void launchCheckWhatFetcher() {
		// throw away the result of the previously launched worker if any prematurely
		if (worker != null) {
			// If the attempt to cancel() succeeds, the worker nullifies 'worker' field
			worker.cancel(false);
		}
		showCheckWhatWaiting();
		// worker is nullified when finished; either due to cancellation or due to the task getting performed
		worker = new SwingWorker<Boolean, Void>() {
			@Override
			protected Boolean doInBackground() throws Exception {
				// NOTE: this method runs in a thread other than EDT
				Thread.sleep(Main.getRandomDelay());
				try (AttendanceDAO dao = new AttendanceDAO()) {
					return dao.hasNotCheckedOutYet(employees.getSelectedEmployee().getId());
				}
			}

			@Override
			protected void done() {
				// NOTE: this method runs in EDT
				//
				// If cancel() method of this SwingWorker is invoked in EDT, then this method is invoked from
				// inside the cancel(). If cancel() is invoked from a thread other than EDT, then this method
				// does not invoked from inside the cancel(). Instead the cancel() schedules invocation of this
				// method to EDT.
				//
				// In this code cancel() is invoked is EDT only
				//
				// NOTE: Invocation of the cancel() can fail, in which case this method is not invoked from inside the
				// cancel(). Instead invocation of this method is scheduled to EDT. The launchCheckWhatFetcher() also
				// runs in EDT. It attempts to cancel() the current worker, and the attempt may fail and this method
				// will not be invoked from cancel(). It will be invoked later. But after the cancellation attempt the
				// launchCheckWhatFetcher() replaces the worker. So we MUST check whether the worker is still this one.

				// This worker could be replaced by another one if the user selected
				// another employee in the combo box while doInBackground() was working
				if (worker == this) {
					// this worker was not abandoned while it was working
					try {
						if (!isCancelled())
							showCheckWhat(get());
						// else: do nothing and throw away any results even if they already produced
					} catch (Throwable err) {
						Main.reportFailureAndTerminate(err);
					} finally {
						// When this SwingWorker instance is finished, nullify the 'worker' field
						// Nullifying is allowed if and only if the worker is this SwingWorker instance
						worker = null;
					}
				} // else: This worker was attempted to cancel() and then replaced with another one
			}
		};
		worker.execute();
	}

	/**
	 * Invoke this method when either {@link #checkIn check in} or {@link #checkOut check out} button gets pressed.
	 * @param actionCommand just the {@link ActionEvent#getActionCommand() action command} issued by the pressed button
	 */
	private void launchCheckTimestampPersister(
			final String actionCommand, final long employeeId, final long timestamp) {
		showWholeViewIsWaiting();
		new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				Thread.sleep(Main.getRandomDelay());
				try (AttendanceDAO dao = new AttendanceDAO()) {
					switch (actionCommand) {
						case ACTION_COMMAND_CHECK_IN: dao.checkIn(employeeId, timestamp); break;
						case ACTION_COMMAND_CHECK_OUT: dao.checkOut(employeeId, timestamp); break;
						default: Main.reportFailureAndTerminate(new Logging(Main.class).error(
								new LogicError("Forbidden condition"),
								"CheckInOutPanel fired an ActionEvent with an unexpected command: " + actionCommand));
					}
				}
				return null;
			}

			@Override
			protected void done() {
				try {
					get(); // check if an error occured in diInBackground()
					showCheckWhat(ACTION_COMMAND_CHECK_IN.equals(actionCommand));
				} catch (Throwable err) {
					Main.reportFailureAndTerminate(err);
				}
			}
		}.execute();
	}
}
