package net.enilink.komma.graphiti.service;

import org.eclipse.graphiti.mm.GraphicsAlgorithmContainer;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

public interface IDiagramService {
	PictogramElement getRootOrFirstElementWithBO(GraphicsAlgorithmContainer element);

	Object getFirstBusinessObject(GraphicsAlgorithmContainer element);
}
