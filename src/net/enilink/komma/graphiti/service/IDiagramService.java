package net.enilink.komma.graphiti.service;

import java.util.Collection;

import org.eclipse.graphiti.mm.GraphicsAlgorithmContainer;
import org.eclipse.graphiti.mm.Property;
import org.eclipse.graphiti.mm.PropertyContainer;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

public interface IDiagramService {
	/**
	 * Retrieve property by a given key. This method also works for deleted
	 * objects.
	 */
	Property getProperty(PropertyContainer propertyContainer, String key);

	/**
	 * Retrieve business object for pictogram. This method also works for
	 * deleted objects.
	 */
	Object getBusinessObjectForPictogramElement(
			PictogramElement pictogramElement);

	PictogramElement getRootOrFirstElementWithBO(
			GraphicsAlgorithmContainer element);

	Collection<Diagram> getLinkedDiagrams(PictogramElement pe,
			boolean createOnDemand);

	Object getFirstBusinessObject(GraphicsAlgorithmContainer element);

	Diagram getTopLevelDiagram();
}
