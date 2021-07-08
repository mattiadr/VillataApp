package server;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class ExtTableModel extends AbstractTableModel {

	private final List<Reservation> data;

	public ExtTableModel(List<Reservation> data) {
		this.data = data;
	}

	@Override
	public int getRowCount() {
		return 2;
	}

	@Override
	public int getColumnCount() {
		return 2;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (rowIndex + columnIndex * 2 >= data.size()) {
			return null;
		}

		Reservation r = data.get(rowIndex + columnIndex * 2);
		return "<html><b>" + r.getName().toUpperCase() + "</b> (" + r.getNum() + ")</html>";
	}

	public void refresh() {
		fireTableDataChanged();
	}

}
