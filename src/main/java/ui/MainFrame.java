package ui;

import javax.swing.*;
import java.awt.*;

/** @author danis.tazeev@gmail.com */
final class MainFrame extends JFrame {
	MainFrame() {
		super("Учёт времени");
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setResizable(false);
	}

	static void switchContent(JFrame frame, Container content) {
		if (frame == null)
			throw new IllegalArgumentException("frame = null");
		if (content == null)
			throw new IllegalArgumentException("content = null");

		frame.setContentPane(content);
		packAndCenter(frame);
	}

	static void packAndCenter(JFrame frame) {
		if (frame == null)
			throw new IllegalArgumentException("frame = null");
		frame.pack();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		Point centerPoint = ge.getCenterPoint();
		centerPoint.translate(-frame.getWidth() >> 1, -frame.getHeight() >> 1);
		frame.setLocation(centerPoint);
	}

	static void packAndCenterFrameOf(JComponent comp) {
		if (comp == null)
			throw new IllegalArgumentException("comp = null");
		JRootPane rootPane = comp.getRootPane();
		if (rootPane != null) // whether the comp is contained in a JFrame?
			packAndCenter((JFrame)rootPane.getParent());
	}
}
