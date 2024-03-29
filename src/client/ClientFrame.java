package client;

import server.Reservation;
import server.ServerMain;
import server.TableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Timer;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class ClientFrame {

	public static final long REQUEST_INTERVAL = 5 * 60 * 1000;

	// networking
	public ObjectInputStream reader;
	public ObjectOutputStream writer;

	private JPanel panel;
	private JScrollPane queueScrollPane;
	private JScrollPane inputScrollPane;
	private final InputPanel inputPanel;
	private JLabel countsLabel;
	private JLabel remainingLabel;

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

	// remaining people list
	private ArrayList<Integer> remainingPeople = null;

	public ClientFrame() {
		// get ip
		String ip = JOptionPane.showInputDialog("Inserisci IP server");
		if (ip == null) {
			System.exit(0);
		}

		try {
			// establish connection
			Socket socket = new Socket(ip, ServerMain.SERVER_SOCKET_PORT);
			writer = new ObjectOutputStream(socket.getOutputStream());
			reader = new ObjectInputStream(socket.getInputStream());
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
		// add click listener
		queueTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int column = queueTable.getColumnModel().getColumnIndexAtX(e.getX()); // get the column of the button
				int row = e.getY() / queueTable.getRowHeight(); //get the row of the button

				// Checking the row or column is valid or not
				if (row < 0 || row >= queueTable.getRowCount() || column < 0 || column >= queueTable.getColumnCount())
					return;

				// single click
				if (e.getButton() == 1 && e.getClickCount() == 1) {
					setRemainingLabel(queueTable.getSelectedRow());
				}

				// double click
				if (e.getButton() == 1 && e.getClickCount() == 2) {
					Reservation r = queueModel.getData().get(row);
					inputPanel.editReservation(r);
				}
			}
		});

		// queue scroll pane props
		queueScrollPane.getViewport().add(queueTable);
		queueScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		queueScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		queueScrollPane.setPreferredSize(new Dimension(queueTable.getPreferredSize().width, queueScrollPane.getPreferredSize().height));

		// add input panel
		inputPanel = new InputPanel(null, null, this);
		inputScrollPane.getViewport().add(inputPanel.panel);

		// create new frame
		// ui elements
		JFrame frame = new JFrame("VillataApp Client");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				try {
					writer.writeObject(Message.end());
				} catch (IOException e) {
					JOptionPane.showMessageDialog(null, "Errore durante la chiusura della connessione.", "Errore", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		frame.setContentPane(panel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	public void setQueueData(List<Reservation> data) {
		queueData.clear();
		queueData.addAll(data);
		queueModel.refresh();
		int tot = queueData.stream().mapToInt(Reservation::getNum).sum();
		String text = "Prenotazioni: " + queueData.size() + ". Totale persone: " + tot + ".";
		countsLabel.setText(text);

		remainingPeople = null;
	}

	private void buildRemaining() {
		remainingPeople = new ArrayList<>();

		remainingPeople.add(0);
		queueData.forEach(r -> remainingPeople.add(remainingPeople.get(remainingPeople.size() - 1) + r.getNum()));
	}

	public void setRemainingLabel(int row) {
		if (remainingPeople == null)
			buildRemaining();
		int remaining = remainingPeople.get(row);
		String text = "Persone precedenti: " + remaining + ".";
		remainingLabel.setText(text);
	}

	public static ReentrantLock mutex = new ReentrantLock();

	public static void main(String[] args) {
		ClientFrame frame = new ClientFrame();

		// start requesting queue data periodically
		new Timer().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					mutex.lock();
					frame.writer.writeObject(Message.dataRequest());
					Object response = frame.reader.readObject();
					if (!(response instanceof Message))
						throw new ClassNotFoundException("L'oggetto ricevuto non è un Message");
					if (!((Message) response).isQueueData)
						throw new ClassNotFoundException("Il messaggio ricevuto non è un queueData");
					frame.setQueueData(((Message) response).data);
				} catch (IOException | ClassNotFoundException e) {
					JOptionPane.showMessageDialog(null, "Errore di comunicazione con il server durante l'aggiornamento periodico.", "Errore", JOptionPane.ERROR_MESSAGE);
				} finally {
					mutex.unlock();
				}
			}
		}, 0, REQUEST_INTERVAL);
	}

}
