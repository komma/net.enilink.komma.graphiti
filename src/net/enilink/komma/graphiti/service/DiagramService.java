package net.enilink.komma.graphiti.service;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.mm.GraphicsAlgorithmContainer;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.IPeService;

import com.google.inject.Inject;

import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;

public class DiagramService implements IDiagramService {
	@Inject
	IFeatureProvider featureProvider;

	@Inject
	IPeService peService;

	public PictogramElement getRootOrFirstElementWithBO(
			GraphicsAlgorithmContainer element) {
		if (element instanceof Diagram) {
			return (PictogramElement) element;
		}
		while (!(element.eContainer() instanceof Diagram)) {
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

	public Collection<Diagram> getLinkedDiagrams(PictogramElement pe,
			boolean createOnDemand) {
		final Diagram currentDiagram = featureProvider.getDiagramTypeProvider()
				.getDiagram();
		final Collection<Diagram> diagrams = new HashSet<Diagram>();

		final Object[] businessObjectsForPictogramElement = featureProvider
				.getAllBusinessObjectsForPictogramElement(pe);
		URI firstUri = null;
		for (Object bo : businessObjectsForPictogramElement) {
			if (bo instanceof IReference) {
				URI uri = ((IReference) bo).getURI();
				if (uri != null) {
					firstUri = uri;
				}
			}
		}
		if (firstUri != null) {
			String diagramId = "diagram_" + firstUri.fragment();

			EObject linkedDiagram = null;
			for (TreeIterator<EObject> i = EcoreUtil.getAllProperContents(
					currentDiagram.eResource().getContents(), false); i
					.hasNext();) {
				EObject eObject = i.next();
				if (eObject instanceof Diagram) {
					if (diagramId.equals(((Diagram) eObject).getName())) {
						linkedDiagram = eObject;
						break;
					}
				}
			}

			if (!(linkedDiagram instanceof Diagram)) {
				if (createOnDemand) {
					Diagram newDiagram = peService.createDiagram(
							currentDiagram.getDiagramTypeId(), diagramId,
							currentDiagram.isSnapToGrid());
					currentDiagram.eResource().getContents().add(newDiagram);

					linkedDiagram = newDiagram;
				} else {
					return diagrams;
				}
			}

			if (!EcoreUtil.equals(currentDiagram, linkedDiagram)) {
				diagrams.add((Diagram) linkedDiagram);
			}
		}

		return diagrams;
	}
}
