package client;

import server.Reservation;

import java.io.Serializable;
import java.util.List;

public class Message implements Serializable {

	// meta info
	public boolean isEnd = false;
	public boolean isAddReservation = false;
	public boolean isEditReservation = false;
	public boolean isDataRequest = false;
	public boolean isQueueError = false;
	public boolean isQueueData = false;

	// reservation data
	public long id = -1;
	public String name = "";
	public int num = 0;
	public long addedTimestamp = 0;
	public long reservedTimestamp = 0;
	public String notes = "";

	// queue error data
	public enum QueueError {
		ERROR_ID, ERROR_NAME
	}

	public QueueError errorType = null;

	// response data
	public List<Reservation> data;

	public static Message end() {
		Message m = new Message();
		m.isEnd = true;
		return m;
	}

	public static Message addReservation(String name, int num, long addedTimestamp, long reservedTimestamp, String notes) {
		Message m = new Message();
		m.isAddReservation = true;
		m.name = name;
		m.num = num;
		m.addedTimestamp = addedTimestamp;
		m.reservedTimestamp = reservedTimestamp;
		m.notes = notes;
		return m;
	}

	public static Message editReservation(long id, String name, int num, long reservedTimestamp, String notes) {
		Message m = new Message();
		m.isEditReservation = true;
		m.id = id;
		m.name = name;
		m.num = num;
		m.reservedTimestamp = reservedTimestamp;
		m.notes = notes;
		return m;
	}

	public static Message dataRequest() {
		Message m = new Message();
		m.isDataRequest = true;
		return m;
	}

	public static Message queueError(QueueError errorType) {
		Message m = new Message();
		m.isQueueError = true;
		m.errorType = errorType;
		return m;
	}

	public static Message queueData(List<Reservation> data) {
		Message m = new Message();
		m.isQueueData = true;
		m.data = data;
		return m;
	}

}
