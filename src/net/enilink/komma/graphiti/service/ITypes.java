package net.enilink.komma.graphiti.service;

import org.eclipse.graphiti.mm.pictograms.PictogramElement;

public interface ITypes {
	final String INTERFACE_TAG = "INTERFACE";
	
	void designateInterface(PictogramElement pe);

	boolean isInterface(PictogramElement pe);
}
