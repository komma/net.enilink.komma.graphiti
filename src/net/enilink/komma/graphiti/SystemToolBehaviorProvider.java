package net.enilink.komma.graphiti;

import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.context.IPictogramElementContext;
import org.eclipse.graphiti.features.context.impl.CustomContext;
import org.eclipse.graphiti.features.custom.ICustomFeature;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.platform.IPlatformImageConstants;
import org.eclipse.graphiti.tb.ContextButtonEntry;
import org.eclipse.graphiti.tb.DefaultToolBehaviorProvider;
import org.eclipse.graphiti.tb.IContextButtonPadData;

import com.google.inject.Inject;

import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.views.IViewerMenuSupport;

public class SystemToolBehaviorProvider extends DefaultToolBehaviorProvider {
	@Inject
	IViewerMenuSupport viewerMenuSupport;

	@Inject
	public SystemToolBehaviorProvider(IDiagramTypeProvider diagramTypeProvider) {
		super(diagramTypeProvider);
	}

	/**
	 * This function is used to add buttons to the button pads of the items
	 */
	@Override
	public IContextButtonPadData getContextButtonPadData(
			IPictogramElementContext context) {
		IContextButtonPadData retVal = super.getContextButtonPadData(context);
		ICustomContext cc = getCustomContext(context);
		IFeatureProvider fp = getFeatureProvider();
		ICustomFeature[] features = fp.getCustomFeatures(cc);

		for (ICustomFeature feature : features) {
			ContextButtonEntry button = new ContextButtonEntry(feature, cc);
			button.setText(feature.getName());
			button.setDescription(feature.getDescription());
			button.addDragAndDropFeature(features[0]);
			button.setIconId(feature.getImageId() != null ? feature
					.getImageId() : IPlatformImageConstants.IMG_EDIT_EXPAND);
			retVal.getDomainSpecificContextButtons().add(button);
		}

		return retVal;
	}

	private ICustomContext getCustomContext(IPictogramElementContext context) {
		CustomContext result = new CustomContext(
				new PictogramElement[] { context.getPictogramElement() });
		GraphicsAlgorithm ga = context.getPictogramElement()
				.getGraphicsAlgorithm();
		result.setX(ga.getX());
		result.setY(ga.getY() + 2 * 50);
		return result;
	}

	@Override
	public Object getAdapter(Class<?> type) {
		if (IViewerMenuSupport.class.equals(type)) {
			return viewerMenuSupport;
		}
		if (IEditingDomainProvider.class.equals(type)) {
			return ((IEditingDomainProvider) getDiagramTypeProvider());
		}
		return super.getAdapter(type);
	}
}
