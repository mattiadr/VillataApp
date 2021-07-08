package server;

import javax.swing.*;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Reservation implements Serializable {

	public static long ID = 0L;

	private final long id;
	private final String name;
	private final int num;
	private final long timestamp;
	private final String notes;
	private final JButton call;
	private final JButton confirm;
	private final JButton complete;
	private final JButton remove;

	public Reservation(String name, int num, long timestamp, String notes, MainFrame frame) {
		this.id = ID++;
		this.name = name;
		this.num = num;
		this.timestamp = timestamp;
		this.notes = notes;

		call = new JButton("Chiama");
		call.addActionListener(e -> frame.callReservation(id));
		confirm = new JButton("Conferma");
		confirm.addActionListener(e -> frame.confirmReservation(id));
		complete = new JButton("Completa");
		complete.addActionListener(e -> frame.completeReservation(id));
		remove = new JButton("Rimuovi");
		remove.addActionListener(e -> frame.removeReservation(id, true));
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getNum() {
		return num;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getTime() {
		Date date = new Date(timestamp);
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		return sdf.format(date);
	}

	public String getNotes() {
		return notes;
	}

	public JButton getCall() {
		return call;
	}

	public JButton getConfirm() {
		return confirm;
	}

	public JButton getComplete() {
		return complete;
	}

	public JButton getRemove() {
		return remove;
	}
}
