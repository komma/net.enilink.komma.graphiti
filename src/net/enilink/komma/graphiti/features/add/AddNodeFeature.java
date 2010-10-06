package net.enilink.komma.graphiti.features.add;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.impl.AbstractAddShapeFeature;
import org.eclipse.graphiti.mm.algorithms.AbstractText;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.algorithms.RoundedRectangle;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeService;
import org.eclipse.jface.viewers.ILabelProvider;

import com.google.inject.Inject;

import net.enilink.komma.concepts.IClass;
import net.enilink.komma.graphiti.Styles;
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
	Styles styles;

	@Inject
	IGaService gaService;

	@Inject
	IPeService peService;

	@Inject
	ILabelProvider labelProvider;

	@Inject
	public AddNodeFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public boolean canAdd(IAddContext context) {
		if (context.getNewObject() instanceof IEntity) {
			// if (context.getTargetContainer() instanceof Diagram) {
			if (!(context.getNewObject() instanceof IClass)) {
				return getFeatureProvider()
						.getPictogramElementForBusinessObject(
								context.getNewObject()) == null;
			}
			return true;
			// }
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
		final ContainerShape container = peService.createContainerShape(
				context.getTargetContainer(), true);
		Rectangle invisibleRectangle = gaService
				.createInvisibleRectangle(container);

		// create link and wire it
		link(container, node);

		final ContainerShape nodeShape = peService.createContainerShape(
				container, true);

		// check whether the context has a size (e.g. from a create feature)
		// otherwise define a default size for the shape
		final int width = context.getWidth() <= 0 ? 50 : context.getWidth();
		final int height = context.getHeight() <= 0 ? 50 : context.getHeight();

		gaService.setLocationAndSize(invisibleRectangle, context.getX(),
				context.getY(), width, height);

		{
			RoundedRectangle roundedRectangle = gaService
					.createRoundedRectangle(nodeShape, 15, 15);
			roundedRectangle.setStyle(styles.getStyleForNode(getDiagram()));

			// create and set graphics algorithm
			GraphicsAlgorithm ga = gaService.createPlatformGraphicsAlgorithm(
					roundedRectangle,
					SystemGraphicsAlgorithmRendererFactory.NODE_FIGURE);
		}

		// SHAPE WITH TEXT
		{
			// create shape for text
			Shape shape = peService.createShape(container, false);

			// create and set text graphics algorithm
			AbstractText text = gaService.createDefaultMultiText(shape,
					labelProvider.getText(node));

			text.setStyle(styles.getStyleForNodeText(getDiagram()));
		}

		peService.createChopboxAnchor(nodeShape);

		layoutPictogramElement(container);

		return container;
	}
}
