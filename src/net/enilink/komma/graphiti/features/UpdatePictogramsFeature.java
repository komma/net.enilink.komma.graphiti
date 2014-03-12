package net.enilink.komma.graphiti.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.Statement;
import net.enilink.komma.graphiti.concepts.Connector;
import net.enilink.komma.graphiti.layout.ILayoutConstants;
import net.enilink.komma.graphiti.service.IDiagramService;
import net.enilink.komma.model.IModel;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.visualization.layout.Connection;
import net.enilink.vocab.visualization.layout.ConnectorShape;
import net.enilink.vocab.visualization.layout.Dimension;
import net.enilink.vocab.visualization.layout.ExpandedShape;
import net.enilink.vocab.visualization.layout.Pictogram;
import net.enilink.vocab.visualization.layout.Point;
import net.enilink.vocab.visualization.layout.Position;
import net.enilink.vocab.visualization.layout.Shape;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IReason;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.context.impl.AddConnectionContext;
import org.eclipse.graphiti.features.context.impl.AddContext;
import org.eclipse.graphiti.features.context.impl.AreaContext;
import org.eclipse.graphiti.features.context.impl.CustomContext;
import org.eclipse.graphiti.features.impl.AbstractUpdateFeature;
import org.eclipse.graphiti.features.impl.Reason;
import org.eclipse.graphiti.mm.algorithms.styles.StylesFactory;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.ConnectionDecorator;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;

public class UpdatePictogramsFeature extends AbstractUpdateFeature implements
		ILayoutConstants {
	@Inject
	IDiagramService diagramService;

	@Inject
	IModel dataModel;

	@Inject
	@Named("layout")
	IModel layoutModel;

	@Inject
	Injector injector;

	@Inject
	public UpdatePictogramsFeature(IFeatureProvider fp) {
		super(fp);
	}

	public boolean canUpdate(IUpdateContext context) {
		return true;
	}

	public IReason updateNeeded(IUpdateContext context) {
		return Reason.createTrueReason();
	}

	protected void createPictograms(Map<Pictogram, PictogramElement> peMap,
			ContainerShape container, IQuery<?> pictogramQuery, Object context) {
		pictogramQuery.setParameter("context", context);

		List<Pictogram> newContainers = new ArrayList<Pictogram>();
		List<Connection> newConnections = new ArrayList<Connection>();
		// retrieve all pictograms for given context
		for (Pictogram p : pictogramQuery.evaluate(Pictogram.class)) {
			PictogramElement pe = null;
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

				Object bo = dataModel.resolve((IReference) s.getLayoutTarget());
				if (p instanceof ConnectorShape) {
					bo = new Connector(bo);

					// find correct node shape
					for (org.eclipse.graphiti.mm.pictograms.Shape shape : container
							.getChildren()) {
						if (shape instanceof ContainerShape) {
							container = (ContainerShape) shape;
						}
					}
				}

				AddContext addCtx = new AddContext(areaCtx, bo);
				addCtx.setTargetContainer(container);
				addCtx.putProperty("update.pictograms", true);
				pe = getFeatureProvider().addIfPossible(addCtx);
			} else if (p instanceof Connection) {
				newConnections.add((Connection) p);
			}
			if (pe != null) {
				peMap.put(p, pe);

				if (pe instanceof ContainerShape) {
					newContainers.add(p);
				}
			}
		}

		// recursively create pictograms for new container shapes
		for (Pictogram newContainer : newContainers) {
			ContainerShape newContainerShape = (ContainerShape) peMap
					.get(newContainer);
			createPictograms(peMap, newContainerShape, pictogramQuery,
					newContainer);

			// expand container if required
			if (newContainer instanceof ExpandedShape) {
				ExpandFeature expandFeature = injector
						.getInstance(ExpandFeature.class);
				expandFeature.execute(new CustomContext(
						new PictogramElement[] { newContainerShape }));
			}
		}

		// add connections after all pictograms have been created
		for (Connection c : newConnections) {
			Object target = c.getLayoutTarget();

			if (target == null) {
				continue;
			}
			PictogramElement startPe = peMap.get(c.getLayoutStart());
			PictogramElement endPe = peMap.get(c.getLayoutEnd());

			Anchor startAnchor = findBestAnchor(startPe);
			Anchor endAnchor = findBestAnchor(endPe);
			if (startAnchor == null || endAnchor == null) {
				continue;
			}

			AddConnectionContext addContext = new AddConnectionContext(
					startAnchor, endAnchor);
			addContext.setNewObject(new Statement((IReference) diagramService
					.getFirstBusinessObject(startAnchor.getParent()),
					(IReference) target, diagramService
							.getFirstBusinessObject(endAnchor.getParent())));
			org.eclipse.graphiti.mm.pictograms.Connection newConnection = (org.eclipse.graphiti.mm.pictograms.Connection) getFeatureProvider()
					.addIfPossible(addContext);

			// set bend points for connection
			if (c.getLayoutPoints() != null
					&& newConnection instanceof FreeFormConnection) {
				FreeFormConnection ffConnection = (FreeFormConnection) newConnection;
				for (Point point : c.getLayoutPoints()) {
					Position position = point.getLayoutPosition();
					if (position == null) {
						continue;
					}
					org.eclipse.graphiti.mm.algorithms.styles.Point newPoint = StylesFactory.eINSTANCE
							.createPoint();

					newPoint.setX(toInt(position.getLayoutX(), 0));
					newPoint.setY(toInt(position.getLayoutY(), 0));
					ffConnection.getBendpoints().add(newPoint);
				}
			}

			// set decorator locations
			if (c.getLayoutDecorators() != null) {
				Iterator<ConnectionDecorator> decoratorIt = newConnection
						.getConnectionDecorators().iterator();
				for (Shape decorator : c.getLayoutDecorators()) {
					if (!decoratorIt.hasNext()) {
						break;
					}
					ConnectionDecorator targetDecorator = decoratorIt.next();
					Position position = decorator.getLayoutPosition();
					if (position == null) {
						continue;
					}
					targetDecorator.setLocation(position.getLayoutX());
				}
			}
		}
	}

	public boolean update(IUpdateContext context) {
		IQuery<?> query = layoutModel.getManager().createQuery(
				PREFIX
						+ "SELECT DISTINCT ?p WHERE {" //
						+ "?p a layout:Pictogram . "
						+ "?p layout:context ?context . " //
						+ "} ORDER BY ?p DESC (?p)");

		Object pictogramsContext = diagramService
				.getFirstBusinessObject(context.getPictogramElement());
		if (pictogramsContext == null) {
			pictogramsContext = RDF.NIL;
		}

		createPictograms(new HashMap<Pictogram, PictogramElement>(),
				(ContainerShape) context.getPictogramElement(), query,
				pictogramsContext);

		return true;
	}

	private Anchor findBestAnchor(PictogramElement pe) {
		if (pe == null) {
			return null;
		}
		if (pe instanceof AnchorContainer
				&& !((AnchorContainer) pe).getAnchors().isEmpty()) {
			return ((AnchorContainer) pe).getAnchors().get(0);
		}
		for (Object child : pe.eContents()) {
			if (child instanceof PictogramElement) {
				Anchor anchor = findBestAnchor((PictogramElement) child);
				if (anchor != null) {
					return anchor;
				}
			}
		}
		return null;
	}

	int toInt(Double d, int defaultValue) {
		if (d == null) {
			return defaultValue;
		}
		return d.intValue();
	}
}