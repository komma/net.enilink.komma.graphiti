package net.enilink.komma.graphiti.features;

import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.graphiti.service.IDiagramService;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.IDirectEditingContext;
import org.eclipse.graphiti.features.impl.AbstractDirectEditingFeature;
import org.eclipse.graphiti.mm.algorithms.AbstractText;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.jface.viewers.ILabelProvider;

import com.google.inject.Inject;

public class DirectEditingFeature extends AbstractDirectEditingFeature {
	@Inject
	IDiagramService diagramService;

	@Inject
	ILabelProvider labelProvider;

	@Inject
	public DirectEditingFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public int getEditingType() {
		return TYPE_MULTILINETEXT;
	}

	@Override
	public String getInitialValue(IDirectEditingContext context) {
		IResource resource = getResource(context.getPictogramElement());

		if (resource != null) {
			return labelProvider.getText(resource);
		}

		return "";
	}

	@Override
	public String checkValueValid(String value, IDirectEditingContext context) {
		if (value.length() < 1) {
			return "Please enter a name.";
		}

		return null;
	}

	@Override
	public void setValue(String value, IDirectEditingContext context) {
		PictogramElement pe = context.getPictogramElement();

		// set the item's name to the new value
		getResource(pe).setRdfsLabel(value);

		updatePictogramElement(diagramService.getRootOrFirstElementWithBO(pe));
	}

	/**
	 * This function is used to determine whether this feature is applicable for
	 * the current object
	 */
	@Override
	public boolean canExecute(IContext context) {
		if (context instanceof IDirectEditingContext) {
			IDirectEditingContext c = (IDirectEditingContext) context;
			PictogramElement pe = c.getPictogramElement();
			return getResource(pe) != null;
		}

		return false;
	}

	IResource getResource(PictogramElement pe) {
		if (pe.getGraphicsAlgorithm() instanceof AbstractText) {
			final Object bo = diagramService.getFirstBusinessObject(pe);

			if (bo instanceof IResource) {
				return (IResource) bo;
			}
		}
		return null;
	}
}
