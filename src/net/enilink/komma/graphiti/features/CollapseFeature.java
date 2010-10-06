package net.enilink.komma.graphiti.features;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;

import com.google.inject.Inject;

public class CollapseFeature extends ExpandFeature {

	@Inject
	public CollapseFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public boolean canExecute(ICustomContext context) {
		for (PictogramElement pe : context.getPictogramElements()) {
			// this is to allow an expanded element to be closed by executing
			// this feature on its container
			if (pe instanceof ContainerShape
					&& types.isExpanded(getNodeShape((ContainerShape) pe))) {
				return true;
			}

		}
		return false;
	}

	protected void collapse(ContainerShape cs) {
		Collection<Diagram> diagrams = diagramService.getLinkedDiagrams(cs,
				true);

		if (diagrams.isEmpty())
			return;

		ContainerShape nodeShape = getNodeShape(cs);

		GraphicsAlgorithm expandedNodeGa = nodeShape.getGraphicsAlgorithm();
		expandedNodeGa.getGraphicsAlgorithmChildren().clear();
		Shape hiddenShape = null;
		for (Shape s : nodeShape.getChildren()) {
			if (!s.isVisible()) {
				hiddenShape = s;
			}
		}

		if (hiddenShape != null) {
			hiddenShape.getGraphicsAlgorithm().setPictogramElement(nodeShape);
			peService.deletePictogramElement(hiddenShape);
		}

		GraphicsAlgorithm nodeGa = nodeShape.getGraphicsAlgorithm();

		nodeGa.setX(expandedNodeGa.getX());
		nodeGa.setY(expandedNodeGa.getY());
		nodeGa.setWidth(expandedNodeGa.getWidth());
		nodeGa.setHeight(expandedNodeGa.getHeight());

		Diagram diagram = diagrams.iterator().next();
		boolean hasConnectors = false;

		LinkedList<Shape> items = new LinkedList<Shape>();
		// determine shapes we have to copy back to the subdiagram
		// we have to prevent children which belong to this item from being
		// moved to the subdiagram,
		// this is done by only handling items which are some kind of
		// structure item, i.e. it has some
		// kind of OWL business item, but we also have to prevent the
		// connectors from being moved
		for (Shape currShape : nodeShape.getChildren()) {
			if (!types.isInterface(currShape)) {
				// this seems to be some kind of item which we want to
				// remove from the children list
				items.add(currShape);
			} else {
				hasConnectors = true;
			}
		}
		// now we remove everything we guess is some kind of item
		nodeShape.getChildren().removeAll(items);

		// here we sync the diagram representing the instance with the
		// contents of the pool
		// copy the children of the container to the diagram instance
		diagram.getChildren().clear();
		diagram.getChildren().addAll(items);

		HashSet<Connection> allConnections = new HashSet<Connection>();

		String val;
		int shiftX, shiftY;
		// float fXAspect, fYAspect;
		int lMargin = 5, tMargin = 5, rMargin = 5, bMargin = 25;

		if (hasConnectors) {
			lMargin += 15;
			tMargin += 15;
			rMargin += 15;
			bMargin += 15;
		}

		val = types.getPoolParameter(nodeShape, SHIFTX_TAG);
		if (val == null)
			return;// WOW! This should not happen...
		shiftX = Integer.parseInt(val);

		val = types.getPoolParameter(nodeShape, SHIFTY_TAG);
		if (val == null)
			return;// WOW! This should not happen...
		shiftY = Integer.parseInt(val);

		LinkedList<Connection> connectorConns = new LinkedList<Connection>();
		LinkedList<Connection> allConnsToRemove = new LinkedList<Connection>();

		// copy connections to the subordered diagram
		// and remove all connections to connectors
		for (Shape c : diagram.getChildren()) {
			if (!(c instanceof ContainerShape)) {
				continue;
			}
			for (Anchor a : getNodeShape((ContainerShape) c).getAnchors()) {
				// filter out connections from/to connectors
				for (Connection conn : a.getIncomingConnections()) {
					if (types.isInterface(conn.getStart().getParent())) {
						connectorConns.add(conn);
						allConnsToRemove.add(conn);
					}
				}
				a.getIncomingConnections().removeAll(connectorConns);
				connectorConns.clear();
				for (Connection conn : a.getOutgoingConnections()) {
					if (types.isInterface(conn.getEnd().getParent())) {
						connectorConns.add(conn);
						allConnsToRemove.add(conn);
					}
				}
				a.getOutgoingConnections().removeAll(connectorConns);
				connectorConns.clear();

				allConnections.addAll(a.getIncomingConnections());
				allConnections.addAll(a.getOutgoingConnections());
			}
			// undo the positioning we did to fit the box
			GraphicsAlgorithm ga = c.getGraphicsAlgorithm();
			ga.setX((int) ((ga.getX() - lMargin)/* / fXAspect */) + shiftX);
			ga.setY((int) ((ga.getY() - tMargin)/* / fYAspect */) + shiftY);
			// the diagram will look like it did before
		}

		diagram.getConnections().clear();
		diagram.getConnections().addAll(allConnections);

		// remove connections to connectors first
		for (Connection c : allConnsToRemove)
			peService.deletePictogramElement(c);

		// remove all connections from this diagram
		getDiagram().getConnections().removeAll(diagram.getConnections());

		// using this, we get a neat freshly layouted image, e.g. text
		// labels will have correct layout
		// it seems to force graphiti to do whatever it also does when
		// resizing a pictogram element
		layoutPictogramElement(cs);
	}

	@Override
	public void execute(ICustomContext context) {
		for (PictogramElement pe : context.getPictogramElements()) {
			pe = diagramService.getRootOrFirstElementWithBO(pe);

			ContainerShape nodeShape = getNodeShape((ContainerShape) pe);
			if (types.isExpanded(nodeShape)) {
				collapse((ContainerShape) pe);
				types.removeExpanded(nodeShape);// must no longer be marked as
												// expanded
			}
		}
	}

	@Override
	public String getDescription() {
		return "Hide this element's structure";
	}

	@Override
	public String getName() {
		return "Collapse";
	}

}
