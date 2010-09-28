package net.enilink.komma.graphiti.features.move;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IMoveShapeContext;
import org.eclipse.graphiti.features.context.impl.MoveShapeContext;
import org.eclipse.graphiti.features.impl.DefaultMoveShapeFeature;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;

public class TestMoveConnectorFeature extends DefaultMoveShapeFeature {

	public TestMoveConnectorFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public boolean canMoveShape(IMoveShapeContext context) {
		if (context.getTargetContainer().equals(context.getSourceContainer()))
			return true;

		return false;// connectors can now be moved!
	}

	@Override
	public void preMoveShape(IMoveShapeContext context) {
		// our aim is to adjust the context in a way that the connector will
		// be moved along the outside of the containing shape
		int dx = context.getDeltaX(), x = context.getX();
		int dy = context.getDeltaY(), y = context.getY();
		PictogramElement pe = context.getPictogramElement();
		Shape s = null;

		if (pe instanceof Shape)
			s = (Shape) pe;

		if (!(context instanceof MoveShapeContext) || (s == null))
			return;// can't do anything...

		ContainerShape cs = s.getContainer();

		int maxX = cs.getGraphicsAlgorithm().getWidth(), newX, newDX;
		int maxY = cs.getGraphicsAlgorithm().getHeight(), newY, newDY;
		int distL, distR, distT, distB;

		maxY -= 20;// this is since the name is in bottom of the object... not
					// nice.

		MoveShapeContext cont = (MoveShapeContext) context;

		// calculate distance to all borders
		distL = x;
		distR = maxX - x;
		distT = y;
		distB = maxY - y;

		int minDist = Math.min(Math.min(Math.min(distL, distR), distT), distB);

		newX = x;
		newY = y;
		newDX = dx;
		newDY = dy;

		// snap to the closest border
		if (minDist == distL)
			newX = 5;
		if (minDist == distR)
			newX = maxX - 15;
		if (minDist == distT)
			newY = 5;
		if (minDist == distB)
			newY = maxY - 15;

		newDY = newY - y;
		newDX = newX - x;

		cont.setY(newY);
		cont.setDeltaY(newDY);
		cont.setX(newX);
		cont.setDeltaX(newDX);
	}
}
