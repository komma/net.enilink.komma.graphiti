package net.enilink.komma.graphiti.features.add;

import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.ModelUtil;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddConnectionContext;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.impl.AbstractAddFeature;
import org.eclipse.graphiti.mm.GraphicsAlgorithmContainer;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ConnectionDecorator;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;

import com.google.inject.Inject;

public class AddConnectionFeature extends AbstractAddFeature {
	@Inject
	IModel model;

	@Inject
	IGaService gaService;

	@Inject
	IPeCreateService peCreateService;

	@Inject
	public AddConnectionFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public boolean canAdd(IAddContext context) {
		if (context instanceof IAddConnectionContext
				&& context.getNewObject() instanceof IStatement) {
			return true;
		}

		if (context instanceof IAddConnectionContext
				&& context.getNewObject() instanceof net.enilink.vocab.komma.Connection) {
			return true;
		}

		return false;
	}

	@Override
	public PictogramElement add(IAddContext context) {
		IAddConnectionContext addConContext = (IAddConnectionContext) context;
		Object addedConnection = context.getNewObject();
		String label = null;
		if (addedConnection instanceof IStatement) {
			label = ModelUtil.getLabel(model
					.resolve(((IStatement) addedConnection).getPredicate()));
		} else if (addedConnection instanceof net.enilink.vocab.komma.Connection) {
			label = ModelUtil.getLabel(model
					.resolve(((IEntity) addedConnection).getReference()));
		}

		// CONNECTION WITH POLYLINE
		Connection connection = peCreateService
				.createFreeFormConnection(getDiagram());
		connection.setStart(addConContext.getSourceAnchor());
		connection.setEnd(addConContext.getTargetAnchor());

		Polyline polyline = gaService.createPolyline(connection);
		// polyline.setStyle(StyleUtil.getStyleForEClass(getDiagram()));

		// create link and wire it
		link(connection, addedConnection);

		// add dynamic text decorator for the reference name
		ConnectionDecorator textDecorator = peCreateService
				.createConnectionDecorator(connection, true, 0.5, true);
		Text text = gaService.createDefaultText(getDiagram(), textDecorator);
		// text.setStyle(StyleUtil.getStyleForEClassText((getDiagram())));
		gaService.setLocation(text, 10, 0);
		// set reference name in the text decorator
		text.setValue(label);

		// add static graphical decorators (composition and navigable)
		ConnectionDecorator cd;
		cd = peCreateService.createConnectionDecorator(connection, false, 1.0,
				true);
		createArrow(cd);
		// cd = PeUtil.createConnectionDecorator(connection, false, 1.0, true);
		// createRhombus(cd);

		return connection;
	}

	private Polyline createArrow(GraphicsAlgorithmContainer gaContainer) {
		Polyline polyline = gaService.createPolyline(gaContainer, new int[] {
				-15, 10, 0, 0, -15, -10 });
		// polyline.setStyle(StyleUtil.getStyleForEClass(getDiagram()));
		return polyline;
	}
}
