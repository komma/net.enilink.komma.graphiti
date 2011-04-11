package net.enilink.layout;

import net.enilink.komma.core.IEntity;
import net.enilink.composition.annotations.Iri;
import net.enilink.vocab.rdf.List;

/** 
 * 
 * @generated 
 */
@Iri("http://enilink.net/vocab/layout#PointList")
public interface PointList extends List<Point>, IEntity {
	/** 
	 * The first item in the subject RDF list.
	 * @generated 
	 */
	@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#first")
	Point getRdfFirst();
	/** 
	 * The first item in the subject RDF list.
	 * @generated 
	 */
	void setRdfFirst(Point rdfFirst);

	/** 
	 * The rest of the subject RDF list after the first item.
	 * @generated 
	 */
	@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest")
	net.enilink.layout.PointList getRdfRest();
	/** 
	 * The rest of the subject RDF list after the first item.
	 * @generated 
	 */
	void setRdfRest(net.enilink.layout.PointList rdfRest);

}
