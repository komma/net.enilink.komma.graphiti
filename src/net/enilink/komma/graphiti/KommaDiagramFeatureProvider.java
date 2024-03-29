package net.enilink.komma.graphiti;

import java.util.ArrayList;
import java.util.List;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.graphiti.concepts.Connector;
import net.enilink.komma.graphiti.features.CollapseFeature;
import net.enilink.komma.graphiti.features.DeleteFeature;
import net.enilink.komma.graphiti.features.DirectEditingFeature;
import net.enilink.komma.graphiti.features.DrillDownFeature;
import net.enilink.komma.graphiti.features.ExpandFeature;
import net.enilink.komma.graphiti.features.LayoutNodeFeature;
import net.enilink.komma.graphiti.features.RemoveFeature;
import net.enilink.komma.graphiti.features.ShowConnectorsFeature;
import net.enilink.komma.graphiti.features.UpdateNodeFeature;
import net.enilink.komma.graphiti.features.UpdatePictogramsFeature;
import net.enilink.komma.graphiti.features.add.AddConnectionFeature;
import net.enilink.komma.graphiti.features.add.AddConnectorFeature;
import net.enilink.komma.graphiti.features.add.AddNodeFeature;
import net.enilink.komma.graphiti.features.create.CreateConnectionFeature;
import net.enilink.komma.graphiti.features.create.CreateNodeFeatureFactory;
import net.enilink.komma.graphiti.features.layout.LayoutDiagramFeature;
import net.enilink.komma.graphiti.service.IDiagramService;
import net.enilink.komma.graphiti.service.ITypes;
import net.enilink.komma.model.IModel;
import net.enilink.vocab.visualization.layout.Connection;

import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICreateConnectionFeature;
import org.eclipse.graphiti.features.ICreateFeature;
import org.eclipse.graphiti.features.IDeleteFeature;
import org.eclipse.graphiti.features.IDirectEditingFeature;
import org.eclipse.graphiti.features.IFeature;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.IMoveShapeFeature;
import org.eclipse.graphiti.features.IRemoveFeature;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.context.IDeleteContext;
import org.eclipse.graphiti.features.context.IDirectEditingContext;
import org.eclipse.graphiti.features.context.ILayoutContext;
import org.eclipse.graphiti.features.context.IMoveShapeContext;
import org.eclipse.graphiti.features.context.IPictogramElementContext;
import org.eclipse.graphiti.features.context.IRemoveContext;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.custom.ICustomFeature;
import org.eclipse.graphiti.features.impl.DefaultMoveShapeFeature;
import org.eclipse.graphiti.features.impl.IIndependenceSolver;
import org.eclipse.graphiti.mm.algorithms.AbstractText;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.ui.features.DefaultFeatureProvider;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

public class KommaDiagramFeatureProvider extends DefaultFeatureProvider {
	@Inject
	Injector injector;

	@Inject
	CreateNodeFeatureFactory createNodeFeatureFactory;

	@Inject
	IModel model;

	@Inject
	ITypes typeService;

	@Inject
	Provider<IDiagramService> diagramService;

	@Inject
	public KommaDiagramFeatureProvider(IDiagramTypeProvider dtp,
			IIndependenceSolver independenceSolver) {
		super(dtp);
		setIndependenceSolver(independenceSolver);
	}

	/**
	 * This function is used to add object creation features. Every object the
	 * plugin is intended to draw must be added here.
	 */
	@Override
	public ICreateFeature[] getCreateFeatures() {
		List<ICreateFeature> features = new ArrayList<ICreateFeature>();

		// features.add(createNodeFeatureFactory.create(SYSTEMS.TYPE_STATION,
		// "Station", "a processing station"));
		// features.add(createNodeFeatureFactory.create(SYSTEMS.TYPE_SOURCE,
		// "Source", "a source for a processing flow"));
		// features.add(createNodeFeatureFactory.create(SYSTEMS.TYPE_SINK,
		// "Sink",
		// "a sink, the end point of a data flow"));
		// features.add(createNodeFeatureFactory.create(SYSTEMS.TYPE_HANDLING,
		// "Transport", "a transportation mechanism"));

		return features.toArray(new ICreateFeature[features.size()]);
	}

