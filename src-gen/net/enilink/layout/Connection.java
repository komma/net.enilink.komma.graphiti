package net.enilink.layout;

import net.enilink.composition.annotations.Iri;

/** 
 * 
 * @generated 
 */
@Iri("http://enilink.net/vocab/layout#Connection")
public interface Connection extends Pictogram {
	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/layout#end")
	Object getLayoutEnd();
	/** 
	 * 
	 * @generated 
	 */
	void setLayoutEnd(Object layoutEnd);

	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/layout#points")
	PointList getLayoutPoints();
	/** 
	 * 
	 * @generated 
	 */
	void setLayoutPoints(PointList layoutPoints);

	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/layout#start")
	Object getLayoutStart();
	/** 
	 * 
	 * @generated 
	 */
	void setLayoutStart(Object layoutStart);

}
