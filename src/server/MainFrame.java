package server;

import client.InputPanel;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Timer;
import java.util.*;
import java.util.stream.Collectors;

public class MainFrame {

	public final static Font mainFont = new Font("SansSerif", Font.PLAIN, 20);
	public final static String BACKUP_LOCATION = FileSystemView.getFileSystemView().getDefaultDirectory().getPath() + "/VillataApp.bak";
	public final static String LOG_LOCATION = FileSystemView.getFileSystemView().getDefaultDirectory().getPath() + "/VillataApp.log";
	public static final long UPDATE_INTERVAL = 5 * 60 * 1000; // 5 min
	public static final long TIME_DUE = 20 * 60 * 1000; // 20 min

	public final JFrame frame;
	public PrintWriter logWriter = null;

	// main panel
	private JPanel panel;

	// queue table data and objects
	private final List<Reservation> queueData = Collections.synchronizedList(new ArrayList<>());
	private final List<Reservation> queueDataApparent = Collections.synchronizedList(new ArrayList<>());
	private final TableModel queueModel = new TableModel(new String[]{"Nome", "Posti", "Ora Ins.", "Ora Pren.", "Note", "Chiama", "Rimuovi", "ID"},
			new Class<?>[]{String.class, Integer.class, String.class, String.class, String.class, JButton.class, JButton.class, Long.class}, queueDataApparent) {
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
					return getData().get(rowIndex).getCall();
				case 6:
					return getData().get(rowIndex).getRemove();
				case 7:
					return getData().get(rowIndex).getId();
				default:
					return null;
			}
		}
	};
	private final JTable queueTable = new JTable(queueModel);

	// queue scroll pane
	private JScrollPane queueScrollPane;

	// screen table data and objects
	private final List<Reservation> screenData = Collections.synchronizedList(new ArrayList<>());
	private final TableModel screenModel = new TableModel(new String[]{"Nome", "Posti", "Note", "Conferma", "Rimuovi", "ID"},
			new Class<?>[]{String.class, Integer.class, String.class, JButton.class, Long.class}, screenData) {
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
				case 0:
					return getData().get(rowIndex).getName();
				case 1:
					return getData().get(rowIndex).getNum();
				case 2:
					return getData().get(rowIndex).getNotes();
				case 3:
					return getData().get(rowIndex).getConfirm();
				case 4:
					return getData().get(rowIndex).getRemove();
				case 5:
					return getData().get(rowIndex).getId();
				default:
					return null;
			}
		}
	};
	private final JTable screenTable = new JTable(screenModel);

	// screen scroll pane
	private JScrollPane screenScrollPane;

	// waiting list table data and objects
	private final List<Reservation> waitingData = Collections.synchronizedList(new ArrayList<>());
	private final TableModel waitingModel = new TableModel(new String[]{"Nome", "Posti", "Note", "Completa", "ID"},
			new Class<?>[]{String.class, Integer.class, String.class, JButton.class, Long.class}, waitingData) {
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
				case 0:
					return getData().get(rowIndex).getName();
				case 1:
					return getData().get(rowIndex).getNum();
				case 2:
					return getData().get(rowIndex).getNotes();
				case 3:
					return getData().get(rowIndex).getComplete();
				case 4:
					return getData().get(rowIndex).getId();
				default:
					return null;
			}
		}
	};
	private final JTable waitingTable = new JTable(waitingModel);

	// waiting list scroll pane
	private JScrollPane waitingScrollPane;

	// top buttons
	private JButton addButton;
	private JCheckBox queueFilterCheckBox;

	// bottom labels
	private JLabel countsLabel;
	private JLabel ipLabel;

	public MainFrame() {

		// open log file
		try {
			logWriter = new PrintWriter(new BufferedWriter(new FileWriter(LOG_LOCATION, true)));
			logWriter.println("LOG START " + java.time.LocalDateTime.now());
			logWriter.println("ID,NAME,NUMBER,ADDED_TIMESTAMP,RESERVED_TIMESTAMP,CALLED_TIMESTAMP,CONFIRMED_TIMESTAMP,COMPLETED_TIMESTAMP,NOTES,STATUS");
			logWriter.flush();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Impossibile aprire il file di LOG.", "Errore", JOptionPane.ERROR_MESSAGE);
		}

		// load backup
		loadData();

		// set queue table props
		// make non selectable
		queueTable.setFocusable(false);
		queueTable.setRowSelectionAllowed(true);
		// set font
		queueTable.setFont(mainFont);
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
		queueTable.getColumnModel().getColumn(5).setPreferredWidth(80);
		queueTable.getColumnModel().getColumn(6).setPreferredWidth(80);
		queueTable.getColumnModel().getColumn(1).setResizable(false);
		queueTable.getColumnModel().getColumn(2).setResizable(false);
		queueTable.getColumnModel().getColumn(3).setResizable(false);
		queueTable.getColumnModel().getColumn(4).setResizable(false);
		queueTable.getColumnModel().getColumn(5).setResizable(false);
		queueTable.getColumnModel().getColumn(6).setResizable(false);
		// set button render
		TableCellRenderer buttonRenderer = (table, value, isSelected, hasFocus, row, column) -> (JButton) value;
		queueTable.getColumn("Chiama").setCellRenderer(buttonRenderer);
		queueTable.getColumn("Rimuovi").setCellRenderer(buttonRenderer);
		// add click listener
		queueTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int column = queueTable.getColumnModel().getColumnIndexAtX(e.getX()); // get the column of the button
				int row = e.getY() / queueTable.getRowHeight(); //get the row of the button

				/*Checking the row or column is valid or not*/
				if (row < queueTable.getRowCount() && row >= 0 && column < queueTable.getColumnCount() && column >= 0) {
					Object value = queueTable.getValueAt(row, column);
					if (value instanceof JButton) {
						/*perform a click event*/
						((JButton) value).doClick();
					}
				}
			}
		});

		// queue scroll pane props
		queueScrollPane.getViewport().add(queueTable);
		queueScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		queueScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		queueScrollPane.setPreferredSize(new Dimension(queueTable.getPreferredSize().width, queueScrollPane.getPreferredSize().height));

		// set screen table props
		// make non selectable
		screenTable.setFocusable(false);
		screenTable.setRowSelectionAllowed(false);
		// set font
		screenTable.setFont(mainFont);
		// center 2nd col
		screenTable.getColumnModel().getColumn(1).setCellRenderer(centerCellRenderer);
		// increase row height
		screenTable.setRowHeight(screenTable.getRowHeight() + 6);
		// set col width
		screenTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
		screenTable.getColumnModel().getColumn(0).setPreferredWidth(200);
		screenTable.getColumnModel().getColumn(1).setPreferredWidth(40);
		screenTable.getColumnModel().getColumn(2).setPreferredWidth(200);
		screenTable.getColumnModel().getColumn(3).setPreferredWidth(80);
		screenTable.getColumnModel().getColumn(4).setPreferredWidth(80);
		screenTable.getColumnModel().getColumn(1).setResizable(false);
		screenTable.getColumnModel().getColumn(2).setResizable(false);
		screenTable.getColumnModel().getColumn(3).setResizable(false);
		screenTable.getColumnModel().getColumn(4).setResizable(false);
		// set button render
		screenTable.getColumn("Conferma").setCellRenderer(buttonRenderer);
		screenTable.getColumn("Rimuovi").setCellRenderer(buttonRenderer);
		// add click listener
		screenTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int column = screenTable.getColumnModel().getColumnIndexAtX(e.getX()); // get the column of the button
				int row = e.getY() / screenTable.getRowHeight(); //get the row of the button

				/*Checking the row or column is valid or not*/
				if (row < screenTable.getRowCount() && row >= 0 && column < screenTable.getColumnCount() && column >= 0) {
					Object value = screenTable.getValueAt(row, column);
					if (value instanceof JButton) {
						/*perform a click event*/
						((JButton) value).doClick();
					}
				}
			}
		});

		// screen scroll pane props
		screenScrollPane.getViewport().add(screenTable);
		screenScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		screenScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		screenScrollPane.setPreferredSize(new Dimension(screenTable.getPreferredSize().width, screenScrollPane.getPreferredSize().height));

		// set waiting list table props
		// make non selectable
		waitingTable.setFocusable(false);
		waitingTable.setRowSelectionAllowed(false);
		// set font
		waitingTable.setFont(mainFont);
		// center 2nd col
		waitingTable.getColumnModel().getColumn(1).setCellRenderer(centerCellRenderer);
		// increase row height
		waitingTable.setRowHeight(waitingTable.getRowHeight() + 6);
		// set col width
		waitingTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
		waitingTable.getColumnModel().getColumn(0).setPreferredWidth(200);
		waitingTable.getColumnModel().getColumn(1).setPreferredWidth(40);
		waitingTable.getColumnModel().getColumn(2).setPreferredWidth(200);
		waitingTable.getColumnModel().getColumn(3).setPreferredWidth(80);
		waitingTable.getColumnModel().getColumn(1).setResizable(false);
		waitingTable.getColumnModel().getColumn(2).setResizable(false);
		waitingTable.getColumnModel().getColumn(3).setResizable(false);
		// set button render
		waitingTable.getColumn("Completa").setCellRenderer(buttonRenderer);
		// add click listener
		waitingTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int column = waitingTable.getColumnModel().getColumnIndexAtX(e.getX()); // get the column of the button
				int row = e.getY() / waitingTable.getRowHeight(); //get the row of the button

				/*Checking the row or column is valid or not*/
				if (row < waitingTable.getRowCount() && row >= 0 && column < waitingTable.getColumnCount() && column >= 0) {
					Object value = waitingTable.getValueAt(row, column);
					if (value instanceof JButton) {
						/*perform a click event*/
						((JButton) value).doClick();
					}
				}
			}
		});

		// screen scroll pane props
		waitingScrollPane.getViewport().add(waitingTable);
		waitingScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		waitingScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		waitingScrollPane.setPreferredSize(new Dimension(waitingTable.getPreferredSize().width, waitingScrollPane.getPreferredSize().height));

		// add top buttons
		MainFrame mf = this;
		addButton.setFocusable(false);
		addButton.addActionListener(e -> new InputPanel(mf, null));

		queueFilterCheckBox.setFocusable(false);
		queueFilterCheckBox.addActionListener(e -> updateQueueData());

		// set ip to label
		try (final DatagramSocket socket = new DatagramSocket()) {
			socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			ipLabel.setText("IP: " + socket.getLocalAddress().getHostAddress());
		} catch (SocketException | UnknownHostException e) {
			ipLabel.setText("IP: unknown");
		}
		// add mouse listener to label
		ipLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try (final DatagramSocket socket = new DatagramSocket()) {
					socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
					ipLabel.setText("IP: " + socket.getLocalAddress().getHostAddress());
				} catch (SocketException | UnknownHostException ex) {
					ipLabel.setText("IP: unknown");
				}
			}
		});

		// create new frame
		// frame
		frame = new JFrame("VillataApp Server");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setContentPane(panel);

		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		// update counts
		updateTotalCounts();

		// start refreshing list periodically
		new Timer().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				updateQueueData();
			}
		}, UPDATE_INTERVAL, UPDATE_INTERVAL);
	}

	private void updateQueueData() {
		queueDataApparent.clear();
		if (queueFilterCheckBox.isSelected()) {
			queueDataApparent.addAll(queueData);
		} else {
			queueDataApparent.addAll(queueData.stream().filter(r -> {
				long now = new Date().getTime();
				return r.getReservedTimestamp() == 0 || r.getReservedTimestamp() - now <= TIME_DUE;
			}).collect(Collectors.toList()));
		}
		queueModel.refresh();
	}

	private void updateTotalCounts() {
		int tot = queueData.stream().mapToInt(Reservation::getNum).sum();
		String text = "Prenotazioni: " + queueData.size() + ". Totale persone: " + tot;
		countsLabel.setText(text);
	}

	public boolean isNameTaken(String name, boolean replace) {
		Optional<Reservation> res = queueData.stream().filter((r) -> r.getName().equals(name)).findFirst();
		if (res.isPresent()) {
			if (replace) {
				removeReservation(res.get().getId(), false);
				return false;
			} else {
				return true;
			}
		} else {
			// we return replace so that if replace is true and name is not present we get an error
			return replace;
		}
	}

	public void addReservation(String name, int num, long addedTimestamp, long reservedTimestamp, String notes, boolean backup) {
		queueData.add(new Reservation(name, num, addedTimestamp, reservedTimestamp, notes, this));
		queueData.sort(Comparator.comparingLong(Reservation::getAddedTimestamp).thenComparingLong(Reservation::getId));
		updateQueueData();
		if (backup) backupData();

		updateTotalCounts();
	}

	public void callReservation(long id) {
		Reservation r = removeReservation(id, false);
		if (r != null) {
			screenData.add(r);
			screenModel.refresh();
		}
	}

	public void confirmReservation(long id) {
		Reservation r = removeReservation(id, false);
		if (r != null) {
			waitingData.add(r);
			waitingModel.refresh();
		}
	}

	public void completeReservation(long id) {
		Reservation res = waitingData.stream().filter(r -> r.getId() == id).findAny().orElse(null);
		if (res == null) return;
		logReservation(res, "COMPLETED");
		waitingData.remove(res);
		waitingModel.refresh();
	}

	public Reservation removeReservation(long id, boolean log) {
		Reservation res = null;
		for (Reservation r : queueData) {
			if (r.getId() == id) {
				if (log)
					logReservation(r, "REMOVED_ON_QUEUE");
				queueData.remove(r);
				updateQueueData();
				res = r;
				break;
			}
		}

		for (Reservation r : screenData) {
			if (r.getId() == id) {
				if (log)
					logReservation(r, "REMOVED_ON_SCREEN");
				screenData.remove(r);
				screenModel.refresh();
				res = r;
				break;
			}
		}

		backupData();

		updateTotalCounts();

		return res;
	}

	public boolean showBackupFailure = true;

	public void backupData() {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(BACKUP_LOCATION));
			oos.writeObject(queueData);
			oos.close();
		} catch (IOException e) {
			if (showBackupFailure) {
				showBackupFailure = false;
				new Thread(() -> JOptionPane.showMessageDialog(null, "Impossibile effettuare backup.\nQuesto messaggio non verrà più mostrato", "Errore", JOptionPane.ERROR_MESSAGE)).start();
			}
		}
	}

	public void loadData() {
		try {
			ObjectInputStream ois = new ObjectInputStream((new FileInputStream(BACKUP_LOCATION)));
			@SuppressWarnings("unchecked")
			List<Reservation> tmp = (List<Reservation>) ois.readObject();
			for (Reservation r : tmp) {
				addReservation(r.getName(), r.getNum(), r.getAddedTimestamp(), r.getReservedTimestamp(), r.getNotes(), false);
			}
			backupData();
			ois.close();
		} catch (FileNotFoundException e) {
			// no backup file
		} catch (IOException | ClassNotFoundException e) {
			JOptionPane.showMessageDialog(null, "Impossibile ricaricare backup.", "Errore", JOptionPane.ERROR_MESSAGE);
		}
	}

	public List<Reservation> getScreenData() {
		return screenData;
	}

	public void logReservation(Reservation r, String status) {
		if (logWriter == null) return;
		// ID, NAME, NUMBER, ADDED_TIMESTAMP, RESERVED_TIMESTAMP, CALLED_TIMESTAMP, CONFIRMED_TIMESTAMP, COMPLETED_TIMESTAMP, NOTES, STATUS
		logWriter.printf("%d,%s,%d,%d,%d,%d,%d,%d,%s,%s%n", r.getId(), r.getName(), r.getNum(), r.getAddedTimestamp(),
				r.getReservedTimestamp(), r.getCalledTimestamp(), r.getConfirmedTimestamp(), r.getCompletedTimestamp(), r.getNotes(), status);
		logWriter.flush();
	}

	public List<Reservation> getQueueData() {
		return queueData;
	}

}
