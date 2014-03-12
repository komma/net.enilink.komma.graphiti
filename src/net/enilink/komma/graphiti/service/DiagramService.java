package net.enilink.komma.graphiti.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;

import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.impl.IIndependenceSolver;
import org.eclipse.graphiti.mm.GraphicsAlgorithmContainer;
import org.eclipse.graphiti.mm.Property;
import org.eclipse.graphiti.mm.PropertyContainer;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.IPeService;

import com.google.inject.Inject;

public class DiagramService implements IDiagramService {
	@Inject
	IFeatureProvider featureProvider;

	@Inject
	IPeService peService;

	@Inject
	IIndependenceSolver independenceSolver;

	/**
	 * Retrieve property by a given key. This method also works for deleted
	 * objects.
	 */
	public Property getProperty(PropertyContainer propertyContainer, String key) {
		Collection<Property> props = propertyContainer.getProperties();
		if (props != null) {
			for (Property p : props) {
				if (key.equals(p.getKey())) {
					return p;
				}
			}
		}
		return null;
	}

	@Override
	public Object getBusinessObjectForPictogramElement(
			PictogramElement pictogramElement) {
		Property property = getProperty(pictogramElement, "independentObject");
		if (property != null && property.getValue() != null) {
			return independenceSolver.getBusinessObjectForKey(property
					.getValue());
		}
		return null;
	}

	public PictogramElement getRootOrFirstElementWithBO(
			GraphicsAlgorithmContainer element) {
		if (element instanceof Diagram) {
			return (PictogramElement) element;
		}
		while (element.eContainer() != null
				&& !(element.eContainer() instanceof Diagram)) {
			if (element instanceof PictogramElement
					&& getBusinessObjectForPictogramElement((PictogramElement) element) != null) {
				return (PictogramElement) element;
			}
			element = (GraphicsAlgorithmContainer) element.eContainer();
		}
		return (PictogramElement) element;
	}

	public Object getFirstBusinessObject(GraphicsAlgorithmContainer element) {
		element = getRootOrFirstElementWithBO(element);
		if (element != null) {
			return getBusinessObjectForPictogramElement((PictogramElement) element);
		}
		return null;
	}

	public Collection<Diagram> getLinkedDiagrams(PictogramElement pe,
			boolean createOnDemand) {
		final Diagram currentDiagram = featureProvider.getDiagramTypeProvider()
				.getDiagram();
		final Collection<Diagram> diagrams = new HashSet<Diagram>();

		final Object[] businessObjectsForPictogramElement = featureProvider
				.getAllBusinessObjectsForPictogramElement(getRootOrFirstElementWithBO(pe));
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
					featureProvider.link(newDiagram, firstUri);
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

	@Override
	public Diagram getTopLevelDiagram() {
		for (TreeIterator<EObject> i = EcoreUtil.getAllProperContents(
				featureProvider.getDiagramTypeProvider().getDiagram()
						.eResource().getContents(), false); i.hasNext();) {
			EObject eObject = i.next();
			if (eObject instanceof Diagram) {
				return (Diagram) eObject;
			}
		}
		throw new NoSuchElementException();
	}
}
