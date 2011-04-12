package net.enilink.komma.graphiti;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.ExecutionException;
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
import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.platform.IDiagramEditor;
import org.eclipse.graphiti.platform.ga.IGraphicsAlgorithmRendererFactory;
import org.eclipse.graphiti.tb.IToolBehaviorProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import net.enilink.komma.KommaCore;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.INotificationListener;
import net.enilink.komma.common.notify.NotificationFilter;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.editor.KommaEditorSupport;
import net.enilink.komma.graphiti.layout.LayoutSynchronizer;
import net.enilink.komma.graphiti.model.ModelSetManager;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

public class SystemDiagramTypeProvider extends AbstractDiagramTypeProvider
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
	public void init(Diagram diagram, IDiagramEditor diagramEditor) {
		super.init(diagram, diagramEditor);

		injector = Guice.createInjector(new SystemDiagramModule(this) {
			protected IModelSet provideModelSet(
					IDiagramTypeProvider diagramTypeProvider,
					ModelSetManager modelSetManager) {
				IModelSet sharedModelSet = getSharedModelSet();
				if (sharedModelSet != null) {
					return sharedModelSet;
				}
				return super.provideModelSet(diagramTypeProvider,
						modelSetManager);
			}

		});

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

		diagramEditor.getEditingDomain().addResourceSetListener(
				new ResourceSetListenerImpl() {
					LayoutSynchronizer layoutSynchronizer = new LayoutSynchronizer(
							getLayoutModel());
					{
						injector.injectMembers(layoutSynchronizer);
					}

					@Override
					public void resourceSetChanged(ResourceSetChangeEvent event) {
						for (Notification notification : event
								.getNotifications()) {
							Object notifier = notification.getNotifier();
							if (!(notifier instanceof EObject)) {
								continue;
							}

							EObject eo = (EObject) notifier;
							// do not work with dangling objects
							if (eo == null || eo.eResource() == null) {
								continue;
							}

							if (!(notification.getFeature() instanceof EStructuralFeature)) {
								continue;
							}
							EStructuralFeature feature = (EStructuralFeature) notification
									.getFeature();

							if (eo instanceof GraphicsAlgorithm
									|| eo instanceof Connection) {
								PictogramElement pe;
								if (eo instanceof Connection) {
									pe = (Connection) eo;
									if (((Connection) pe).getStart() == null
											|| ((Connection) pe).getEnd() == null) {
										continue;
									}
								} else {
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

									Object bo = getFeatureProvider()
											.getBusinessObjectForPictogramElement(
													pe);
									// only synchronize pictogram elements with
									// business objects
									if (bo == null
											|| !(bo instanceof IReference || bo instanceof IStatement)) {
										continue;
									}

									System.out.println("changed: " + pe
											+ "\n\tbo: " + bo + "\n\tga: "
											+ pe.getGraphicsAlgorithm()
											+ "\n\tfeature: "
											+ notification.getFeature()
											+ "\n\tvalue: "
											+ notification.getNewValue());

									layoutSynchronizer.addForUpdate(pe,
											feature, bo);
								}
							}
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
																		.update(progressMonitor);
																return CommandResult
																		.newOKCommandResult();
															}
														}, null, null);
									} catch (ExecutionException e) {
										e.printStackTrace();
										return new Status(IStatus.ERROR,
												KommaCore.PLUGIN_ID,
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

	protected IModelSet getSharedModelSet() {
		// try to use existing model set
		URI modelUri = URIImpl.createURI(getDiagram().eResource().getURI()
				.appendFileExtension("owl").toString());

		IWorkbenchPage page = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		if (page != null) {
			for (IEditorReference editorRef : page.getEditorReferences()) {
				IEditorPart editor = editorRef.getEditor(false);
				if (editor instanceof SystemDiagramEditor
						&& editor != getDiagramEditor()) {
					IDiagramTypeProvider typeProvider = ((IDiagramEditor) editor)
							.getDiagramTypeProvider();
					if (typeProvider instanceof SystemDiagramTypeProvider
							&& modelUri
									.equals(((SystemDiagramTypeProvider) typeProvider)
											.getModel().getURI())) {
						return ((SystemDiagramTypeProvider) typeProvider)
								.getModel().getModelSet();
					}
				}
			}
		}
		return null;
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

		IModelSet modelSet = injector.getInstance(IModelSet.class);
		modelSet.removeListener(notificationListener);
		if (getSharedModelSet() == null) {
			// this editor has the only instance of this model set so just
			// dispose it along with the associated editor support
			modelSet.dispose();

			KommaEditorSupport<?> editorSupport = injector
					.getInstance(new Key<KommaEditorSupport<SystemDiagramEditor>>() {
					});
			editorSupport.dispose();
		}

		super.dispose();
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
