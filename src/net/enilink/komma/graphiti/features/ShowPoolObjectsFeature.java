package net.enilink.komma.graphiti.features;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.context.impl.AddConnectionContext;
import org.eclipse.graphiti.features.context.impl.AddContext;
import org.eclipse.graphiti.features.custom.AbstractCustomFeature;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.RoundedRectangle;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeService;
import org.eclipse.graphiti.util.ColorConstant;

import com.google.inject.Inject;

import net.enilink.vocab.owl.Thing;
import net.enilink.vocab.systems.Handling;
import net.enilink.vocab.systems.Sink;
import net.enilink.vocab.systems.Source;
import net.enilink.vocab.systems.Station;
import net.enilink.komma.graphiti.Styles;
import net.enilink.komma.graphiti.features.create.IURIFactory;
import net.enilink.komma.model.IModel;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;

public class ShowPoolObjectsFeature extends AbstractCustomFeature {
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
	public ShowPoolObjectsFeature(IFeatureProvider fp) {
		super(fp);
	}
	
	@Override
	public String getName(){
		return new String("Show pooled objects");
	}
	
	@Override
	public String getDescription(){
		return new String("Show the contained element structure");
	}
	
	@Override
	public boolean canExecute(ICustomContext context){
		PictogramElement[] pes = context.getPictogramElements();
		
		if((pes == null) || (pes.length > 1))
			return false;
		
		PictogramElement pe = pes[0];
		
		Object bo = getBusinessObjectForPictogramElement(pe);
		
		if(!(pe instanceof ContainerShape))
			return false;// we need a shape representation to check the parent
		
		ContainerShape cs = (ContainerShape)pe;
		
		if(!cs.getContainer().equals(getDiagram()))
			return false;// only allow this feature for first level instances
		
		if(bo instanceof Station)//IReference)
			return true;// possibly we might want to check whether this item has a subordered diagram?
		
		return false;
	}

