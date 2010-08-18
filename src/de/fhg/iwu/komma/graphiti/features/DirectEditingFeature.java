package de.fhg.iwu.komma.graphiti.features;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.IDirectEditingContext;
import org.eclipse.graphiti.features.impl.AbstractDirectEditingFeature;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;

import net.enilink.vocab.systems.System;

public class DirectEditingFeature extends AbstractDirectEditingFeature {

	public DirectEditingFeature(IFeatureProvider fp) {
		super(fp);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int getEditingType() {
		return TYPE_TEXT;
	}

	@Override
	public String getInitialValue(IDirectEditingContext context) {
		Object bo = getBusinessObjectForPictogramElement(context
				.getPictogramElement());

		if (bo instanceof System) {
			return ((System) bo).getSystemsName();
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
		Object bo = getBusinessObjectForPictogramElement(pe);

		if (bo instanceof System) {
			// set the item's name to the new value
			((System) bo).setSystemsName(value);
			// an update will have to be done here, but I think the update
			// feature will have to be implemented before this will affect
			// anything
			if (pe instanceof Shape) {
				updatePictogramElement(((Shape) pe).getContainer());
			}
		}
	}

	/**
	 * This function is used to determine whether this feature is applicable for
	 * the current object
	 */
	@Override
	public boolean canExecute(IContext context) {
		if (context instanceof IDirectEditingContext) {
			IDirectEditingContext c = (IDirectEditingContext) context;
			Object bo = getBusinessObjectForPictogramElement(c
					.getPictogramElement());

			if (bo instanceof System) {
				return true;
			}
		}

		return false;
	}

}
