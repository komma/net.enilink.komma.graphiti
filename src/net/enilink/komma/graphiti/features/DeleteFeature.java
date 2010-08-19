package net.enilink.komma.graphiti.features;

import java.util.Collections;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.ui.features.DefaultDeleteFeature;

import com.google.inject.Inject;

import net.enilink.komma.model.IModel;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IStatement;

public class DeleteFeature extends DefaultDeleteFeature {
	@Inject
	IModel model;

	@Inject
	public DeleteFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public void deleteBusinessObject(Object bo) {
		if (bo instanceof IEntity) {
			model.getManager().remove(bo);
		} else if (bo instanceof IStatement) {
			model.getManager().remove(Collections.singleton((IStatement) bo));
		}
	}
}
