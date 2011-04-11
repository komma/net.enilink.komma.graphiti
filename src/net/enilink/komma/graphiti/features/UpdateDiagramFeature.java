package net.enilink.komma.graphiti.features;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IReason;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.context.impl.AddConnectionContext;
import org.eclipse.graphiti.features.context.impl.AddContext;
import org.eclipse.graphiti.features.context.impl.AreaContext;
import org.eclipse.graphiti.features.impl.AbstractUpdateFeature;
import org.eclipse.graphiti.features.impl.Reason;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeService;
import org.eclipse.jface.viewers.ILabelProvider;

import com.google.inject.Inject;

import net.enilink.komma.graphiti.SystemDiagramTypeProvider;
import net.enilink.komma.graphiti.layout.ILayoutConstants;
import net.enilink.komma.model.IModel;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.layout.Connection;
import net.enilink.layout.Dimension;
import net.enilink.layout.Pictogram;
import net.enilink.layout.Position;
import net.enilink.layout.Shape;

public class UpdateDiagramFeature extends AbstractUpdateFeature implements
		ILayoutConstants {
	@Inject
	IGaService gaService;

	@Inject
	IPeService peService;

	@Inject
	ILabelProvider labelProvider;

	@Inject
	public UpdateDiagramFeature(IFeatureProvider fp) {
		super(fp);
	}

	public boolean canUpdate(IUpdateContext context) {
		return true;
	}

	public IReason updateNeeded(IUpdateContext context) {
		return Reason.createTrueReason();
	}

	public boolean update(IUpdateContext context) {
		SystemDiagramTypeProvider typeProvider = (SystemDiagramTypeProvider) getFeatureProvider()
				.getDiagramTypeProvider();
		IModel dataModel = typeProvider.getModel();
		IModel layoutModel = typeProvider.getLayoutModel();

		IQuery<?> query = layoutModel
				.getManager()
				.createQuery(
						PREFIX
								+ "SELECT ?p WHERE {" //
								+ "{?p a layout:Shape } UNION {?p a layout:Connection} . "
								// +
								// "OPTIONAL {?p layout:context ?c} FILTER (!bound(?c))"
								+ "}");
		for (Pictogram p : query.evaluate(Pictogram.class)) {
			if (p instanceof Shape) {
				Shape s = (Shape) p;
				AreaContext areaCtx = new AreaContext();
				if (s.getLayoutDimension() != null) {
					Dimension d = s.getLayoutDimension();
					areaCtx.setSize(toInt(d.getLayoutX(), 25),
							toInt(d.getLayoutY(), 25));
				}
				if (s.getLayoutPosition() != null) {
					Position position = s.getLayoutPosition();
					areaCtx.setLocation(toInt(position.getLayoutX(), 0),
							toInt(position.getLayoutY(), 0));
				}

				AddContext addCtx = new AddContext(areaCtx,
						dataModel.resolve((IReference) s.getLayoutTarget()));
				addCtx.setTargetContainer(getDiagram());
				getFeatureProvider().addIfPossible(addCtx);
			} else if (p instanceof Connection) {
				Connection c = (Connection) p;
				Object start = c.getLayoutStart();
				Object end = c.getLayoutEnd();
				Object target = c.getLayoutTarget();

				if (start == null || end == null || target == null) {
					continue;
				}

				// AddConnectionContext addConnectionCtx = new
				// AddConnectionContext(sourceAnchor, targetAnchor);
			}
		}

		return false;
	}

	int toInt(Double d, int defaultValue) {
		if (d == null) {
			return defaultValue;
		}
		return d.intValue();
	}
}