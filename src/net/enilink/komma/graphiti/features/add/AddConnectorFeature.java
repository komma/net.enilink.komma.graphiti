package net.enilink.komma.graphiti.features.add;

import net.enilink.komma.graphiti.Styles;
import net.enilink.komma.graphiti.concepts.Connector;
import net.enilink.komma.graphiti.service.ITypes;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.impl.AbstractAddShapeFeature;
import org.eclipse.graphiti.mm.algorithms.Ellipse;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeService;

import com.google.inject.Inject;

public class AddConnectorFeature extends AbstractAddShapeFeature {
	@Inject
	Styles styles;

	@Inject
	IGaService gaService;

	@Inject
	IPeService peService;

	@Inject
	ITypes types;

	@Inject
	public AddConnectorFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public boolean canAdd(IAddContext context) {
		if (context.getTargetContainer() instanceof PictogramElement
				&& context.getNewObject() instanceof Connector
				&& context.getTargetContainer() instanceof ContainerShape) {
			return true;
		}

		return false;
	}

	@Override
	public PictogramElement add(IAddContext context) {
		ContainerShape nodeShape = (ContainerShape) context
				.getTargetContainer();

		// we currently have no connector for this item, so we
		// create one
		Shape connShape = peService.createShape(nodeShape, true);
		types.designateInterface(connShape);

		Ellipse ellipse = gaService.createEllipse(connShape);
		ellipse.setStyle(styles.getStyleForConnector(null));

		gaService.setLocationAndSize(ellipse, context.getX(), context.getY(),
				10, 10, true);

		peService.createChopboxAnchor(connShape);

		// link the newly created shape with it's bo
		link(connShape,
				((Connector) context.getNewObject()).getBusinessObject());
		return connShape;
	}
}
