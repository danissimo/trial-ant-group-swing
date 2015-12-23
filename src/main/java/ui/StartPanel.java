package ui;

import javax.swing.*;
import java.awt.event.ActionListener;

/** @author danis.tazeev@gmail.com */
final class StartPanel extends ActionContent {
	static final String ACTION_COMMAND_CHECK_IN_OUT = "checkInOut";
	static final String ACTION_COMMAND_DAILY_REPORT = "dailyReport";
	static final String ACTION_COMMAND_HISTORY = "history";

	StartPanel() {
		ActionListener relay = this::fireActionPerformed;

		JButton checkInOut = new JButton("Отметиться");
		checkInOut.setActionCommand(ACTION_COMMAND_CHECK_IN_OUT);
		checkInOut.addActionListener(relay);

		JButton dailyReport = new JButton("Отчёт за день");
		dailyReport.setActionCommand(ACTION_COMMAND_DAILY_REPORT);
		dailyReport.addActionListener(relay);

		JButton history = new JButton("История");
		history.setActionCommand(ACTION_COMMAND_HISTORY);
		history.addActionListener(relay);

		add(checkInOut);
		add(dailyReport);
		add(history);
	}
}
