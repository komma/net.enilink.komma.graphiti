package net.enilink.komma.graphiti.features.add;

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

import net.enilink.vocab.systems.Handling;
import net.enilink.vocab.systems.Sink;
import net.enilink.vocab.systems.Source;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.graphiti.SystemGraphicsAlgorithmRendererFactory;
import net.enilink.komma.graphiti.features.create.IURIFactory;
import net.enilink.komma.model.IModel;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;

public class AddNodeFeature extends AbstractAddShapeFeature {
	@Inject
	IModel model;

	@Inject
	IURIFactory uriFactory;

	@Inject
	public AddNodeFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public boolean canAdd(IAddContext context) {
		if (context.getNewObject() instanceof IEntity) {
			if (context.getTargetContainer() instanceof Diagram) {
				if (!(context.getNewObject() instanceof IClass)) {
					return getFeatureProvider()
							.getPictogramElementForBusinessObject(
									context.getNewObject()) == null;
				}
				return true;
			}
		}

		return false;
	}

	@Override
	public PictogramElement add(IAddContext context) {
		IEntity node;
		if (context.getNewObject() instanceof IClass) {
			node = model.getManager().createNamed(uriFactory.createURI(),
					(IReference) context.getNewObject());
		} else {
			node = (IEntity) context.getNewObject();
		}

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
		} else if (node instanceof Handling) {
			gaId = SystemGraphicsAlgorithmRendererFactory.TRANSPORTFIGURE;
		} else {
			// default for unknown node types
			gaId = SystemGraphicsAlgorithmRendererFactory.STATIONFIGURE;
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
