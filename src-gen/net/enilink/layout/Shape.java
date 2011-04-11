package net.enilink.layout;

import net.enilink.composition.annotations.Iri;

/** 
 * 
 * @generated 
 */
@Iri("http://enilink.net/vocab/layout#Shape")
public interface Shape extends Pictogram {
	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/layout#dimension")
	Dimension getLayoutDimension();
	/** 
	 * 
	 * @generated 
	 */
	void setLayoutDimension(Dimension layoutDimension);

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
