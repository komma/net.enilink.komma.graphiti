package de.fhg.iwu.komma.graphiti.features;

import java.util.Collections;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IRemoveContext;
import org.eclipse.graphiti.features.impl.DefaultRemoveFeature;

import com.google.inject.Inject;

import net.enilink.komma.model.IModel;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IStatement;

public class RemoveFeature extends DefaultRemoveFeature {
	@Inject
	IModel model;

	@Inject
	public RemoveFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public void preRemove(IRemoveContext context) {
		Object bo = getBusinessObjectForPictogramElement(context
				.getPictogramElement());

		if (bo instanceof IEntity) {
			model.getManager().remove(bo);
		} else if (bo instanceof IStatement) {
			model.getManager().remove(Collections.singleton((IStatement) bo));
		}
	}
}
