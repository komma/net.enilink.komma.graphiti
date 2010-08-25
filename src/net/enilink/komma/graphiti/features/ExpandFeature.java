package net.enilink.komma.graphiti.features;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.mm.pictograms.Diagram;

import com.google.inject.Inject;

public class ExpandFeature extends DrillDownFeature {
	@Inject
	public ExpandFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	protected void openDiagram(ICustomContext context, Diagram diagram) {
		// final PictogramElement pe = context.getPictogramElements()[0];
		//
		// if (pe instanceof ContainerShape) {
		// ContainerShape shape = peService.createContainerShape(
		// (ContainerShape) pe, false);
		//
		// }
	}

	@Override
	public String getDescription() {
		return "Expand the diagram associated with this node"; //$NON-NLS-1$
	}

	@Override
	public String getName() {
		return "Expand associated diagram"; //$NON-NLS-1$
	}
}
