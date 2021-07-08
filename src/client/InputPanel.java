package client;

import server.MainFrame;

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
	private JCheckBox replaceBox;
	private JButton sendButton;
	private JButton button2;

	// last modified vars
	private String lastName = null;
	private String lastNum = null;
	private String lastTime = null;
	private String lastNotes = null;

	public InputPanel(MainFrame mainFrame, ClientFrame clientFrame) {
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
			String name = nameField.getText().replaceAll(",", "");
			if (name.isEmpty()) {
				JOptionPane.showMessageDialog(null, "Il nome non può essere vuoto.", "Errore", JOptionPane.ERROR_MESSAGE);
				nameField.requestFocus();
				return;
			}

			int num;
			try {
				num = Integer.parseInt(numField.getText());
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

			String time = timeField.getText();
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

			String notes = notesField.getText().replaceAll(",", " ");

			if (mainFrame == null) {
				// send to writer
				try {
					// send new reservation to server
					clientFrame.writer.writeObject(Message.reservation(name, num, addedTimestamp, reservedTimestamp, notes, replaceBox.isSelected()));
					// read response
					Object response = clientFrame.reader.readObject();
					if (response instanceof Message && ((Message) response).isQueueError) {
						if (!replaceBox.isSelected()) {
							JOptionPane.showMessageDialog(null, "\"" + name + "\" è già stato utilizzato.", "Errore", JOptionPane.WARNING_MESSAGE);
						} else {
							JOptionPane.showMessageDialog(null, "\"" + name + "\" non può essere sostituito perché non esiste.", "Errore", JOptionPane.WARNING_MESSAGE);
						}
						nameField.requestFocus();
						return;
					} else if (response instanceof Message && ((Message) response).isQueueData) {
						clientFrame.setQueueData(((Message) response).data);
					} else {
						throw new ClassNotFoundException("Received message is not a que response.");
					}
				} catch (IOException x) {
					JOptionPane.showMessageDialog(null, "Errore di comunicazione con il server.", "Errore", JOptionPane.ERROR_MESSAGE);
					return;
				} catch (ClassNotFoundException | ClassCastException x) {
					JOptionPane.showMessageDialog(null, "Ricevuti dati errati dal server.", "Errore", JOptionPane.ERROR_MESSAGE);
					return;
				}
			} else {
				// send to mainFrame
				if (mainFrame.isNameTaken(name, replaceBox.isSelected())) {
					if (!replaceBox.isSelected()) {
						JOptionPane.showMessageDialog(null, "\"" + name + "\" è già stato utilizzato.", "Errore", JOptionPane.WARNING_MESSAGE);
					} else {
						JOptionPane.showMessageDialog(null, "\"" + name + "\" non può essere sostituito perché non esiste.", "Errore", JOptionPane.WARNING_MESSAGE);
					}
					nameField.requestFocus();
					return;
				} else {
					mainFrame.addReservation(name, num, addedTimestamp, reservedTimestamp, notes, true);
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
					JOptionPane.showMessageDialog(null, "Nessuna prenotazione modificabile.", "Errore", JOptionPane.ERROR_MESSAGE);
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

		if (mainFrame != null) {
			// create new dialog
			dialog = new JDialog(mainFrame.frame, "Inserisci Prenotazione", true);
			dialog.setContentPane(panel);
			dialog.pack();
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		}
	}
}
