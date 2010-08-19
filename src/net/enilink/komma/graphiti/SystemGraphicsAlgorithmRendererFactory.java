package net.enilink.komma.graphiti;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.platform.ga.IGraphicsAlgorithmRenderer;
import org.eclipse.graphiti.platform.ga.IGraphicsAlgorithmRendererFactory;
import org.eclipse.graphiti.platform.ga.IRendererContext;

import com.google.inject.Inject;

import net.enilink.vocab.systems.System;
import net.enilink.komma.graphiti.graphical.GenericEllipseFigure;
import net.enilink.komma.graphiti.graphical.GenericRectangleFigure;
import net.enilink.komma.graphiti.graphical.StationFigure;
import net.enilink.komma.graphiti.graphical.TransportFigure;

public class SystemGraphicsAlgorithmRendererFactory implements
		IGraphicsAlgorithmRendererFactory {
	// we seem to need string constants for all objects we provide:
	public final static String STATIONFIGURE = "test.station";
	public final static String TRANSPORTFIGURE = "test.transport";
	public final static String SOURCEFIGURE = "test.source";
	public final static String SINKFIGURE = "test.sink";
	public final static String INPUTCONNECTORFIGURE = "test.connector.input";
	public final static String OUTPUTCONNECTORFIGURE = "test.connector.output";

	@Inject
	IFeatureProvider fp;

	@Override
	public IGraphicsAlgorithmRenderer createGraphicsAlgorithmRenderer(
			IRendererContext rendererContext) {
		java.lang.System.out
				.print("call to TestGraphicsAlgorithmRendererFactory::createGraphicsAlgorithmRenderer()\n");

		PictogramElement pe = rendererContext.getGraphicsAlgorithm()
				.getPictogramElement();

		IGraphicsAlgorithmRenderer renderer = null;
		if (STATIONFIGURE.equals(rendererContext.getPlatformGraphicsAlgorithm()
				.getId())) {
			renderer = createStationFigure(pe);
		} else if (TRANSPORTFIGURE.equals(rendererContext
				.getPlatformGraphicsAlgorithm().getId())) {
			renderer = createTransportFigure(pe);
		} else if (SOURCEFIGURE.equals(rendererContext
				.getPlatformGraphicsAlgorithm().getId())) {
			renderer = createSourceFigure();
		} else if (SINKFIGURE.equals(rendererContext
				.getPlatformGraphicsAlgorithm().getId())) {
			renderer = createSinkFigure();
		} else if (INPUTCONNECTORFIGURE.equals(rendererContext
				.getPlatformGraphicsAlgorithm().getId())) {
			renderer = createInputConnectorFigure();
		} else if (OUTPUTCONNECTORFIGURE.equals(rendererContext
				.getPlatformGraphicsAlgorithm().getId())) {
			renderer = createOutputConnectorFigure();
		}

		return renderer;
	}

	private StationFigure createStationFigure(PictogramElement pe) {
		StationFigure figure = new StationFigure();

		if (pe != null) {
			Object bo = fp.getBusinessObjectForPictogramElement(pe);

			if (bo instanceof System) {
				figure.setLabel(((System) bo).getSystemsName());
			}
		}

		return figure;
	}

	private TransportFigure createTransportFigure(PictogramElement pe) {
		TransportFigure figure = new TransportFigure();

		if (pe != null) {
			Object bo = fp.getBusinessObjectForPictogramElement(pe);

			if (bo instanceof System) {
				figure.setCaption(((System) bo).getSystemsName());
			}
		}

		return figure;
	}

	private GenericEllipseFigure createSourceFigure() {
		GenericEllipseFigure figure = new GenericEllipseFigure();

		figure.setBackgroundColor(ColorConstants.red);
		figure.setForegroundColor(ColorConstants.blue);
		figure.setCaption("Q");

		return figure;
	}

	private GenericEllipseFigure createSinkFigure() {
		GenericEllipseFigure figure = new GenericEllipseFigure();

		figure.setBackgroundColor(ColorConstants.blue);
		figure.setForegroundColor(ColorConstants.gray);
		figure.setCaption("S");

		return figure;
	}

	private GenericRectangleFigure createInputConnectorFigure() {
		GenericRectangleFigure figure = new GenericRectangleFigure();

		figure.setCaption("I");

		return figure;
	}

	private GenericRectangleFigure createOutputConnectorFigure() {
		GenericRectangleFigure figure = new GenericRectangleFigure();

		figure.setCaption("O");

		return figure;
	}
}
