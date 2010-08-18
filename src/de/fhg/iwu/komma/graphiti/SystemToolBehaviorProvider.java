package de.fhg.iwu.komma.graphiti;

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

public class SystemToolBehaviorProvider extends DefaultToolBehaviorProvider {
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

		// Object bo =
		// getBusinessObjectForPictogramElement(context.getPictogramElement());

		ContextButtonEntry button = new ContextButtonEntry(features[0], cc);
		button.setText("Great feature");
		button.setDescription("Click here to whitness a very great feature!");
		button.addDragAndDropFeature(features[0]);
		button.setIconId(IPlatformImageConstants.IMG_EDIT_EXPAND);
		retVal.getDomainSpecificContextButtons().add(button);

		return retVal;
	}

	// I copied this from the eCore example
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
		if (IEditingDomainProvider.class.equals(type)) {
			return ((IEditingDomainProvider) getDiagramTypeProvider());
		}
		return super.getAdapter(type);
	}
}
