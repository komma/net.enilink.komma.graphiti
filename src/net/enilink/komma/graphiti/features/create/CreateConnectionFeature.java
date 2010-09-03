package net.enilink.komma.graphiti.features.create;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICreateConnectionContext;
import org.eclipse.graphiti.features.context.impl.AddConnectionContext;
import org.eclipse.graphiti.features.impl.AbstractCreateConnectionFeature;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.IPeService;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import com.google.inject.Inject;

import net.enilink.vocab.systems.Interface;
import net.enilink.vocab.systems.Station;
import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.graphiti.IDiagramEditorExt;
import net.enilink.komma.graphiti.service.IDiagramService;
import net.enilink.komma.model.IModel;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;
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

	private static final String SELECT_APPLICABLE_CONNECTION_OBJECTS = ISparqlConstants.PREFIX //
			+ "SELECT DISTINCT ?connectionType ?subjectProperty ?objectProperty " //
			+ "WHERE { " //
			+ "?subject rdf:type ?subjectType ." //
			+ "?object rdf:type ?objectType . " //
			+ "{" //
			+ "		?subjectProperty rdfs:domain ?subjectType ." //
			+ "		?subjectProperty rdfs:range ?connectionType ." //
			+ "} UNION {" //
			+ "		?subjectType rdfs:subClassOf ?subjectRestriction ."
			+ "		?subjectRestriction owl:onProperty ?subjectProperty" //
			+ "		{?subjectRestriction owl:allValuesFrom ?connectionType} UNION {?subjectRestriction owl:someValuesFrom ?connectionType}" //
			+ "}" //
			+ "?connectionType rdfs:subClassOf komma:Connection ."
			+ "{" //
			+ "		?objectProperty rdfs:domain ?connectionType ." //
			+ "		?objectProperty rdfs:range ?objectType ." //
			+ "} UNION {" //
			+ "		?connectionType rdfs:subClassOf ?objectRestriction ."
			+ "		?objectRestriction owl:onProperty ?objectProperty" //
			+ "		{?objectRestriction owl:allValuesFrom ?objectType} UNION {?objectRestriction owl:someValuesFrom ?objectType}" //
			+ "}" //

			/*
			 * + "OPTIONAL {" // + "		?subject rdf:type ?someSubjectType . " //
			 * +
			 * "		?someSubjectType rdfs:subClassOf ?otherRestr . ?otherRestr owl:onProperty ?property; owl:allValuesFrom ?someRange . "
			 * // +
			 * "		?someRange owl:complementOf ?complementClass . ?object rdf:type [rdfs:subClassOf ?complementClass] . "
			 * // +
			 * "		FILTER (?restriction != ?otherRestr && ?someSubjectType != ?otherRestr)"
			 * // + "}"
			 */

			+ "FILTER (?subjectType != ?subjectRestriction && ?objectType != ?objectRestriction)"
			// + "?property rdfs:subPropertyOf komma:relatedTo ." //
			+ "OPTIONAL {" //
			+ "	?otherSubjectProperty rdfs:subPropertyOf ?subjectProperty ." //
			+ "	?otherObjectProperty rdfs:subPropertyOf ?objectProperty ." //
			+ "	{" //
			+ "		?otherSubjectProperty rdfs:domain ?subjectType ." //
			+ "		?otherSubjectProperty rdfs:range ?connectionType ." //
			+ "	} UNION {" //
			+ "		?subjectType rdfs:subClassOf ?otherSubjectRestriction ."
			+ "		?otherSubjectRestriction owl:onProperty ?otherSubjectProperty ." //
			+ "		{?otherSubjectRestriction owl:allValuesFrom ?connectionType} UNION {?otherSubjectRestriction owl:someValuesFrom ?connectionType}"
			+ "	}" //
			+ "	{" //
			+ "		?otherObjectProperty rdfs:domain ?connectionType ." //
			+ "		?otherObjectProperty rdfs:range ?objectType ." //
			+ "	} UNION {" //
			+ "		?connectionType rdfs:subClassOf ?otherObjectRestriction ."
			+ "		?otherObjectRestriction owl:onProperty ?otherObjectProperty ." //
			+ "		{?otherObjectRestriction owl:allValuesFrom ?objectType} UNION {?otherObjectRestriction owl:someValuesFrom ?objectType}"
			+ "	}" //
			+ "	FILTER (?subjectProperty != ?otherSubjectProperty && ?objectProperty != ?otherObjectProperty)" //
			+ "}" //
			+ "FILTER (! bound(?otherSubjectProperty) && ! bound(?otherObjectProperty))" //

			+ "} ORDER BY ?connectionType";

	@Inject
	IURIFactory uriFactory;
	
	@Inject
	IAdapterFactory adapterFactory;

	@Inject
	IModel model;

	@Inject
	IDiagramEditorExt editor;

	@Inject
	IDiagramService diagramService;
	
	@Inject
	IPeService peService;

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
		
		// these are used to allow connections only between items in the same container, e.g.
		// connecting an instance contained in another item to a top level item is forbidden
		ContainerShape sContainer = null,tContainer = null;
		PictogramElement sPE = context.getSourcePictogramElement(),tPE = context.getTargetPictogramElement();
		
		if(sPE != null){
			Object bo = getBusinessObjectForPictogramElement(sPE);
			
			if(sPE instanceof Shape){
				sContainer = ((Shape)sPE).getContainer();
				
				// this is needed to handle connectors properly
				if(bo instanceof Interface)
					sContainer = sContainer.getContainer();
			}
		}

		if(tPE != null){
			Object bo = getBusinessObjectForPictogramElement(tPE);
			
			if(tPE instanceof Shape){
				tContainer = ((Shape)tPE).getContainer();
				
				if(bo instanceof Interface)
					tContainer = tContainer.getContainer();
			}
		}

		// to prevent null pointer exceptions
		if((null == sContainer) || (null == tContainer))
			return false;
		
		if (source != null && target != null && !source.equals(target) && sContainer.equals(tContainer)) {
			// check whether target is composed of other components
			Collection<Diagram> tDiagrams = getLinkedDiagrams(context.getTargetPictogramElement());
			boolean retVal = true;
			
			if(!tDiagrams.isEmpty()){
				Diagram diq = (tDiagrams.toArray(new Diagram[0]))[0];

				retVal = (diq.getChildren().size() == 0);
			}
				
			return retVal;
		}
		return false;
	}

	public boolean canStartConnection(ICreateConnectionContext context) {
		// return true if start anchor belongs to a EClass
		IEntity source = getEntity(context.getSourceAnchor());
		if (source != null) {
			boolean retVal = true;
			
			Collection<Diagram> diagrams = getLinkedDiagrams(context.getSourcePictogramElement());
			
			if(!diagrams.isEmpty()){
				Diagram diq = (diagrams.toArray(new Diagram[0]))[0];
			
				retVal = (diq.getChildren().size() == 0);
			}
						
			return retVal;
		}
		/*else{
			Object bo = getBusinessObjectForPictogramElement(context.getSourcePictogramElement());
			if(bo instanceof Interface){
				getEntity(context.getSourceAnchor());
				return true;
			}
		}*/
		return false;
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

			// query for connection objects between source and target

			IExtendedIterator<?> connClassAndProps = source.getKommaManager()
					.createQuery(SELECT_APPLICABLE_CONNECTION_OBJECTS)
					.setParameter("subject", source)
					.setParameter("object", target).evaluate();

			List<ConnectionContainer> connections = new ArrayList<ConnectionContainer>();
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
							new AdapterFactoryLabelProvider(adapterFactory));
					selectionDialog.setHelpAvailable(false);
					selectionDialog.setElements(connections.toArray(new ConnectionContainer[0]));
					if (selectionDialog.open() == Window.OK) {
						selection = (ConnectionContainer) selectionDialog.getFirstResult();
					}
				}

				if (selection == null) {
					return null;
				}
				
				// create new business object
				IEntity connObject = model.getManager().createNamed(uriFactory.createURI(), selection.getConcept());
				IStatement stmtLeft = createStatement(source, selection.getSourceProperty(), connObject);
				IStatement stmtRight = createStatement(connObject, selection.getTargetProperty(), target);

				// add connection for business object
				AddConnectionContext addContext = new AddConnectionContext(
						context.getSourceAnchor(), context.getTargetAnchor());
				addContext.setNewObject(connObject);
				newConnection = (Connection) getFeatureProvider()
						.addIfPossible(addContext);
				
			} else {
				// plain
				IProperty[] properties = source.getKommaManager()
						.createQuery(SELECT_APPLICABLE_CONNECTION_PROPERTIES)
						.setParameter("subject", source)
						.setParameter("object", target)
						.evaluate(IProperty.class).toList()
						.toArray(new IProperty[0]);

				if (properties.length == 0) {
					return null;
				}

				IProperty property = null;
				if (properties.length == 1) {
					property = properties[0];
				} else {
					ElementListSelectionDialog selectionDialog = new ElementListSelectionDialog(
							PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow().getShell(),
							new AdapterFactoryLabelProvider(adapterFactory));
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
	
	protected Collection<Diagram> getLinkedDiagrams(PictogramElement pe) {
		final Collection<Diagram> diagrams = new HashSet<Diagram>();
		
		// to get rid of some exceptions.
		if(pe instanceof Shape){
			/*Anchor ac = (Anchor)pe;
			
			if(ac.getParent() != getDiagram())*/
			if(((Shape)pe).getContainer() != getDiagram())
				return diagrams;
		}

		final Object[] businessObjectsForPictogramElement = getAllBusinessObjectsForPictogramElement(pe);
		URI firstUri = null;
		for (Object bo : businessObjectsForPictogramElement) {
			if (bo instanceof IReference) {
				URI uri = ((IReference) bo).getURI();
				if (uri != null) {
					firstUri = uri;
				}
			}
		}
		if (firstUri != null) {
			String diagramId = "diagram_" + firstUri.fragment();

			EObject linkedDiagram = null;
			for (TreeIterator<EObject> i = EcoreUtil.getAllProperContents(
					getDiagram().eResource().getContents(), false); i.hasNext();) {
				EObject eObject = i.next();
				if (eObject instanceof Diagram) {
					if (diagramId.equals(((Diagram) eObject).getName())) {
						linkedDiagram = eObject;
						break;
					}
				}
			}

			if (!(linkedDiagram instanceof Diagram)) {
				Diagram newDiagram = peService.createDiagram(getDiagram()
						.getDiagramTypeId(), diagramId, getDiagram()
						.isSnapToGrid());
				getDiagram().eResource().getContents().add(newDiagram);

				linkedDiagram = newDiagram;
			}

			if (!EcoreUtil.equals(getDiagram(), linkedDiagram)) {
				diagrams.add((Diagram) linkedDiagram);
			}
		}

		return diagrams;
	}

}