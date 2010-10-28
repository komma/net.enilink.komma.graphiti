package net.enilink.komma.graphiti.features.create;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICreateConnectionContext;
import org.eclipse.graphiti.features.context.impl.AddConnectionContext;
import org.eclipse.graphiti.features.impl.AbstractCreateConnectionFeature;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.IPeService;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import com.google.inject.Inject;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.graphiti.features.util.IQueries;
import net.enilink.komma.graphiti.service.IDiagramService;
import net.enilink.komma.graphiti.service.ITypes;
import net.enilink.komma.model.IModel;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.Statement;

public class CreateConnectionFeature extends AbstractCreateConnectionFeature
		implements IQueries {
	@Inject
	IURIFactory uriFactory;

	@Inject
	IModel model;

	@Inject
	IDiagramService diagramService;

	@Inject
	IPeService peService;

	@Inject
	ITypes types;

	@Inject
	ILabelProvider labelProvider;

	@Inject
	public CreateConnectionFeature(IFeatureProvider fp) {
		// provide name and description for the UI, e.g. the palette
		super(fp, "Connection", "Create connection"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected PictogramElement getContainer(Anchor anchor) {
		PictogramElement pe = (PictogramElement) anchor.eContainer();
		boolean isInterface = false;
		if (types.isInterface(pe)) {
			isInterface = true;
		} else if (types.isExpanded(pe)) {
			// directly use container if supplied pe is the node shape itself
			pe = (PictogramElement) pe.eContainer();
		}
		while (!(pe instanceof Diagram || !isInterface && types.isExpanded(pe))) {
			pe = (PictogramElement) pe.eContainer();
		}
		return pe;
	}

	public boolean canCreate(ICreateConnectionContext context) {
		if (context.getSourceAnchor() == null
				|| context.getTargetAnchor() == null) {
			return false;
		}

		// do not allow direct connections between elements contained in an
		// expanded node and other nodes
		if (!getContainer(context.getSourceAnchor()).equals(
				getContainer(context.getTargetAnchor()))) {
			return false;
		}

		IEntity source = getEntity(context.getSourceAnchor());
		IEntity target = getEntity(context.getTargetAnchor());

		return source != null && target != null && !source.equals(target);
	}

	public boolean canStartConnection(ICreateConnectionContext context) {
		IEntity source = getEntity(context.getSourceAnchor());
		return source != null;
	}

	class ConnectionContainer {
		private IClass concept;
		private IProperty sourceProp;
		private IProperty targetProp;

		public ConnectionContainer(IClass concept, IProperty sourceProp,
				IProperty targetProp) {
			this.concept = concept;
			this.sourceProp = sourceProp;
			this.targetProp = targetProp;
		}

		public String toString() {
			return sourceProp.getURI() + " -> " + concept.getURI() + " -> "
					+ targetProp.getURI();
		}

		public IClass getConcept() {
			return concept;
		}

		public IProperty getSourceProperty() {
			return sourceProp;
		}

		public IProperty getTargetProperty() {
			return targetProp;
		}
	}

	public Connection create(ICreateConnectionContext context) {
		Connection newConnection = null;

		// get Systems which should be connected
		IEntity source = getEntity(context.getSourceAnchor());
		IEntity target = getEntity(context.getTargetAnchor());

		if (source != null && target != null) {
			List<ConnectionContainer> connections = new ArrayList<ConnectionContainer>();
			
			// query for connection objects between source and target
			IExtendedIterator<?> connClassAndProps = source.getKommaManager()
					.createQuery(SELECT_APPLICABLE_CONNECTION_OBJECTS)
					.setParameter("subject", source)
					.setParameter("object", target).evaluate();

			while (connClassAndProps.hasNext()) {
				Object[] results = (Object[]) connClassAndProps.next();
				// expect connection class, source and target properties
				if (results.length == 3)
					connections.add(new ConnectionContainer(
							(IClass) results[0], (IProperty) results[1],
							(IProperty) results[2]));
			}

			// connection objects or plain connections
			if (!connections.isEmpty()) {
				// objects
				ConnectionContainer selection = null;
				if (connections.size() == 1) {
					selection = connections.get(0);
				} else {
					ElementListSelectionDialog selectionDialog = new ElementListSelectionDialog(
							PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow().getShell(),
							labelProvider);
					selectionDialog.setHelpAvailable(false);
					selectionDialog.setElements(connections
							.toArray(new ConnectionContainer[0]));
					if (selectionDialog.open() == Window.OK) {
						selection = (ConnectionContainer) selectionDialog
								.getFirstResult();
					}
				}

				if (selection == null) {
					return null;
				}

				// create new business object
				IEntity connObject = model.getManager().createNamed(
						uriFactory.createURI(), selection.getConcept());
				IStatement stmtLeft = createStatement(source,
						selection.getSourceProperty(), connObject);
				IStatement stmtRight = createStatement(connObject,
						selection.getTargetProperty(), target);

				// add connection for business object
				AddConnectionContext addContext = new AddConnectionContext(
						context.getSourceAnchor(), context.getTargetAnchor());
				addContext.setNewObject(connObject);
				newConnection = (Connection) getFeatureProvider()
						.addIfPossible(addContext);

			} else {
				// plain
				List<IProperty> properties = source.getKommaManager()
						.createQuery(SELECT_APPLICABLE_CONNECTION_PROPERTIES)
						.setParameter("subject", source)
						.setParameter("object", target)
						.evaluate(IProperty.class).toList();

				if (properties.isEmpty()) {
					return null;
				}

				// sort built-in properties after user defined properties
				Collections.sort(properties, IResource.RANK_COMPARATOR);
				// filter built-in properties, if more specialized properties
				// are available
				// TODO make filtering configurable
				int start = 0;
				for (IProperty property : properties) {
					if (property.isOntLanguageTerm()) {
						break;
					}
					start++;
				}
				if (start != 0) {
					properties = properties.subList(0, start);
				}

				IProperty property = null;
				if (properties.size() == 1) {
					property = properties.iterator().next();
				} else {
					ElementListSelectionDialog selectionDialog = new ElementListSelectionDialog(
							PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow().getShell(),
							labelProvider);
					selectionDialog.setHelpAvailable(false);
					selectionDialog.setElements(properties.toArray());
					if (selectionDialog.open() == Window.OK) {
						property = (IProperty) selectionDialog.getFirstResult();
					}
				}

				if (property == null) {
					return null;
				}

				// create new business object
				IStatement stmt = createStatement(source, property, target);

				// add connection for business object
				AddConnectionContext addContext = new AddConnectionContext(
						context.getSourceAnchor(), context.getTargetAnchor());
				addContext.setNewObject(stmt);
				newConnection = (Connection) getFeatureProvider()
						.addIfPossible(addContext);
			}
		}

		return newConnection;
	}

	/**
	 * Returns the entity belonging to the anchor, or null if not available.
	 */
	private IEntity getEntity(Anchor anchor) {
		if (anchor != null) {
			Object obj = diagramService.getFirstBusinessObject(anchor
					.getParent());
			if (obj instanceof IEntity) {
				return (IEntity) obj;
			}
		}
		return null;
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
}