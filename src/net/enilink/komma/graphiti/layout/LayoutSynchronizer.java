package net.enilink.komma.graphiti.layout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.URI;
import net.enilink.komma.graphiti.service.IDiagramService;
import net.enilink.komma.graphiti.service.ITypes;
import net.enilink.komma.model.IModel;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.visualization.layout.ConnectorShape;
import net.enilink.vocab.visualization.layout.Dimension;
import net.enilink.vocab.visualization.layout.ExpandedShape;
import net.enilink.vocab.visualization.layout.LAYOUT;
import net.enilink.vocab.visualization.layout.Pictogram;
import net.enilink.vocab.visualization.layout.Point;
import net.enilink.vocab.visualization.layout.PointList;
import net.enilink.vocab.visualization.layout.Position;
import net.enilink.vocab.visualization.layout.Shape;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.graphiti.mm.algorithms.AlgorithmsPackage;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.PictogramsPackage;

import com.google.inject.Inject;

public class LayoutSynchronizer implements ILayoutConstants {
	static interface LayoutUpdater {
		void update(Pictogram p, PictogramElement sourcePe,
				EStructuralFeature feature);
	}

	static class ConnectionInfo {
		Connection connection;
		PictogramElement start, end;

		ConnectionInfo(Connection connection, PictogramElement start,
				PictogramElement end) {
			this.connection = connection;
			this.start = start;
			this.end = end;
		}
	}

	static class UpdateInfo {
		Set<EStructuralFeature> feature = new LinkedHashSet<EStructuralFeature>();
		Object target;
	}

	@Inject
	protected IDiagramService diagramService;

	@Inject
	protected ITypes types;

	protected Map<EStructuralFeature, LayoutUpdater> updaters = new HashMap<>();
	protected IModel layoutModel;
	protected Set<Object> updateCommands = new HashSet<>();

	protected Set<PictogramElement> deletedShapes = new HashSet<>();

	protected List<ConnectionInfo> deletedConnections = new ArrayList<>();

	protected Map<PictogramElement, UpdateInfo> updates = new HashMap<>();

	protected Map<List<?>, Object> shapeCache = new HashMap<>();

	public LayoutSynchronizer(IModel layoutModel) {
		this.layoutModel = layoutModel;
		init();
	}

	public void addForDeletion(PictogramElement pe) {
		deletedShapes.add(pe);
	}

	public void addForDeletion(Connection connection, PictogramElement start,
			PictogramElement end) {
		deletedConnections.add(new ConnectionInfo(connection, start, end));
	}

	public void addForUpdate(PictogramElement pe, EStructuralFeature feature,
			Object target) {
		if (!isRelevantFeature(feature)) {
			return;
		}

		UpdateInfo info = updates.get(pe);
		if (info == null) {
			info = new UpdateInfo();
			updates.put(pe, info);
			info.target = target;
		}
		info.feature.add(feature);
	}

	protected PictogramElement getParent(PictogramElement pe,
			Map<PictogramElement, PictogramElement> parentMap) {
		EObject parent = pe.eContainer();
		if (parent != null) {
			return (PictogramElement) parent;
		}
		return parentMap.get(pe);
	}

	protected Shape getShape(IQuery<Shape> shapeQuery, PictogramElement pe,
			Map<PictogramElement, PictogramElement> parentMap) {
		if (!(pe instanceof Diagram)) {
			// traverse up to root or first element with business object
			PictogramElement parent;
			while ((parent = getParent(pe, parentMap)) != null
					&& !(parent instanceof Diagram)) {
				if (diagramService
						.getBusinessObjectForPictogramElement((PictogramElement) pe) != null) {
					break;
				}
				pe = parent;
			}
		}
		Object target = diagramService.getFirstBusinessObject(pe);
		if (target == null) {
			return null;
		}
		Object context = null;
		PictogramElement parent = getParent(pe, parentMap);
		if (types.isInterface(pe)) {
			context = getShape(shapeQuery, parent, parentMap);
		} else {
			if (parent != null) {
				context = diagramService.getFirstBusinessObject(parent);
			}
		}
		if (context == null) {
			context = RDF.NIL;
		}

		List<?> key = Arrays.asList(Shape.class, target, context);
		Shape shape = (Shape) shapeCache.get(key);
		if (shape == null && !shapeCache.containsKey(key)) {
			shapeQuery.setParameter("target", target);
			shapeQuery.setParameter("context", context);
			shape = first(shapeQuery.evaluate());
		}
		shapeCache.put(key, shape);
		return shape;
	}

