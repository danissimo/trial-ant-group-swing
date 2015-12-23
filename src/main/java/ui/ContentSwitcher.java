package ui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Supplier;

/** @author danis.tazeev@gmail.com */
final class ContentSwitcher implements ActionListener {
	private final Supplier<JPanel> contentSupplier;

	ContentSwitcher(Supplier<JPanel> contentSupplier) {
		if (contentSupplier == null)
			throw new IllegalArgumentException("contentSupplier = null");
		this.contentSupplier = contentSupplier;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JFrame frame = (JFrame)((JComponent)e.getSource()).getRootPane().getParent();
		MainFrame.switchContent(frame, contentSupplier.get());
	}
}
