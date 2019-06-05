package org.petersen.landc.ui;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EtchedBorder;
import javax.swing.text.*;

import org.petersen.landc.PacketType;
import org.petersen.landc.exceptions.TrafficException;
import org.petersen.landc.net.Chatter;
import org.petersen.landc.net.CheckName;

import java.awt.Color;
import java.awt.Component;
//import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Toolkit;
import java.awt.event.*;

import java.net.URISyntaxException;
import java.net.URL;

import java.io.*;

import java.security.CodeSource;
import java.text.AttributedCharacterIterator;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

/**
 * 
 * @author Claus Petersen
 * 
 * the actual chat window
 */
@SuppressWarnings("serial")
public final class ChatWindow extends JFrame implements ActionListener,
		WindowListener, KeyListener, InputMethodListener {

	// application version to display
	private final float VERSION = 1.3f;

	// application stage to display (Alpha, Beta, Release)
	private final String STAGE = "Beta";

	// A label control for the chat window
	private JLabel chatLabel;

	// A label control for the nicks window
	private JLabel nicksLabel;

	// A text control to show peoples messages
	private JTextPane theChat;

	// a style definition to be associated with inserted chat text
	private Style chatStyle;

	// a style document associated with the chat JTextPane
	private StyledDocument chatDoc;

	// to be able to scroll through messages
	private JScrollPane chatScroller;

	// A text control to show peoples names
	private JTextPane theNicks;

	// a style definition to be associated with inserted nicknames
	private Style nicksStyle;

	// a style document associated with the nicknames JTextPane
	private StyledDocument nicksDoc;

	// to be able to scroll through nick names
	private JScrollPane nickScroller;

	// a style definition to be associated with chat input for icons
	private Style inputStyle;

	// a style document associated with the chat input JTextPane
	private StyledDocument inputDoc;

	// A text control where you write the message
	private JTextPane chatInput;

	// A button control to send text from chatInput
	private JButton sendBtn;

	// the icon box with icon to choose from
	private JComboBox<IconEntry> iconBox;

	// a list for all nick names
	private List<String> nameList;

	// for our nickname entered at startup
	private String nickName;

	// our Chatter class (well, for network operations)
	private Chatter chatter;

	// the option dialog
	private OptionDialog optionsDialog;

	// the option button
	private JButton optionBtn;

	// the splash loader for loading data
	private StartLoader loader;

	// map for icon relations
	private HashMap<CharSequence, ImageIcon> iconMap;

	// matcher to match the regular expressions of icons
	private Pattern iconPattern;

	// name resolving thread
	private CheckName checkName;

	// a timer for the name resolving procedure
	private Timer checkNameTimer;

	// the icon used locally in the chat
	private ImageIcon icon;

	// dimension of the window
	private int width, height;

	/**
	 * Main Constructor
	 * 
	 * @param desktopSize
	 *            the size of the desktop in a Dimension Class for the chat
	 *            window to calculate the center
	 */
	public ChatWindow(Dimension desktopSize) {

		// sets the default borders, title and close, maximize and minimize
		// buttons etc. of the window to show
		JFrame.setDefaultLookAndFeelDecorated(false); // doesn't seem to have any
		// effect though, maybe
		// just remove

		// sets the window title
		setTitle(String.format("LAN Discover Chat v%.2g %s", VERSION, STAGE));

		// sets what to do when the upper right close button is pressed
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		// sets if the window can be resized by a user (true by default, but if
		// not desired can be set to false)
		setResizable(true);

		// the window must be able to fit the controls, not to small
		if (desktopSize.width <= 1024 || desktopSize.height <= 768) {
			width = 768;
			height = 548;
		} else {

			width = (int) (desktopSize.width * .45);
			height = (int) (desktopSize.height * .436);
		}

		// sets the minimum size of the window, a user cannot resize below this
		// point
		setMinimumSize(new Dimension(width, height));

		// sets the maximum size of the window, a user cannot resize above this
		// point
		//setMaximumSize(new Dimension(width + (int) (desktopSize.width * .5),
		//		height + (int) (desktopSize.height * .5)));

		// load the icon of the chat and the window with transparency
		try {
			icon = getImage("img/icon.png", "icon");
			setIconImage(icon.getImage());
		} catch (IOException e) {

			JOptionPane.showMessageDialog(this, "Failed to load icon", "Error",
					JOptionPane.ERROR_MESSAGE);
			dispose();
		}

		// initialize the loader here
		try {
			loader = new StartLoader(desktopSize);

			// construct the Chatter class
			chatter = new Chatter(this);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(
					this,
					"failed to initialize splash screen\nError: "
							+ e.getLocalizedMessage(), "Error",
					JOptionPane.ERROR_MESSAGE, icon);
			//dispose();
		} catch (IllegalArgumentException e) {
			JOptionPane.showMessageDialog(
					this,
					"failed to initialize splash screen\nError: "
							+ e.getLocalizedMessage(), "Error",
					JOptionPane.ERROR_MESSAGE, icon);
			//dispose();
		} catch (TrafficException e) {
			JOptionPane.showMessageDialog(this,
					"failed to initialize network\nError: " + e.getLocalizedMessage(),
					"Error", JOptionPane.ERROR_MESSAGE, icon);
			dispose();
		}

		// calculate the window to the center of the screen
		int locX = (int) (desktopSize.width * .5 - width * .5);
		int locY = (int) (desktopSize.height * .5 - height * .5);

		// then set the window to the calculated
		setSize(551, 400);
		setLocation(locX, locY);
		
		optionsDialog = new OptionDialog(this, Color.BLACK);

		// ArrayList to keep track of the nicknames (at least one will be entered)
		nameList = new ArrayList<String>(1);

		checkNameTimer = new Timer(1000, new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {

				if (chatter.isAlive())
					checkNameTimer.stop();
				else if (!checkName.isAlive()) {

					dispose();
					checkNameTimer.stop();
				}
			}
		});
		
		// added to know when we close the window
		addWindowListener(this);

		// run the phase where we ask for nick name
		checkName = new CheckName(loader, chatter, this);

		checkName.start();
		checkNameTimer.start();
	}

	// getter for the icon
	public ImageIcon getIcon() {
		return icon;
	}

	// getter for the nickname
	public String getName() {

		// returns the nickName field to caller of method
		return nickName;
	}

	// setter for the nickname
	public void setName(String name) {

		// sets the nickName field
		this.nickName = name;
	}

	// to check if a name is in the name list
	public boolean isNameTaken(AtomicReference<String> name) {

		// returns true if name is found in the name list
		return nameList.contains(name.get());
	}

	// used to add names to the nickname control
	public void addName(AtomicReference<String> name) {

		if (isNameTaken(name)) {
			return;
		} else
			; // just continue

		nameList.add(new String(name.get()));
		
		if (name.get().equals(nickName)) {

			StyleConstants.setForeground(nicksStyle, optionsDialog.getChatTextColor());
		} else {
			
			StyleConstants.setForeground(nicksStyle, Color.BLACK);
		}
		
		try {
			
			nicksDoc.insertString(theNicks.getCaretPosition(),
					name.get() + '\n', nicksStyle);
		} catch (BadLocationException e) {
			
			JOptionPane.showMessageDialog(this,
					"failed to insert: " + e.getLocalizedMessage(), "Warning",
					JOptionPane.WARNING_MESSAGE);
		}
	}

	public void removeName(String name) {

		nameList.remove(name);

		try {
			nicksDoc.remove(0, nicksDoc.getLength());

			for (ListIterator<String> itrList = nameList.listIterator(); itrList
					.hasNext();) {
				String currentName = itrList.next();

				if (currentName.equals(nickName)) {
					
					StyleConstants.setForeground(nicksStyle, optionsDialog.getChatTextColor());
				} else {
					
					StyleConstants.setForeground(nicksStyle, Color.BLACK);
				}
				
				nicksDoc.insertString(theNicks.getCaretPosition(),
						currentName + '\n', nicksStyle);
			}
		} catch (BadLocationException e) {
			JOptionPane.showMessageDialog(this,
					"failed to insert: " + e.getLocalizedMessage(), "Warning",
					JOptionPane.WARNING_MESSAGE);
		}
	}

	// adds text to the chat control
	public void addText(String name, String text, int font, int fontSize,
			Color color) {

		StyleConstants.setBold(chatStyle, true);
		StyleConstants.setForeground(chatStyle, color);
		StyleConstants.setFontFamily(chatStyle, optionsDialog.getChatFont(font).getFamily());
		StyleConstants.setFontSize(chatStyle, fontSize);

		int textStart = 0;

		try {
			chatDoc.insertString(chatDoc.getLength(), name + ": ", chatStyle);
			StyleConstants.setBold(chatStyle, false);

			Matcher match = iconPattern.matcher(text);

			if (match.find()) {

				// replace icon text codes with inserted icon icons within
				// the text *start*
				do {

					chatDoc.insertString(chatDoc.getLength(),
							text.substring(textStart, match.start()), chatStyle);
					textStart = match.end();

					Style emoIconStyle = chatDoc.addStyle("icon", chatStyle);
					CharSequence subSequence = text.subSequence(match.start(),
							match.end());
					ImageIcon icon = iconMap.get(subSequence);

					StyleConstants.setIcon(emoIconStyle, icon);
					chatDoc.insertString(chatDoc.getLength(),
							icon.getDescription(), emoIconStyle);
					chatDoc.removeStyle("icon");
					text.replace(subSequence, "");

					match.find();
				} while (!match.hitEnd());
				// replace icon text codes with inserted icon icons within
				// the text *end*

				if (text.trim() != "") {
					chatDoc.insertString(chatDoc.getLength(),
							text.substring(textStart, text.length()) + "\n",
							chatStyle);
				} else {
					chatDoc.insertString(chatDoc.getLength(), "\n", chatStyle);
				}
			} else {
				chatDoc.insertString(chatDoc.getLength(), text + "\n",
						chatStyle);
			}
		} catch (BadLocationException e) {
			JOptionPane.showMessageDialog(this,
					"failed to insert: " + e.getLocalizedMessage(), "Warning",
					JOptionPane.WARNING_MESSAGE);
		}
	}

	// stop receiving when window is disposed
	@Override
	public void dispose() {

		if (chatter != null)
			chatter.interrupt();

		if (loader != null)
			loader.dispose();

		if (checkName != null && !checkName.isInterrupted())
			checkName.interrupt();

		super.dispose();
	}

	// Configure the controls for the window
	public void initControls() {
		chatLabel = new JLabel("Chat");
		nicksLabel = new JLabel("Nicknames");
		chatScroller = new JScrollPane();
		nickScroller = new JScrollPane();
		sendBtn = new JButton("Send");
		optionBtn = new JButton("Options");

		theNicks = new JTextPane();
		nickScroller.setViewportView(theNicks);
		
		// disable user input, set a border and style of text
		theNicks.setEditable(false);
		
		nicksDoc = theNicks.getStyledDocument();

		// make instances of our field members in the class
		theChat = new JTextPane();
		chatScroller.setViewportView(theChat);
				
		// disable user input, set a border
		theChat.setEditable(false);
		theChat.setContentType("text/html");
						
		chatDoc = theChat.getStyledDocument();
		
		chatScroller
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		chatScroller.setAutoscrolls(true);
		chatScroller.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
				BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)));
		nickScroller
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		nickScroller
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		nickScroller.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
				BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)));

		Style defStyle = StyleContext.getDefaultStyleContext().getStyle(
				StyleContext.DEFAULT_STYLE);
		Style chatStyleDef = chatDoc.addStyle("chatRegular", defStyle);
		chatStyle = chatDoc.addStyle("chatBold", chatStyleDef);
		nicksStyle = nicksDoc.addStyle("nicksStyle", defStyle);
		StyleConstants.setBold(nicksStyle, true);
		StyleConstants.setFontFamily(nicksStyle, "Arial");
		StyleConstants.setFontSize(nicksStyle, 14);
		StyleConstants.setForeground(nicksStyle, Color.BLACK);

		sendBtn.setActionCommand("sendPush");
		sendBtn.addActionListener(this);

		optionBtn.setActionCommand("optionPush");
		optionBtn.addActionListener(this);
		chatInput = new JTextPane();
		
				inputDoc = chatInput.getStyledDocument();
				inputStyle = inputDoc.addStyle("inputStyle", defStyle);
				chatInput.setBorder(BorderFactory.createLineBorder(Color.BLACK));
				chatInput.addKeyListener(this);
				chatInput.addInputMethodListener(this);
		
		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(iconBox, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED))
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
							.addGroup(groupLayout.createSequentialGroup()
								.addComponent(chatScroller, GroupLayout.DEFAULT_SIZE, 359, Short.MAX_VALUE)
								.addPreferredGap(ComponentPlacement.RELATED))
							.addGroup(groupLayout.createSequentialGroup()
								.addComponent(chatLabel)
								.addPreferredGap(ComponentPlacement.RELATED))
							.addGroup(groupLayout.createSequentialGroup()
								.addComponent(chatInput, GroupLayout.DEFAULT_SIZE, 371, Short.MAX_VALUE)
								.addPreferredGap(ComponentPlacement.RELATED))))
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(nickScroller, GroupLayout.DEFAULT_SIZE, 144, Short.MAX_VALUE)
						.addGroup(groupLayout.createSequentialGroup()
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(sendBtn, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(optionBtn, GroupLayout.DEFAULT_SIZE, 72, Short.MAX_VALUE))
						.addComponent(nicksLabel))
					.addGap(10))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(chatLabel)
						.addComponent(nicksLabel))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(nickScroller, GroupLayout.DEFAULT_SIZE, 307, Short.MAX_VALUE)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(chatScroller, GroupLayout.PREFERRED_SIZE, 287, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(iconBox, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
						.addComponent(chatInput, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
							.addComponent(optionBtn, Alignment.TRAILING, 0, 0, Short.MAX_VALUE)
							.addComponent(sendBtn, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 16, Short.MAX_VALUE)))
					.addContainerGap())
		);
		getContentPane().setLayout(groupLayout);
	}

	public Font[] loadFonts() throws IOException, FontFormatException,
			URISyntaxException {

		ClassLoader loader = getClass().getClassLoader();
		CodeSource src = ChatWindow.class.getProtectionDomain().getCodeSource();
		List<String> fontNames = new ArrayList<String>();

		URL jar = src.getLocation();
		ZipInputStream zip = new ZipInputStream(jar.openStream());
		ZipEntry ze = null;

		while ((ze = zip.getNextEntry()) != null) {

			String entryName = ze.getName();

			if (entryName.startsWith("fonts") && entryName.endsWith(".TTF")) {

				fontNames.add(entryName);
			}
		}

		Font[] fonts = new Font[fontNames.size()];
		int cnt = 0;

		for (Iterator<String> itr = fontNames.iterator(); itr.hasNext();) {

			fonts[cnt++] = Font.createFont(Font.TRUETYPE_FONT, loader
					.getResource(itr.next()).openStream());
		}

		return fonts;
	}

	// initialize icon faces
	public void initIcons() {

		iconMap = new HashMap<CharSequence, ImageIcon>(30);
		iconPattern = Pattern
				.compile(":(\\bGRIN\\b||\\bSTUNNED\\b||\\bCOOL\\b||\\bSMILE\\b||\\bTONGUE\\b||\\bWINK\\b"
						+ "||\\bCRY\\b||\\bSHOCKED\\b||\\bANGRY\\b||\\bFROWN\\b||\\bHEART\\b||\\bSLANT\\b"
						+ "||\\bSTRAIGHTFACE\\b):");

		try {
			iconMap.put(":GRIN:", getImage("icons/grin.png", ":GRIN:"));
			iconMap.put(":COOL:", getImage("icons/cool.png", ":COOL:"));
			iconMap.put(":CRY:", getImage("icons/cry.png", ":CRY:"));
			iconMap.put(":SHOCKED:", getImage("icons/shocked.png", ":SHOCKED:"));
			iconMap.put(":SMILE:", getImage("icons/smile.png", ":SMILE:"));
			iconMap.put(":TONGUE:", getImage("icons/tongue.png", ":TONGUE:"));
			iconMap.put(":WINK:", getImage("icons/wink.png", ":WINK:"));
			iconMap.put(":ANGRY:", getImage("icons/angry.png", ":ANGRY:"));
			iconMap.put(":FROWN:", getImage("icons/frown.png", ":FROWN:"));
			iconMap.put(":HEART:", getImage("icons/heart.png", ":HEART:"));
			iconMap.put(":SLANT:", getImage("icons/slant.png", ":SLANT:"));
			iconMap.put(":STRAIGHTFACE:", getImage("icons/straightface.png", ":STRAIGHTFACE:"));
			
			// set out custom renderer to the combo box
			iconBox = new JComboBox<IconEntry>();
			iconBox.setRenderer(new ListCellRenderer<IconEntry>() {
				
				public Component getListCellRendererComponent(
						JList<? extends IconEntry> list, IconEntry value, int index,
						boolean isSelected, boolean cellHasFocus) {
					
					if (cellHasFocus) {
						value.setBackground(list.getSelectionBackground());
						value.setForeground(list.getSelectionForeground());
						value.setBorder(BorderFactory.createRaisedBevelBorder());
					} else {
						value.setBackground(list.getBackground());
						value.setForeground(list.getForeground());
						value.setBorder(BorderFactory.createEmptyBorder());
					}
					
					value.setIcon(value.getImageIcon());
					
					return value;
				}
			});
			
			for (Iterator<Entry<CharSequence, ImageIcon>> itr = iconMap.entrySet().iterator(); itr.hasNext();)
				iconBox.addItem(new IconEntry(itr.next().getValue()));
			
			iconBox.addItemListener(new iconSelected(iconBox));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, e.getLocalizedMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
			dispose();
		}
	}

	private ImageIcon getImage(String path, String description)
			throws IOException {

		java.net.URL url = getClass().getClassLoader().getResource(path);

		if (url != null) {
			return new ImageIcon(url, description);
		} else {
			throw new IOException("failed to load an image");
		}
	}

	// abstract methods from WindowListener interface that we don't use
	public void windowDeactivated(WindowEvent event) {
	}

	public void windowActivated(WindowEvent event) {
	}

	public void windowDeiconified(WindowEvent event) {
	}

	public void windowIconified(WindowEvent event) {
	}

	public void windowOpened(WindowEvent event) {
	}

	public void windowClosed(WindowEvent event) {
	}

	// we send the last packet to leave chat when the window is closed
	public void windowClosing(WindowEvent event) {

		try {
			chatter.sendPacket(PacketType.LEAVE_CHAT, nickName);
		} catch (TrafficException e) {
			JOptionPane.showInternalMessageDialog(this, e.getLocalizedMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public void keyTyped(KeyEvent e) {

		// so pipe cannot be typed
		if (e.getKeyChar() == '|') {
			e.consume();
		} else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
			// check for icon regex and delete it
		} else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
			// check for icon regex and delete it
		}
	}

	// abstract methods from keyListener interface that we don't use
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
			e.consume();
			doChat(chatInput.getText());
		}
	}

	// can send on enter press
	public void keyReleased(KeyEvent e) {
	}

	// abstract methods from inputMethodListener interface that we don't use
	public void caretPositionChanged(InputMethodEvent e) {
	}

	// pipe character will interfere with packet structure if sent
	public void inputMethodTextChanged(InputMethodEvent e) {

		for (AttributedCharacterIterator itr = e.getText(); itr.getEndIndex() == AttributedCharacterIterator.DONE;) {

			if (itr.next() == '|')
				e.consume();
		}
	}

	private void doChat(String input) {
		if (!input.isEmpty() && !input.trim().isEmpty()) {

			chatInput.setText(null);

			// remove any pipe characters that would interfere with the packet
			// structure
			while (input.contains("|")) {

				input = input.replace('|', '\b');
			}

			try {
				chatter.sendPacket(PacketType.HERES_SOME_TEXT, String.format(
						"%s|%s|%d|%d|%d", nickName, input, optionsDialog
								.getFontIndex(), optionsDialog.getChatFont(optionsDialog.getFontIndex())
								.getSize(), optionsDialog.getChatTextColor()
								.getRGB()));
			} catch (TrafficException e) {
				JOptionPane.showInternalMessageDialog(this, e.getLocalizedMessage(),
						"Error", JOptionPane.ERROR_MESSAGE);
			}
		} else {
		} // just continue
	}

	// why the ActionListener is used, so we run this method when our send
	// button is pressed
	public void actionPerformed(ActionEvent action) {

		if (action.getActionCommand().equals("sendPush")) {
			doChat(chatInput.getText());
		} else if (action.getActionCommand().equals("optionPush")) {

			optionsDialog
					.setLocation(
							(int) ((getWidth() * .5)
									- (optionsDialog.getWidth() * .5) + getX()),
							(int) ((getHeight() * .5)
									- (optionsDialog.getHeight() * .5) + getY()));
			optionsDialog.setVisible(true);

			try {

				// construct a Writer to write in the configuration file
				BufferedWriter writer = new BufferedWriter(new FileWriter(
						"config"));

				writer.write(String.format("address:%s", chatter.getAddress()));
				writer.newLine();
				writer.write(String.format("port:%d", chatter.getPort()));
				writer.newLine();
				writer.write(String.format("font:%d",
						optionsDialog.getFontIndex()));
				writer.newLine();
				writer.write(String.format("fsize:%d",
						optionsDialog.getFontSizeIndex()));
				writer.newLine();
				writer.write(String.format("fcolor:%d", optionsDialog
						.getChatTextColor().getRGB()));
				writer.close();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this,
						"Config File: " + e.getLocalizedMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	// Err, the starting point of the application (you know that, right)
	public static void main(String[] args) {

		// invoke interface for thread safety
		SwingUtilities.invokeLater(new Runnable() {

			// swing related as to run when event is invoked, here we construct
			// GUI
			public void run() {

				try {
				if (Float.parseFloat(System.getProperty("java.version")
						.substring(0, 3)) >= 1.7) {

					// Instance of ChatWindow with the whole desktop screen as a
					// parameter
					new ChatWindow(Toolkit.getDefaultToolkit().getScreenSize());
				} else {
					JOptionPane.showMessageDialog(null,
							"Java JRE 1.7 or higher is required", "Error",
							JOptionPane.ERROR_MESSAGE);
				}
				} catch (NumberFormatException ex) {
					
					JOptionPane.showMessageDialog(null,
							ex.getLocalizedMessage(), "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});
	}

	private class iconSelected implements ItemListener {

		private final JComponent parent;

		public iconSelected(final JComponent parent) {

			this.parent = parent;
		}

		public void itemStateChanged(ItemEvent e) {

			if (e.getStateChange() == ItemEvent.DESELECTED)
				return;

			Style emoIconStyle = inputDoc.addStyle("icon", inputStyle);
			ImageIcon icon = ((IconEntry)e.getItem()).getImageIcon();

			StyleConstants.setIcon(emoIconStyle, icon);

			try {
				inputDoc.insertString(inputDoc.getLength(),
						icon.getDescription(), emoIconStyle);
			} catch (BadLocationException ex) {
				JOptionPane.showMessageDialog(parent,
						"failed to insert: " + ex.getLocalizedMessage(), "Warning",
						JOptionPane.WARNING_MESSAGE);
			}

			inputDoc.removeStyle("icon");
		}
	}

	public OptionDialog getOptionDialog() {

		return optionsDialog;
	}
}
