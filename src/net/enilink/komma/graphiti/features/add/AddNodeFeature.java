package net.enilink.komma.graphiti.features.add;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.impl.AbstractAddShapeFeature;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.algorithms.RoundedRectangle;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.algorithms.styles.Orientation;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeService;
import org.eclipse.graphiti.util.ColorConstant;
import org.eclipse.graphiti.util.IColorConstant;

import com.google.inject.Inject;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.graphiti.StyleUtil;
import net.enilink.komma.graphiti.SystemGraphicsAlgorithmRendererFactory;
import net.enilink.komma.graphiti.features.create.IURIFactory;
import net.enilink.komma.model.IModel;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;

public class AddNodeFeature extends AbstractAddShapeFeature {
	static final IColorConstant NODE_TEXT_FOREGROUND = new ColorConstant(51,
			51, 153);

	@Inject
	IModel model;

	@Inject
	IURIFactory uriFactory;

	@Inject
	IAdapterFactory adapterFactory;

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

		// CONTAINER SHAPE WITH ROUNDED RECTANGLE+
		final IGaService gaService = Graphiti.getGaService();
		final IPeService peService = Graphiti.getPeService();
		final ContainerShape container = peService.createContainerShape(
				context.getTargetContainer(), true);
		// create link and wire it
		link(container, node);

		// check whether the context has a size (e.g. from a create feature)
		// otherwise define a default size for the shape
		final int width = context.getWidth() <= 0 ? 50 : context.getWidth();
		final int height = context.getHeight() <= 0 ? 50 : context.getHeight();

		{
			Rectangle invisibleRectangle = Graphiti.getGaCreateService()
					.createInvisibleRectangle(container);
			gaService.setLocationAndSize(invisibleRectangle, context.getX(),
					context.getY(), width, height + 20);

			RoundedRectangle roundedRectangle = Graphiti.getGaCreateService()
					.createRoundedRectangle(invisibleRectangle, 15, 15);
			roundedRectangle.setStyle(StyleUtil.getStyleForNode(getDiagram()));

			// create and set graphics algorithm
			GraphicsAlgorithm ga = Graphiti.getGaCreateService()
					.createPlatformGraphicsAlgorithm(roundedRectangle,
							SystemGraphicsAlgorithmRendererFactory.NODE_FIGURE);
		}

		// SHAPE WITH TEXT
		{
			// create shape for text
			Shape shape = peService.createShape(container, false);

			AdapterFactoryLabelProvider labelProvider = new AdapterFactoryLabelProvider(
					adapterFactory);

			// create and set text graphics algorithm
			Text text = gaService.createDefaultText(shape,
					labelProvider.getText(node));

			labelProvider.dispose();

			text.setForeground(manageColor(NODE_TEXT_FOREGROUND));
			text.setHorizontalAlignment(Orientation.ALIGNMENT_CENTER);
			text.setVerticalAlignment(Orientation.ALIGNMENT_TOP);
			text.getFont().setBold(true);

			// // create link and wire it
			// link(shape, node);
		}

		peService.createChopboxAnchor(container);

		layoutPictogramElement(container);

		return container;
	}
}
