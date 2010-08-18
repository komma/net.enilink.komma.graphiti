package de.fhg.iwu.komma.graphiti.features.create;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICreateConnectionContext;
import org.eclipse.graphiti.features.context.impl.AddConnectionContext;
import org.eclipse.graphiti.features.impl.AbstractCreateConnectionFeature;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.Connection;

import com.google.inject.Inject;

import net.enilink.vocab.systems.SYSTEMS;
import net.enilink.vocab.systems.System;
import net.enilink.komma.model.IModel;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.Statement;

public class CreateConnectionFeature extends AbstractCreateConnectionFeature {
	@Inject
	IModel model;

	@Inject
	public CreateConnectionFeature(IFeatureProvider fp) {
		// provide name and description for the UI, e.g. the palette
		super(fp, "Connection", "Create connection"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public boolean canCreate(ICreateConnectionContext context) {
		// return true if both anchors belong to a EClass
		// and those Systems are not identical
		System source = getSystem(context.getSourceAnchor());
		System target = getSystem(context.getTargetAnchor());
		if (source != null && target != null && !source.equals(target)) {
			return true;
		}
		return false;
	}

	public boolean canStartConnection(ICreateConnectionContext context) {
		// return true if start anchor belongs to a EClass
		if (getSystem(context.getSourceAnchor()) != null) {
			return true;
		}
		return false;
	}

	public Connection create(ICreateConnectionContext context) {
		Connection newConnection = null;

		// get Systems which should be connected
		System source = getSystem(context.getSourceAnchor());
		System target = getSystem(context.getTargetAnchor());

		if (source != null && target != null) {
			// create new business object
			IStatement stmt = createStatement(source, target);

			// add connection for business object
			AddConnectionContext addContext = new AddConnectionContext(
					context.getSourceAnchor(), context.getTargetAnchor());
			addContext.setNewObject(stmt);
			newConnection = (Connection) getFeatureProvider().addIfPossible(
					addContext);
		}

		return newConnection;
	}

	/**
	 * Returns the System belonging to the anchor, or null if not available.
	 */
	private System getSystem(Anchor anchor) {
		if (anchor != null) {
			Object obj = getBusinessObjectForPictogramElement(anchor
					.getParent());
			if (obj instanceof System) {
				return (System) obj;
			}
		}
		return null;
	}

	/**
	 * Creates a EReference between two Systems.
	 */
	private IStatement createStatement(System source, System target) {
		Statement stmt = new Statement(source, SYSTEMS.PROPERTY_CONNECTEDTO,
				target);
		model.getManager().add(stmt);
		return stmt;
	}

}