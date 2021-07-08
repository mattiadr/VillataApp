package server;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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

				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

				String line;

				while (!(line = reader.readLine()).equals("/end")) {
					// res = [name, num, time, notes, replace]
					String[] res = line.split(",");
					long que;
					if (mainFrame.isNameTaken(res[0], res[4].equals("true"))) {
						que = -1;
					} else {
						que = mainFrame.addReservation(res[0], Integer.parseInt(res[1]), Long.parseLong(res[2]), res[3], true);
					}
					writer.println(que);
				}

				reader.close();
				writer.close();
				socket.close();
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Errore di lettura dal client.", "Errore", JOptionPane.ERROR_MESSAGE);
		}

	}

}
