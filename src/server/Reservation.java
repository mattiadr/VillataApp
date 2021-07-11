package server;

import javax.swing.*;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Reservation implements Serializable {

	public static long ID = 0L;

	private final long id;
	private String name;
	private int num;
	private final long addedTimestamp;
	private long reservedTimestamp;
	private String notes;
	private final JButton call;
	private final JButton confirm;
	private final JButton complete;
	private final JButton remove;

	private long calledTimestamp;
	private long confirmedTimestamp;
	private long completedTimestamp;

	public Reservation(String name, int num, long addedTimestamp, long reservedTimestamp, String notes, MainFrame frame) {
		this.id = ID++;
		this.name = name;
		this.num = num;
		this.addedTimestamp = addedTimestamp;
		this.reservedTimestamp = reservedTimestamp;
		this.notes = notes;

		call = new JButton("Chiama");
		call.addActionListener(e -> {
			this.setCalledTimestamp();
			frame.callReservation(id);
		});
		confirm = new JButton("Conferma");
		confirm.addActionListener(e -> {
			this.setConfirmedTimestamp();
			frame.confirmReservation(id);
		});
		complete = new JButton("Completa");
		complete.addActionListener(e -> {
			this.setCompletedTimestamp();
			frame.completeReservation(id);
		});
		remove = new JButton("Rimuovi");
		remove.addActionListener(e -> {
			this.setCompletedTimestamp();
			frame.removeReservation(id, true);
		});
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public long getAddedTimestamp() {
		return addedTimestamp;
	}

	public String getAddedTime() {
		Date date = new Date(addedTimestamp);
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		return sdf.format(date);
	}

	public long getReservedTimestamp() {
		return reservedTimestamp;
	}

	public void setReservedTimestamp(long reservedTimestamp) {
		this.reservedTimestamp = reservedTimestamp;
	}

	public String getReservedTime() {
		if (reservedTimestamp == 0) return "ASAP";
		Date date = new Date(reservedTimestamp);
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		return sdf.format(date);
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
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

	public long getCalledTimestamp() {
		return calledTimestamp;
	}

	public void setCalledTimestamp() {
		this.calledTimestamp = new Date().getTime();
	}

	public long getConfirmedTimestamp() {
		return confirmedTimestamp;
	}

	public void setConfirmedTimestamp() {
		this.confirmedTimestamp = new Date().getTime();
	}

	public long getCompletedTimestamp() {
		return completedTimestamp;
	}

	public void setCompletedTimestamp() {
		this.completedTimestamp = new Date().getTime();
	}

}
