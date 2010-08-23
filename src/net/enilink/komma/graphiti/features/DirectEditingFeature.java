package net.enilink.komma.graphiti.features;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.IDirectEditingContext;
import org.eclipse.graphiti.features.impl.AbstractDirectEditingFeature;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

import com.google.inject.Inject;

import net.enilink.komma.concepts.IResource;
import net.enilink.komma.graphiti.service.IDiagramService;

public class DirectEditingFeature extends AbstractDirectEditingFeature {
	@Inject
	IDiagramService diagramService;

	@Inject
	public DirectEditingFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public int getEditingType() {
		return TYPE_TEXT;
	}

	@Override
	public String getInitialValue(IDirectEditingContext context) {
		IResource resource = getResource(context.getPictogramElement());

		if (resource != null) {
			String label = resource.getRdfsLabel();
			if (label != null) {
				return label;
			}
		}

		return "";
	}

	@Override
	public String checkValueValid(String value, IDirectEditingContext context) {
		if (value.length() < 1)
			return "Please enter a name.";
		if (value.contains("\n"))
			return "Line breaks are not allowed in a name.";

		return null;
	}

	@Override
	public void setValue(String value, IDirectEditingContext context) {
		PictogramElement pe = context.getPictogramElement();

		// set the item's name to the new value
		getResource(pe).setRdfsLabel(value);

		updatePictogramElement(diagramService.getRootElement(pe));
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
		if (pe.getGraphicsAlgorithm() instanceof Text) {
			final Object bo = diagramService.getRootBusinessObject(pe);

			if (bo instanceof IResource) {
				return (IResource) bo;
			}
		}
		return null;
	}
}
