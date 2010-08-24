package net.enilink.komma.graphiti.graphical;

import java.net.URL;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.StackLayout;
import org.eclipse.draw2d.geometry.Translatable;
import org.eclipse.gmf.runtime.draw2d.ui.mapmode.IMapMode;
import org.eclipse.gmf.runtime.draw2d.ui.render.RenderedImage;
import org.eclipse.gmf.runtime.draw2d.ui.render.factory.RenderedImageFactory;
import org.eclipse.gmf.runtime.draw2d.ui.render.figures.ScalableImageFigure;
import org.eclipse.graphiti.platform.ga.IGraphicsAlgorithmRenderer;

public class NodeFigure extends Figure implements IGraphicsAlgorithmRenderer,
		IMapMode {

	Shape shapeFigure;

	public NodeFigure(URL url) {
		setLayoutManager(new StackLayout());

		RenderedImage image = RenderedImageFactory.getInstance(url);
		if (image != null) {
			ScalableImageFigure imageFigure = new ScalableImageFigure(image,
					false, true, true);

			add(imageFigure);
		}
	}

	@Override
	protected void paintFigure(Graphics graphics) {
		// do nothing
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
