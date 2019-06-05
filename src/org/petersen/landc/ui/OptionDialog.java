package org.petersen.landc.ui;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.*;

/**
 * 
 * @author Claus Petersen
 * 
 */
@SuppressWarnings("serial")
public final class OptionDialog extends JDialog implements ActionListener {

	// Components
	private JButton chatTextColorBtn;
	private JLabel chatTextSample;
	private JLabel fontLabel;
	private JLabel fontSizeLabel;
	private JComboBox<String> displayFonts;
	private Font[] fonts;
	private JComboBox<Integer> fontSizes;
	private Color chatTextColor;

	public OptionDialog(JFrame parent, Color initialColor) {

		super(parent, "Options", true);

		chatTextColor = initialColor;

		chatTextColorBtn = new JButton("Select Color");
		chatTextSample = new JLabel("Chat Text Color");
		fontLabel = new JLabel("Font");
		fontSizeLabel = new JLabel("Font Size");
		displayFonts = new JComboBox<String>();

		fontSizes = new JComboBox<Integer>(new Integer[] { 10, 12, 14, 16, 18,
				20, 22, 24 });

		setResizable(false);
		setSize(380, 140);
		Font btnFont = new Font(Font.MONOSPACED, Font.PLAIN
				| Font.TRUETYPE_FONT, 12);

		chatTextColorBtn.setFont(btnFont);
		chatTextColorBtn.setActionCommand("chatTextPush");
		chatTextColorBtn.addActionListener(this);

		chatTextSample.setOpaque(true);
		chatTextSample.setForeground(initialColor);
		chatTextSample.setBackground(distinguishColor(initialColor));

		displayFonts.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				
				if (e.getStateChange() == ItemEvent.SELECTED) {
					
					chatTextSample.setFont(fonts[displayFonts.getSelectedIndex()].deriveFont(12f));
				}
			}
		});
		
		createLayout(getContentPane());
	}

	private Color distinguishColor(Color color) {
		return new Color(Math.abs(color.getRed() - 0xFF), Math.abs(color
				.getGreen() - 0xFF), Math.abs(color.getBlue() - 0xFF));
	}

	private void createLayout(Container container) {

		GroupLayout grpLayout = new GroupLayout(container);
		container.setLayout(grpLayout);

		grpLayout.setAutoCreateGaps(true);
		grpLayout.setAutoCreateContainerGaps(true);

		GroupLayout.SequentialGroup hGroup = grpLayout.createSequentialGroup();

		hGroup.addGroup(grpLayout.createParallelGroup().addComponent(fontLabel)
				.addComponent(displayFonts).addComponent(chatTextColorBtn));
		hGroup.addGroup(grpLayout.createParallelGroup()
				.addComponent(fontSizeLabel).addComponent(fontSizes)
				.addComponent(chatTextSample));
		grpLayout.setHorizontalGroup(hGroup);

		GroupLayout.SequentialGroup vGroup = grpLayout.createSequentialGroup();

		vGroup.addGroup(grpLayout.createParallelGroup(Alignment.BASELINE)
				.addComponent(fontLabel).addComponent(fontSizeLabel));
		vGroup.addGroup(grpLayout.createParallelGroup(Alignment.BASELINE)
				.addComponent(displayFonts).addComponent(fontSizes));
		vGroup.addGroup(grpLayout.createParallelGroup(Alignment.BASELINE)
				.addComponent(chatTextColorBtn).addComponent(chatTextSample));
		grpLayout.setVerticalGroup(vGroup);
	}

	public Color getChatTextColor() {
		return chatTextColor;
	}

	public void setChatTextColor(Color color) {
		chatTextColor = color;

		if (color != null) {
			chatTextSample.setForeground(color);
			chatTextSample.setBackground(distinguishColor(color));
		} else
			;
	}

	public void setFonts(Font[] fonts) {

		this.fonts = fonts;

		for (Font font : fonts) {
			displayFonts.addItem(font.getFontName());
		}
		
		chatTextSample.setFont(fonts[displayFonts.getSelectedIndex()]);
	}

	public Font getChatFont(int index) {

		Font font = fonts[index];
		return font.deriveFont(fontSizes
				.getItemAt(fontSizes.getSelectedIndex()).floatValue());
	}

	public int getFontIndex() {
		return displayFonts.getSelectedIndex();
	}

	public int getFontSizeIndex() {
		return fontSizes.getSelectedIndex();
	}

	public void setChatFont(int font) {
		displayFonts.setSelectedIndex(font);
	}

	public void setChatFontSize(int fontSize) {
		fontSizes.setSelectedIndex(fontSize);
	}

	public void actionPerformed(ActionEvent e) {

		if (e.getActionCommand().equals("chatTextPush")) {
			chatTextColor = JColorChooser.showDialog(this,
					"Choose a text color for the chat", chatTextColor);

			if (chatTextColor != null) {
				chatTextSample.setForeground(chatTextColor);
				chatTextSample.setBackground(distinguishColor(chatTextColor));
			} else
				; // nothing
		}
	}
}
