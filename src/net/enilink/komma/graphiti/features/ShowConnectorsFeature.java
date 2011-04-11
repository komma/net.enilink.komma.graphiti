package net.enilink.komma.graphiti.features;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.custom.AbstractCustomFeature;
import org.eclipse.graphiti.mm.algorithms.Ellipse;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeService;

import com.google.inject.Inject;

import net.enilink.komma.graphiti.Styles;
import net.enilink.komma.graphiti.service.IDiagramService;
import net.enilink.komma.graphiti.service.ITypes;
import net.enilink.komma.core.IReference;

public class ShowConnectorsFeature extends AbstractCustomFeature {
	@Inject
	IPeService peService;

	@Inject
	IGaService gaService;

	@Inject
	Styles styles;

	@Inject
	ITypes types;

	@Inject
	IDiagramService diagramService;

	@Inject
	public ShowConnectorsFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public void execute(ICustomContext context) {
		for (PictogramElement pe : context.getPictogramElements()) {
			if (!(pe instanceof ContainerShape)) {
				continue;
			}

			ContainerShape nodeShape = (ContainerShape) pe;
			for (Shape shape : nodeShape.getChildren()) {
				if (shape instanceof ContainerShape) {
					nodeShape = (ContainerShape) shape;
				}
			}

			EList<Shape> csChildren = nodeShape.getChildren();
			Map<IReference, Shape> currentConnectors = new HashMap<IReference, Shape>();

			// first we determine all connectors we already have
			for (Shape s : csChildren) {
				if (types.isInterface(s)) {
					currentConnectors
							.put((IReference) getBusinessObjectForPictogramElement(s),
									s);
				}
			}

			Collection<? extends Shape> children;
			if (types.isExpanded(nodeShape)) {
				children = new ArrayList<Shape>(nodeShape.getChildren());
			} else {
				Collection<Diagram> diagrams = diagramService
						.getLinkedDiagrams(pe, false);
				if (diagrams.isEmpty()) {
					return;
				}
				children = diagrams.iterator().next().getChildren();
			}

			List<Shape> toLayout = new LinkedList<Shape>();

			// we want all direct children's business objects
			for (Shape s : children) {
				// skip interfaces in case of an expanded node
				if (types.isInterface(s)) {
					continue;
				}

				Object bo = getBusinessObjectForPictogramElement(s);

				if (bo instanceof IReference) {
					Shape connShape = currentConnectors.remove(bo);
					if (connShape == null) {
						// we currently have no connector for this item, so we
						// create one
						Shape connectorShape = peService.createShape(nodeShape,
								true);
						types.designateInterface(connectorShape);

						Ellipse ellipse = gaService
								.createEllipse(connectorShape);
						ellipse.setStyle(styles.getStyleForConnector(null));

						peService.createChopboxAnchor(connectorShape);

						// link the newly created shape with it's bo
						link(connectorShape, bo);
						// boPe = newShape;
						connShape = connectorShape;

						toLayout.add((Shape) connShape);
					}
				}
			}

			// everything that remains in the list connectorShapes does not have
			// an assigned instance in the referenced diagram any more. Delete
			// it.
			for (Shape s : currentConnectors.values()) {
				peService.deletePictogramElement(s);
			}

			// now we got all shapes together and need to layout them
			int currY = 5;
			for (Shape s : toLayout) {
				gaService.setLocationAndSize(s.getGraphicsAlgorithm(), 5,
						currY, 10, 10);
				currY += 15;
			}
		}
	}

	@Override
	public boolean canExecute(ICustomContext context) {
		for (PictogramElement pe : context.getPictogramElements()) {
			if (!(pe instanceof ContainerShape)) {
				return false;
			}

			ContainerShape nodeShape = (ContainerShape) pe;
			for (Shape shape : nodeShape.getChildren()) {
				if (shape instanceof ContainerShape) {
					nodeShape = (ContainerShape) shape;
				}
			}

			if (types.isExpanded(nodeShape)) {
				return true;
			} else {
				Collection<Diagram> diagrams = diagramService
						.getLinkedDiagrams(pe, false);
				if (diagrams.isEmpty()) {
					return false;
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public String getName() {
		return new String("Show connectors");
	}

	@Override
	public String getDescription() {
		return new String("Creates connectors for internal components");
	}
}
