package net.enilink.layout;

import net.enilink.vocab.owl.Thing;
import net.enilink.komma.core.IEntity;
import net.enilink.composition.annotations.Iri;
import java.util.Set;

/** 
 * 
 * @generated 
 */
@Iri("http://enilink.net/vocab/layout#SpatialThing")
public interface SpatialThing extends Thing, IEntity {
	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/layout#extent")
	Set<SpatialExtent> getLayoutExtent();
	/** 
	 * 
	 * @generated 
	 */
	void setLayoutExtent(Set<? extends SpatialExtent> layoutExtent);

}
