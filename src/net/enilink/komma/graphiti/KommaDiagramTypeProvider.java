package net.enilink.komma.graphiti;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.INotificationListener;
import net.enilink.komma.common.notify.NotificationFilter;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.editor.KommaEditorSupport;
import net.enilink.komma.edit.ui.rcp.project.ProjectModelSetManager;
import net.enilink.komma.graphiti.layout.LayoutSynchronizer;
import net.enilink.komma.graphiti.service.IDiagramService;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.transaction.ResourceSetChangeEvent;
import org.eclipse.emf.transaction.ResourceSetListenerImpl;
import org.eclipse.gef.EditPart;
import org.eclipse.graphiti.dt.AbstractDiagramTypeProvider;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.PictogramsPackage;
import org.eclipse.graphiti.platform.IDiagramBehavior;
import org.eclipse.graphiti.platform.ga.IGraphicsAlgorithmRendererFactory;
import org.eclipse.graphiti.tb.IToolBehaviorProvider;
import org.eclipse.ui.progress.UIJob;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

public class KommaDiagramTypeProvider extends AbstractDiagramTypeProvider
		implements IEditingDomainProvider {
	private Injector injector;
	private IAdapterFactory platformAdapterFactory;

	private INotificationListener<INotification> notificationListener = new INotificationListener<INotification>() {
		@Override
		public NotificationFilter<INotification> getFilter() {
			return null;
		}

		@Override
		public void notifyChanged(
				Collection<? extends INotification> notifications) {
			Set<PictogramElement> elements = null;

			IFeatureProvider featureProvider = getFeatureProvider();
			for (INotification notification : notifications) {
				if (notification.getSubject() instanceof IReference
						&& ((IReference) notification.getSubject()).getURI() != null) {
					PictogramElement element = featureProvider
							.getPictogramElementForBusinessObject(notification
									.getSubject());
					if (element != null) {
						if (elements == null) {
							elements = new HashSet<PictogramElement>();
						}
						elements.add(element);
					}
				}
			}
			if (elements != null) {
				getNotificationService()
						.updatePictogramElements(
								elements.toArray(new PictogramElement[elements
										.size()]));
			}
		}
	};

	@Override
	public void init(Diagram diagram, IDiagramBehavior diagramEditor) {
		super.init(diagram, diagramEditor);

		IProject project = ((KommaDiagramEditor) diagramEditor
				.getDiagramContainer()).getProject();
		injector = Guice.createInjector(new KommaDiagramModule(project, this));
		setFeatureProvider(injector.getInstance(IFeatureProvider.class));
		platformAdapterFactory = new IAdapterFactory() {
			@Override
			public @SuppressWarnings("rawtypes")
			Class[] getAdapterList() {
				return new Class[] { IValue.class, IReference.class };
			}

			@Override
			public Object getAdapter(Object adaptableObject,
					@SuppressWarnings("rawtypes") Class adapterType) {
				PictogramElement element = null;
				if (adaptableObject instanceof PictogramElement) {
					element = (PictogramElement) adaptableObject;
				} else if (adaptableObject instanceof EditPart
						&& ((EditPart) adaptableObject).getModel() instanceof PictogramElement) {
					element = (PictogramElement) ((EditPart) adaptableObject)
							.getModel();
				}
				return element == null ? null : getFeatureProvider()
						.getBusinessObjectForPictogramElement(element);
			}
		};
		Platform.getAdapterManager().registerAdapters(platformAdapterFactory,
				EditPart.class);
		Platform.getAdapterManager().registerAdapters(platformAdapterFactory,
				PictogramElement.class);

		IModelSet modelSet = injector.getInstance(IModelSet.class);
		modelSet.addListener(notificationListener);

		final IDiagramService diagramService = injector
				.getInstance(IDiagramService.class);
		diagramEditor.getEditingDomain().addResourceSetListener(
				new ResourceSetListenerImpl() {
					LayoutSynchronizer layoutSynchronizer = new LayoutSynchronizer(
							getLayoutModel());
					{
						injector.injectMembers(layoutSynchronizer);
					}

					EStructuralFeature startFeature = PictogramsPackage.eINSTANCE
							.getConnection_Start(),
							endFeature = PictogramsPackage.eINSTANCE
									.getConnection_End(),
							containerFeature = PictogramsPackage.eINSTANCE
									.getShape_Container(),
							parentFeature = PictogramsPackage.eINSTANCE
									.getAnchor_Parent();

					Object getBusinessObject(PictogramElement pe) {
						Object bo = getFeatureProvider()
								.getBusinessObjectForPictogramElement(pe);
						// restrict business objects to RDF resources or
						// statements
						return bo instanceof IReference
								|| bo instanceof IStatement ? bo : null;
					}

					class ConnectionInfo {
						PictogramElement start, end;
					}

					@Override
					public void resourceSetChanged(ResourceSetChangeEvent event) {
						Set<PictogramElement> deletedShapes = new HashSet<>();
						Map<Connection, ConnectionInfo> deletedConnections = new HashMap<>();
						final Map<PictogramElement, PictogramElement> parentMap = new HashMap<>();
						for (Notification notification : event
								.getNotifications()) {
							Object notifier = notification.getNotifier();
							if (!(notifier instanceof EObject)) {
								continue;
							}

							EObject eo = (EObject) notifier;
							if (!(notification.getFeature() instanceof EStructuralFeature)) {
								continue;
							}
							EStructuralFeature feature = (EStructuralFeature) notification
									.getFeature();

							if (eo.eResource() == null) {
								if (eo instanceof Connection) {
									ConnectionInfo conn = deletedConnections
											.get(eo);
									if (conn == null) {
										conn = new ConnectionInfo();
										deletedConnections.put((Connection) eo,
												conn);
									}
									if (startFeature.equals(feature)) {
										conn.start = (PictogramElement) notification
												.getOldValue();
									} else if (endFeature.equals(feature)) {
										conn.end = (PictogramElement) notification
												.getOldValue();
									}
								} else if (eo instanceof PictogramElement
										&& containerFeature.equals(feature)) {
									parentMap.put((PictogramElement) eo,
											(PictogramElement) notification
													.getOldValue());
									deletedShapes.add((PictogramElement) eo);
								} else if (eo instanceof Anchor
										&& parentFeature.equals(feature)) {
									parentMap.put((Anchor) eo,
											(PictogramElement) notification
													.getOldValue());
								}
								continue;
							}

							if (eo instanceof GraphicsAlgorithm
									|| eo instanceof Connection) {
								PictogramElement pe = null;
								if (eo instanceof Connection) {
									pe = (Connection) eo;
									if (((Connection) pe).getStart() == null
											|| ((Connection) pe).getEnd() == null) {
										continue;
									}
								} else if (eo instanceof GraphicsAlgorithm) {
									GraphicsAlgorithm ga = (GraphicsAlgorithm) eo;
									// do only use top-most graphics algorithm
									if (ga.getParentGraphicsAlgorithm() != null) {
										continue;
									}
									pe = ga.getPictogramElement();
								}
								if (pe != null) {
									// unmapped property
									if (!layoutSynchronizer
											.isRelevantFeature(feature)) {
										continue;
									}

									Object bo = getBusinessObject(pe);
									// only synchronize pictogram elements with
									// business objects
									if (bo == null) {
										continue;
									}

									// System.out.println("changed: " + pe
									// + "\n\tbo: " + bo + "\n\tga: "
									// + pe.getGraphicsAlgorithm()
									// + "\n\tfeature: "
									// + notification.getFeature()
									// + "\n\tvalue: "
									// + notification.getNewValue());

									layoutSynchronizer.addForUpdate(pe,
											feature, bo);
								}
							}
						}

						// schedule pictogram elements for deletion
						for (PictogramElement pe : deletedShapes) {
							Object bo = diagramService
									.getBusinessObjectForPictogramElement(pe);
							if (bo instanceof IReference) {
								layoutSynchronizer.addForDeletion(pe);
							}
						}

						for (Map.Entry<Connection, ConnectionInfo> entry : deletedConnections
								.entrySet()) {
							ConnectionInfo conn = entry.getValue();
							layoutSynchronizer.addForDeletion(entry.getKey(),
									conn.start, conn.end);
						}

						if (layoutSynchronizer.hasUpdates()) {
							new UIJob("Update layout") {
								@Override
								public IStatus runInUIThread(
										IProgressMonitor monitor) {
									try {
										return getEditingDomain()
												.getCommandStack().execute(
														new SimpleCommand() {

															@Override
															protected CommandResult doExecuteWithResult(
																	IProgressMonitor progressMonitor,
																	IAdaptable info)
																	throws ExecutionException {
																layoutSynchronizer
																		.update(parentMap,
																				progressMonitor);
																return CommandResult
																		.newOKCommandResult();
															}
														}, null, null);
									} catch (ExecutionException e) {
										return new Status(IStatus.ERROR,
												KommaGraphitiPlugin.PLUGIN_ID,
												"Error while updating layout",
												e);
									}
								}
							}.schedule();
						}
					}

					@Override
					public boolean isPostcommitOnly() {
						return true;
					}
				});
	}

	@Override
	public IGraphicsAlgorithmRendererFactory getGraphicsAlgorithmRendererFactory() {
		return injector.getInstance(IGraphicsAlgorithmRendererFactory.class);
	}

	@Override
	public IToolBehaviorProvider[] getAvailableToolBehaviorProviders() {
		return injector.getInstance(new Key<Set<IToolBehaviorProvider>>() {
		}).toArray(new IToolBehaviorProvider[0]);
	}

	public IModel getModel() {
		return injector.getInstance(IModel.class);
	}

	public IModel getLayoutModel() {
		return injector
				.getInstance(Key.get(IModel.class, Names.named("layout")));
	}

	@Override
	public void dispose() {
		if (platformAdapterFactory != null) {
			Platform.getAdapterManager().unregisterAdapters(
					platformAdapterFactory);
			platformAdapterFactory = null;
		}

		super.dispose();

		IModelSet modelSet = injector.getInstance(IModelSet.class);
		modelSet.removeListener(notificationListener);

		KommaEditorSupport<?> editorSupport = injector
				.getInstance(new Key<KommaEditorSupport<KommaDiagramEditor>>() {
				});
		editorSupport.dispose();

		// remove reference to shared model set
		ProjectModelSetManager modelSetManager = injector
				.getInstance(ProjectModelSetManager.class);
		modelSetManager.removeClient(this);
	}

	@Override
	public IEditingDomain getEditingDomain() {
		return injector.getInstance(IEditingDomainProvider.class)
				.getEditingDomain();
	}

	@Override
	public boolean isAutoUpdateAtStartup() {
		// required for automatic diagram generation
		return true;
	}

}
