package net.enilink.komma.graphiti.features;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.custom.AbstractCustomFeature;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.RoundedRectangle;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeService;
import org.eclipse.graphiti.util.ColorConstant;

import com.google.inject.Inject;

import net.enilink.komma.graphiti.service.IDiagramService;
import net.enilink.komma.graphiti.service.ITypes;
import net.enilink.komma.core.IReference;

public class ExpandFeature extends AbstractCustomFeature {
	@Inject
	IDiagramService diagramService;

	@Inject
	IGaService gaService;

	@Inject
	IPeService peService;

	protected final String SHIFTX_TAG = "shiftX";

	protected final String SHIFTY_TAG = "shiftY";

	@Inject
	ITypes types;

	@Inject
	public ExpandFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public boolean canExecute(ICustomContext context) {
		for (PictogramElement pe : context.getPictogramElements()) {
			pe = diagramService.getRootOrFirstElementWithBO(pe);

			if (!(pe instanceof ContainerShape))
				return false;// we need a shape representation to check the
								// parent

			ContainerShape cs = (ContainerShape) pe;

			if (!cs.getContainer().equals(getDiagram()))
				return false;// only allow this feature for first level
								// instances

			Object bo = getBusinessObjectForPictogramElement(pe);
			// only allow expanding RDF objects
			if (bo instanceof IReference)
				return true;// possibly we might want to check whether this item
							// has
							// a subordered diagram?
		}

		return false;
	}

	@Override
	public void execute(ICustomContext context) {
		for (PictogramElement pe : context.getPictogramElements()) {
			pe = diagramService.getRootOrFirstElementWithBO(pe);

			ContainerShape nodeShape = getNodeShape((ContainerShape) pe);
			if (types.isExpanded(nodeShape)) {
				continue;
			}
			types.designateExpanded(nodeShape);

			Rectangle rect = null;

			Collection<Diagram> diagrams = diagramService.getLinkedDiagrams(pe,
					false);
			if (!diagrams.isEmpty()) {
				Diagram diagram = diagrams.iterator().next();
				diagram = EcoreUtil.copy(diagram);

				EList<Connection> conns = diagram.getConnections();
				getDiagram().getConnections().addAll(conns);

				// determine the number of pixels to shift all elements, i.e.
				// the
				// smallest x and y coordinates of the contained elements
				// also determine maximum values
				for (Shape s : diagram.getChildren()) {
					GraphicsAlgorithm ga = s.getGraphicsAlgorithm();

					if (rect == null) {
						rect = new Rectangle(ga.getX(), ga.getY(),
								ga.getWidth(), ga.getHeight());
					} else {
						rect.union(ga.getX(), ga.getY(), ga.getWidth(),
								ga.getHeight());
					}
				}

				// add all diagram elements to our new container shape
				nodeShape.getChildren().addAll(diagram.getChildren());
			}

			// create a new shape which will replace the current
			GraphicsAlgorithm nodeGA = nodeShape.getGraphicsAlgorithm();

			if (rect == null) {
				rect = new Rectangle();
			}

			// set reasonable width and height
			rect.width = Math.max(nodeGA.getWidth(), rect.width);
			rect.height = Math.max(nodeGA.getHeight(), rect.height);

			// compute size of outer container
			rect.width += nodeGA.getX() + pe.getGraphicsAlgorithm().getWidth()
					- nodeGA.getWidth();
			rect.height += nodeGA.getY()
					+ pe.getGraphicsAlgorithm().getHeight()
					- nodeGA.getHeight();

			gaService.setSize(pe.getGraphicsAlgorithm(), rect.width,
					rect.height);

			Shape hiddenShape = peService.createShape(nodeShape, false);
			hiddenShape.setVisible(false);

			nodeGA.setPictogramElement(hiddenShape);

			RoundedRectangle rr = gaService.createRoundedRectangle(nodeShape,
					10, 10);
			rr.setBackground(gaService.manageColor(getDiagram(),
					new ColorConstant(255, 255, 255)));

			// consider margins when shifting objects
			int shiftX = rect.x - 15;
			int shiftY = rect.y - 15;

			List<Shape> connectors = new LinkedList<Shape>();
			for (Shape child : nodeShape.getChildren()) {
				if (types.isInterface(child)) {
					// connectors are handled different since they shall be
					// connected to their instances later
					connectors.add(child);
					continue;
				}

				GraphicsAlgorithm ga = child.getGraphicsAlgorithm();

				// position child elements
				ga.setX(ga.getX() - shiftX);
				ga.setY(ga.getY() - shiftY);
			}

			// we need to add the children to this diagram first for the
			// connection
			// method to work.
			// otherwise getPictogramElementForBusinessObject() does not work.
			// so now here we create the connections between connectors and the
			// instances which they represent
			for (Shape currShape : connectors) {
				Object boundaryBo = getBusinessObjectForPictogramElement(currShape);

				// connect the connector with its associated diagram element
				for (PictogramElement itemPe : getFeatureProvider()
						.getAllPictogramElementsForBusinessObject(boundaryBo)) {
					if (itemPe instanceof ContainerShape
							&& (!itemPe.equals(currShape))) {
						itemPe = getNodeShape((ContainerShape) itemPe);

						Connection conn = peService
								.createFreeFormConnection(getDiagram());
						conn.setEnd(currShape.getAnchors().get(0));
						conn.setStart(((Shape) itemPe).getAnchors().get(0));
						gaService.createPolyline(conn);
					}
				}
			}

			types.setPoolParameter(nodeShape, SHIFTX_TAG, shiftX);
			types.setPoolParameter(nodeShape, SHIFTY_TAG, shiftY);

			layoutPictogramElement(pe);
		}
	}

	@Override
	public String getDescription() {
		return "Show this element's structure";
	}

	@Override
	public String getName() {
		return "Expand";
	}

	protected ContainerShape getNodeShape(ContainerShape shape) {
		for (Shape child : shape.getChildren()) {
			if (child instanceof ContainerShape) {
				return (ContainerShape) child;
			}
		}
		return shape;
	}
}
