package org.petersen.landc.ui;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 * 
 * @author Claus Petersen
 *
 * An entry in a combo box
 */
@SuppressWarnings("serial")
final class IconEntry extends JLabel {

	private ImageIcon icon;
	
	public IconEntry(ImageIcon icon) {
		
		super(icon);
		this.icon = icon;
	}
	
	public ImageIcon getImageIcon() {
		
		return icon;
	}
}
