package net.enilink.layout;

import net.enilink.composition.annotations.Iri;

/** 
 * 
 * @generated 
 */
@Iri("http://enilink.net/vocab/layout#Pictogram")
public interface Pictogram extends SpatialThing {
	/** 
	 * Represents a context where some information is valid.
	 * E.g., a pictogram may either be valid in the context of another pictogram or in the context of an arbitrary object.
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/layout#context")
	Object getLayoutContext();
	/** 
	 * Represents a context where some information is valid.
	 * E.g., a pictogram may either be valid in the context of another pictogram or in the context of an arbitrary object.
	 * @generated 
	 */
	void setLayoutContext(Object layoutContext);

	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/layout#target")
	Object getLayoutTarget();
	/** 
	 * 
	 * @generated 
	 */
	void setLayoutTarget(Object layoutTarget);

}
