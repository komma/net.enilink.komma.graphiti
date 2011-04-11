package net.enilink.komma.graphiti.layout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.graphiti.mm.algorithms.AlgorithmsPackage;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.PictogramsPackage;

import com.google.inject.Inject;

import net.enilink.commons.iterator.IClosableIterator;
import net.enilink.vocab.rdf.RDF;
import net.enilink.komma.graphiti.service.IDiagramService;
import net.enilink.komma.model.IModel;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IGraph;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.LinkedHashGraph;
import net.enilink.layout.Dimension;
import net.enilink.layout.LAYOUT;
import net.enilink.layout.Pictogram;
import net.enilink.layout.Point;
import net.enilink.layout.PointList;
import net.enilink.layout.Position;
import net.enilink.layout.Shape;

public class LayoutSynchronizer implements ILayoutConstants {
	static interface LayoutUpdater {
		void update(Pictogram p, PictogramElement sourcePe,
				EStructuralFeature feature);
	}

	static class UpdateInfo {
		Set<EStructuralFeature> feature = new LinkedHashSet<EStructuralFeature>();
		Object target;
	}

	@Inject
	protected IDiagramService diagramService;

	protected Map<EStructuralFeature, LayoutUpdater> updaters = new HashMap<EStructuralFeature, LayoutUpdater>();
	protected IModel layoutModel;
	protected Set<Object> updateCommands = new HashSet<Object>();

	protected Map<PictogramElement, UpdateInfo> updates = new HashMap<PictogramElement, UpdateInfo>();

	protected IGraph transientEntities = new LinkedHashGraph();

	public LayoutSynchronizer(IModel layoutModel) {
		this.layoutModel = layoutModel;
		init();
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

	public void update(IProgressMonitor progressMonitor) {
		transientEntities.clear();

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
			IQuery<net.enilink.layout.Connection> connectionQuery = em
					.createQuery(
							PREFIX
									+ "SELECT ?pe WHERE {"
									+ "?pe a layout:Connection; layout:target ?target; layout:start ?start; layout:end ?end; layout:context ?context"
									+ "}").bindResultType(
							net.enilink.layout.Connection.class);

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
					Object start = getFirstBusinessObject(((Connection) pe)
							.getStart());
					Object end = getFirstBusinessObject(((Connection) pe)
							.getEnd());
					if (start == null || end == null) {
						continue;
					}

					connectionQuery.setParameter("target", target);
					connectionQuery.setParameter("start", start);
					connectionQuery.setParameter("end", end);
					connectionQuery.setParameter("context", context);

					net.enilink.layout.Connection connection = first(connectionQuery
							.evaluate());
					if (connection == null) {
						connection = em
								.create(net.enilink.layout.Connection.class);
						connection.setLayoutTarget(target);
					}
					p = connection;
				} else {
					shapeQuery.setParameter("target", updateInfo.target);
					shapeQuery.setParameter("context", context);

					Shape shape = first(shapeQuery.evaluate());
					if (shape == null) {
						shape = em.create(Shape.class);
						shape.setLayoutTarget(updateInfo.target);
					}
					p = shape;
				}
				for (EStructuralFeature feature : updateInfo.feature) {
					LayoutUpdater updater = updaters.get(feature);
					if (updater != null) {
						updater.update(p, pe, feature);
					}
				}
			}
			em.getTransaction().commit();
		} finally {
			updates.clear();

			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
		}
	}

	protected <T> T first(Iterator<T> it) {
		try {
			if (it.hasNext()) {
				return it.next();
			}
			return null;
		} finally {
			if (it instanceof IClosableIterator) {
				((IClosableIterator<?>) it).close();
			}
		}
	}

	protected Dimension getDimension(Shape shape) {
		Dimension d = shape.getLayoutDimension();
		if (d == null) {
			IStatement stmt = first(transientEntities.filter(shape,
					LAYOUT.PROPERTY_DIMENSION, null).iterator());
			if (stmt != null) {
				d = (Dimension) stmt.getObject();
			}
		}
		if (d == null) {
			d = layoutModel.getManager().create(Dimension.class);
			shape.setLayoutDimension(d);
			transientEntities.add(shape, LAYOUT.PROPERTY_DIMENSION, d);
		}
		return d;
	}

	protected Position getPosition(Shape shape) {
		Position p = shape.getLayoutPosition();
		if (p == null) {
			IStatement stmt = first(transientEntities.filter(shape,
					LAYOUT.PROPERTY_POSITION, null).iterator());
			if (stmt != null) {
				p = (Position) stmt.getObject();
			}
		}
		if (p == null) {
			p = layoutModel.getManager().create(Position.class);
			shape.setLayoutPosition(p);
			transientEntities.add(shape, LAYOUT.PROPERTY_POSITION, p);
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

						net.enilink.layout.Connection c = (net.enilink.layout.Connection) p;
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
								position.setLayoutX((double) bendPoint.getY());
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
				Connection c = (Connection) sourcePe;
				Object obj = getFirstBusinessObject(c.getStart());
				if (obj == null || obj instanceof IReference) {
					((net.enilink.layout.Connection) p)
							.setLayoutStart(obj);
				}
			}
		});
		updaters.put(pp.getConnection_End(), new LayoutUpdater() {
			@Override
			public void update(Pictogram p, PictogramElement sourcePe,
					EStructuralFeature feature) {
				Connection c = (Connection) sourcePe;
				Object obj = getFirstBusinessObject(c.getEnd());
				if (obj == null || obj instanceof IReference) {
					((net.enilink.layout.Connection) p)
							.setLayoutStart(obj);
				}
			}
		});
	}

	protected Object getFirstBusinessObject(Anchor anchor) {
		return diagramService.getFirstBusinessObject(anchor.getParent());
	}

	public boolean isRelevantFeature(EStructuralFeature feature) {
		return updaters.containsKey(feature);
	}

	public boolean hasUpdates() {
		return !updates.isEmpty();
	}
}
