package org.petersen.landc.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.*;

@SuppressWarnings("serial")
public final class StartLoader extends JWindow {

	private JProgressBar loader;
	private BufferedImage logo;
	private Paint p;

	public StartLoader(Dimension screenSize) throws IOException {

		GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();

		int loaderHeight = (int) (screenSize.height <= 1024 ? 80 : screenSize.height * .02);
		loader = new JProgressBar();
		
		if (gc.isTranslucencyCapable()) {
			BufferedImage source = ImageIO.read(getClass()
					.getClassLoader().getResource("img/logo.png"));
			logo = gc.createCompatibleImage(
					source.getWidth(), source.getHeight(), BufferedImage.BITMASK);
			Graphics logoGraphics = logo.getGraphics();
			logoGraphics.drawImage(source, 0, 0, null);
	
			WritableRaster wr = logo.getRaster();
	
			for (int i = 0; i < logo.getWidth(); i++) {
				for (int j = 0; j < logo.getHeight(); j++) {
	
					int[] color = new int[4];
					wr.getPixel(i, j, color);
	
					if (color[1] == 255)
						wr.setPixel(i, j, new int[] { color[0], color[1], color[2],
								0 });
				}
			}
			
			p = new GradientPaint(0.0f, 0.0f, new Color(0, 0, 0, 0), 0.0f,
					getHeight(), new Color(0, 0, 0, 0), true);
			setSize(logo.getWidth(), logo.getHeight() + loaderHeight);
			loader.setSize(logo.getWidth(), loaderHeight);
		} else {
			int loaderWidth = (int)(screenSize.width * .16);
			
			logo = null;
			setSize(loaderWidth, loaderHeight);
			loader.setSize(loaderWidth, loaderHeight);
		}
		
		setBackground(new Color(0, 0, 0, 0));
		setLocation((int) (screenSize.width * .5 - getWidth() * .5),
				(int) (screenSize.height * .5 - getHeight() * .5));
		setLayout(new BorderLayout());

		loader.setValue(0);
		loader.setIndeterminate(false);
		loader.setBorderPainted(false);
		loader.setOpaque(true);
		loader.setOrientation(SwingConstants.HORIZONTAL);
		loader.setStringPainted(true);

		add(loader, BorderLayout.PAGE_END);
	}

	@Override
	public void paint(Graphics g) {

		if (logo != null) {
			Graphics2D g2D = (Graphics2D) g;
	
			g2D.setPaint(p);
			g2D.fillRect(0, 0, getWidth(), getHeight());
			//g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
			//		RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2D.setRenderingHint(RenderingHints.KEY_RENDERING,
					RenderingHints.VALUE_RENDER_SPEED);
			g2D.drawImage(logo, 0, 0, logo.getWidth(), logo.getHeight(), null);
		}
		
		super.paint(g);
	}

	public JProgressBar getProgressBar() {

		return loader;
	}
}
