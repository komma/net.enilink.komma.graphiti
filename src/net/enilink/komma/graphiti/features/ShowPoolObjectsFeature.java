package net.enilink.komma.graphiti.features;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
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
import net.enilink.komma.graphiti.service.ITypes;
import net.enilink.komma.model.IModel;
import net.enilink.komma.core.IReference;
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
	ITypes types;

	@Inject
	public ShowPoolObjectsFeature(IFeatureProvider fp) {
		super(fp);

		if (null == connectorConns)
			connectorConns = new LinkedList<Connection>();
	}

	@Override
	public String getName() {
		return new String("Show pooled objects");
	}

	@Override
	public String getDescription() {
		return new String("Show the contained element structure");
	}

	private static PictogramElement currOpen = null;
	private static int shiftX, shiftY;
	private static float fXAspect, fYAspect;
	private static LinkedList<Connection> connectorConns = null;
	// private static EList<Shape> shapeChildren;
	// private static EList<GraphicsAlgorithm> gaChildren;
	// private static ContainerShape storedCS;
	private static GraphicsAlgorithm storedGA;

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
		if ((bo == null) && cs.getChildren().contains(currOpen))
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

		Collection<Diagram> diagrams = getLinkedDiagrams(pe);

		if (diagrams.size() < 1) {
			// something goofy must have happened...
			return;
		}

		Diagram diq = diagrams.toArray(new Diagram[0])[0];

		// this prevents empty diagrams from being opened
		// if(diq.getChildren().size() < 1)
		// return;

		// close the currently open instance first if appropriate
		closeCurrentPool();

		if (pe.equals(currOpen)) {
			currOpen = null;
			return;// we only close the open instance
		}

		currOpen = pe;

		diq = EcoreUtil.copy(diq);

		EList<Connection> conns = diq.getConnections();
		getDiagram().getConnections().addAll(conns);

		// create a new shape which will replace the current
		ContainerShape oldCS;
		GraphicsAlgorithm oldGA;

		oldCS = (ContainerShape) pe;
		oldGA = oldCS.getGraphicsAlgorithm();

		int width = oldGA.getWidth();
		int height = oldGA.getHeight();
		int oldWidth = width;
		int oldHeight = height;
		int x = oldGA.getX(), y = oldGA.getY();

		// we use the shifts to put the leftmost item to the leftmost side of
		// the box, same applies for the topmost one
		shiftX = Integer.MAX_VALUE;
		shiftY = Integer.MAX_VALUE;
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
		width *= factor;
		height *= factor;

		// we store a copy of the current graphics algorithm of the item.
		// this appears to be the easiest way of restoring it when the pool is
		// closed
		storedGA = EcoreUtil.copy(oldGA);

		// connectors get special handling since we want to connect them to
		// their instances
		LinkedList<Shape> connectors = new LinkedList<Shape>();

		for (Shape child : oldCS.getChildren()) {
			if (types.isInterface(pe)) {
				// connectors are handled different since they shall be
				// connected to their instances later
				connectors.add(child);
				// this all doesn't work properly...
				GraphicsAlgorithm childGA = child.getGraphicsAlgorithm();
				// we move the connector such that it is in the same
				// relative position as before
				// fXRatio = (float)(childGA.getX() - 5) / (float)(oldWidth
				// - 10 - 10);
				// fYRatio = (float)(childGA.getY() - 5) / (float)(oldHeight
				// - 30 - 10);
				// connectors are 10 pixels wide and high, so we need to
				// consider this

				// childGA.setX((int)Math.ceil(fXRatio * width) + 5);
				// childGA.setY((int)Math.ceil(fYRatio * (height - 30)) +
				// 5);
				// make sure connectors keeps aligned to the border
				// we only need to cover connectors aligned to the right or
				// bottom border
				if (childGA.getX() == (oldWidth - 15)) {
					childGA.setX(width - 15);
				}
				if (childGA.getY() == (oldHeight - 35)) {
					childGA.setY(height - 15);
				}
			}
			// all other items are considered to be some kind of decorator and
			// are not updated here.
			// this will hopefully be done by the call to
			// layoutPictogramElement() at the end of this function
		}

		// here we create a new graphics algorithm for the shape.
		// it temporarily replaces the old one until we close the pool
		Rectangle invisibleRectangle = gaService
				.createInvisibleRectangle(oldCS);
		gaService.setLocationAndSize(invisibleRectangle, x, y, width,
				height + 20);

		RoundedRectangle rr = gaService.createRoundedRectangle(
				invisibleRectangle, 10, 10);
		rr.setBackground(gaService.manageColor(getDiagram(), new ColorConstant(
				255, 255, 255)));

		// since we don't want arbitrarily large boxes, we scale positions to
		// fit into our created box.
		fXAspect = (float) (width - 10 - maxXW) / (float) maxX;
		fYAspect = (float) (height - 10 - maxYH) / (float) maxY;

		// ...and here or no good diagram will result when changes are stored
		// back to the subdiagram.
		if (0 == nChildren) {
			fXAspect = 1.0f;
			fYAspect = 1.0f;
		}

		for (Shape s : diq.getChildren()) {
			GraphicsAlgorithm ga = s.getGraphicsAlgorithm();

			// position child elements
			ga.setX((int) ((ga.getX() - shiftX) * fXAspect) + 5);
			ga.setY((int) ((ga.getY() - shiftY) * fYAspect) + 5);
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
				if (itemPe instanceof Shape && (!types.isInterface(itemPe))) {
					Connection conn = peService
							.createFreeFormConnection(getDiagram());
					conn.setEnd((currShape.getAnchors().toArray(new Anchor[0]))[0]);
					conn.setStart((((Shape) itemPe).getAnchors()
							.toArray(new Anchor[0]))[0]);
					gaService.createPolyline(conn);

					// we keep track of all connections we created so we can
					// easily
					// dump them when closing the pool
					connectorConns.add(conn);
				}
			}
		}

		layoutPictogramElement(oldCS);
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

	protected void closeCurrentPool() {
		if (currOpen != null) {
			if (!(currOpen instanceof ContainerShape))
				return;

			ContainerShape cs = (ContainerShape) currOpen;
			GraphicsAlgorithm csGa;
			csGa = cs.getGraphicsAlgorithm();

			Collection<Diagram> diags = getLinkedDiagrams(cs);

			if (diags.size() < 1)
				return;

			Diagram diq = diags.toArray(new Diagram[0])[0];

			LinkedList<Shape> items = new LinkedList<Shape>();
			// determine shapes we have to copy back to the subdiagram
			for (Shape currShape : cs.getChildren()) {
				int connMaxX, connMaxY;
				connMaxX = storedGA.getWidth() - 15;
				connMaxY = storedGA.getHeight() - 35;

				if (!types.isInterface(currShape)) {
					// this seems to be some kind of item which we want to
					// remove from the children list
					items.add(currShape);
				} else {
					// we need to put the connector back to it's original
					// position
					// float fXRatio,fYRatio;
					GraphicsAlgorithm childGA = currShape
							.getGraphicsAlgorithm();
					/*
					 * fXRatio = (float)(childGA.getX() - 5) /
					 * (float)(csGa.getWidth() - 10 - 10); fYRatio =
					 * (float)(childGA.getY() - 5) / (float)(csGa.getHeight() -
					 * 30 - 10); childGA.setX((int)Math.ceil(storedGA.getWidth()
					 * * fXRatio) + 5);
					 * childGA.setY((int)Math.ceil((storedGA.getHeight() - 30) *
					 * fYRatio) + 5);
					 */
					// make sure connectors will be inside the allowed range
					// afterwards
					if (childGA.getX() > connMaxX)
						childGA.setX(connMaxX);
					if (childGA.getY() > connMaxY)
						childGA.setY(connMaxY);
					// connectors are not removed!
				}
			}
			// now we remove everything we guess is some kind of item
			cs.getChildren().removeAll(items);
			getDiagram().getConnections().removeAll(connectorConns);

			// here we sync the diagram representing the instance with the
			// contents of the pool
			// copy the children of the container to the diagram instance
			diq.getChildren().clear();
			diq.getChildren().addAll(items);

			HashSet<Connection> allConnections = new HashSet<Connection>();

			// copy connections to the subordered diagram
			for (Shape c : diq.getChildren()) {
				for (Anchor a : c.getAnchors()) {
					allConnections.addAll(a.getIncomingConnections());
					allConnections.addAll(a.getOutgoingConnections());
				}
				// undo the positioning we did to fit the box
				GraphicsAlgorithm ga = c.getGraphicsAlgorithm();
				ga.setX((int) ((ga.getX() - 5) / fXAspect) + shiftX);
				ga.setY((int) ((ga.getY() - 5) / fYAspect) + shiftY);
				// the diagram will look like it did before
			}

			diq.getConnections().clear();
			diq.getConnections().addAll(allConnections);

			// remove connections to connectors first
			for (Connection c : connectorConns)
				peService.deletePictogramElement(c);

			connectorConns.clear();

			// remove all connections from this diagram
			getDiagram().getConnections().removeAll(diq.getConnections());

			// the stored graphics algorithm still holds the old position.
			// if the other one has been moved, it would jump back to there
			// which is not desirable.
			// so we put it where the other GA is located currently
			storedGA.setX(csGa.getX());
			storedGA.setY(csGa.getY());
			cs.setGraphicsAlgorithm(storedGA);

			// using this, we get a neat freshly layouted image, e.g. text
			// labels will have correct layout
			// it seems to force graphiti to do whatever it also does when
			// resizing a pictogram element
			layoutPictogramElement(cs);
		}
	}

}
