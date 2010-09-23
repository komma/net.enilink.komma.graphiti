package net.enilink.komma.graphiti.features;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.emf.common.util.EList;
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
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.ChopboxAnchor;
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
import net.enilink.vocab.systems.Interface;
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
		
		if(null == connectorConns)
			connectorConns = new LinkedList<Connection>();
	}
	
	@Override
	public String getName(){
		return new String("Show pooled objects");
	}
	
	@Override
	public String getDescription(){
		return new String("Show the contained element structure");
	}
	
	private static PictogramElement currOpen = null;
	private static int shiftX,shiftY;
	private static float fXAspect,fYAspect;
	private static LinkedList<Connection> connectorConns = null;
	
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
		
		// this is to allow an open pool to be closed by executing this feature on its container
		if((bo == null) && cs.getChildren().contains(currOpen))
			return true;
	
		// only allow expanding station objects
		if(bo instanceof Station)//IReference)
			return true;// possibly we might want to check whether this item has a subordered diagram?
		
		return false;
	}

	@Override
	public void execute(ICustomContext context) {
		PictogramElement pe = context.getPictogramElements()[0];
		
		if(!(pe instanceof ContainerShape))
			return;
		
		Collection<Diagram> diagrams = getLinkedDiagrams(pe);
		
		if(diagrams.size() < 1){
			// this closes an open pool when its container is target of this execute
			if(((ContainerShape)pe).getChildren().contains(currOpen))
				closeCurrentPool();
			return;
		}
		
		Diagram diq = diagrams.toArray(new Diagram[0])[0];
		
		// this prevents empty diagrams from being opened
		//if(diq.getChildren().size() < 1)
		//	return;
		
		// close the currently open instance first if appropriate
		closeCurrentPool();
		
		if(((ContainerShape)pe).equals(currOpen))
			return;// we only close the open instance
		
		currOpen = pe;
		
		diq = EcoreUtil.copy(diq);
		
		EList<Connection> conns = diq.getConnections();
		getDiagram().getConnections().addAll(conns);
		
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
		gaService.setLocationAndSize(oldGA, 5, 5, width >> 1, height >> 1);
		
		// put old cs into new one
		oldCS.setContainer(newCS);
		
		// we use the shifts to put the leftmost item to the leftmost side of the box, same applies for the topmost one
		shiftX = Integer.MAX_VALUE;
		shiftY = Integer.MAX_VALUE;
		// these values are used to determine the needed box size so that everything could be displayed
		int maxX = Integer.MIN_VALUE,maxY = Integer.MIN_VALUE;
		int maxW = Integer.MIN_VALUE,maxH = Integer.MIN_VALUE;
		int maxXW = 0,maxYH = 0;
		int currX,currY,currW,currH,nChildren = 0;
		
		nChildren = diq.getChildren().size();
		
		// determine the number of pixels to shift all elements, i.e. the smallest x and y coordinates of the contained elements
		// also determine maximum values
		for(Shape s: diq.getChildren()){
			GraphicsAlgorithm ga = s.getGraphicsAlgorithm();
			currX = ga.getX();
			currY = ga.getY();
			currW = ga.getWidth();
			currH = ga.getHeight();
			
			if(shiftX > currX)
				shiftX = currX;
			if(shiftY > currY)
				shiftY = currY;
			if(currW > maxW)
				maxW = currW;
			if(currH > maxH)
				maxH = currH;
			
			if((currX + currW) > maxX){
				maxX = currX;
				maxXW = currW;
			}
			if((currY + currH) > maxY){
				maxY = currY;
				maxYH = currH;
			}
		}
		
		// if the subdiagram is empty, we need to make sure we have valid values here...
		if(0 == nChildren){
			// don't put them to the very leftmost corner.
			shiftX = 30;
			shiftY = 30;
		}
		
		// now move all shapes
		maxX -= shiftX;
		maxY -= shiftY;
		
		// now we know how large our box should be to have a one-to-one scale for our pool.
		// we need to decide how large we want it to be.
		int factor = (int)Math.ceil(Math.sqrt(nChildren));
		if(factor <= 1)
			factor = 2;// we always want a minimum factor of one. In fact, this only applies when the subdiagram is empty.
		width *= factor;
		height *= factor;
		//width <<= 1;
		//height <<= 1;
		
		RoundedRectangle rr = gaService.createRoundedRectangle(newCS, 10, 10);
		gaService.setLocationAndSize(rr, x, y, width, height);
		rr.setBackground(gaService.manageColor(getDiagram(), new ColorConstant(255,255,255)));
		
		// since we don't want arbitrarily large boxes, we scale positions to fit into our created box.
		/*float */fXAspect = (float)(width - 10 - maxXW) / (float)maxX;
		/*float */fYAspect = (float)(height - 75 - maxYH) / (float)maxY;
		
		// ...and here or no good diagram will result when changes are stored back to the subdiagram.
		if(0 == nChildren){
			fXAspect = 1.0f;
			fYAspect = 1.0f;
		}
		
		for(Shape s: diq.getChildren()){
			GraphicsAlgorithm ga = s.getGraphicsAlgorithm();
			
			// position child elements
			ga.setX((int)((ga.getX() - shiftX) * fXAspect) + 5);
			ga.setY((int)((ga.getY() - shiftY) * fYAspect) + 70);
		}
		
		// add all diagram elements to our new container shape
		newCS.getChildren().addAll(diq.getChildren());
		
		// we need to add the children to this diagram first for the connection method to work.
		// otherwise getPictogramElementForBusinessObject() does not work.
		int currConnY = 5;
		LinkedList<Shape> connectors = new LinkedList<Shape>();
		// if the container shows connectors, we also put them into the new shape
		for(Shape currShape: oldCS.getChildren()){
			Object bo = getBusinessObjectForPictogramElement(currShape);
			
			if(bo instanceof Interface){
				connectors.add(currShape);
			}
		}
		// this is used to prevent concurrent modification of the children list
		// of the container which does not work properly.
		for(Shape currShape: connectors){
			currShape.setContainer(newCS);
			GraphicsAlgorithm ga = currShape.getGraphicsAlgorithm();
			gaService.setLocation(ga, 5, currConnY);
			currConnY += 5 + ga.getHeight();
			
			Interface i = (Interface)getBusinessObjectForPictogramElement(currShape);
			Object sys = i.getSystemsConnectedFrom();
			
			// connect the connector with its associated diagram element
			PictogramElement itemPe = getFeatureProvider().getPictogramElementForBusinessObject(sys);
			
			if(itemPe instanceof Shape){
				Connection conn = peService.createFreeFormConnection(getDiagram());
				conn.setEnd((currShape.getAnchors().toArray(new Anchor[0]))[0]);
				conn.setStart((((Shape)itemPe).getAnchors().toArray(new Anchor[0]))[0]);
				gaService.createPolyline(conn);
				
				// we keepe track of all connections we created so we can easily dump them when closing the pool
				connectorConns.add(conn);
			}
		}
		
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
	
	protected void closeCurrentPool(){
		if(currOpen != null){
			if(!(currOpen instanceof ContainerShape))
				return;
			
			ContainerShape container,cs = (ContainerShape)currOpen;
			container = cs.getContainer();
			GraphicsAlgorithm containerGa,csGa;
			containerGa = container.getGraphicsAlgorithm();
			csGa = cs.getGraphicsAlgorithm();
			
			Collection<Diagram> diags = getLinkedDiagrams(cs);
			
			if(diags.size() < 1)
				return;
			
			Diagram diq = diags.toArray(new Diagram[0])[0];
			
			// set the shape of our instance back on top
			cs.setContainer(container.getContainer());
			
			int currConnY = 5;
			LinkedList<Shape> connectors = new LinkedList<Shape>();
			// put connectors back where they belong
			for(Shape currShape: container.getChildren()){
				Object bo = getBusinessObjectForPictogramElement(currShape);
				
				if(bo instanceof Interface){
					connectors.add(currShape);
				}
			}
			// same two-step procedure as above to prevent concurrent modifications
			for(Shape currShape: connectors){
				currShape.setContainer(cs);
				GraphicsAlgorithm ga = currShape.getGraphicsAlgorithm();
				gaService.setLocation(ga, 5, currConnY);
				currConnY += 5 + ga.getHeight();
			}
			
			// here we sync the diagram representing the instance with the contents of the pool
			// copy the children of the container to the diagram instance
			diq.getChildren().clear();
			diq.getChildren().addAll(container.getChildren());
			
			HashSet<Connection> allConnections = new HashSet<Connection>();
			
			// copy connections to the subordered diagram
			for(Shape c: diq.getChildren()){//container.getChildren()){
				for(Anchor a: c.getAnchors()){
					allConnections.addAll(a.getIncomingConnections());
					allConnections.addAll(a.getOutgoingConnections());
				}
				// undo the positioning we did to fit the box
				GraphicsAlgorithm ga = c.getGraphicsAlgorithm();
				ga.setX((int)((ga.getX() - 5) / fXAspect) + shiftX);
				ga.setY((int)((ga.getY() - 70) / fYAspect) + shiftY);
				// the diagram will look like it did before
			}
			
			diq.getConnections().clear();
			diq.getConnections().addAll(allConnections);
			
			Collection<Shape> retain = new LinkedList<Shape>();
			retain.add(cs);
			
			// remove all connections from this diagram
			getDiagram().getConnections().removeAll(diq.getConnections());
			//if(!connectorConns.isEmpty())
			//	getDiagram().getConnections().removeAll(connectorConns);
			for(Connection c: connectorConns)
				peService.deletePictogramElement(c);
			
			connectorConns.clear();
			
			int x,y,width,height;
			x = containerGa.getX();
			y = containerGa.getY();
			width = csGa.getWidth() << 1;
			height = csGa.getHeight() << 1;
			
			// set container to its old position
			gaService.setLocationAndSize(csGa, x, y, width, height);
			
			// delete the container
			peService.deletePictogramElement(container);
			
			currOpen = null;
		}
	}

}
