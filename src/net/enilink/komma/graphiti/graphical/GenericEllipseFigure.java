package net.enilink.komma.graphiti.graphical;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Ellipse;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.graphiti.platform.ga.IGraphicsAlgorithmRenderer;
import org.eclipse.graphiti.platform.ga.IVisualState;
import org.eclipse.graphiti.platform.ga.IVisualStateChangeListener;
import org.eclipse.graphiti.platform.ga.IVisualStateHolder;
import org.eclipse.graphiti.platform.ga.VisualState;
import org.eclipse.graphiti.platform.ga.VisualStateChangedEvent;

public class GenericEllipseFigure extends Ellipse implements
		IGraphicsAlgorithmRenderer, IVisualStateChangeListener,
		IVisualStateHolder {

	private static final int NORMAL_LINE_WIDTH = 2;
	private static final int SELECTED_LINE_WIDTH = 4;

	private VisualState visualState = null;

	private boolean isTextBold;

	private String caption = "";

	public GenericEllipseFigure() {
		lineWidth = NORMAL_LINE_WIDTH;
		isTextBold = false;
		// foreground color is the color used for the outline
		setForegroundColor(ColorConstants.black);
	}

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

		// System.out.print("call to SourceFigure::visualStateChanged()\n");

		switch (selectionFeedback) {
		case IVisualState.SELECTION_PRIMARY:
			isTextBold = true;
			setLineWidth(SELECTED_LINE_WIDTH);
			break;
		case IVisualState.SELECTION_SECONDARY:
			break;
		case IVisualState.SELECTION_FEEDBACK_OFF:
			isTextBold = false;
			setLineWidth(NORMAL_LINE_WIDTH);
			break;
		default:
			break;
		}
	}

	@Override
	public void outlineShape(Graphics g) {
		// System.out.print("call to SourceFigure::outlineShape()\n");
		// first we simply call the parent's outlineShape function since we
		// would do exactly the same here.
		super.outlineShape(g);

		// then we draw a Q into the center of our ellipse.
		Rectangle r = getBounds().getCopy();

		// FontData[] allFonts = g.getFont().getFontData(),oldFont;
		// Device fontDevice = g.getFont().getDevice();
		//
		// oldFont = allFonts.clone();

		// on X, there may be more than one font, we set the style for each.
		// for(FontData current: allFonts){
		// current.setStyle((isTextBold) ? org.eclipse.swt.SWT.BOLD :
		// org.eclipse.swt.SWT.NORMAL);
		// current.setHeight(r.height/2);
		// }

		// g.setFont(new Font(fontDevice,allFonts));

		int fWidth, fHeight;

		fWidth = g.getFontMetrics().getAverageCharWidth();
		fHeight = g.getFontMetrics().getHeight();

		// now we finally draw the Q
		g.drawText(caption, r.x + (r.width - fWidth) / 2, r.y
				+ (r.height - fHeight) / 2);

		// undo our changes to the fonts
		// g.setFont(new Font(fontDevice,oldFont));
	}

	public void setCaption(String newCaption) {
		if (null != newCaption)
			caption = newCaption;
	}
}
