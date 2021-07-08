package client;

import server.Reservation;
import server.TableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientFrame {

	// networking
	public BufferedReader reader;
	public PrintWriter writer;

	private JScrollPane queueScrollPane;
	private JScrollPane inputScrollPane;
	private JPanel panel;

	// queue table data and objects
	private final List<Reservation> queueData = Collections.synchronizedList(new ArrayList<>());
	private final TableModel queueModel = new TableModel(new String[]{"Nome", "Posti", "Ora Ins.", "Ora Pren.", "Note", "ID"},
			new Class<?>[]{String.class, Integer.class, String.class, String.class, String.class, Long.class}, queueData) {
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
				case 0:
					return getData().get(rowIndex).getName();
				case 1:
					return getData().get(rowIndex).getNum();
				case 2:
					return getData().get(rowIndex).getAddedTime();
				case 3:
					return getData().get(rowIndex).getReservedTime();
				case 4:
					return getData().get(rowIndex).getNotes();
				case 5:
					return getData().get(rowIndex).getId();
				default:
					return null;
			}
		}
	};

	public ClientFrame() {
		// get ip
		String ip = JOptionPane.showInputDialog("Inserisci IP server");
		if (ip == null) {
			System.exit(0);
		}

		try {
			// establish connection
			Socket socket = new Socket(ip, 5000);
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Impossibile connettersi al server.", "Errore", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}

		// set queue table props
		JTable queueTable = new JTable(queueModel);
		// make non selectable
		queueTable.setFocusable(false);
		queueTable.setRowSelectionAllowed(true);
		// set font
		queueTable.setFont(new Font("SansSerif", Font.PLAIN, 20));
		// center cols
		DefaultTableCellRenderer centerCellRenderer = new DefaultTableCellRenderer();
		centerCellRenderer.setHorizontalAlignment(JLabel.CENTER);
		queueTable.getColumnModel().getColumn(1).setCellRenderer(centerCellRenderer);
		queueTable.getColumnModel().getColumn(2).setCellRenderer(centerCellRenderer);
		queueTable.getColumnModel().getColumn(3).setCellRenderer(centerCellRenderer);
		// increase row height
		queueTable.setRowHeight(queueTable.getRowHeight() + 6);
		// set col width
		queueTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
		queueTable.getColumnModel().getColumn(0).setPreferredWidth(200);
		queueTable.getColumnModel().getColumn(1).setPreferredWidth(25);
		queueTable.getColumnModel().getColumn(2).setPreferredWidth(50);
		queueTable.getColumnModel().getColumn(3).setPreferredWidth(50);
		queueTable.getColumnModel().getColumn(4).setPreferredWidth(200);
		queueTable.getColumnModel().getColumn(1).setResizable(false);
		queueTable.getColumnModel().getColumn(2).setResizable(false);
		queueTable.getColumnModel().getColumn(3).setResizable(false);
		queueTable.getColumnModel().getColumn(4).setResizable(false);

		// queue scroll pane props
		queueScrollPane.getViewport().add(queueTable);
		queueScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		queueScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		queueScrollPane.setPreferredSize(new Dimension(queueTable.getPreferredSize().width, queueScrollPane.getPreferredSize().height));

		// add input panel
		InputPanel inputPanel = new InputPanel(null, this);
		inputScrollPane.getViewport().add(inputPanel.panel);

		// create new frame
		// ui elements
		JFrame frame = new JFrame("VillataApp Client");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				writer.println("/end");
			}
		});
		frame.setContentPane(panel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		new ClientFrame();
	}

}