	protected URI generateURI(IEntityManager em, String prefix) {
		return em.getNamespace("").appendLocalPart(
				prefix + UUID.randomUUID().toString());
	}

	protected Shape getOrCreateShape(IQuery<Shape> shapeQuery,
			PictogramElement pe) {
		pe = (PictogramElement) diagramService.getRootOrFirstElementWithBO(pe);
		Object target = diagramService.getFirstBusinessObject(pe);
		if (target == null) {
			return null;
		}

		Object context = null;
		if (types.isInterface(pe)) {
			context = getOrCreateShape(shapeQuery,
					(PictogramElement) pe.eContainer());
		} else {
			if (pe.eContainer() != null) {
				context = diagramService
						.getFirstBusinessObject((PictogramElement) pe
								.eContainer());
			}
		}
		if (context == null) {
			context = RDF.NIL;
		}

		List<?> key = Arrays.asList(Shape.class, target, context);
		Shape shape = (Shape) shapeCache.get(key);
		if (shape == null) {
			shapeQuery.setParameter("target", target);
			shapeQuery.setParameter("context", context);
			shape = first(shapeQuery.evaluate());
		}
		if (shape == null) {
			shape = layoutModel.getManager().createNamed(
					generateURI(layoutModel.getManager(), "shape_"),
					types.isInterface(pe) ? ConnectorShape.class : Shape.class);
			shape.setLayoutTarget(target);
			shape.setLayoutContext(context);
		}
		shapeCache.put(key, shape);
		if (types.isExpanded(pe)) {
			layoutModel.getManager()
					.designateEntity(shape, ExpandedShape.class);
		} else {
			layoutModel.getManager().removeDesignation(shape,
					ExpandedShape.class);
		}
		return shape;
	}

