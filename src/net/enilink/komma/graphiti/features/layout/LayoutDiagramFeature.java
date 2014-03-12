package net.enilink.komma.graphiti.features.layout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.enilink.komma.graphiti.IKommaDiagramImages;
import net.enilink.komma.graphiti.service.IDiagramService;

import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.graph.CompoundDirectedGraph;
import org.eclipse.draw2d.graph.CompoundDirectedGraphLayout;
import org.eclipse.draw2d.graph.Edge;
import org.eclipse.draw2d.graph.EdgeList;
import org.eclipse.draw2d.graph.Node;
import org.eclipse.draw2d.graph.NodeList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.custom.AbstractCustomFeature;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.algorithms.styles.StylesFactory;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;

import com.google.inject.Inject;

/**
 * Maps the Graphiti Diagram to a graph structure which can be consumed by the
 * GEF Layouter, layouts the graph structure and maps the new coordinates back
 * to the diagram. Refresh is triggered automatically by the changes on the
 * diagram model.
 * 
 * Disclaimer: this is just an example to show how to plug an arbitrary layouter
 * into a Graphiti diagram editor. For instance, the basic layouting here does
 * not consider bendpoints etc.
 * 
 */
public class LayoutDiagramFeature extends AbstractCustomFeature {

	/**
	 * Minimal distance between nodes.
	 */
	private static final int PADDING = 30;

	@Inject
	IDiagramService diagramService;

	@Inject
	public LayoutDiagramFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public String getDescription() {
		return "Layout diagram with GEF Layouter"; //$NON-NLS-1$
	}

	@Override
	public String getName() {
		return "Layout Diagram"; //$NON-NLS-1$
	}

	@Override
	public boolean canExecute(ICustomContext context) {
		return true;
	}

	@Override
	public void execute(ICustomContext context) {
		final CompoundDirectedGraph graph = mapDiagramToGraph();
		graph.setDefaultPadding(new Insets(PADDING));
		new CompoundDirectedGraphLayout().visit(graph);
		mapGraphCoordinatesToDiagram(graph);
	}

	private Diagram mapGraphCoordinatesToDiagram(CompoundDirectedGraph graph) {
		NodeList myNodes = new NodeList();
		myNodes.addAll(graph.nodes);
		myNodes.addAll(graph.subgraphs);
		for (Object object : myNodes) {
			Node node = (Node) object;
			Shape shape = (Shape) node.data;
			shape.getGraphicsAlgorithm().setX(node.x);
			shape.getGraphicsAlgorithm().setY(node.y);
			shape.getGraphicsAlgorithm().setWidth(node.width);
			shape.getGraphicsAlgorithm().setHeight(node.height);
		}

		for (Edge edge : (List<Edge>) graph.edges) {
			Connection connection = (Connection) edge.data;
			if (connection instanceof FreeFormConnection) {
				List<Point> bendpoints = ((FreeFormConnection) connection)
						.getBendpoints();
				bendpoints.clear();

				org.eclipse.draw2d.geometry.Point p = new org.eclipse.draw2d.geometry.Point();
				PointList points = edge.getPoints();
				// skip first and last point
				for (int i = 1; i < points.size() - 1; i++) {
					points.getPoint(p, i);

					Point bendpoint = StylesFactory.eINSTANCE.createPoint();
					bendpoint.setX(p.x);
					bendpoint.setY(p.y);
					bendpoints.add(bendpoint);
				}
			}
		}
		return null;
	}

	private CompoundDirectedGraph mapDiagramToGraph() {
		Map<AnchorContainer, Node> shapeToNode = new HashMap<AnchorContainer, Node>();
		Diagram d = getDiagram();
		CompoundDirectedGraph dg = new CompoundDirectedGraph();
		EdgeList edgeList = new EdgeList();
		NodeList nodeList = new NodeList();
		EList<Shape> children = d.getChildren();
		for (Shape shape : children) {
			Node node = new Node();
			GraphicsAlgorithm ga = shape.getGraphicsAlgorithm();
			node.x = ga.getX();
			node.y = ga.getY();
			node.width = ga.getWidth();
			node.height = ga.getHeight();
			node.data = shape;
			shapeToNode.put(shape, node);
			nodeList.add(node);
		}
		EList<Connection> connections = d.getConnections();
		for (Connection connection : connections) {
			if (connection.getStart() == null || connection.getEnd() == null) {
				// ignore invalid connections
				continue;
			}

			Node source = getNodeForPe(shapeToNode, connection.getStart()
					.getParent());
			Node target = getNodeForPe(shapeToNode, connection.getEnd()
					.getParent());
			if (source == null || target == null || source == target) {
				continue;
			}
			Edge edge = new Edge(source, target);
			edge.data = connection;
			edgeList.add(edge);
		}
		dg.nodes = nodeList;
		dg.edges = edgeList;
		return dg;
	}

	protected Node getNodeForPe(Map<AnchorContainer, Node> shapeToNode,
			PictogramElement pe) {
		while (pe != null) {
			Node node = shapeToNode.get(pe);
			if (node != null) {
				return node;
			}
			pe = (PictogramElement) pe.eContainer();
		}
		return null;
	}

	@Override
	public String getImageId() {
		return IKommaDiagramImages.LAYOUT_IMG;
	}

}
