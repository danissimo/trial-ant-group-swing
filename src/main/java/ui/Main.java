package ui;

import da.DAO;
import da.DbInitializer;
import da.EmployeeDAO;
import da.EmployeeDTO;
import shared.Logging;
import shared.LogicError;

import javax.swing.*;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/** @author danis.tazeev@gmail.com */
final class Main {
	static final long DAY_MILLIS = TimeUnit.DAYS.toMillis(1);

	/**
	 * @param ts timestamp in millis from midnight, January 1, 1970 UTC
	 * @return millis from midnight, January 1, 1970 UTC, corresponding to midnight in default timezone
	 */
	static long toLocalMidnight(long ts) {
		TimeZone tz = TimeZone.getDefault();
		ts += tz.getOffset(ts);
		ts -= ts % DAY_MILLIS;
		ts -= tz.getOffset(ts);
		return ts;
	}

	private static int maxDelaySecs;
	private static Random rnd;

	static int getRandomDelay() {
		if (maxDelaySecs <= 0)
			return 0;
		return rnd.nextInt(maxDelaySecs * 1000 + 1);
	}

	public static void main(String... args) {
		final String opt = "--max-delay-secs";
		if (args.length >= 2) {
			if (opt.equals(args[0])) {
				try {
					maxDelaySecs = Integer.parseInt(args[1]);
					if (maxDelaySecs < 0) {
						System.err.printf("Invalid %s. Expected non-negative integer. Supplied: %d"
								+ "%nProceeding with no fake delays...%n", opt, maxDelaySecs);
						maxDelaySecs = 0;
					} else
						rnd = new Random();
				} catch (NumberFormatException ex) {
					System.err.printf("Invalid %s. Expected integer. Supplied: '%s'"
							+ "%nProceeding with no fake delays...%n", opt, args[1]);
				}
			} else
				System.err.printf("Unknown option: '%s'"
						+ "%nUsage: java %s [%s <non-negative int>] [<anything>]"
						+ "%nProceeding anyway...%n", args[0], Main.class.getName(), opt);
		}

		Runtime.getRuntime().addShutdownHook(new Thread("DAO Terminator") {
			@Override
			public void run() { DAO.terminate(); }
		});

		try {
			EmployeesModel employeesModel = new EmployeesModel();

			SwingUtilities.invokeAndWait(() -> {
				JFrame frame = new MainFrame();
				JOptionPane.setRootFrame(frame);

				StartPanel startPanel = new StartPanel();
				CheckInOutPanel checkInOutPanel = new CheckInOutPanel(employeesModel);
				DailyReportPanel dailyReportPanel = new DailyReportPanel(employeesModel);
				HistoryPanel historyPanel = new HistoryPanel(employeesModel);

				final ContentSwitcher switcher2start = new ContentSwitcher(() -> startPanel);
				final ContentSwitcher switcher2checkInOut = new ContentSwitcher(() -> checkInOutPanel);
				final ContentSwitcher switcher2dailyReport = new ContentSwitcher(() -> dailyReportPanel);
				final ContentSwitcher switcher2history = new ContentSwitcher(() -> historyPanel);

				startPanel.addActionListener(e -> {
					switch (e.getActionCommand()) {
						case StartPanel.ACTION_COMMAND_CHECK_IN_OUT: switcher2checkInOut.actionPerformed(e); break;
						case StartPanel.ACTION_COMMAND_DAILY_REPORT: switcher2dailyReport.actionPerformed(e); break;
						case StartPanel.ACTION_COMMAND_HISTORY: switcher2history.actionPerformed(e); break;
						default: reportFailureAndTerminate(new Logging(Main.class).error(
								new LogicError("Forbidden condition"),
								"StartPanel fired an ActionEvent with an unexpected command: " + e.getActionCommand()));
					}
				});
				checkInOutPanel.addActionListener(switcher2start::actionPerformed);
				dailyReportPanel.addActionListener(switcher2start::actionPerformed);
				historyPanel.addActionListener(switcher2start::actionPerformed);

				MainFrame.switchContent(frame, startPanel);
				frame.setVisible(true);
			});
			// InterruptedException - never happens
			// InvocationTargetException - never happens

			DbInitializer.initialize();
			Thread.sleep(getRandomDelay());
			try (EmployeeDAO dao = new EmployeeDAO()) {
				final EmployeeDTO[] employees = dao.selectAllEmployeesOrderedByName();
				SwingUtilities.invokeLater(() -> employeesModel.setEmployees(employees));
			}
		} catch (Throwable err) {
			reportFailureAndTerminate(err);
		}
	}

	static void reportFailureAndTerminate(Throwable err) {
		new Logging(Main.class).error(err, "Unrecoverable error");
		SwingUtilities.invokeLater(() -> {
			// It's OK to block EventDispatchThread (EDT) with a modal dialog.
			// In such a case a secondary loop is started pumping events.
			JOptionPane.showMessageDialog(JOptionPane.getRootFrame(),
					err, "Unrecoverable error", JOptionPane.ERROR_MESSAGE);
			System.exit(-1);
		});
	}
}
