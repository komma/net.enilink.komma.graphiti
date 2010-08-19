package net.enilink.komma.graphiti.graphical;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.RoundedRectangle;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.graphiti.platform.ga.IGraphicsAlgorithmRenderer;
import org.eclipse.graphiti.platform.ga.IVisualState;
import org.eclipse.graphiti.platform.ga.IVisualStateChangeListener;
import org.eclipse.graphiti.platform.ga.IVisualStateHolder;
import org.eclipse.graphiti.platform.ga.VisualState;
import org.eclipse.graphiti.platform.ga.VisualStateChangedEvent;
import org.eclipse.swt.graphics.Color;

// obviously, this is the way to go for complex layouts
public class StationFigure extends RoundedRectangle implements
		IGraphicsAlgorithmRenderer, IVisualStateChangeListener,
		IVisualStateHolder {

	// org.eclipse.swt.graphics.Color is used here, not the color from Graphiti
	// for some reason
	private static final Color NORMAL_BG_COLOR = ColorConstants.yellow;
	private static final Color SELECTED_BG_COLOR = ColorConstants.blue;
	private static final Color NORMAL_ELEM_COLOR = ColorConstants.red;
	private static final Color SELECTED_ELEM_COLOR = ColorConstants.orange;
	private static final Color NORMAL_TEXT_COLOR = ColorConstants.black;
	private static final Color SELECTED_TEXT_COLOR = ColorConstants.white;
	private static final int NORMAL_LINE_WIDTH = 1;
	private static final int SELECTED_LINE_WIDTH = 3;

	// this holds the visual state of the object
	private VisualState visualState;

	private String label = "new station";

	Color selectedElemColor, selectedTextColor;

	public StationFigure() {
		// set standard values
		setCornerDimensions(new Dimension(4, 4));
		setBackgroundColor(NORMAL_BG_COLOR);
		setLineWidth(NORMAL_LINE_WIDTH);
		selectedElemColor = NORMAL_ELEM_COLOR;
		selectedTextColor = NORMAL_TEXT_COLOR;
	}

	// returns the object's visual state variable
	@Override
	public IVisualState getVisualState() {
		if (null == visualState) {
			visualState = new VisualState();
			visualState.addChangeListener(this);
		}

		return visualState;
	}

	// used to apply changes to the object's visual state
	@Override
	public void visualStateChanged(VisualStateChangedEvent e) {
		int selectionFeedback = visualState.getSelectionFeedback();

		// the object being selected as the primary or secondary is
		// distinguised, don't know what that means by now
		switch (selectionFeedback) {
		case IVisualState.SELECTION_PRIMARY:
			// this marks the object as being selected by the user
			// we set some variables to visualize the selected state
			setBackgroundColor(SELECTED_BG_COLOR);// deriving the object from a
													// draw2D object provided us
													// with this fine
													// possibility
			setLineWidth(SELECTED_LINE_WIDTH);
			selectedElemColor = SELECTED_ELEM_COLOR;
			selectedTextColor = SELECTED_TEXT_COLOR;
			break;
		case IVisualState.SELECTION_SECONDARY:
			// don't know what this is...
			break;
		case IVisualState.SELECTION_FEEDBACK_OFF:
			// my current guess is that this is used when the object is
			// unselected
			// we set back the visual style to unselected
			setBackgroundColor(NORMAL_BG_COLOR);
			setLineWidth(NORMAL_LINE_WIDTH);
			selectedElemColor = NORMAL_ELEM_COLOR;
			selectedTextColor = NORMAL_TEXT_COLOR;
			break;
		default:
			break;
		}
	}

	@Override
	protected void fillShape(Graphics g) {
		// could be used to use anti aliasing
		// if(ADVANCED_GRAPHICS)
		// g.setAntialias(SWT.ON);
		Rectangle bounds = getInnerBounds(), block;
		PointList arrow = new PointList();

		g.setBackgroundColor(getBackgroundColor());

		g.fillRoundRectangle(bounds, corner.width, corner.height);// ?

		block = new Rectangle(bounds.width / 8 + bounds.x, 3 * bounds.height
				/ 5 + bounds.y, 3 * bounds.width / 4, bounds.height / 5);
		g.setBackgroundColor(selectedElemColor);
		g.fillRectangle(block);

		arrow.addPoint(bounds.width / 2 + bounds.x - bounds.width / 8, bounds.y
				+ bounds.height / 10);
		arrow.addPoint(bounds.width / 2 + bounds.x, 5 * bounds.height / 10
				+ bounds.y);
		arrow.addPoint(bounds.width / 2 + bounds.x + bounds.width / 8, bounds.y
				+ bounds.height / 10);
		g.fillPolygon(arrow);
	}

	@Override
	protected void outlineShape(Graphics g) {
		Rectangle innerBounds = getInnerBounds();

		super.outlineShape(g);

		if (label != null) {
			// draw the name of the station
			// FontData[] allFonts = g.getFont().getFontData(), oldFont;
			// Device fontDevice = g.getFont().getDevice();

			// oldFont = allFonts.clone();
			// int neededWidth = label.length()
			// * g.getFontMetrics().getAverageCharWidth();
			// float factor = ((float) innerBounds.width) / ((float)
			// neededWidth);
			// int newCharHeight = Math.min(
			// (int) Math.floor(((float) g.getFontMetrics().getHeight())
			// * factor), innerBounds.height / 4);

			// for(FontData current: allFonts){
			// current.setHeight(newCharHeight);
			// }
			// // change font
			// g.setFont(new Font(fontDevice,allFonts));

			Color oldFGColor = getForegroundColor();

			setForegroundColor(selectedTextColor);
			int textWidth = label.length()
					* g.getFontMetrics().getAverageCharWidth();

			g.drawText(label, innerBounds.x + innerBounds.width / 2 - textWidth
					/ 2, innerBounds.y + 3 * innerBounds.height / 4 - 6);

			// undo changes we applied to the font
			// g.setFont(new Font(fontDevice,oldFont));

			setForegroundColor(oldFGColor);
		}
	}

	private Rectangle getInnerBounds() {
		Rectangle r = getBounds().getCopy();
		float lineInset = Math.max(1.0f, getLineWidthFloat()) / 2.0f;
		int leftInset = (int) Math.floor(lineInset);
		int rightInset = (int) Math.ceil(lineInset);

		r.x += leftInset;
		r.y += leftInset;
		r.height -= (leftInset + rightInset);
		r.width -= (leftInset + rightInset);

		return r;
	}

	public void setLabel(String label) {
		this.label = label;
	}
}
