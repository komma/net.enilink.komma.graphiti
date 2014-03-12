package net.enilink.komma.graphiti.features.add;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.Statement;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.graphiti.GraphicsAlgorithmRendererFactory;
import net.enilink.komma.graphiti.Styles;
import net.enilink.komma.graphiti.features.create.IURIFactory;
import net.enilink.komma.graphiti.features.util.Queries;
import net.enilink.komma.graphiti.graphical.IGraphitiProvider;
import net.enilink.komma.graphiti.service.IDiagramService;
import net.enilink.komma.graphiti.service.ITypes;
import net.enilink.komma.model.IModel;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.impl.AbstractAddShapeFeature;
import org.eclipse.graphiti.mm.algorithms.AbstractText;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeService;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import com.google.inject.Inject;

public class AddNodeFeature extends AbstractAddShapeFeature {
	@Inject
	IModel model;

	@Inject
	IURIFactory uriFactory;

	@Inject
	Styles styles;

	@Inject
	IGaService gaService;

	@Inject
	IPeService peService;

	@Inject
	ILabelProvider labelProvider;

	@Inject
	ITypes types;

	@Inject
	IDiagramService diagramService;

	@Inject
	IAdapterFactory adapterFactory;

	@Inject
	public AddNodeFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public boolean canAdd(IAddContext context) {
		if (context.getTargetContainer() instanceof Diagram
				|| types.isExpanded(diagramService
						.getRootOrFirstElementWithBO(context
								.getTargetContainer()))) {
			if (context.getNewObject() instanceof IEntity) {
				if (!(context.getNewObject() instanceof IClass)) {
					return getFeatureProvider()
							.getPictogramElementForBusinessObject(
									context.getNewObject()) == null;
				}
				return true;
			}
		}

		return false;
	}

	protected IReference getProperty(IEntity target, IEntity nodeOrClass) {
		// plain
		IProperty[] properties;
		if (nodeOrClass instanceof IClass) {
			properties = model
					.getManager()
					.createQuery(
							Queries.SELECT_APPLICABLE_CHILD_PROPERTIES_IF_OBJECT_IS_TYPE)
					.setParameter("subject", target)
					.setParameter("objectType", nodeOrClass)
					.evaluate(IProperty.class).toList()
					.toArray(new IProperty[0]);
		} else {
			properties = model.getManager()
					.createQuery(Queries.SELECT_APPLICABLE_CHILD_PROPERTIES)
					.setParameter("subject", target)
					.setParameter("object", nodeOrClass)
					.evaluate(IProperty.class).toList()
					.toArray(new IProperty[0]);
		}

		if (properties.length == 0) {
			return null;
		}

		IProperty property = null;
		if (properties.length == 1) {
			property = properties[0];
		} else {
			ElementListSelectionDialog selectionDialog = new ElementListSelectionDialog(
					PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getShell(), labelProvider);
			selectionDialog.setHelpAvailable(false);
			selectionDialog.setElements(properties);
			if (selectionDialog.open() == Window.OK) {
				property = (IProperty) selectionDialog.getFirstResult();
			}
		}

		return property;
	}

	/**
	 * Creates a connection between two entities.
	 */
	private IStatement createStatement(IEntity source, IReference property,
			IEntity target) {
		Statement stmt = new Statement(source, property, target);
		model.getManager().add(stmt);
		return stmt;
	}

	@Override
	public PictogramElement add(IAddContext context) {
		ContainerShape targetContainer = context.getTargetContainer();

		boolean updatePictograms = Boolean.TRUE.equals(context
				.getProperty("update.pictograms"));

		Object bo = null;
		IReference property = null;
		if (!updatePictograms) {
			bo = diagramService.getFirstBusinessObject(targetContainer);

			if (bo instanceof IReference) {
				bo = model.resolve((IReference) bo);
				property = getProperty(model.resolve((IReference) bo),
						(IEntity) context.getNewObject());
				if (property == null) {
					return null;
				}
			}
		}

		IEntity node;
		if (!updatePictograms && context.getNewObject() instanceof IClass) {
			node = model.getManager().createNamed(uriFactory.createURI(),
					(IReference) context.getNewObject());
		} else {
			node = (IEntity) context.getNewObject();
		}

		if (property != null) {
			createStatement((IEntity) bo, property, node);
		}

		// get an IGraphitiNodeFigureProvider from the adapter factory
		IGraphitiProvider graphitiProvider = (IGraphitiProvider) adapterFactory
				.adapt(node, IGraphitiProvider.class);

		// CONTAINER SHAPE WITH ROUNDED RECTANGLE
		final ContainerShape container = peService.createContainerShape(
				targetContainer, true);
		Rectangle invisibleRectangle = gaService
				.createInvisibleRectangle(container);

		// create link and wire it
		link(container, node);

		final ContainerShape nodeShape = peService.createContainerShape(
				container, true);

		// check whether the context has a size (e.g. from a create feature)
		// otherwise define a default size for the shape
		final int width = context.getWidth() <= 0 ? 50 : context.getWidth();
		final int height = context.getHeight() <= 0 ? 50 : context.getHeight();

		gaService.setLocationAndSize(invisibleRectangle, context.getX(),
				context.getY(), width, height);

		{
			GraphicsAlgorithm nodeVis = null;

			// try to get a default node visualization from the graphiti
			// provider
			if (graphitiProvider != null) {
				nodeVis = graphitiProvider.getNodeShape(node, gaService,
						nodeShape, diagramService.getTopLevelDiagram());
			}

			// default node visualization
			if (nodeVis == null) {
				nodeVis = gaService.createRoundedRectangle(nodeShape, 15, 15);

				nodeVis.setStyle(styles.getStyleForNode(null));
			}

			// create and set graphics algorithm
			GraphicsAlgorithm ga = gaService.createPlatformGraphicsAlgorithm(
					nodeVis, GraphicsAlgorithmRendererFactory.NODE_FIGURE);

		}

		// SHAPE WITH TEXT
		{
			// create shape for text
			Shape shape = peService.createShape(container, false);

			// create and set text graphics algorithm
			AbstractText text = gaService.createDefaultMultiText(getDiagram(),
					shape, labelProvider.getText(node));

			text.setStyle(styles.getStyleForNodeText(null));
		}

		peService.createChopboxAnchor(nodeShape);

		layoutPictogramElement(container);

		return container;
	}
}
