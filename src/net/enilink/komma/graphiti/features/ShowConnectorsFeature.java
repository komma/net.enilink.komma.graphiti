package net.enilink.komma.graphiti.features;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.custom.AbstractCustomFeature;
import org.eclipse.graphiti.mm.algorithms.Ellipse;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeService;

import com.google.inject.Inject;

import net.enilink.vocab.systems.Interface;
import net.enilink.vocab.systems.SYSTEMS;
import net.enilink.vocab.systems.TestConnector;
import net.enilink.komma.graphiti.Styles;
import net.enilink.komma.graphiti.features.create.IURIFactory;
import net.enilink.komma.model.IModel;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;

public class ShowConnectorsFeature extends AbstractCustomFeature {

	@Inject
	IPeService peService;
	
	@Inject
	IGaService gaService;
	
	@Inject
	Styles styles;
	
	@Inject
	IModel model;
	
	@Inject
	IURIFactory uriFactory;
	
	@Inject
	public ShowConnectorsFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public void execute(ICustomContext context) {
		PictogramElement pe = context.getPictogramElements()[0];
		Collection<Diagram> diagrams = getLinkedDiagrams(pe);
		//Map<>
		ContainerShape cs;
		if(pe instanceof ContainerShape){
			cs = (ContainerShape)pe;
		}
		else
			return;
		
		EList<Shape> csChildren = cs.getChildren();
		List<Shape> connectorShapes = new LinkedList<Shape>();
		Map<IReference,Shape> currentConnectors = new HashMap<IReference,Shape>();
		
		// first we determine all connectors we already have
		for(Shape s: csChildren){
			Object bo = getBusinessObjectForPictogramElement(s);
			
			if(bo instanceof Interface){
				connectorShapes.add(s);
				currentConnectors.put((IReference)((Interface)bo).getSystemsConnectedFrom(), s);
			}
		}

		/*EList<Shape> children = null;
		
		if(pe instanceof ContainerShape){
			children = ((ContainerShape) pe).getChildren();
		}*/
		
		// first, we need all model elements which are contained in the subordered diagram
		if(diagrams.size() < 1)
			return;// there is no subordered diagram.
		
		// take the first diagram for testing first
		Diagram diq = diagrams.toArray(new Diagram[0])[0];
		
		//int refCnt = 0;
		
		/*Object o = getBusinessObjectForPictogramElement(pe);
		
		if(!(o instanceof IObject))
			return;// doesn't work...
		
		IObject bobject = (IObject)o;
		Set<IObject> containedObjects = bobject.getContents();*/
		
		List<Shape> toLayout = new LinkedList<Shape>();
		
		// we want all direct children's business objects
		for(Shape s: diq.getChildren()){
			Object bo = getBusinessObjectForPictogramElement(s);
			
			if(bo instanceof IReference){
				// simply count first...
				//refCnt++;
				//PictogramElement boPe = getFeatureProvider().getPictogramElementForBusinessObject(bo);
				Shape connShape = currentConnectors.get(bo);
				
				if(connShape == null){//boPe == null){
					// we currently have no connector for this item, so we create one
					Shape newShape = peService.createShape(cs,true);
					
					Ellipse newElli = gaService.createEllipse(newShape);
					newElli.setStyle(styles.getStyleForToggle(getDiagram()));
					
					TestConnector connector = (TestConnector)model.getManager().createNamed(uriFactory.createURI(), SYSTEMS.TYPE_TESTCONNECTOR);
					connector.setSystemsConnectedFrom(bo);
					peService.createChopboxAnchor(newShape);
					
					// link the newly created shape with it's bo
					link(newShape,connector);
					//boPe = newShape;
					connShape = newShape;
				}
				else
					connectorShapes.remove(connShape);
				
				toLayout.add((Shape)connShape);//boPe);
			}
		}
		
		// everything that remains in the list connectorShapes does not have an assigned instance in the
		// referenced diagram any more. Delete it.
		for(Shape s: connectorShapes){
			peService.deletePictogramElement(s);
		}
		
		// now we got all shapes together and need to layout them
		int currY = 5;
		
		for(Shape s: toLayout){
			gaService.setLocationAndSize(s.getGraphicsAlgorithm(), 5, currY, 10, 10);
			currY += 15;
		}
		//System.out.print(refCnt + " subordered model elements were found\n");
	}
	
	@Override
	public boolean canExecute(ICustomContext context){
		PictogramElement[] pes = context.getPictogramElements();
		Collection<Diagram> linkedDiagrams = getLinkedDiagrams(pes[0]);
		// first check, if one EClass is selected
		if (pes != null && pes.length == 1) {
			Object bo = getBusinessObjectForPictogramElement(pes[0]);
			if (bo instanceof IReference) {
				// // then forward to super-implementation, which checks if
				// // this EClass is associated with other diagrams
				// return super.canExecute(context);
				//return true;
				return (linkedDiagrams.size() > 0);
			}
		}
		return false;
	}
	
	@Override
	public String getName(){
		return new String("Show connectors");
	}
	
	@Override
	public String getDescription(){
		return new String("Creates connectors for internal components");
	}
	
	protected Collection<Diagram> getLinkedDiagrams(PictogramElement pe) {
		final Collection<Diagram> diagrams = new HashSet<Diagram>();

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
