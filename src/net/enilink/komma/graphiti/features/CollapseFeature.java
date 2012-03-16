package net.enilink.komma.graphiti.features;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
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
			if (pe instanceof ContainerShape && types.isExpanded(pe)) {
				return true;
			}

		}
		return false;
	}

	protected void collapse(ContainerShape cs) {
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

		LinkedList<Shape> items = new LinkedList<Shape>();
		for (Shape currShape : nodeShape.getChildren()) {
			if (!types.isInterface(currShape)) {
				// this seems to be some kind of item which we want to
				// remove from the children list
				items.add(currShape);
			}
		}

		// remove all connections of child elements
		Set<Connection> allConnections = new HashSet<Connection>();
		for (Shape c : items) {
			if (!(c instanceof ContainerShape)) {
				continue;
			}
			allConnections.addAll(peService
					.getAllConnections(getNodeShape((ContainerShape) c)));
		}
		getDiagram().getConnections().removeAll(allConnections);

		// now we remove everything we guess is some kind of item
		nodeShape.getChildren().removeAll(items);

		// using this, we get a neat freshly
		// layouted image, e.g. text labels will have correct layout
		// it seems to force graphiti to do whatever it also does when
		// resizing a pictogram element
		layoutPictogramElement(cs);
	}

	@Override
	public void execute(ICustomContext context) {
		for (PictogramElement pe : context.getPictogramElements()) {
			pe = diagramService.getRootOrFirstElementWithBO(pe);

			if (types.isExpanded(pe)) {
				collapse((ContainerShape) pe);
				types.removeExpanded(pe);// must no longer be marked as
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
