package net.enilink.layout;

import net.enilink.composition.annotations.Iri;

/** 
 * 
 * @generated 
 */
@Iri("http://enilink.net/vocab/layout#Vector")
public interface Vector extends SpatialExtent {
	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/layout#x")
	Double getLayoutX();
	/** 
	 * 
	 * @generated 
	 */
	void setLayoutX(Double layoutX);

	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/layout#y")
	Double getLayoutY();
	/** 
	 * 
	 * @generated 
	 */
	void setLayoutY(Double layoutY);

	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/layout#z")
	Double getLayoutZ();
	/** 
	 * 
	 * @generated 
	 */
	void setLayoutZ(Double layoutZ);

}