	public void update(Map<PictogramElement, PictogramElement> parentMap,
			IProgressMonitor progressMonitor) {
		shapeCache.clear();

		IEntityManager em = layoutModel.getManager();
		try {
			// TODO investigate transaction isolation modes to ensure that
			// uncommitted statements are readable
			em.getTransaction().begin();

			IQuery<Shape> shapeQuery = em
					.createQuery(
							PREFIX
									+ "SELECT ?pe WHERE {?pe a layout:Shape; layout:target ?target; layout:context ?context}")
					.bindResultType(Shape.class);
			IQuery<net.enilink.vocab.visualization.layout.Connection> connectionQuery = em
					.createQuery(
							PREFIX
									+ "SELECT ?pe WHERE {"
									+ "?pe a layout:Connection; layout:target ?target; layout:start ?start; layout:end ?end; layout:context ?context"
									+ "}")
					.bindResultType(
							net.enilink.vocab.visualization.layout.Connection.class);

			for (Map.Entry<PictogramElement, UpdateInfo> entry : updates
					.entrySet()) {
				Object context = RDF.NIL;

				PictogramElement pe = entry.getKey();
				UpdateInfo updateInfo = entry.getValue();

				Pictogram p;
				if (pe instanceof Connection) {
					Object target = updateInfo.target;
					if (target instanceof IStatement) {
						target = ((IStatement) target).getPredicate();
					}

					Shape start = getOrCreateShape(shapeQuery,
							((Connection) pe).getStart().getParent());
					if (start == null) {
						continue;
					}
					Shape end = getOrCreateShape(shapeQuery, ((Connection) pe)
							.getEnd().getParent());
					if (end == null) {
						continue;
					}

					connectionQuery.setParameter("target", target);
					connectionQuery.setParameter("start", start);
					connectionQuery.setParameter("end", end);
					connectionQuery.setParameter("context", context);

					net.enilink.vocab.visualization.layout.Connection connection = first(connectionQuery
							.evaluate());
					if (connection == null) {
						connection = em
								.createNamed(
										generateURI(em, "shape_"),
										net.enilink.vocab.visualization.layout.Connection.class);
						connection.setLayoutTarget(target);
						connection.setLayoutContext(context);
						connection.setLayoutStart(start);
						connection.setLayoutEnd(end);
					}
					p = connection;
				} else {
					p = getOrCreateShape(shapeQuery, pe);
				}
				for (EStructuralFeature feature : updateInfo.feature) {
					LayoutUpdater updater = updaters.get(feature);
					if (updater != null) {
						updater.update(p, pe, feature);
					}
				}
			}
			// delete connections
			for (ConnectionInfo conn : deletedConnections) {
				Object target = diagramService
						.getFirstBusinessObject(conn.connection);
				if (target instanceof IStatement) {
					connectionQuery.setParameter("target",
							((IStatement) target).getPredicate());
					connectionQuery
							.setParameter(
									"start",
									getShape(shapeQuery,
											getParent(conn.start, parentMap),
											parentMap));
					connectionQuery.setParameter(
							"end",
							getShape(shapeQuery,
									getParent(conn.end, parentMap), parentMap));
					net.enilink.vocab.visualization.layout.Connection connection = first(connectionQuery
							.evaluate());
					if (connection != null) {
						em.removeRecursive(connection, true);
					}
				}
			}
			// delete shapes
			for (PictogramElement toDelete : deletedShapes) {
				delete(shapeQuery, toDelete, parentMap);
			}
			if (!deletedShapes.isEmpty()) {
				// cleanup: delete possible dangling connections
				List<net.enilink.vocab.visualization.layout.Connection> danglingConnections = em
						.createQuery(
								PREFIX
										+ "SELECT ?pe WHERE { ?pe a layout:Connection FILTER NOT EXISTS { ?pe layout:start []; layout:end [] }}")
						.evaluate(
								net.enilink.vocab.visualization.layout.Connection.class)
						.toList();
				for (net.enilink.vocab.visualization.layout.Connection c : danglingConnections) {
					c.getEntityManager().removeRecursive(c, true);
				}
			}
			em.getTransaction().commit();
		} finally {
			updates.clear();
			deletedShapes.clear();
			deletedConnections.clear();

			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
		}
	}

	protected void delete(IQuery<Shape> shapeQuery, PictogramElement pe,
			Map<PictogramElement, PictogramElement> parentMap) {
		Shape shape = getShape(shapeQuery, pe, parentMap);
		if (shape != null) {
			shape.getEntityManager().removeRecursive(shape, true);
		}
	}

