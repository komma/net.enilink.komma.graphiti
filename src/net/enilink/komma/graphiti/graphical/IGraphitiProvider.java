package net.enilink.komma.graphiti.graphical;

import java.util.List;

import net.enilink.komma.core.IEntity;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.concepts.IResource;

import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.services.IGaService;

public interface IGraphitiProvider {

	NodeFigure getNodeFigure(IResource node);

	GraphicsAlgorithm getNodeShape(IEntity node, IGaService gaService,
			ContainerShape nodeShape, Diagram topLevelDiagram);

	List<IProperty> getSupportedConnections(IEntity source, IEntity target);
}
