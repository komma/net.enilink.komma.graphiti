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

public class GenericRectangleFigure extends RectangleFigure implements
		IGraphicsAlgorithmRenderer, IVisualStateChangeListener,
		IVisualStateHolder {

	VisualState visualState = null;

	private String caption = "";

	@Override
	public IVisualState getVisualState() {
		if (null == visualState) {
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
	public void outlineShape(Graphics g) {
		// System.out.print("call to SourceFigure::outlineShape()\n");
		// first we simply call the parent's outlineShape function since we
		// would do exactly the same here.
		super.outlineShape(g);

		if (caption.length() == 0)
			return;

		// then we draw a Q into the center of our ellipse.
		Rectangle r = getBounds().getCopy();

//		FontData[] allFonts = g.getFont().getFontData(), oldFont;
//		Device fontDevice = g.getFont().getDevice();
//
//		oldFont = allFonts.clone();
//
//		// on X, there may be more than one font, we set the style for each.
//		for (FontData current : allFonts) {
//			current.setStyle(org.eclipse.swt.SWT.NORMAL);
//			current.setHeight(r.height / 2);
//		}
//
//		g.setFont(new Font(fontDevice, allFonts));

		int fWidth, fHeight;

		fWidth = g.getFontMetrics().getAverageCharWidth();
		fHeight = g.getFontMetrics().getHeight();

		// now we finally draw the Q
		g.drawText(caption, r.x + (r.width - fWidth) / 2, r.y
				+ (r.height - fHeight) / 2);

		// undo our changes to the fonts
		// g.setFont(new Font(fontDevice, oldFont));
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String newCaption) {
		if (newCaption != null)
			caption = new String(newCaption);
	}
}
