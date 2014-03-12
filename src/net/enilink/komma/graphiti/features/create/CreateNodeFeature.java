package net.enilink.komma.graphiti.features.create;

import net.enilink.komma.core.IReference;
import net.enilink.komma.model.IModel;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICreateContext;
import org.eclipse.graphiti.features.impl.AbstractCreateFeature;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class CreateNodeFeature extends AbstractCreateFeature {
	@Inject
	IModel model;

	@Inject
	IURIFactory uriFactory;

	private IReference type;

	/**
	 * Create an instance of the supplied model element
	 * 
	 * @param fp
	 *            the FeatureProvider this feature belongs to
	 * @param name
	 *            the feature's name
	 * @param description
	 *            a more verbose description of the feature
	 */
	@Inject
	public CreateNodeFeature(IFeatureProvider fp, @Assisted IReference type,
			@Assisted("name") String name,
			@Assisted("description") String description) {
		super(fp, name, description);
		this.type = type;
	}

	@Override
	public boolean canCreate(ICreateContext context) {
		return context.getTargetContainer() != null;
	}

	/**
	 * Possibly, this function is called if an instance of this feature is
	 * created....
	 */
	@Override
	public Object[] create(ICreateContext context) {
		Object modelObject = model.getManager().createNamed(
				uriFactory.createURI(), type);

		addGraphicalRepresentation(context, modelObject);

		return new Object[] { modelObject };
	}

}
