package net.enilink.komma.graphiti.service;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.mm.GraphicsAlgorithmContainer;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

import com.google.inject.Inject;

public class DiagramService implements IDiagramService {
	@Inject
	IFeatureProvider featureProvider;

	public PictogramElement getRootOrFirstElementWithBO(
			GraphicsAlgorithmContainer element) {
		while (!(element instanceof ContainerShape || element.eContainer() instanceof Diagram)) {
			if (element instanceof PictogramElement
					&& featureProvider
							.getBusinessObjectForPictogramElement((PictogramElement) element) != null) {
				return (PictogramElement) element;
			}

			element = (GraphicsAlgorithmContainer) element.eContainer();
		}

		return (PictogramElement) element;
	}

	public Object getFirstBusinessObject(GraphicsAlgorithmContainer element) {
		element = getRootOrFirstElementWithBO(element);

		if (element instanceof PictogramElement) {
			return featureProvider
					.getBusinessObjectForPictogramElement((PictogramElement) element);
		}
		return null;
	}
}
