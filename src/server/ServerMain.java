package server;

import client.Message;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

	public static final int SERVER_SOCKET_PORT = 5000;

	public static void main(String[] args) {

		MainFrame mainFrame = new MainFrame();
		ExtFrame extFrame = new ExtFrame(mainFrame.getScreenData());
		Thread extThread = new Thread(extFrame);
		extThread.start();

		// start reading socket for requests from client
		try {
			ServerSocket serverSocket = new ServerSocket(SERVER_SOCKET_PORT);

			while (true) {
				// accept connection
				Socket socket = serverSocket.accept();
				ObjectOutputStream writer = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream reader = new ObjectInputStream(socket.getInputStream());

				// start thread to handle connection
				new Thread(() -> handleClient(mainFrame, socket, reader, writer)).start();
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Un client ha provato a collegarsi, ma ha fallito.", "Errore", JOptionPane.ERROR_MESSAGE);
		}

	}

	public static void handleClient(MainFrame mainFrame, Socket socket, ObjectInputStream reader, ObjectOutputStream writer) {
		while (true) {
			try {
				Message m = (Message) reader.readObject();

				if (m.isEnd) {
					JOptionPane.showMessageDialog(null, "Uno dei client si è disconnesso.", "Info", JOptionPane.INFORMATION_MESSAGE);
					break;
				} else if (m.isAddReservation) {
					if (mainFrame.isNameTaken(m.name, -1)) {
						writer.writeObject(Message.queueError(Message.QueueError.ERROR_NAME));
					} else {
						// res = [name, num, added_time, reserved_time, notes, replace]
						mainFrame.addReservation(m.name, m.num, m.addedTimestamp, m.reservedTimestamp, m.notes, true);
						writer.writeObject(Message.queueData(mainFrame.getQueueData()));
						writer.reset();
					}
				} else if (m.isEditReservation) {
					if (mainFrame.isIdFree(m.id)) {
						writer.writeObject(Message.queueError(Message.QueueError.ERROR_ID));
					} else if (mainFrame.isNameTaken(m.name, m.id)) {
						writer.writeObject(Message.queueError(Message.QueueError.ERROR_NAME));
					} else {
						mainFrame.editReservation(m.id, m.name, m.num, m.reservedTimestamp, m.notes);
						writer.writeObject(Message.queueData(mainFrame.getQueueData()));
						writer.reset();
					}
				} else if (m.isDataRequest) {
					writer.writeObject(Message.queueData(mainFrame.getQueueData()));
					writer.reset();
				}
			} catch (IOException | ClassNotFoundException e) {
				JOptionPane.showMessageDialog(null, "Errore durante la comunicazione con uno dei client. La connessione è stata interrotta.", "Errore", JOptionPane.ERROR_MESSAGE);
				break;
			}
		}

		try {
			reader.close();
			writer.close();
			socket.close();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Errore durante la chiusura della connessione con uno dei client.", "Errore", JOptionPane.ERROR_MESSAGE);
		}
	}

}