	@Override
	public ICreateConnectionFeature[] getCreateConnectionFeatures() {
		return new ICreateConnectionFeature[] { injector
				.getInstance(CreateConnectionFeature.class) };
	}

	@Inject
	IAdapterFactory adapterFactory;

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
		IAddFeature addFeature = (IAddFeature) adapterFactory.adapt(newObject,
				IAddFeature.class);
		if (null != addFeature) {
			return addFeature;
		}
		if (newObject instanceof IEntity
				&& !(newObject instanceof IProperty || newObject instanceof Connection)) {
			return injector.getInstance(AddNodeFeature.class);
		}
		if (newObject instanceof IStatement || newObject instanceof Connection) {
			return injector.getInstance(AddConnectionFeature.class);
		}
		if (newObject instanceof Connector) {
			return injector.getInstance(AddConnectorFeature.class);
		}
		return super.getAddFeature(context);
	}

	@Override
	public Object getBusinessObjectForPictogramElement(
			PictogramElement pictogramElement) {
		return diagramService.get().getBusinessObjectForPictogramElement(
				pictogramElement);
	}

	@Override
	public ILayoutFeature getLayoutFeature(ILayoutContext context) {
		PictogramElement pe = context.getPictogramElement();
		Object bo = injector.getInstance(IDiagramService.class)
				.getFirstBusinessObject(pe);
		ILayoutFeature layoutFeature = (ILayoutFeature) adapterFactory.adapt(
				bo, ILayoutFeature.class);
		if (layoutFeature != null) {
			return layoutFeature;
		}
		if (pe instanceof ContainerShape && bo instanceof IResource) {
			return injector.getInstance(LayoutNodeFeature.class);
		}
		return super.getLayoutFeature(context);
	}

	@Override
	public IMoveShapeFeature getMoveShapeFeature(IMoveShapeContext context) {
		if (typeService.isInterface(context.getShape())) {
			return new DefaultMoveShapeFeature(this) {
				@Override
				protected void postMoveShape(IMoveShapeContext context) {
					layoutPictogramElement(injector.getInstance(
							IDiagramService.class).getRootOrFirstElementWithBO(
							context.getShape().getContainer()));
				}
			};
		}

		return super.getMoveShapeFeature(context);
	}

	@Override
	public IDeleteFeature getDeleteFeature(IDeleteContext context) {
		return injector.getInstance(DeleteFeature.class);
	}

	@Override
	public IRemoveFeature getRemoveFeature(IRemoveContext context) {
		return injector.getInstance(RemoveFeature.class);
	}

	@Override
	public IUpdateFeature getUpdateFeature(IUpdateContext context) {
		// update diagram according to layout model
		if (context.getPictogramElement() instanceof Diagram
				|| Boolean.TRUE
						.equals(context.getProperty("update.pictograms"))) {
			return injector.getInstance(UpdatePictogramsFeature.class);
		}

		Object bo = getBusinessObjectForPictogramElement(context
				.getPictogramElement());
		if (bo instanceof IResource) {
			return injector.getInstance(UpdateNodeFeature.class);
		}
		return super.getUpdateFeature(context);
	}

	@Override
	public IFeature[] getDragAndDropFeatures(IPictogramElementContext context) {
		// simply return all create connection features
		return getCreateConnectionFeatures();
	}

	@Override
	public IDirectEditingFeature getDirectEditingFeature(
			IDirectEditingContext context) {
		PictogramElement pe = context.getPictogramElement();
		if (pe.getGraphicsAlgorithm() instanceof AbstractText) {
			while (!(pe instanceof ContainerShape || pe.eContainer() instanceof Diagram)) {
				pe = (PictogramElement) pe.eContainer();
			}

			final Object bo = getBusinessObjectForPictogramElement(pe);

			// for directed flow objects, we want the direct editing feature
			if (bo instanceof IResource) {
				return injector.getInstance(DirectEditingFeature.class);
			}
		}

		return super.getDirectEditingFeature(context);
	}

	@Override
	public ICustomFeature[] getCustomFeatures(ICustomContext context) {
		return new ICustomFeature[] {
				injector.getInstance(DrillDownFeature.class),
				injector.getInstance(ExpandFeature.class),
				injector.getInstance(CollapseFeature.class),
				injector.getInstance(ShowConnectorsFeature.class),
				injector.getInstance(LayoutDiagramFeature.class)
		};
	}
}
