package net.enilink.komma.graphiti.service;

import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.IPeService;

import com.google.inject.Inject;

public class Types implements ITypes {
	@Inject
	IPeService peService;

	@Override
	public boolean isInterface(PictogramElement pe) {
		return peService.getProperty(pe, INTERFACE_TAG) != null;
	}

	@Override
	public void designateInterface(PictogramElement pe) {
		peService.setPropertyValue(pe, INTERFACE_TAG, "");
	}

	@Override
	public boolean isExpanded(PictogramElement pe) {
		return peService.getProperty(pe, EXPANDED_TAG) != null;
	}

	@Override
	public void designateExpanded(PictogramElement pe) {
		peService.setPropertyValue(pe, EXPANDED_TAG, "");
	}
}