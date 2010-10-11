package net.enilink.komma.graphiti.features;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.impl.DefaultRemoveFeature;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Shape;

import com.google.inject.Inject;

public class RemoveFeature extends DefaultRemoveFeature {
	@Inject
	public RemoveFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	protected void removeAllConnections(Shape shape) {
		// ensures that also all connections of child elements are removed
		super.removeAllConnections(shape);

		if (shape instanceof ContainerShape) {
			for (Shape child : ((ContainerShape) shape).getChildren()) {
				removeAllConnections(child);
			}
		}
	}
}
