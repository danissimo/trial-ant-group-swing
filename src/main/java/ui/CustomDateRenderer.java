package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/** @author danis.tazeev@gmail.com */
class CustomDateRenderer extends DefaultTableCellRenderer {
	private final DateFormat df = new SimpleDateFormat("dd.MM.yy HH:mm");;
	private Font font;

	CustomDateRenderer() {
		setHorizontalAlignment(SwingConstants.TRAILING);
	}

	@Override
	public Font getFont() {
		if (font == null) {
			Font fontPrototype = super.getFont();
			if (fontPrototype != null)
				font = new Font(Font.MONOSPACED, fontPrototype.getStyle(), fontPrototype.getSize());
		}
		return font;
	}

	@Override
	protected void setValue(Object value) {
		setText(value == null ? "" : df.format((Date)value));
	}
}
