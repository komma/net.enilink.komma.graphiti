package net.enilink.komma.graphiti;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.Platform;
import org.eclipse.gef.EditPart;
import org.eclipse.graphiti.dt.AbstractDiagramTypeProvider;
import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.platform.IDiagramEditor;
import org.eclipse.graphiti.platform.ga.IGraphicsAlgorithmRendererFactory;
import org.eclipse.graphiti.tb.IToolBehaviorProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;

import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.INotificationListener;
import net.enilink.komma.common.notify.NotificationFilter;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.editor.KommaEditorSupport;
import net.enilink.komma.graphiti.model.ModelSetManager;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.core.IReference;
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
				PictogramElement element = featureProvider
						.getPictogramElementForBusinessObject(notification
								.getSubject());
				if (element != null) {
					if (elements == null) {
						elements = new HashSet<PictogramElement>();
					}
					elements.add(element);
				}

				// if (notification instanceof IStatementNotification) {
				// IStatementNotification stmtNotification =
				// (IStatementNotification) notification;
				// }
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
					IDiagramTypeProvider diagramTypeProvider, ModelSetManager modelSetManager) {
				IModelSet sharedModelSet = getSharedModelSet();
				if (sharedModelSet != null) {
					return sharedModelSet;
				}
				return super.provideModelSet(diagramTypeProvider, modelSetManager);
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
}
