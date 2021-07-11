package client;

import server.MainFrame;
import server.Reservation;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputPanel {

	// ui elements
	private JDialog dialog;
	public JPanel panel;

	// input panel elements
	private JTextField nameField;
	private JTextField numField;
	private JTextField timeField;
	private JTextField notesField;
	private JButton sendButton;
	private JButton cancelButton;

	private boolean isEditMode = false;
	private long editingId = -1;

	// editing reservation must be set only if we called this from MainFrame and not when called from ClientFrame
	public InputPanel(MainFrame mainFrame, Reservation editingReservation, ClientFrame clientFrame) {
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

		// sendButton settings
		sendButton.setFocusable(false);
		sendButton.addActionListener(e -> {
			String name = nameField.getText().replaceAll(",", "").trim();
			if (name.isEmpty()) {
				JOptionPane.showMessageDialog(null, "Il nome non può essere vuoto.", "Errore", JOptionPane.ERROR_MESSAGE);
				nameField.requestFocus();
				return;
			}

			int num;
			try {
				num = Integer.parseInt(numField.getText().trim());
			} catch (NumberFormatException x) {
				JOptionPane.showMessageDialog(null, "Il numero di posti deve essere un intero.", "Errore", JOptionPane.ERROR_MESSAGE);
				numField.requestFocus();
				return;
			}
			if (num <= 0) {
				JOptionPane.showMessageDialog(null, "Il numero di posti deve essere maggiore di zero.", "Errore", JOptionPane.ERROR_MESSAGE);
				numField.requestFocus();
				return;
			}

			String time = timeField.getText().trim();
			long addedTimestamp = new Date().getTime();
			long reservedTimestamp;
			if (time.isEmpty()) {
				reservedTimestamp = 0;
			} else {
				Matcher m = timeRegex.matcher(time);
				if (!m.find() || m.group(1) == null) {
					JOptionPane.showMessageDialog(null, "Il campo dell' ora deve essere vuoto o contenere un ora valida.", "Errore", JOptionPane.ERROR_MESSAGE);
					timeField.requestFocus();
					return;
				}
				int hh = Integer.parseInt(m.group(1));
				int mm = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
				if (hh < 0 || hh >= 24 || mm < 0 || mm >= 60) {
					JOptionPane.showMessageDialog(null, "Ora o minuti fuori dal range corretto.", "Errore", JOptionPane.ERROR_MESSAGE);
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

			String notes = notesField.getText().replaceAll(",", " ").trim();

			if (mainFrame == null) {
				// send to writer
				try {
					// send reservation to server
					ClientFrame.mutex.lock();
					if (!isEditMode) {
						// send new reservation
						clientFrame.writer.writeObject(Message.addReservation(name, num, addedTimestamp, reservedTimestamp, notes));
					} else {
						// send edit reservation
						clientFrame.writer.writeObject(Message.editReservation(editingId, name, num, reservedTimestamp, notes));
					}
					// read response
					Object response = clientFrame.reader.readObject();
					if (response instanceof Message && ((Message) response).isQueueError) {
						Message err = (Message) response;
						if (err.errorType == Message.QueueError.ERROR_NAME) {
							JOptionPane.showMessageDialog(null, "\"" + name + "\" è già stato utilizzato.", "Errore", JOptionPane.WARNING_MESSAGE);
							nameField.requestFocus();
						} else if (err.errorType == Message.QueueError.ERROR_ID) {
							JOptionPane.showMessageDialog(null, "\"" + name + "\" (ID: " + editingId + ") non può essere sostituito perché non esiste.", "Errore", JOptionPane.WARNING_MESSAGE);
							exitEditMode();
						} else {
							JOptionPane.showMessageDialog(null, "Ricevuto errore con codice sconosciuto.", "Errore", JOptionPane.ERROR_MESSAGE);
						}
						return;
					} else if (response instanceof Message && ((Message) response).isQueueData) {
						clientFrame.setQueueData(((Message) response).data);
						if (isEditMode)
							exitEditMode();
					} else {
						throw new ClassNotFoundException("Received message is not a queue response.");
					}
				} catch (IOException x) {
					JOptionPane.showMessageDialog(null, "Errore di comunicazione con il server.", "Errore", JOptionPane.ERROR_MESSAGE);
					return;
				} catch (ClassNotFoundException | ClassCastException x) {
					JOptionPane.showMessageDialog(null, "Ricevuti dati errati dal server.", "Errore", JOptionPane.ERROR_MESSAGE);
					return;
				} finally {
					ClientFrame.mutex.unlock();
				}
			} else {
				// send to mainFrame
				if (!isEditMode) {
					// adding new reservation
					if (mainFrame.isNameTaken(name, -1)) {
						JOptionPane.showMessageDialog(null, "\"" + name + "\" è già stato utilizzato.", "Errore", JOptionPane.WARNING_MESSAGE);
						nameField.requestFocus();
						return;
					} else {
						mainFrame.addReservation(name, num, addedTimestamp, reservedTimestamp, notes, true);
					}
				} else {
					// editing existing reservation
					if (mainFrame.isIdFree(editingId)) {
						JOptionPane.showMessageDialog(null, "\"" + name + "\" (ID: " + editingId + ") non può essere sostituito perché non esiste.", "Errore", JOptionPane.WARNING_MESSAGE);
					} else if (mainFrame.isNameTaken(name, editingId)) {
						JOptionPane.showMessageDialog(null, "\"" + name + "\" è già stato utilizzato.", "Errore", JOptionPane.WARNING_MESSAGE);
						nameField.requestFocus();
						return;
					} else {
						mainFrame.editReservation(editingId, name, num, reservedTimestamp, notes);
					}
				}
				dialog.dispose();
			}

			nameField.setText("");
			numField.setText("");
			timeField.setText("");
			notesField.setText("");
			nameField.requestFocus();
		});

		// cancelButton settings
		// if we are in the server set visible
		cancelButton.setVisible(mainFrame != null);
		cancelButton.setFocusable(false);
		if (mainFrame == null) {
			// we are in the client
			cancelButton.addActionListener(e -> exitEditMode());
		} else {
			// we are in the server
			cancelButton.addActionListener(e -> dialog.dispose());
		}

		// if we received a reservation in the constructor, enter edit mode
		if (editingReservation != null)
			editReservation(editingReservation);

		if (mainFrame != null) {
			// create new dialog
			dialog = new JDialog(mainFrame.frame, "Inserisci Prenotazione", true);
			dialog.setContentPane(panel);
			dialog.pack();
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		}
	}

	public void editReservation(Reservation r) {
		isEditMode = true;
		editingId = r.getId();
		sendButton.setText("Modifica");
		cancelButton.setVisible(true);

		nameField.setText(r.getName());
		numField.setText(String.valueOf(r.getNum()));
		if (r.getReservedTimestamp() != 0)
			timeField.setText(r.getReservedTime());
		else
			timeField.setText("");
		notesField.setText(r.getNotes());
	}

	public void exitEditMode() {
		nameField.setText("");
		numField.setText("");
		timeField.setText("");
		notesField.setText("");
		nameField.requestFocus();

		isEditMode = false;
		editingId = -1;
		sendButton.setText("Invia");
		cancelButton.setVisible(false);
	}
}
