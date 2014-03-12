package net.enilink.komma.graphiti;

import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.views.IViewerMenuSupport;
import net.enilink.komma.graphiti.service.IDiagramService;

import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.context.IPictogramElementContext;
import org.eclipse.graphiti.features.context.impl.CustomContext;
import org.eclipse.graphiti.features.custom.ICustomFeature;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.platform.IPlatformImageConstants;
import org.eclipse.graphiti.tb.ContextButtonEntry;
import org.eclipse.graphiti.tb.DefaultToolBehaviorProvider;
import org.eclipse.graphiti.tb.IContextButtonPadData;

import com.google.inject.Inject;

public class ToolBehaviorProvider extends DefaultToolBehaviorProvider {
	@Inject
	IViewerMenuSupport viewerMenuSupport;

	@Inject
	IDiagramService diagramService;

	@Inject
	public ToolBehaviorProvider(IDiagramTypeProvider diagramTypeProvider) {
		super(diagramTypeProvider);
	}

	/**
	 * This function is used to add buttons to the button pads of the items
	 */
	@Override
	public IContextButtonPadData getContextButtonPad(
			IPictogramElementContext context) {
		IContextButtonPadData padData = super.getContextButtonPad(context);
		ICustomContext cc = getCustomContext(context);
		ICustomFeature[] features = getFeatureProvider().getCustomFeatures(cc);
		for (ICustomFeature feature : features) {
			ContextButtonEntry button = new ContextButtonEntry(feature, cc);
			button.setText(feature.getName());
			button.setDescription(feature.getDescription());
			button.addDragAndDropFeature(features[0]);
			button.setIconId(feature.getImageId() != null ? feature
					.getImageId() : IPlatformImageConstants.IMG_EDIT_EXPAND);
			padData.getDomainSpecificContextButtons().add(button);
		}

		return padData;
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
	public GraphicsAlgorithm getSelectionBorder(PictogramElement pe) {
		PictogramElement rootPe = diagramService
				.getRootOrFirstElementWithBO(pe);
		if (rootPe != null && rootPe.getGraphicsAlgorithm() != null) {
			return rootPe.getGraphicsAlgorithm();
		}
		return super.getSelectionBorder(pe);
	}

	@Override
	public PictogramElement getSelection(PictogramElement originalPe,
			PictogramElement[] oldSelection) {
		PictogramElement pe = diagramService
				.getRootOrFirstElementWithBO(originalPe);
		if (pe != null && !(pe instanceof Connection)) {
			return pe;
		}
		return super.getSelection(originalPe, oldSelection);
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
