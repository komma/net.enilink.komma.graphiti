package de.fhg.iwu.komma.graphiti;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICreateConnectionFeature;
import org.eclipse.graphiti.features.ICreateFeature;
import org.eclipse.graphiti.features.IDirectEditingFeature;
import org.eclipse.graphiti.features.IMoveShapeFeature;
import org.eclipse.graphiti.features.IRemoveFeature;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.context.IDirectEditingContext;
import org.eclipse.graphiti.features.context.IMoveShapeContext;
import org.eclipse.graphiti.features.context.IRemoveContext;
import org.eclipse.graphiti.features.custom.ICustomFeature;
import org.eclipse.graphiti.features.impl.IIndependenceSolver;
import org.eclipse.graphiti.ui.features.DefaultFeatureProvider;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.fhg.iwu.komma.graphiti.features.DirectEditingFeature;
import de.fhg.iwu.komma.graphiti.features.RemoveFeature;
import de.fhg.iwu.komma.graphiti.features.TestGreatFeature;
import de.fhg.iwu.komma.graphiti.features.add.AddConnectionFeature;
import de.fhg.iwu.komma.graphiti.features.add.AddNodeFeature;
import de.fhg.iwu.komma.graphiti.features.create.CreateConnectionFeature;
import de.fhg.iwu.komma.graphiti.features.create.CreateNodeFeatureFactory;
import net.enilink.vocab.systems.SYSTEMS;
import net.enilink.vocab.systems.System;
import net.enilink.komma.model.IModel;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

public class SystemFeatureProvider extends DefaultFeatureProvider {
	@Inject
	Injector injector;

	@Inject
	CreateNodeFeatureFactory createNodeFeatureFactory;

	@Inject
	IModel model;

	@Inject
	public SystemFeatureProvider(IDiagramTypeProvider dtp) {
		super(dtp);

		setIndependenceSolver(new IIndependenceSolver() {
			@Override
			public String getKeyForBusinessObject(Object bo) {
				if (bo instanceof IStatement) {
					IStatement stmt = (IStatement) bo;
					return "[" + getKeyForBusinessObject(stmt.getSubject())
							+ ","
							+ getKeyForBusinessObject(stmt.getPredicate())
							+ "," + getKeyForBusinessObject(stmt.getObject())
							+ "]";
				} else if (bo instanceof IReference) {
					URI uri = ((IReference) bo).getURI();
					if (uri != null) {
						return uri.toString();
					}
				}
				throw new IllegalArgumentException(
						"Key requested for unnamed object.");
			}

			@Override
			public Object getBusinessObjectForKey(String key) {
				if (key.startsWith("[")) {
					Matcher matcher = Pattern.compile("\\[(.*),(.*),(.*)\\]")
							.matcher(key);
					if (matcher.matches()) {
						return new Statement(
								URIImpl.createURI(matcher.group(1)),
								URIImpl.createURI(matcher.group(2)),
								URIImpl.createURI(matcher.group(3)));
					}
					return null;
				}
				try {
					URI uri = URIImpl.createURI(key);
					return model.resolve(uri);
				} catch (Exception e) {
					return null;
				}
			}
		});
	}

	/**
	 * This function is used to add object creation features. Every object the
	 * plugin is intended to draw must be added here.
	 */
	@Override
	public ICreateFeature[] getCreateFeatures() {
		List<ICreateFeature> features = new ArrayList<ICreateFeature>();

		features.add(createNodeFeatureFactory.create(SYSTEMS.TYPE_STATION,
				"Station", "a processing station"));
		features.add(createNodeFeatureFactory.create(SYSTEMS.TYPE_SOURCE,
				"Source", "a source for a processing flow"));
		features.add(createNodeFeatureFactory.create(SYSTEMS.TYPE_SINK, "Sink",
				"a sink, the end point of a data flow"));
		features.add(createNodeFeatureFactory.create(SYSTEMS.TYPE_HANDLING,
				"Transport", "a transportation mechanism"));

		return features.toArray(new ICreateFeature[features.size()]);
	}

	@Override
	public ICreateConnectionFeature[] getCreateConnectionFeatures() {
		return new ICreateConnectionFeature[] { injector
				.getInstance(CreateConnectionFeature.class) };
	}

	/**
	 * This function is called each time a newly created PictogramElement
	 * (created by a call to one of the add features) shall be added to the
	 * diagram. This allows to handle different kinds of objects in different
	 * ways. Here, it is used to handle the IDirectedFlowObject instances which
	 * provide powerful possibilities for creating connections. With their help,
	 * connections can be allowed or forbidden with editor mechanisms.
	 */
	@Override
	public IAddFeature getAddFeature(IAddContext context) {
		Object newObject = context.getNewObject();
		if (newObject instanceof System) {
			return injector.getInstance(AddNodeFeature.class);
		}
		if (newObject instanceof IStatement) {
			return injector.getInstance(AddConnectionFeature.class);
		}
		return super.getAddFeature(context);
	}

	@Override
	public IMoveShapeFeature getMoveShapeFeature(IMoveShapeContext context) {
		Object bo = getBusinessObjectForPictogramElement(context.getShape());

		// if (bo instanceof IDirectedFlowConnector)
		// return new TestMoveConnectorFeature(this);

		return super.getMoveShapeFeature(context);
	}

	@Override
	public IRemoveFeature getRemoveFeature(IRemoveContext context) {
		return injector.getInstance(RemoveFeature.class);
	}

	@Override
	public IDirectEditingFeature getDirectEditingFeature(
			IDirectEditingContext context) {
		Object bo = getBusinessObjectForPictogramElement(context
				.getPictogramElement());

		// for directed flow objects, we want the direct editing feature
		if (bo instanceof System) {
			return injector.getInstance(DirectEditingFeature.class);
		}

		return super.getDirectEditingFeature(context);
	}

	@Override
	public ICustomFeature[] getCustomFeatures(ICustomContext context) {
		return new ICustomFeature[] { injector
				.getInstance(TestGreatFeature.class) };
	}
}
