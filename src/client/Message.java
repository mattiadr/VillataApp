package client;

import server.Reservation;

import java.io.Serializable;
import java.util.List;

public class Message implements Serializable {

	// meta info
	public boolean isEnd = false;
	public boolean isReservation = false;
	public boolean isDataRequest = false;
	public boolean isQueueError = false;
	public boolean isQueueData = false;

	// reservation data
	public String name = "";
	public int num = 0;
	public long addedTimestamp = 0;
	public long reservedTimestamp = 0;
	public String notes = "";
	public boolean replace = false;

	// response data
	public List<Reservation> data;

	public static Message end() {
		Message m = new Message();
		m.isEnd = true;
		return m;
	}

	public static Message reservation(String name, int num, long addedTimestamp, long reservedTimestamp, String notes, boolean replace) {
		Message m = new Message();
		m.isReservation = true;
		m.name = name;
		m.num = num;
		m.addedTimestamp = addedTimestamp;
		m.reservedTimestamp = reservedTimestamp;
		m.notes = notes;
		m.replace = replace;
		return m;
	}

	public static Message dataRequest() {
		Message m = new Message();
		m.isDataRequest = true;
		return m;
	}

	public static Message queueError() {
		Message m = new Message();
		m.isQueueError = true;
		return m;
	}

	public static Message queueData(List<Reservation> data) {
		Message m = new Message();
		m.isQueueData = true;
		m.data = data;
		return m;
	}

	public String toString() {
		return isEnd + " " + isReservation + " " + isDataRequest + " " + isQueueError + " " + isQueueData;
	}

}
