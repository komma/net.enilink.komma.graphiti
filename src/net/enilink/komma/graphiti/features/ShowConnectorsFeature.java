package net.enilink.komma.graphiti.features;

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
		PictogramElement pe = context.getPictogramElements()[0];
		Collection<Diagram> diagrams = diagramService.getLinkedDiagrams(pe,
				true);

		if (!(pe instanceof ContainerShape)) {
			return;
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

		/*
		 * EList<Shape> children = null;
		 * 
		 * if(pe instanceof ContainerShape){ children = ((ContainerShape)
		 * pe).getChildren(); }
		 */

		// first, we need all model elements which are contained in the
		// subordered diagram
		if (diagrams.size() < 1)
			return;// there is no subordered diagram.

		// take the first diagram for testing first
		Diagram diq = diagrams.toArray(new Diagram[0])[0];

		List<Shape> toLayout = new LinkedList<Shape>();

		// we want all direct children's business objects
		for (Shape s : diq.getChildren()) {
			Object bo = getBusinessObjectForPictogramElement(s);

			if (bo instanceof IReference) {
				// simply count first...
				// refCnt++;
				// PictogramElement boPe =
				// getFeatureProvider().getPictogramElementForBusinessObject(bo);
				Shape connShape = currentConnectors.remove(bo);
				if (connShape == null) {// boPe == null){
					// we currently have no connector for this item, so we
					// create one
					Shape connectorShape = peService.createShape(nodeShape,
							true);
					types.designateInterface(connectorShape);

					Ellipse newElli = gaService.createEllipse(connectorShape);
					newElli.setStyle(styles.getStyleForToggle(getDiagram()));

					peService.createChopboxAnchor(connectorShape);

					// link the newly created shape with it's bo
					link(connectorShape, bo);
					// boPe = newShape;
					connShape = connectorShape;
				}

				toLayout.add((Shape) connShape);
			}
		}

		// everything that remains in the list connectorShapes does not have an
		// assigned instance in the
		// referenced diagram any more. Delete it.
		for (Shape s : currentConnectors.values()) {
			peService.deletePictogramElement(s);
		}

		// now we got all shapes together and need to layout them
		int currY = 5;

		for (Shape s : toLayout) {
			gaService.setLocationAndSize(s.getGraphicsAlgorithm(), 5, currY,
					10, 10);
			currY += 15;
		}
		// System.out.print(refCnt + " subordered model elements were found\n");
	}

	@Override
	public boolean canExecute(ICustomContext context) {
		PictogramElement[] pes = context.getPictogramElements();
		Collection<Diagram> linkedDiagrams = diagramService.getLinkedDiagrams(
				pes[0], false);
		if (pes != null && pes.length == 1) {
			Object bo = getBusinessObjectForPictogramElement(pes[0]);
			if (bo instanceof IReference) {
				return !linkedDiagrams.isEmpty();
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
