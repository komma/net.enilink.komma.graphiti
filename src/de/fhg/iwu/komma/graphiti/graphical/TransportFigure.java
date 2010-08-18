package de.fhg.iwu.komma.graphiti.graphical;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.graphiti.platform.ga.IGraphicsAlgorithmRenderer;
import org.eclipse.graphiti.platform.ga.IVisualState;
import org.eclipse.graphiti.platform.ga.IVisualStateChangeListener;
import org.eclipse.graphiti.platform.ga.IVisualStateHolder;
import org.eclipse.graphiti.platform.ga.VisualState;
import org.eclipse.graphiti.platform.ga.VisualStateChangedEvent;

public class TransportFigure extends RectangleFigure implements
		IGraphicsAlgorithmRenderer, IVisualStateChangeListener,
		IVisualStateHolder {

	private VisualState visualState = null;

	private String caption = "";

	@Override
	public IVisualState getVisualState() {
		if (visualState == null) {
			visualState = new VisualState();
			visualState.addChangeListener(this);
		}

		return visualState;
	}

	@Override
	public void visualStateChanged(VisualStateChangedEvent e) {
		int selectionFeedback = visualState.getSelectionFeedback();

		switch (selectionFeedback) {
		case IVisualState.SELECTION_FEEDBACK_OFF:
			break;
		case IVisualState.SELECTION_PRIMARY:
			break;
		case IVisualState.SELECTION_SECONDARY:
			break;
		}
	}

	@Override
	protected void fillShape(Graphics g) {
		// do nothing
	}

	@Override
	public void outlineShape(Graphics g) {
		// System.out.print("call to SourceFigure::outlineShape()\n");
		// first we simply call the parent's outlineShape function since we
		// would do exactly the same here.
		super.outlineShape(g);

		if (caption.length() < 1)
			return;

		// then we draw a Q into the center of our ellipse.
		Rectangle r = getBounds().getCopy();

		// FontData[] allFonts = g.getFont().getFontData(),oldFont;
		// Device fontDevice = g.getFont().getDevice();
		//
		// int charWidth = g.getFontMetrics().getAverageCharWidth(),charHeight =
		// g.getFontMetrics().getHeight();
		// int maxWidth = (int)Math.floor(r.width / (caption.length() *
		// charWidth));
		// float factor = (float)charWidth / (float)maxWidth;
		// int maxHeight = (int)Math.floor((float)charHeight / factor);
		//
		// oldFont = allFonts.clone();
		//
		// // on X, there may be more than one font, we set the style for each.
		// for(FontData current: allFonts){
		// current.setStyle(org.eclipse.swt.SWT.NORMAL);
		// current.setHeight(maxHeight);
		// }
		//
		// g.setFont(new Font(fontDevice,allFonts));

		int fWidth, fHeight;

		fWidth = (g.getFontMetrics().getAverageCharWidth() * caption.length()) / 2;
		fHeight = g.getFontMetrics().getHeight();

		// now we finally draw the Q
		g.drawText(caption, r.x + (r.width - fWidth) / 2, r.y
				+ (r.height - fHeight) / 2);

		// undo our changes to the fonts
		// g.setFont(new Font(fontDevice,oldFont));
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String newCaption) {
		if (newCaption != null)
			caption = new String(newCaption);
	}

}