	protected <T> T first(Iterator<T> it) {
		try {
			if (it.hasNext()) {
				return it.next();
			}
			return null;
		} finally {
			if (it instanceof AutoCloseable) {
				try {
					((AutoCloseable) it).close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}

	protected Dimension getDimension(Shape shape) {
		Dimension d = shape.getLayoutDimension();
		if (d == null) {
			d = (Dimension) shapeCache.get(Arrays.asList(shape,
					LAYOUT.PROPERTY_DIMENSION));
		}
		if (d == null) {
			d = layoutModel.getManager().create(Dimension.class);
			shape.setLayoutDimension(d);
			shapeCache.put(Arrays.asList(shape, LAYOUT.PROPERTY_DIMENSION), d);
		}
		return d;
	}

	protected Position getPosition(Shape shape) {
		Position p = shape.getLayoutPosition();
		if (p == null) {
			p = (Position) shapeCache.get(Arrays.asList(shape,
					LAYOUT.PROPERTY_POSITION));
		}
		if (p == null) {
			p = layoutModel.getManager().create(Position.class);
			shape.setLayoutPosition(p);
			shapeCache.put(Arrays.asList(shape, LAYOUT.PROPERTY_POSITION), p);
		}
		return p;
	}

	protected void init() {
		AlgorithmsPackage ap = AlgorithmsPackage.eINSTANCE;
		updaters.put(ap.getGraphicsAlgorithm_Height(), new LayoutUpdater() {
			@Override
			public void update(Pictogram p, PictogramElement sourcePe,
					EStructuralFeature feature) {
				getDimension((Shape) p).setLayoutY(
						(double) sourcePe.getGraphicsAlgorithm().getHeight());
			}
		});
		updaters.put(ap.getGraphicsAlgorithm_Width(), new LayoutUpdater() {
			@Override
			public void update(Pictogram p, PictogramElement sourcePe,
					EStructuralFeature feature) {
				getDimension((Shape) p).setLayoutX(
						(double) sourcePe.getGraphicsAlgorithm().getWidth());
			}
		});
		updaters.put(ap.getGraphicsAlgorithm_X(), new LayoutUpdater() {
			@Override
			public void update(Pictogram p, PictogramElement sourcePe,
					EStructuralFeature feature) {
				getPosition((Shape) p).setLayoutX(
						(double) sourcePe.getGraphicsAlgorithm().getX());
			}
		});
		updaters.put(ap.getGraphicsAlgorithm_Y(), new LayoutUpdater() {
			@Override
			public void update(Pictogram p, PictogramElement sourcePe,
					EStructuralFeature feature) {
				getPosition((Shape) p).setLayoutY(
						(double) sourcePe.getGraphicsAlgorithm().getY());
			}
		});

		PictogramsPackage pp = PictogramsPackage.eINSTANCE;
		updaters.put(pp.getFreeFormConnection_Bendpoints(),
				new LayoutUpdater() {
					@Override
					public void update(Pictogram p, PictogramElement sourcePe,
							EStructuralFeature feature) {
						IEntityManager manager = p.getEntityManager();

						net.enilink.vocab.visualization.layout.Connection c = (net.enilink.vocab.visualization.layout.Connection) p;
						PointList points = c.getLayoutPoints();
						// remove all existing bend points
						if (points != null) {
							manager.removeRecursive(points, true);
						}

						if (!((FreeFormConnection) sourcePe).getBendpoints()
								.isEmpty()) {
							points = manager.create(PointList.class);
							c.setLayoutPoints(points);

							// create bend points according to the supplied
							// Graphiti connection
							for (org.eclipse.graphiti.mm.algorithms.styles.Point bendPoint : ((FreeFormConnection) sourcePe)
									.getBendpoints()) {
								Point point = manager.create(Point.class);
								Position position = manager
										.create(Position.class);
								position.setLayoutX((double) bendPoint.getX());
								position.setLayoutY((double) bendPoint.getY());
								point.setLayoutPosition(position);

								points.add(point);
							}
						}
					}
				});
		updaters.put(pp.getConnection_Start(), new LayoutUpdater() {
			@Override
			public void update(Pictogram p, PictogramElement sourcePe,
					EStructuralFeature feature) {
				// do nothing, already handled by update method
			}
		});
		updaters.put(pp.getConnection_End(), new LayoutUpdater() {
			@Override
			public void update(Pictogram p, PictogramElement sourcePe,
					EStructuralFeature feature) {
				// do nothing, already handled by update method
			}
		});
	}

	public boolean isRelevantFeature(EStructuralFeature feature) {
		return updaters.containsKey(feature);
	}

	public boolean hasUpdates() {
		return !(updates.isEmpty() && deletedConnections.isEmpty() && deletedShapes
				.isEmpty());
	}
}
