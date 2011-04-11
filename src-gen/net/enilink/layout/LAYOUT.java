package net.enilink.layout;

import net.enilink.komma.core.URIImpl;
import net.enilink.komma.core.URI;

public interface LAYOUT {
	public static final String NAMESPACE = "http://enilink.net/vocab/layout#";
	public static final URI NAMESPACE_URI = URIImpl.createURI(NAMESPACE);

	public static final URI TYPE_POINT = NAMESPACE_URI.appendFragment("Point");

	public static final URI TYPE_POSITION = NAMESPACE_URI.appendFragment("Position");

	public static final URI TYPE_VECTOR = NAMESPACE_URI.appendFragment("Vector");

	public static final URI TYPE_SPATIALEXTENT = NAMESPACE_URI.appendFragment("SpatialExtent");

	public static final URI TYPE_CONNECTION = NAMESPACE_URI.appendFragment("Connection");

	public static final URI TYPE_PICTOGRAM = NAMESPACE_URI.appendFragment("Pictogram");

	public static final URI TYPE_DIMENSION = NAMESPACE_URI.appendFragment("Dimension");

	public static final URI TYPE_SHAPE = NAMESPACE_URI.appendFragment("Shape");

	public static final URI TYPE_SPATIALTHING = NAMESPACE_URI.appendFragment("SpatialThing");

	public static final URI TYPE_RELATIVEPOSITION = NAMESPACE_URI.appendFragment("RelativePosition");

	public static final URI TYPE_POINTLIST = NAMESPACE_URI.appendFragment("PointList");

	public static final URI PROPERTY_POINTS = NAMESPACE_URI.appendFragment("points");

	public static final URI PROPERTY_END = NAMESPACE_URI.appendFragment("end");

	public static final URI PROPERTY_START = NAMESPACE_URI.appendFragment("start");

	public static final URI PROPERTY_Y = NAMESPACE_URI.appendFragment("y");

	public static final URI PROPERTY_Z = NAMESPACE_URI.appendFragment("z");

	public static final URI PROPERTY_EXTENT = NAMESPACE_URI.appendFragment("extent");

	public static final URI PROPERTY_POSITION = NAMESPACE_URI.appendFragment("position");

	public static final URI PROPERTY_DIMENSION = NAMESPACE_URI.appendFragment("dimension");

	public static final URI PROPERTY_CONTEXT = NAMESPACE_URI.appendFragment("context");

	public static final URI PROPERTY_TARGET = NAMESPACE_URI.appendFragment("target");

	public static final URI PROPERTY_REFERENCE = NAMESPACE_URI.appendFragment("reference");

	public static final URI PROPERTY_X = NAMESPACE_URI.appendFragment("x");

}
