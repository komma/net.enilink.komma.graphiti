package de.fhg.iwu.komma.graphiti.features.move;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IMoveShapeContext;
import org.eclipse.graphiti.features.impl.DefaultMoveShapeFeature;

public class TestMoveTransportFeature extends DefaultMoveShapeFeature {

	public TestMoveTransportFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public boolean canMoveShape(IMoveShapeContext context) {

		boolean ret = super.canMoveShape(context);

		// check further details only if move allowed by default feature
		if (ret) {

			// don't allow move if package name has the length of one
			Object bo = getBusinessObjectForPictogramElement(context.getShape());
			if (bo instanceof EPackage) {
				EPackage p = (EPackage) bo;
				if (p.getName() != null && p.getName().length() == 1) {
					ret = false;
				}
			}
		}
		return ret;
	}
}
