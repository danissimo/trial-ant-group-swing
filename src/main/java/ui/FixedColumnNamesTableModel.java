package ui;

import javax.swing.table.AbstractTableModel;

/** @author danis.tazeev@gmail.com */
abstract class FixedColumnNamesTableModel extends AbstractTableModel {
	private final String[] columnNames;

	FixedColumnNamesTableModel(String[] columnNames) {
		if (columnNames == null)
			throw new IllegalArgumentException("columnNames = null");
		this.columnNames = columnNames;
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public String getColumnName(int col) {
		return columnNames[col];
	}
}
