package net.enilink.komma.graphiti.service;

import org.eclipse.graphiti.mm.pictograms.PictogramElement;

public interface ITypes {
	final String INTERFACE_TAG = "INTERFACE";
	final String EXPANDED_TAG = "EXPANDED";
	
	void designateInterface(PictogramElement pe);

	boolean isInterface(PictogramElement pe);
	
	void designateExpanded(PictogramElement pe);
	
	boolean isExpanded(PictogramElement pe);
}
