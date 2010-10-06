package net.enilink.komma.graphiti.features;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IReason;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.impl.AbstractUpdateFeature;
import org.eclipse.graphiti.features.impl.Reason;
import org.eclipse.graphiti.mm.algorithms.AbstractText;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.algorithms.styles.Orientation;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeService;
import org.eclipse.jface.viewers.ILabelProvider;

import com.google.inject.Inject;

import net.enilink.komma.concepts.IResource;

public class UpdateNodeFeature extends AbstractUpdateFeature {
	@Inject
	IGaService gaService;

	@Inject
	IPeService peService;

	@Inject
	ILabelProvider labelProvider;

	@Inject
	public UpdateNodeFeature(IFeatureProvider fp) {
		super(fp);
	}

	public boolean canUpdate(IUpdateContext context) {
		// return true, if linked business object is a Class
		Object bo = getBusinessObjectForPictogramElement(context
				.getPictogramElement());
		return bo instanceof IResource;
	}

	public IReason updateNeeded(IUpdateContext context) {
		// two strings that have to be retrieved and compared
		String pictogramName = null;
		PictogramElement pictogramElement = context.getPictogramElement();
		if (pictogramElement instanceof ContainerShape) {
			ContainerShape cs = (ContainerShape) pictogramElement;
			for (Shape shape : cs.getChildren()) {
				if (shape.getGraphicsAlgorithm() instanceof AbstractText) {
					AbstractText text = (AbstractText) shape
							.getGraphicsAlgorithm();
					pictogramName = text.getValue();
				}
			}
		}

		String businessName = null;
		// Retrieve value from business model
		Object bo = getBusinessObjectForPictogramElement(pictogramElement);
		if (bo instanceof IResource) {
			businessName = labelProvider.getText(bo);
		}

		// compare values
		boolean needed = pictogramName != null
				&& !pictogramName.equals(businessName);
		if (needed) {
			return Reason.createTrueReason("Name is out of date");
		} else {
			return Reason.createFalseReason();
		}
	}

	public boolean update(IUpdateContext context) {
		String businessName = null;

		// Retrieve value from business model
		PictogramElement pictogramElement = context.getPictogramElement();
		Object bo = getBusinessObjectForPictogramElement(pictogramElement);
		if (bo instanceof IResource) {
			businessName = labelProvider.getText(bo);

			// Set name in pictogram model
			if (pictogramElement instanceof ContainerShape) {
				ContainerShape cs = (ContainerShape) pictogramElement;
				for (Shape shape : cs.getChildren()) {
					if (shape.getGraphicsAlgorithm() instanceof AbstractText) {
						AbstractText text = (AbstractText) shape
								.getGraphicsAlgorithm();

						if (text instanceof Text) {
							AbstractText newText = gaService
									.createMultiText(shape);
							newText.setStyle(text.getStyle());

							text = newText;
						}

						text.setHorizontalAlignment(Orientation.ALIGNMENT_CENTER);
						text.setVerticalAlignment(Orientation.ALIGNMENT_TOP);
						text.setValue(businessName);

						layoutPictogramElement(pictogramElement);

						return true;
					}
				}
			}
		}

		return false;
	}

}