	@Override
	public void execute(ICustomContext context) {
		PictogramElement pe = context.getPictogramElements()[0];
		
		Collection<Diagram> diagrams = getLinkedDiagrams(pe);
		
		if((diagrams.size() < 1) || !(pe instanceof ContainerShape))
			return;
		
		Diagram diq = diagrams.toArray(new Diagram[0])[0];
		
		diq = EcoreUtil.copy(diq);
		
		getDiagram().getConnections().addAll(diq.getConnections());
		
		// create a new shape which will replace the current
		ContainerShape oldCS,newCS;
		GraphicsAlgorithm oldGA;
		
		oldCS = (ContainerShape)pe;
		oldGA = oldCS.getGraphicsAlgorithm();
		
		// new cs has the same container as the old one
		newCS = peService.createContainerShape(oldCS.getContainer(), true);
		
		int width = oldGA.getWidth();
		int height = oldGA.getHeight();
		int x = oldGA.getX(),y = oldGA.getY();
		
		// reduce old cs size
		//oldGA.
		//oldGA.setWidth(width >> 1);
		//oldGA.setHeight(height >> 1);
		gaService.setLocationAndSize(oldGA, 5, 5, width >> 1, height >> 1);
		
		width <<= 1;
		height <<= 1;
		
		// position in upper left corner
		//oldGA.setX(5);
		//oldGA.setY(5);
		
		RoundedRectangle rr = gaService.createRoundedRectangle(newCS, 10, 10);
		gaService.setLocationAndSize(rr, x, y, width, height);
		rr.setBackground(gaService.manageColor(getDiagram(), new ColorConstant(255,255,255)));
		
		// put old cs into new one
		oldCS.setContainer(newCS);
		
		int shiftX = Integer.MAX_VALUE,shiftY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE,maxY = Integer.MIN_VALUE;
		int currX,currY,nChildren = 0;
		
		// determine the number of pixels to shift all elements, i.e. the smallest x and y coordinates of the contained elements
		// also determine maximum values
		for(Shape s: diq.getChildren()){
			GraphicsAlgorithm ga = s.getGraphicsAlgorithm();
			currX = ga.getX();
			currY = ga.getY();
			
			if(shiftX > currX)
				shiftX = currX;
			if(shiftY > currY)
				shiftY = currY;
			
			if((currX + ga.getWidth()) > maxX)
				maxX = currX + ga.getWidth();
			if((currX + ga.getHeight()) > maxY)
				maxY = currY + ga.getHeight();
			
			nChildren++;
		}
		
		// now move all shapes
		//shiftX = 5 - shiftX;
		//shiftY = 70 - shiftY;
		maxX -= shiftX;
		maxY -= shiftY;
		
		float fXAspect = (float)(width - 10) / (float)maxX;
		float fYAspect = (float)(height - 75) / (float)maxY;
		
		for(Shape s: diq.getChildren()){
			GraphicsAlgorithm ga = s.getGraphicsAlgorithm();
			
			// position child elements
			ga.setX((int)((ga.getX() - shiftX) * fXAspect) + 5);
			ga.setY((int)((ga.getY() - shiftY) * fYAspect) + 70);
		}
		
		newCS.getChildren().addAll(diq.getChildren());
		
		// now try and add all elements contained in the referenced diagram
		/*for(Shape s: diq.getChildren()){
			Object bo = getBusinessObjectForPictogramElement(s);
			
			if(bo instanceof IReference){
				AddContext add = new AddContext();
				add.setSize(50, 40);
				add.setLocation(currX, height - 50);
				currX += 65;
				add.setTargetContainer(newCS);
				
				Thing t = (Thing)bo;
				IReference ref = (IReference)bo;
				
				IReference newItem = null;
				
				if(bo instanceof Source)
					newItem = model.getManager().createNamed(uriFactory.createURI(), Source.class);
				if(bo instanceof Sink)
					newItem = model.getManager().createNamed(uriFactory.createURI(), Sink.class);
				if(bo instanceof Station)
					newItem = model.getManager().createNamed(uriFactory.createURI(), Station.class);
				if(bo instanceof Handling)
					newItem = model.getManager().createNamed(uriFactory.createURI(), Handling.class);
				
				add.setNewObject(newItem);
				Set<Thing> newSame,oldSame;
				
				oldSame = t.getOwlSameAs();
				newSame = ((Thing)newItem).getOwlSameAs();
				
				oldSame.add((Thing)newItem);
				newSame.add(t);
				
				t.setOwlSameAs(oldSame);
				((Thing)newItem).setOwlSameAs(newSame);
				
				//getFeatureProvider().addIfPossible(add);
				IAddFeature af = getFeatureProvider().getAddFeature(add);
				af.add(add);
			}*/
			
			// now try to add connections
			/*for(Connection c: diq.getConnections()){
				Object boStart = getBusinessObjectForPictogramElement(c.getStart());
				Object boEnd = getBusinessObjectForPictogramElement(c.getEnd());
				
				// we need to get the business objects of the copies we made
				// these are stored as sameAs in the bo's we currently got
				Object start = getBusinessObjectInThisDiagram((Thing)boStart);
				Object end = getBusinessObjectInThisDiagram((Thing)boEnd);
				PictogramElement peStart,peEnd;
				peStart = getFeatureProvider().getPictogramElementForBusinessObject(start);
				peEnd = getFeatureProvider().getPictogramElementForBusinessObject(end);
				
				AddConnectionContext add = new AddConnectionContext((Anchor)peStart,(Anchor)peEnd);
			}
		}*/
		
		peService.createChopboxAnchor(newCS);
		layoutPictogramElement(newCS);
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
	
	protected Object getBusinessObjectInThisDiagram(Thing t){
		Object result = null;
		
		for(Thing other: t.getOwlSameAs()){
			PictogramElement pe = getFeatureProvider().getPictogramElementForBusinessObject(other);
			ContainerShape cs = (ContainerShape)pe;
			
			do{
				cs = cs.getContainer();
			}while(!(cs instanceof Diagram));
			
			if(getDiagram().equals(cs)){
				result = other;
				break;
			}
		}
		
		return result;
	}

}
