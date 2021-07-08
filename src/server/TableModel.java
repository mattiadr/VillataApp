package server;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public abstract class TableModel extends AbstractTableModel {

	private final String[] columnNames;
	private final Class<?>[] columnTypes;

	private final List<Reservation> data;

	public TableModel(String[] columnNames, Class<?>[] columnTypes, List<Reservation> data) {
		this.columnNames = columnNames;
		this.columnTypes = columnTypes;
		this.data = data;
	}

	public List<Reservation> getData() {
		return data;
	}

	@Override
	public int getRowCount() {
		return data.size();
	}

	@Override
	public int getColumnCount() {
		// use -1 to hide ID column
		return columnNames.length - 1;
	}

	@Override
	public String getColumnName(int columnIndex) {
		return columnNames[columnIndex];
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return columnTypes[columnIndex];
	}

	@Override
	public abstract Object getValueAt(int rowIndex, int columnIndex);

	public void refresh() {
		fireTableDataChanged();
	}
}
