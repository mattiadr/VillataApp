package client;

import server.MainFrame;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientFrame {

	private final MainFrame mainFrame;

	// networking
	private BufferedReader reader;
	private PrintWriter writer;

	// ui elements
	private JFrame frame;
	private JDialog dialog;
	private JPanel panel;
	private JTextField nameField;
	private JTextField numField;
	private JTextField timeField;
	private JTextField notesField;
	private JCheckBox replaceBox;
	private JButton sendButton;
	private JButton button2;

	private String lastName = null;
	private String lastNum = null;
	private String lastTime = null;
	private String lastNotes = null;

	public ClientFrame(MainFrame mainFrame) {

		this.mainFrame = mainFrame;

		if (mainFrame == null) {
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
		}

		// time regex
		Pattern timeRegex = Pattern.compile("\\D*(\\d+)\\D*(\\d+)?\\D*");

		// text field settings
		KeyAdapter keyListener = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					sendButton.doClick();
				}
			}
		};
		nameField.addKeyListener(keyListener);
		numField.addKeyListener(keyListener);
		timeField.addKeyListener(keyListener);
		notesField.addKeyListener(keyListener);

		// checkbox settings
		replaceBox.setFocusable(false);

		// sendButton settings
		sendButton.setFocusable(false);
		sendButton.addActionListener(e -> {
			String name = nameField.getText();
			if (name.isEmpty()) {
				JOptionPane.showMessageDialog(frame, "Il nome non può essere vuoto.", "Errore", JOptionPane.ERROR_MESSAGE);
				nameField.requestFocus();
				return;
			}

			int num;
			try {
				num = Integer.parseInt(numField.getText());
			} catch (NumberFormatException x) {
				JOptionPane.showMessageDialog(frame, "Il numero di posti deve essere un intero.", "Errore", JOptionPane.ERROR_MESSAGE);
				numField.requestFocus();
				return;
			}
			if (num <= 0) {
				JOptionPane.showMessageDialog(frame, "Il numero di posti deve essere maggiore di zero.", "Errore", JOptionPane.ERROR_MESSAGE);
				numField.requestFocus();
				return;
			}

			String time = timeField.getText();
			long addedTimestamp = new Date().getTime();
			long reservedTimestamp;
			if (time.isEmpty()) {
				reservedTimestamp = 0;
			} else {
				Matcher m = timeRegex.matcher(time);
				if (!m.find() || m.group(1) == null) {
					JOptionPane.showMessageDialog(frame, "Il campo dell' ora deve essere vuoto o contenere un ora valida.", "Errore", JOptionPane.ERROR_MESSAGE);
					timeField.requestFocus();
					return;
				}
				int hh = Integer.parseInt(m.group(1));
				int mm = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
				if (hh < 0 || hh >= 24 || mm < 0 || mm >= 60) {
					JOptionPane.showMessageDialog(frame, "Ora o minuti fuori dal range corretto.", "Errore", JOptionPane.ERROR_MESSAGE);
					timeField.requestFocus();
					return;
				}
				Calendar c = Calendar.getInstance();
				c.setTime(new Date());
				if (hh < 12) c.add(Calendar.DATE, 1);
				c.set(Calendar.HOUR_OF_DAY, hh);
				c.set(Calendar.MINUTE, mm);
				c.set(Calendar.SECOND, 0);
				c.set(Calendar.MILLISECOND, 0);
				reservedTimestamp = c.getTimeInMillis();
			}

			String notes = notesField.getText().replaceAll(",", " ");

			if (mainFrame == null) {
				// send to writer
				writer.printf("%s,%d,%d,%d,%s,%s\n", name, num, addedTimestamp, reservedTimestamp, notes, replaceBox.isSelected());

				try {
					int ret = Integer.parseInt(reader.readLine());
					if (ret == -1) {
						if (!replaceBox.isSelected()) {
							JOptionPane.showMessageDialog(frame, "\"" + name + "\" è già stato utilizzato.", "Errore", JOptionPane.WARNING_MESSAGE);
						} else {
							JOptionPane.showMessageDialog(frame, "\"" + name + "\" non può essere sostituito perché non esiste.", "Errore", JOptionPane.WARNING_MESSAGE);
						}
						nameField.requestFocus();
						return;
					} else {
						JOptionPane.showMessageDialog(frame, "Sono presenti " + ret + " persone in coda.", "Info", JOptionPane.INFORMATION_MESSAGE);
					}
				} catch (IOException x) {
					JOptionPane.showMessageDialog(frame, "Errore di comunicazione con il server.", "Errore", JOptionPane.ERROR_MESSAGE);
					return;
				}
			} else {
				// send to mainFrame
				if (mainFrame.isNameTaken(name, replaceBox.isSelected())) {
					if (!replaceBox.isSelected()) {
						JOptionPane.showMessageDialog(frame, "\"" + name + "\" è già stato utilizzato.", "Errore", JOptionPane.WARNING_MESSAGE);
					} else {
						JOptionPane.showMessageDialog(frame, "\"" + name + "\" non può essere sostituito perché non esiste.", "Errore", JOptionPane.WARNING_MESSAGE);
					}
					nameField.requestFocus();
					return;
				} else {
					long que = mainFrame.addReservation(name, num, addedTimestamp, reservedTimestamp, notes, true);
					JOptionPane.showMessageDialog(frame, "Sono presenti " + que + " persone in coda.", "Info", JOptionPane.INFORMATION_MESSAGE);
					dialog.dispose();
				}
			}

			lastName = nameField.getText();
			lastNum = numField.getText();
			lastTime = timeField.getText();
			lastNotes = notesField.getText();

			nameField.setText("");
			numField.setText("");
			timeField.setText("");
			notesField.setText("");
			replaceBox.setSelected(false);
			nameField.requestFocus();
		});

		// button2 settings
		button2.setText(mainFrame == null ? "Modifica Ultimo" : "Annulla");
		button2.setFocusable(false);
		if (mainFrame == null) {
			button2.addActionListener(e -> {
				if (lastName == null) {
					JOptionPane.showMessageDialog(frame, "Nessuna prenotazione modificabile.", "Errore", JOptionPane.ERROR_MESSAGE);
					return;
				}

				nameField.setText(lastName);
				numField.setText(lastNum);
				timeField.setText(lastTime);
				notesField.setText(lastNotes);
				replaceBox.setSelected(true);
				nameField.requestFocus();
			});
		} else {
			button2.addActionListener(e -> dialog.dispose());
		}

		if (mainFrame == null) {
			// create new frame
			frame = new JFrame("VillataApp Client");
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
		} else {
			// create new dialog
			dialog = new JDialog(mainFrame.frame, "Inserisci Prenotazione", true);
			dialog.setContentPane(panel);
			dialog.pack();
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		}
	}

	public static void main(String[] args) {
		new ClientFrame(null);
	}
}
