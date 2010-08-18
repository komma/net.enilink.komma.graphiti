package de.fhg.iwu.komma.graphiti.features.create;

import com.google.inject.assistedinject.Assisted;

import net.enilink.komma.core.IReference;

public interface CreateNodeFeatureFactory {
	CreateNodeFeature create(IReference type, @Assisted("name") String name,
			@Assisted("description") String description);
}