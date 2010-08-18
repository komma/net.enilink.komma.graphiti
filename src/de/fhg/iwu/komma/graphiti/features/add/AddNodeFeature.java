package de.fhg.iwu.komma.graphiti.features.add;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.impl.AbstractAddShapeFeature;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IPeCreateService;

import com.google.inject.Inject;

import de.fhg.iwu.komma.graphiti.SystemGraphicsAlgorithmRendererFactory;
import net.enilink.vocab.systems.Handling;
import net.enilink.vocab.systems.Sink;
import net.enilink.vocab.systems.Source;
import net.enilink.vocab.systems.Station;
import net.enilink.vocab.systems.System;

public class AddNodeFeature extends AbstractAddShapeFeature {
	@Inject
	public AddNodeFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public boolean canAdd(IAddContext context) {
		if (context.getNewObject() instanceof System) {
			if (context.getTargetContainer() instanceof Diagram)
				return true;
		}

		return false;
	}

	@Override
	public PictogramElement add(IAddContext context) {
		if (!(context.getNewObject() instanceof System)) {
			return null;
		}

		System node = (System) context.getNewObject();

		// CONTAINER SHAPE WITH ROUNDED RECTANGLE
		final IPeCreateService peCreateService = Graphiti.getPeCreateService();
		final ContainerShape containerShape = peCreateService
				.createContainerShape(context.getTargetContainer(), true);

		// check whether the context has a size (e.g. from a create feature)
		// otherwise define a default size for the shape
		final int width = context.getWidth() <= 0 ? 50 : context.getWidth();
		final int height = context.getHeight() <= 0 ? 50 : context.getHeight();

		Graphiti.getPeCreateService().createChopboxAnchor(containerShape);

		String gaId = null;
		if (node instanceof Source) {
			gaId = SystemGraphicsAlgorithmRendererFactory.SOURCEFIGURE;
		} else if (node instanceof Sink) {
			gaId = SystemGraphicsAlgorithmRendererFactory.SINKFIGURE;
		} else if (node instanceof Station) {
			gaId = SystemGraphicsAlgorithmRendererFactory.STATIONFIGURE;
		} else if (node instanceof Handling) {
			gaId = SystemGraphicsAlgorithmRendererFactory.TRANSPORTFIGURE;
		}
		if (gaId != null) {
			GraphicsAlgorithm ga = Graphiti.getGaCreateService()
					.createPlatformGraphicsAlgorithm(containerShape, gaId);

			// this places our new figure in the global diagram
			Graphiti.getGaService().setLocationAndSize(ga, context.getX(),
					context.getY(), width, height);
			layoutPictogramElement(containerShape);
		}

		link(containerShape, node);

		return containerShape;
	}

}
