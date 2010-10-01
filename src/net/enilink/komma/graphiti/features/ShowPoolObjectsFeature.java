package net.enilink.komma.graphiti.features;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.custom.AbstractCustomFeature;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.algorithms.RoundedRectangle;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeService;
import org.eclipse.graphiti.util.ColorConstant;

import com.google.inject.Inject;

import net.enilink.vocab.systems.Station;
import net.enilink.komma.graphiti.Styles;
import net.enilink.komma.graphiti.features.create.IURIFactory;
import net.enilink.komma.graphiti.service.IDiagramService;
import net.enilink.komma.graphiti.service.ITypes;
import net.enilink.komma.model.IModel;
import net.enilink.komma.core.IReference;

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
	ITypes types;

	@Inject
	IDiagramService diagramService;

	@Inject
	public ShowPoolObjectsFeature(IFeatureProvider fp) {
		super(fp);

		// if (null == connectorConns)
		// connectorConns = new LinkedList<Connection>();
	}

	@Override
	public String getName() {
		return new String("Show pooled objects");
	}

	@Override
	public String getDescription() {
		return new String("Show the contained element structure");
	}

	private final String SHIFTX_TAG = "shiftX";
	private final String SHIFTY_TAG = "shiftY";
	private final String XASPECT_TAG = "xAspect";
	private final String YASPECT_TAG = "yAspect";
	private final String ITEMWIDTH_TAG = "originalWidth";
	private final String ITEMHEIGHT_TAG = "originaHeight";

	// private static PictogramElement currOpen = null;
	// private static int shiftX, shiftY;
	// private static float fXAspect, fYAspect;
	// private static LinkedList<Connection> connectorConns = null;
	// private static EList<Shape> shapeChildren;
	// private static EList<GraphicsAlgorithm> gaChildren;
	// private static ContainerShape storedCS;
	// private static GraphicsAlgorithm storedGA;

	@Override
	public boolean canExecute(ICustomContext context) {
		PictogramElement[] pes = context.getPictogramElements();

		if ((pes == null) || (pes.length > 1))
			return false;

		PictogramElement pe = pes[0];

		Object bo = getBusinessObjectForPictogramElement(pe);

		if (!(pe instanceof ContainerShape))
			return false;// we need a shape representation to check the parent

		ContainerShape cs = (ContainerShape) pe;

		if (!cs.getContainer().equals(getDiagram()))
			return false;// only allow this feature for first level instances

		// this is to allow an open pool to be closed by executing this feature
		// on its container
		if (types.isPooled(pe))// (bo == null) &&
								// cs.getChildren().contains(currOpen))
			return true;

		// only allow expanding station objects
		if (bo instanceof Station)// IReference)
			return true;// possibly we might want to check whether this item has
						// a subordered diagram?

		return false;
	}

	@Override
	public void execute(ICustomContext context) {
		PictogramElement pe = context.getPictogramElements()[0];

		if (!(pe instanceof ContainerShape))
			return;

		// we need to create a new diagram here if there is none
		Collection<Diagram> diagrams = diagramService.getLinkedDiagrams(pe,
				true);

		if (diagrams.size() < 1) {
			// something goofy must have happened...
			return;
		}

		Diagram diq = diagrams.toArray(new Diagram[0])[0];

		// this prevents empty diagrams from being opened
		// if(diq.getChildren().size() < 1)
		// return;

		// close the currently open instance first if appropriate
		// closeCurrentPool();

		if (types.isPooled(pe)) {// pe.equals(currOpen)) {
			// currOpen = null;
			closePool(pe);
			types.removePooled(pe);// must no longer be marked as pooled
			return;// we only close the open instance
		}

		types.markPooled(pe);

		diq = EcoreUtil.copy(diq);

		EList<Connection> conns = diq.getConnections();
		getDiagram().getConnections().addAll(conns);

		// create a new shape which will replace the current
		ContainerShape oldCS;
		GraphicsAlgorithm oldGA;

		oldCS = (ContainerShape) pe;
		oldGA = oldCS.getGraphicsAlgorithm();

		// diq.setContainer(oldCS);

		int width = oldGA.getWidth();
		int height = oldGA.getHeight();
		int oldWidth = width;
		int oldHeight = height;
		int x = oldGA.getX(), y = oldGA.getY();

		// we use the shifts to put the leftmost item to the leftmost side of
		// the box, same applies for the topmost one
		int shiftX = Integer.MAX_VALUE;
		int shiftY = Integer.MAX_VALUE;
		// these values are used to determine the needed box size so that
		// everything could be displayed
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
		int maxW = Integer.MIN_VALUE, maxH = Integer.MIN_VALUE;
		int maxXW = 0, maxYH = 0;
		int currX, currY, currW, currH, nChildren = 0;

		nChildren = diq.getChildren().size();

		// determine the number of pixels to shift all elements, i.e. the
		// smallest x and y coordinates of the contained elements
		// also determine maximum values
		for (Shape s : diq.getChildren()) {
			GraphicsAlgorithm ga = s.getGraphicsAlgorithm();
			currX = ga.getX();
			currY = ga.getY();
			currW = ga.getWidth();
			currH = ga.getHeight();

			if (shiftX > currX)
				shiftX = currX;
			if (shiftY > currY)
				shiftY = currY;
			if (currW > maxW)
				maxW = currW;
			if (currH > maxH)
				maxH = currH;

			if ((currX + currW) > (maxX + maxXW)) {
				maxX = currX;
				maxXW = currW;
			}
			if ((currY + currH) > (maxY + maxYH)) {
				maxY = currY;
				maxYH = currH;
			}
		}

		// if the subdiagram is empty, we need to make sure we have valid values
		// here...
		if (0 == nChildren) {
			// don't put them to the very leftmost corner.
			shiftX = 30;
			shiftY = 30;
			maxX = width * 2;
			maxY = height * 2;
		}

		// now move all shapes
		maxX -= shiftX;
		maxY -= shiftY;

		// now we know how large our box should be to have a one-to-one scale
		// for our pool.
		// we need to decide how large we want it to be.
		int factor = (int) Math.ceil(Math.sqrt(nChildren));
		if (factor <= 1)
			factor = 2;// we always want a minimum factor of one. In fact, this
						// only applies when the subdiagram is empty.
		// width *= factor;
		// height *= factor;

		// connectors get special handling since we want to connect them to
		// their instances
		LinkedList<Shape> connectors = new LinkedList<Shape>();

		for (Shape child : oldCS.getChildren()) {
			if (types.isInterface(child)) {
				// connectors are handled different since they shall be
				// connected to their instances later
				connectors.add(child);
			}
			// all other items are considered to be some kind of decorator and
			// are not updated here.
			// this will hopefully be done by the call to
			// layoutPictogramElement() at the end of this function
		}

		int lMargin = 5, tMargin = 5, rMargin = 5, bMargin = 25;

		if (!connectors.isEmpty()) {
			// leave space for connectors
			lMargin += 15;
			tMargin += 15;
			rMargin += 15;
			bMargin += 15;
		}

		width = maxX + maxXW + lMargin + rMargin;
		height = maxY + maxYH + tMargin + bMargin;

		ContainerShape newCS = peService.createContainerShape(oldCS, false);
		newCS.setVisible(false);

		oldGA.setPictogramElement(newCS);

		types.designatePoolContainer(newCS);

		Rectangle invisibleRectangle = gaService
				.createInvisibleRectangle(oldCS);

		gaService.setLocationAndSize(invisibleRectangle, x, y, width, height);

		RoundedRectangle rr = gaService.createRoundedRectangle(
		/* oldGA */invisibleRectangle, 10, 10);
		rr.setBackground(gaService.manageColor(getDiagram(), new ColorConstant(
				255, 255, 255)));

		// since we don't want arbitrarily large boxes, we scale positions to
		// fit into our created box.
		// float fXAspect = (float) (width - (lMargin + rMargin) - maxXW) /
		// (float) maxX;
		// float fYAspect = (float) (height - (tMargin + bMargin) - maxYH) /
		// (float) maxY;

		// ...and here or no good diagram will result when changes are stored
		// back to the subdiagram.
		/*
		 * if (0 == nChildren) { fXAspect = 1.0f; fYAspect = 1.0f; }
		 */

		for (Shape s : diq.getChildren()) {
			GraphicsAlgorithm ga = s.getGraphicsAlgorithm();

			// position child elements
			ga.setX((int) ((ga.getX() - shiftX)/* * fXAspect */) + lMargin);
			ga.setY((int) ((ga.getY() - shiftY)/* * fYAspect */) + tMargin);
		}

		// add all diagram elements to our new container shape
		oldCS.getChildren().addAll(diq.getChildren());

		// we need to add the children to this diagram first for the connection
		// method to work.
		// otherwise getPictogramElementForBusinessObject() does not work.
		// so now here we create the connections between connectors and the
		// instances which they represent
		for (Shape currShape : connectors) {
			Object boundaryBo = getBusinessObjectForPictogramElement(currShape);

			// connect the connector with its associated diagram element
			for (PictogramElement itemPe : getFeatureProvider()
					.getAllPictogramElementsForBusinessObject(boundaryBo)) {
				if (itemPe instanceof Shape && (!itemPe.equals(currShape))) {
					Connection conn = peService
							.createFreeFormConnection(getDiagram());
					conn.setEnd((currShape.getAnchors().toArray(new Anchor[0]))[0]);
					conn.setStart((((Shape) itemPe).getAnchors()
							.toArray(new Anchor[0]))[0]);
					gaService.createPolyline(conn);
				}
			}
		}

		types.setPoolParameter(pe, SHIFTX_TAG, shiftX);
		types.setPoolParameter(pe, SHIFTY_TAG, shiftY);
		// types.setPoolParameter(pe, XASPECT_TAG, fXAspect);
		// types.setPoolParameter(pe, YASPECT_TAG, fYAspect);
		types.setPoolParameter(pe, ITEMWIDTH_TAG, oldWidth);
		types.setPoolParameter(pe, ITEMHEIGHT_TAG, oldHeight);

		layoutPictogramElement(oldCS);
	}

	protected void closePool(PictogramElement pe) {
		if (pe != null) {
			if (!(pe instanceof ContainerShape))
				return;

			ContainerShape cs = (ContainerShape) pe;
			GraphicsAlgorithm csGa;
			csGa = cs.getGraphicsAlgorithm();

			int w, h, x, y;

			x = cs.getGraphicsAlgorithm().getX();
			y = cs.getGraphicsAlgorithm().getY();
			
			csGa.getGraphicsAlgorithmChildren().clear();

			LinkedList<Shape> toTop = new LinkedList<Shape>();

			for (Shape s : cs.getChildren()) {
				if (types.isPoolContainer(s)) {
					toTop.add(s);
				}
			}

			for (Shape s : toTop) {
				s.getGraphicsAlgorithm().setPictogramElement(pe);
				peService.deletePictogramElement(s);
			}

			w = Integer.parseInt(types.getPoolParameter(pe, ITEMWIDTH_TAG));
			h = Integer.parseInt(types.getPoolParameter(pe, ITEMHEIGHT_TAG));

			csGa = cs.getGraphicsAlgorithm();

			//csGa.setWidth(w);
			//csGa.setHeight(h);
			csGa.setX(x);
			csGa.setY(y);

			Collection<Diagram> diags = diagramService.getLinkedDiagrams(cs,
					false);

			if (diags.size() < 1)
				return;

			Diagram diq = diags.toArray(new Diagram[0])[0];
			boolean hasConnectors = false;

			LinkedList<Shape> items = new LinkedList<Shape>();
			// determine shapes we have to copy back to the subdiagram
			// we have to prevent children which belong to this item from being
			// moved to the subdiagram,
			// this is done by only handling items which are some kind of
			// structure item, i.e. it has some
			// kind of OWL business item, but we also have to prevent the
			// connectors from being moved
			for (Shape currShape : cs.getChildren()) {
				if (isStructureItem(currShape) && !types.isInterface(currShape)) {
					// this seems to be some kind of item which we want to
					// remove from the children list
					items.add(currShape);
				}
				if (types.isInterface(currShape))
					hasConnectors = true;
			}
			// now we remove everything we guess is some kind of item
			cs.getChildren().removeAll(items);

			// here we sync the diagram representing the instance with the
			// contents of the pool
			// copy the children of the container to the diagram instance
			diq.getChildren().clear();
			diq.getChildren().addAll(items);

			HashSet<Connection> allConnections = new HashSet<Connection>();

			String val;
			int shiftX, shiftY;
			// float fXAspect, fYAspect;
			int lMargin = 5, tMargin = 5, rMargin = 5, bMargin = 25;

			if (hasConnectors) {
				lMargin += 15;
				tMargin += 15;
				rMargin += 15;
				bMargin += 15;
			}

			val = types.getPoolParameter(pe, SHIFTX_TAG);
			if (val == null)
				return;// WOW! This should not happen...
			shiftX = Integer.parseInt(val);

			val = types.getPoolParameter(pe, SHIFTY_TAG);
			if (val == null)
				return;// WOW! This should not happen...
			shiftY = Integer.parseInt(val);

			/*
			 * val = types.getPoolParameter(pe, XASPECT_TAG); if (val == null)
			 * return;// WOW! This should not happen... fXAspect =
			 * Float.parseFloat(val);
			 * 
			 * val = types.getPoolParameter(pe, YASPECT_TAG); if (val == null)
			 * return;// WOW! This should not happen... fYAspect =
			 * Float.parseFloat(val);
			 */

			LinkedList<Connection> connectorConns = new LinkedList<Connection>();
			LinkedList<Connection> allConnsToRemove = new LinkedList<Connection>();

			// copy connections to the subordered diagram
			// and remove all connections to connectors
			for (Shape c : diq.getChildren()) {
				for (Anchor a : c.getAnchors()) {
					// filter out connections from/to connectors
					for (Connection conn : a.getIncomingConnections()) {
						if (types.isInterface(conn.getStart().getParent())) {
							connectorConns.add(conn);
							allConnsToRemove.add(conn);
						}
					}
					a.getIncomingConnections().removeAll(connectorConns);
					connectorConns.clear();
					for (Connection conn : a.getOutgoingConnections()) {
						if (types.isInterface(conn.getEnd().getParent())) {
							connectorConns.add(conn);
							allConnsToRemove.add(conn);
						}
					}
					a.getOutgoingConnections().removeAll(connectorConns);
					connectorConns.clear();

					allConnections.addAll(a.getIncomingConnections());
					allConnections.addAll(a.getOutgoingConnections());
				}
				// undo the positioning we did to fit the box
				GraphicsAlgorithm ga = c.getGraphicsAlgorithm();
				ga.setX((int) ((ga.getX() - lMargin)/* / fXAspect */) + shiftX);
				ga.setY((int) ((ga.getY() - tMargin)/* / fYAspect */) + shiftY);
				// the diagram will look like it did before
			}

			diq.getConnections().clear();
			diq.getConnections().addAll(allConnections);

			// remove connections to connectors first
			for (Connection c : allConnsToRemove)
				peService.deletePictogramElement(c);

			// remove all connections from this diagram
			getDiagram().getConnections().removeAll(diq.getConnections());

			// using this, we get a neat freshly layouted image, e.g. text
			// labels will have correct layout
			// it seems to force graphiti to do whatever it also does when
			// resizing a pictogram element
			layoutPictogramElement(pe);// cs);
		}
	}

	private boolean isStructureItem(PictogramElement pe) {
		Object bo = getBusinessObjectForPictogramElement(pe);
		boolean retVal = false;

		if (bo != null) {
			if (bo instanceof IReference)
				retVal = true;
		}

		return retVal;
	}

}
