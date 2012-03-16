package net.enilink.komma.graphiti.graphical;

import java.util.List;

import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.services.IGaService;

import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.core.IEntity;

public interface IGraphitiProvider {

	public NodeFigure getNodeFigure(IResource node);

	public GraphicsAlgorithm getNodeShape(IEntity node, IGaService gaService,
			ContainerShape nodeShape, Diagram topLevelDiagram);

	public List<IProperty> getSupportedConnections(IEntity source,
			IEntity target);
}
