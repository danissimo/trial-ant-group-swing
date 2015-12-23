package ui;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** @author danis.tazeev@gmail.com */
abstract class ActionContent extends JPanel {
	private final EventListenerList listeners = new EventListenerList();

	ActionContent() {}

	ActionContent(LayoutManager layout) {
		super(layout);
	}

	void addActionListener(ActionListener l) {
		if (l == null)
			throw new IllegalArgumentException("l = null");
		listeners.add(ActionListener.class, l);
	}

	void removeActionListener(ActionListener l) {
		if (l == null)
			return;
		listeners.remove(ActionListener.class, l);
	}

	void fireActionPerformed(ActionEvent e) {
		if (e == null)
			throw new IllegalArgumentException("e = null");
		Object[] lx = listeners.getListenerList();
		assert lx != null;
		if (lx.length <= 0)
			return;
		ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, e.getActionCommand(), e.getWhen(), 0);
		for (int i = lx.length - 1; i > 0; i -= 2) {
			assert lx[i - 1] == ActionListener.class;
			((ActionListener)lx[i]).actionPerformed(event);
		}
	}
}
