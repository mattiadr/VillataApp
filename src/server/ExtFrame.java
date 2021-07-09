package server;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ExtFrame implements Runnable {

	public static final int IMAGE_TIMER = 15;
	public final static String IMAGES_LOCATION = System.getProperty("user.home") + "/VillataApp/";

	// frame
	private JFrame frame;

	// panel
	private JPanel panel;

	// table data and objects
	private final ExtTableModel tableModel;
	private JTable table;
	private JPanel imgPanel;

	// image to display in imgPanel
	private BufferedImage currentImage;

	public ExtFrame(List<Reservation> tableData) {

		// create table
		tableModel = new ExtTableModel(tableData);
		table.setModel(tableModel);

		// set table props
		// make not selectable
		table.setFocusable(false);
		table.setRowSelectionAllowed(false);
		// set row height
		table.setRowHeight(100);
		// set col width
		table.getColumnModel().getColumn(0).setPreferredWidth(1920 / 2);
		table.getColumnModel().getColumn(1).setPreferredWidth(1920 / 2);
		// set col centered
		DefaultTableCellRenderer centerCellRenderer = new DefaultTableCellRenderer();
		centerCellRenderer.setHorizontalAlignment(JLabel.CENTER);
		table.getColumnModel().getColumn(0).setCellRenderer(centerCellRenderer);
		table.getColumnModel().getColumn(1).setCellRenderer(centerCellRenderer);
		// set border
		table.setBorder(new MatteBorder(0, 0, 2, 0, Color.BLACK));

		// fullscreen button
		JButton fullscreenButton = new JButton("Fullscreen");
		fullscreenButton.addActionListener(e -> {
			JFrame oldFrame = frame;
			frame = new JFrame("VillataApp Monitor");
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			frame.setContentPane(panel);
			frame.setUndecorated(true);
			frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
			frame.setLocationRelativeTo(oldFrame);
			frame.setVisible(true);
			oldFrame.dispose();
		});

		// create new frame
		frame = new JFrame("VillataApp Monitor");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setContentPane(fullscreenButton);
		frame.setPreferredSize(new Dimension(250, 100));
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	@Override
	public void run() {

		ArrayList<BufferedImage> images = new ArrayList<>();

		File dir = new File(IMAGES_LOCATION);
		JOptionPane.showMessageDialog(frame, "Carico immagini da: \"" + dir.getAbsolutePath() + "\"", "Info", JOptionPane.INFORMATION_MESSAGE);
		if (dir.isDirectory()) {
			for (File f : dir.listFiles()) {
				if (!f.isFile()) continue;
				try {
					images.add(ImageIO.read(f));
				} catch (IOException e) {
					JOptionPane.showMessageDialog(frame, "Impossibile aprire immagine.\n" + f.getAbsolutePath(), "Errore", JOptionPane.ERROR_MESSAGE);
				}
			}
		}

		// start refreshing screen periodically
		new Timer().scheduleAtFixedRate(new TimerTask() {

			int changeImage = 0;
			int imageIndex = 0;

			@Override
			public void run() {
				// refresh table
				tableModel.refresh();

				// change image if needed
				if (changeImage <= 0 && images.size() > 0) {
					currentImage = images.get(imageIndex++);
					imgPanel.repaint();
					imageIndex %= images.size();
					changeImage = IMAGE_TIMER;
				}
				changeImage--;
			}
		}, 1000, 1000);
	}

	private void createUIComponents() {
		imgPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				// draw white background
				g.setColor(Color.WHITE);
				g.drawRect(0, 0, this.getWidth(), this.getHeight());

				// draw current image
				if (currentImage == null) return;
//				g.drawImage(currentImage, 0, 0, this.getWidth() - 200, this.getHeight(), null);
				int ww = this.getWidth();
				int wh = this.getHeight();
				int iw = currentImage.getWidth();
				int ih = currentImage.getHeight();
				double f = Math.min((double) ww / iw, (double) wh / ih);
				int w = (int) (iw * f);
				int h = (int) (ih * f);
				g.drawImage(currentImage, (ww - w) / 2, (wh - h) / 2, w, h, null);
			}
		};
	}

}
