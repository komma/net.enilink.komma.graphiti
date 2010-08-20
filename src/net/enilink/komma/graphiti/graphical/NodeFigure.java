package net.enilink.komma.graphiti.graphical;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.draw2d.BorderLayout;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.RoundedRectangle;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.geometry.Translatable;
import org.eclipse.gmf.runtime.draw2d.ui.mapmode.IMapMode;
import org.eclipse.gmf.runtime.draw2d.ui.render.factory.RenderedImageFactory;
import org.eclipse.gmf.runtime.draw2d.ui.render.figures.ScalableImageFigure;
import org.eclipse.graphiti.platform.ga.IGraphicsAlgorithmRenderer;
import org.eclipse.graphiti.platform.ga.IVisualState;
import org.eclipse.graphiti.platform.ga.IVisualStateChangeListener;
import org.eclipse.graphiti.platform.ga.IVisualStateHolder;
import org.eclipse.graphiti.platform.ga.VisualState;
import org.eclipse.graphiti.platform.ga.VisualStateChangedEvent;
import org.eclipse.swt.graphics.Color;

import net.enilink.komma.graphiti.KommaGraphitiPlugin;

public class NodeFigure extends RoundedRectangle implements
		IGraphicsAlgorithmRenderer, IVisualStateChangeListener,
		IVisualStateHolder, IMapMode {

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

	Color selectedElemColor, selectedTextColor;

	ScalableImageFigure imageFigure;

	public NodeFigure(String path) {
		// set standard values
		setBackgroundColor(NORMAL_BG_COLOR);
		setLineWidth(NORMAL_LINE_WIDTH);
		selectedElemColor = NORMAL_ELEM_COLOR;
		selectedTextColor = NORMAL_TEXT_COLOR;

		imageFigure = new ScalableImageFigure(
				RenderedImageFactory.getInstance(FileLocator.find(
						KommaGraphitiPlugin.getPlugin().getBundle(), new Path(
								path), null)), true, true, true);
		add(imageFigure);
	}

	@Override
	public void setBounds(Rectangle rect) {
		imageFigure.setBounds(rect);
		super.setBounds(rect);
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
	public int LPtoDP(int logicalUnit) {
		return logicalUnit;
	}

	@Override
	public int DPtoLP(int deviceUnit) {
		return deviceUnit;
	}

	@Override
	public Translatable DPtoLP(Translatable t) {
		t.performScale(1.0);
		return t;
	}

	@Override
	public Translatable LPtoDP(Translatable t) {
		t.performScale(1.0);
		return t;
	}
}
