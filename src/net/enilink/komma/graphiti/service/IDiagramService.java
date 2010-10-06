package net.enilink.komma.graphiti.service;

import java.util.Collection;

import org.eclipse.graphiti.mm.GraphicsAlgorithmContainer;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

public interface IDiagramService {
	PictogramElement getRootOrFirstElementWithBO(
			GraphicsAlgorithmContainer element);

	Collection<Diagram> getLinkedDiagrams(PictogramElement pe,
			boolean createOnDemand);

	Object getFirstBusinessObject(GraphicsAlgorithmContainer element);

	Diagram getTopLevelDiagram();
}
