package net.enilink.komma.graphiti.features.move;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IMoveShapeContext;
import org.eclipse.graphiti.features.impl.DefaultMoveShapeFeature;

public class TestMoveConnectorFeature extends DefaultMoveShapeFeature {

	public TestMoveConnectorFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public boolean canMoveShape(IMoveShapeContext context){
		return false;// connectors must not be moved
	}
}
