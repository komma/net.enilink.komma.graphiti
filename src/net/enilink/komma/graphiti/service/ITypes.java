package net.enilink.komma.graphiti.service;

import org.eclipse.graphiti.mm.pictograms.PictogramElement;

public interface ITypes {
	final String INTERFACE_TAG = "INTERFACE";
	final String EXPANDED_TAG = "EXPANDED";
	final String POOLCONTAINER_TAG = "POOLCONTAINER";

	void designateInterface(PictogramElement pe);

	boolean isInterface(PictogramElement pe);

	void designateExpanded(PictogramElement pe);

	boolean isExpanded(PictogramElement pe);

	void removeExpanded(PictogramElement pe);

	void setPoolParameter(PictogramElement pe, String parameter, Object value);

	String getPoolParameter(PictogramElement pe, String parameter);
}
