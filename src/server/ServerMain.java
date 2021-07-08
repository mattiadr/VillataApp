package server;

import client.Message;

import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

	public static void main(String[] args) {

		MainFrame mainFrame = new MainFrame();
		ExtFrame extFrame = new ExtFrame(mainFrame.getScreenData());
		Thread extThread = new Thread(extFrame);
		extThread.start();

		// start reading socket for requests from client
		try {
			ServerSocket serverSocket = new ServerSocket(5000);

			while (true) {
				Socket socket = serverSocket.accept();

				ObjectInputStream reader = new ObjectInputStream(socket.getInputStream());
				ObjectOutputStream writer = new ObjectOutputStream(socket.getOutputStream());

				while (true) {
					Message m = (Message) reader.readObject();

					if (m.isEnd)
						break;
					else if (m.isReservation) {
						if (mainFrame.isNameTaken(m.name, m.replace)) {
							writer.writeObject(Message.queueError());
						} else {
							// res = [name, num, added_time, reserved_time, notes, replace]
							mainFrame.addReservation(m.name, m.num, m.addedTimestamp, m.reservedTimestamp, m.notes, true);
							writer.writeObject(Message.queueData(mainFrame.getQueueData()));
							writer.reset();
						}
					} else if (m.isDataRequest) {
						writer.writeObject(Message.queueData(mainFrame.getQueueData()));
						writer.reset();
					}
				}

				reader.close();
				writer.close();
				socket.close();
			}
		} catch (IOException | ClassNotFoundException e) {
			JOptionPane.showMessageDialog(null, "Errore di lettura dal client.", "Errore", JOptionPane.ERROR_MESSAGE);
		}

	}

}
