package net.enilink.komma.graphiti;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.ConfigurableFeatureProviderWrapper;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.platform.ga.IGraphicsAlgorithmRendererFactory;
import org.eclipse.graphiti.tb.IToolBehaviorProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryProvider;
import com.google.inject.multibindings.Multibinder;

import net.enilink.vocab.systems.SYSTEMS;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.editor.KommaEditorSupport;
import net.enilink.komma.edit.ui.views.IViewerMenuSupport;
import net.enilink.komma.graphiti.features.create.CreateNodeFeature;
import net.enilink.komma.graphiti.features.create.CreateNodeFeatureFactory;
import net.enilink.komma.graphiti.features.create.IURIFactory;
import net.enilink.komma.graphiti.model.ModelSetManager;
import net.enilink.komma.graphiti.service.DiagramService;
import net.enilink.komma.graphiti.service.IDiagramService;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

public class SystemDiagramModule extends AbstractModule {
	private static class URIFactory implements IURIFactory {
		@Inject
		IModel model;

		@Override
		public synchronized URI createURI() {
			return model.getURI().namespace()
					.appendFragment("object_" + UUID.randomUUID().toString());
		}
	}

	ModelSetManager modelSetManager;
	IDiagramTypeProvider typeProvider;

	public static class EditorSupport extends
			KommaEditorSupport<SystemDiagramEditor> implements
			IViewerMenuSupport {
		IModelSet modelSet;

		@Inject
		public EditorSupport(SystemDiagramEditor editor) {
			super(editor);
		}

		@Override
		protected IResourceLocator getResourceLocator() {
			return KommaGraphitiPlugin.INSTANCE;
		}

		@Inject
		public void setModelSetAndEditingDomain(IModelSet modelSet,
				IEditingDomainProvider editingDomainProvider) {
			this.modelSet = modelSet;

			initializeEditingDomain();
		}

		@Override
		protected IModelSet createModelSet() {
			return modelSet;
		}
	}

	public SystemDiagramModule(IDiagramTypeProvider typeProvider) {
		this.typeProvider = typeProvider;
	}

	@Override
	protected void configure() {
		modelSetManager = new ModelSetManager();

		bind(IDiagramTypeProvider.class).toInstance(typeProvider);
		bind(IGraphicsAlgorithmRendererFactory.class).to(
				SystemGraphicsAlgorithmRendererFactory.class).in(
				Singleton.class);
		Multibinder<IToolBehaviorProvider> toolBehaviors = Multibinder
				.newSetBinder(binder(), IToolBehaviorProvider.class);
		toolBehaviors.addBinding().to(SystemToolBehaviorProvider.class)
				.in(Singleton.class);

		bind(CreateNodeFeatureFactory.class).toProvider(
				FactoryProvider.newFactory(CreateNodeFeatureFactory.class,
						CreateNodeFeature.class));

		bind(IURIFactory.class).to(URIFactory.class).in(Singleton.class);

		bind(EditorSupport.class).in(Singleton.class);
		bind(new TypeLiteral<KommaEditorSupport<SystemDiagramEditor>>() {
		}).to(EditorSupport.class);
		bind(IViewerMenuSupport.class).to(EditorSupport.class);
		bind(IDiagramService.class).to(DiagramService.class);
	}

	@Provides
	protected IProxyService provideProxyService() {
		BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass())
				.getBundleContext();
		ServiceReference serviceRef = bundleContext
				.getServiceReference(IProxyService.class.getName());
		if (serviceRef != null) {
			return (IProxyService) bundleContext.getService(serviceRef);
		}
		return null;
	}

	@Provides
	@Singleton
	protected IModelSet provideModelSet(IDiagramTypeProvider diagramTypeProvider) {
		org.eclipse.emf.common.util.URI uri = diagramTypeProvider.getDiagram()
				.eResource().getURI();
		IProject project = null;
		if (uri.isPlatform()) {
			IResource resource = ResourcesPlugin.getWorkspace().getRoot()
					.findMember(uri.toPlatformString(true));
			if (resource != null) {
				project = resource.getProject();
			}
		}

		return modelSetManager.createModelSet(project);
	}

	@Provides
	protected IDiagramEditorExt provideDiagramEditor(
			IDiagramTypeProvider diagramTypeProvider) {
		return (IDiagramEditorExt) diagramTypeProvider.getDiagramEditor();
	}

	@Provides
	protected SystemDiagramEditor provideSystemDiagramEditor(
			IDiagramTypeProvider diagramTypeProvider) {
		return (SystemDiagramEditor) diagramTypeProvider.getDiagramEditor();
	}

	@Provides
	@Singleton
	protected IModel provideModel(IModelSet modelSet,
			IDiagramTypeProvider diagramTypeProvider) {
		URI modelUri = URIImpl.createURI(diagramTypeProvider.getDiagram()
				.eResource().getURI().appendFileExtension("owl").toString());

		IModel model = modelSet.createModel(modelUri);
		Map<Object, Object> options = new HashMap<Object, Object>();
		if (modelSet.getURIConverter().exists(modelUri, options)) {
			try {
				model.load(options);
			} catch (IOException e) {
				KommaGraphitiPlugin.INSTANCE.log(e);
			}
		}

		model.addImport(SYSTEMS.NAMESPACE_URI.trimFragment(), "systems");
		return model;
	}

	@Provides
	@Singleton
	protected IEditingDomainProvider provideEditingDomainProvider(
			IModelSet modelSet) {
		return (IEditingDomainProvider) modelSet.adapters().getAdapter(
				IEditingDomainProvider.class);
	}

	@Provides
	@Singleton
	protected IFeatureProvider provideFeatureProvider(
			SystemDiagramFeatureProvider systemFeatureProvider) {
		return new ConfigurableFeatureProviderWrapper(systemFeatureProvider);
	}

	@Provides
	protected IAdapterFactory provideAdapterFactory(
			IEditingDomainProvider editingDomainProvider) {
		return ((AdapterFactoryEditingDomain) editingDomainProvider
				.getEditingDomain()).getAdapterFactory();
	}
};