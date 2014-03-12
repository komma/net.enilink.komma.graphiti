package net.enilink.komma.graphiti.features.create;

import net.enilink.komma.core.IReference;

import com.google.inject.assistedinject.Assisted;

public interface CreateNodeFeatureFactory {
	CreateNodeFeature create(IReference type, @Assisted("name") String name,
			@Assisted("description") String description);
}