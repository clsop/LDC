package org.petersen.landc.net;

import java.awt.Color;
import java.awt.FontFormatException;
import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.*;

import org.petersen.landc.PacketType;
import org.petersen.landc.exceptions.ConfigException;
import org.petersen.landc.exceptions.TrafficException;
import org.petersen.landc.ui.*;

/**
 * 
 * @author Claus Petersen
 * 
 * This is the class that should resolve a given name
 */
public final class CheckName extends Thread {

	private boolean quit;
	private AtomicReference<String> nickRef;
	private StartLoader loader;
	private Chatter chatter;
	private ChatWindow window;
	private int chatFont;

	public CheckName(StartLoader loader, Chatter chatter, ChatWindow window) {

		quit = false;
		nickRef = new AtomicReference<String>(null);
		this.loader = loader;
		this.chatter = chatter;
		this.window = window;
	}

	private void initConfig() {

		String address = null, read = null;
		short port = 0;
		BufferedReader reader = null;
		OptionDialog optionsDialog = window.getOptionDialog();

		try {

			// construct a Reader to read configuration file
			reader = new BufferedReader(new FileReader("config"));

			if ((read = reader.readLine()).subSequence(0, 8).equals("address:")) {
				address = read.substring(8);
			} else {
				throw new ConfigException("Config File: failed to read address");
			}

			if ((read = reader.readLine()).subSequence(0, 5).equals("port:")) {
				port = Short.parseShort(read.substring(5));
			} else {
				throw new ConfigException("Config File: failed to read port");
			}

			if ((read = reader.readLine()).subSequence(0, 5).equals("font:")) {
				chatFont = Integer.parseInt(read.substring(5));
			} else {
				throw new ConfigException("Config File: failed to read font");
			}

			if ((read = reader.readLine()).subSequence(0, 6).equals("fsize:")) {
				optionsDialog.setChatFontSize(Integer.parseInt(read
						.substring(6)));
			} else {
				throw new ConfigException(
						"Config File: failed to read font size");
			}

			if ((read = reader.readLine()).subSequence(0, 7).equals("fcolor:")) {
				optionsDialog.setChatTextColor(new Color(Integer.parseInt(read
						.substring(7))));
			} else {
				throw new ConfigException(
						"Config File: failed to read font color");
			}

			reader.close();

			chatter.setPort(port);
			chatter.setAddress(InetAddress.getByName(address));
		} catch (ConfigException e) {

			// failed to read configuration content, tell us and then exit
			JOptionPane.showMessageDialog(window, e.getLocalizedMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
			window.dispose();
		} catch (NumberFormatException e) {

			// cannot parse port, tell us and then exit
			JOptionPane.showMessageDialog(window,
					"Invalid port: " + e.getLocalizedMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
			window.dispose();
		} catch (UnknownHostException e) {

			// something went wrong, tell us and then exit
			JOptionPane.showMessageDialog(window,
					"Invalid address: " + e.getLocalizedMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
			window.dispose();
		} catch (FileNotFoundException e) {

			// configuration file was not found, tell us and then exit
			JOptionPane.showMessageDialog(window,
					"File not found: " + e.getLocalizedMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
			window.dispose();
		} catch (IOException e) {

			// something went wrong, tell us and then exit
			JOptionPane.showMessageDialog(window,
					"I/O read error: " + e.getLocalizedMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
			window.dispose();
		} catch (TrafficException e) {
			JOptionPane.showMessageDialog(window,
					"Network error: " + e.getLocalizedMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
			window.dispose();
		} finally {

			try {

				reader.close();
			} catch (IOException e) {

				// something went wrong, tell us and then exit
				JOptionPane.showMessageDialog(window,
						"I/O read error: " + e.getLocalizedMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
				window.dispose();
			}
		}
	}

	@Override
	public void run() {

		while (!isInterrupted()) {

			// hide loader if still showing
			if (loader != null && loader.isVisible())
				loader.setVisible(false);

			// variable to chat icon for dialogs
			Icon icon = window.getIcon();

			// Nickname dialog to write a nick
			nickRef.set(JOptionPane.showInputDialog(window,
					"Enter a nickname.", "Nickname",
					JOptionPane.QUESTION_MESSAGE));

			// Check if input is has been canceled
			if (nickRef.get() == null || nickRef.get().isEmpty()) {

				// get answer to quit
				int answer = 0;

				if (icon != null) {
					answer = JOptionPane
							.showConfirmDialog(
									window,
									"You need a nickname to start.\n Do you really wanna quit ?",
									"Quit ?", JOptionPane.YES_NO_OPTION,
									JOptionPane.QUESTION_MESSAGE, icon);
				} else {
					answer = JOptionPane
							.showConfirmDialog(
									window,
									"You need a nickname to start.\n Do you really wanna quit ?",
									"Quit ?", JOptionPane.YES_NO_OPTION);
				}

				// quit
				if (answer == 0) {
					quit = true;
				} else {
					nickRef.set(null);
				}
			} else {

				// test for white spaces
				String nTest = nickRef.get().trim();

				if (nTest.isEmpty()) {
					nickRef.set(null);
				} else {

					// loader shows up for the user
					if (loader != null) {

						loader.getProgressBar().setString("Reading Config...");
						loader.setVisible(true);
					}

					PacketType type = PacketType.NONE;
					String nick = new String(nickRef.get());

					JProgressBar bar = loader.getProgressBar();

					// initialize configuration
					initConfig();

					bar.setString("Listing Name...");
					bar.setValue(1);

					try {
						chatter.sendPacket(PacketType.REQUEST_NAME,
								nickRef.get());

						while (type != PacketType.NAME_TAKEN) {
							type = chatter.receivePacket(nickRef);

							if (type == PacketType.NONE) {

								// name okay, we use it
								window.setName(nick);
								nickRef.set("OK");

								bar.setString("Listing Name... OK");
								bar.setValue(20);
								Thread.sleep(500);
								
								// map smileys to hash map
								bar.setString("Loading Smileys...");
								bar.setValue(40);
								window.initIcons();
								
								// initialize controls
								bar.setString("Initializing Controls...");
								bar.setValue(60);
								window.initControls();

								bar.setString("Loading Fonts...");
								bar.setValue(80);
								window.getOptionDialog().setFonts(
										window.loadFonts());
								window.getOptionDialog().setChatFont(chatFont);

								// starts the chatter into listening mode
								chatter.enable();
								bar.setValue(100);

								// loader is not used anymore
								if (loader != null)
									loader.setVisible(false);

								// display the window
								window.setVisible(true);

								interrupt();
								break;
							} else {
							} // just continue
						}

						if (type == PacketType.NAME_TAKEN) {

							if (loader != null)
								loader.setVisible(false);

							if (icon != null) {
								JOptionPane.showMessageDialog(loader,
										"Sorry, the name is already taken",
										"Name Taken",
										JOptionPane.INFORMATION_MESSAGE, icon);
							} else {
								JOptionPane.showMessageDialog(loader,
										"Sorry, the name is already taken",
										"Name Taken",
										JOptionPane.INFORMATION_MESSAGE);
							}

							nickRef.set(null);
						}
					} catch (TrafficException e) {

						if (icon != null) {
							JOptionPane.showMessageDialog(loader,
									e.getLocalizedMessage(), "Error",
									JOptionPane.ERROR_MESSAGE, icon);
						} else {
							JOptionPane.showMessageDialog(loader,
									e.getLocalizedMessage(), "Error",
									JOptionPane.ERROR_MESSAGE);
						}
					} catch (InterruptedException e) {
						JOptionPane.showMessageDialog(loader, e.getLocalizedMessage(),
								"Error", JOptionPane.ERROR_MESSAGE);
					} catch (FontFormatException e) {
						JOptionPane.showMessageDialog(loader, e.getLocalizedMessage(),
								"Error", JOptionPane.ERROR_MESSAGE);
					} catch (URISyntaxException e) {
						JOptionPane.showMessageDialog(loader, e.getLocalizedMessage(),
								"Error", JOptionPane.ERROR_MESSAGE);
					} catch (IOException e) {
						JOptionPane.showMessageDialog(loader, e.getLocalizedMessage(),
								"Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}

			if (quit) {
				interrupt();
			} else;
		}
	}
}
