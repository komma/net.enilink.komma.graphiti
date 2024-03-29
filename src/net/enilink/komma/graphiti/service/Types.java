package net.enilink.komma.graphiti.service;

import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.IPeService;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class Types implements ITypes {
	@Inject
	IPeService peService;

	@Inject
	Provider<IDiagramService> diagramService;

	@Override
	public boolean isInterface(PictogramElement pe) {
		return diagramService.get().getProperty(pe, INTERFACE_TAG) != null;
	}

	@Override
	public void designateInterface(PictogramElement pe) {
		peService.setPropertyValue(pe, INTERFACE_TAG, "");
	}

	@Override
	public boolean isExpanded(PictogramElement pe) {
		return diagramService.get().getProperty(pe, EXPANDED_TAG) != null;
	}

	@Override
	public void designateExpanded(PictogramElement pe) {
		peService.setPropertyValue(pe, EXPANDED_TAG, "");
	}

	@Override
	public void removeExpanded(PictogramElement pe) {
		peService.removeProperty(pe, EXPANDED_TAG);
	}

	@Override
	public void setPoolParameter(PictogramElement pe, String parameter,
			Object value) {
		peService.setPropertyValue(pe, parameter, value.toString());
	}

	@Override
	public String getPoolParameter(PictogramElement pe, String parameter) {
		return peService.getPropertyValue(pe, parameter);
	}
}