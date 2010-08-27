package net.enilink.komma.graphiti.features.create;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICreateConnectionContext;
import org.eclipse.graphiti.features.context.impl.AddConnectionContext;
import org.eclipse.graphiti.features.impl.AbstractCreateConnectionFeature;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import com.google.inject.Inject;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.graphiti.IDiagramEditorExt;
import net.enilink.komma.graphiti.service.IDiagramService;
import net.enilink.komma.model.IModel;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.Statement;
import net.enilink.komma.util.ISparqlConstants;

public class CreateConnectionFeature extends AbstractCreateConnectionFeature {
	private static final String SELECT_APPLICABLE_CONNECTION_PROPERTIES = ISparqlConstants.PREFIX //
			+ "SELECT DISTINCT ?property " //
			+ "WHERE { " //
			+ "?subject rdf:type ?subjectType ." //
			+ "?object rdf:type ?objectType . " //
			+ "{" //
			+ "		?property rdfs:domain ?subjectType ." //
			+ "		?property rdfs:range ?objectType ." //
			+ "} UNION {" //
			+ "		?subjectType rdfs:subClassOf ?restriction ."
			+ "		?restriction owl:onProperty ?superProperty . ?property rdfs:subPropertyOf ?superProperty ." //
			+ "		{?restriction owl:allValuesFrom ?objectType} UNION {?restriction owl:someValuesFrom ?objectType}" //
			+ "}" //
			+ "OPTIONAL {" //
			+ "		?subject rdf:type ?someSubjectType . " //
			+ "		?someSubjectType rdfs:subClassOf ?otherRestr . ?otherRestr owl:onProperty ?property; owl:allValuesFrom ?someRange . " //
			+ "		?someRange owl:complementOf ?complementClass . ?object rdf:type [rdfs:subClassOf ?complementClass] . "
			+ "		FILTER (?restriction != ?otherRestr && ?someSubjectType != ?otherRestr)"
			+ "}"
			+ "FILTER (?subjectType != ?restriction && ! bound(?complementClass))"
			+ "OPTIONAL {" //
			+ "	?otherProperty rdfs:subPropertyOf ?property ." //
			+ "	{" //
			+ "		?otherProperty rdfs:domain ?subjectType ." //
			+ "		?otherProperty rdfs:range ?objectType ." //
			+ "	} UNION {" //
			+ "		?subjectType rdfs:subClassOf ?otherRestriction ."
			+ "		?otherRestriction owl:onProperty ?otherProperty ." //
			+ "		{?otherRestriction owl:allValuesFrom ?objectType} UNION {?otherRestriction owl:someValuesFrom ?objectType}" //
			+ "	}" //
			+ "	FILTER (?property != ?otherProperty)" //
			+ "}" //
			+ "FILTER (! bound(?otherProperty))" //
			+ "} ORDER BY ?property";

	@Inject
	IAdapterFactory adapterFactory;

	@Inject
	IModel model;

	@Inject
	IDiagramEditorExt editor;

	@Inject
	IDiagramService diagramService;

	@Inject
	public CreateConnectionFeature(IFeatureProvider fp) {
		// provide name and description for the UI, e.g. the palette
		super(fp, "Connection", "Create connection"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public boolean canCreate(ICreateConnectionContext context) {
		// return true if both anchors belong to a EClass
		// and those Systems are not identical
		IEntity source = getEntity(context.getSourceAnchor());
		IEntity target = getEntity(context.getTargetAnchor());
		if (source != null && target != null && !source.equals(target)) {
			return true;
		}
		return false;
	}

	public boolean canStartConnection(ICreateConnectionContext context) {
		// return true if start anchor belongs to a EClass
		if (getEntity(context.getSourceAnchor()) != null) {
			return true;
		}
		return false;
	}

	public Connection create(ICreateConnectionContext context) {
		Connection newConnection = null;

		// get Systems which should be connected
		IEntity source = getEntity(context.getSourceAnchor());
		IEntity target = getEntity(context.getTargetAnchor());

		if (source != null && target != null) {
			IProperty[] properties = source.getKommaManager()
					.createQuery(SELECT_APPLICABLE_CONNECTION_PROPERTIES)
					.setParameter("subject", source)
					.setParameter("object", target).evaluate(IProperty.class)
					.toList().toArray(new IProperty[0]);

			if (properties.length == 0) {
				return null;
			}

			IProperty property = null;
			if (properties.length == 1) {
				property = properties[0];
			} else {
				ElementListSelectionDialog selectionDialog = new ElementListSelectionDialog(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow()
								.getShell(), new AdapterFactoryLabelProvider(
								adapterFactory));
				selectionDialog.setHelpAvailable(false);
				selectionDialog.setElements(properties);
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
			newConnection = (Connection) getFeatureProvider().addIfPossible(
					addContext);
		}

		return newConnection;
	}

	/**
	 * Returns the entity belonging to the anchor, or null if not available.
	 */
	private IEntity getEntity(Anchor anchor) {
		if (anchor != null) {
			Object obj = diagramService.getRootBusinessObject(anchor
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