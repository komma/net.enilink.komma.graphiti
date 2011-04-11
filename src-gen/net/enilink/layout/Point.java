package net.enilink.layout;

import net.enilink.composition.annotations.Iri;

/** 
 * 
 * @generated 
 */
@Iri("http://enilink.net/vocab/layout#Point")
public interface Point extends SpatialThing {
	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/layout#position")
	Position getLayoutPosition();
	/** 
	 * 
	 * @generated 
	 */
	void setLayoutPosition(Position layoutPosition);

}
