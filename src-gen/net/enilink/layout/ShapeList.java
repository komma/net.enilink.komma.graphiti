package net.enilink.layout;

import net.enilink.komma.core.IEntity;
import net.enilink.composition.annotations.Iri;
import net.enilink.vocab.rdf.List;

/** 
 * 
 * @generated 
 */
@Iri("http://enilink.net/vocab/layout#ShapeList")
public interface ShapeList extends List<Shape>, IEntity {
	/** 
	 * The first item in the subject RDF list.
	 * @generated 
	 */
	@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#first")
	Shape getRdfFirst();
	/** 
	 * The first item in the subject RDF list.
	 * @generated 
	 */
	void setRdfFirst(Shape rdfFirst);

	/** 
	 * The rest of the subject RDF list after the first item.
	 * @generated 
	 */
	@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest")
	net.enilink.layout.ShapeList getRdfRest();
	/** 
	 * The rest of the subject RDF list after the first item.
	 * @generated 
	 */
	void setRdfRest(net.enilink.layout.ShapeList rdfRest);

}
