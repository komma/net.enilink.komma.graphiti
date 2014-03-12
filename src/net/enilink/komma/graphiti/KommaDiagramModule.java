package net.enilink.komma.graphiti;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.editor.KommaEditorSupport;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.edit.ui.rcp.project.ProjectModelSetManager;
import net.enilink.komma.edit.ui.views.IViewerMenuSupport;
import net.enilink.komma.graphiti.features.create.CreateNodeFeature;
import net.enilink.komma.graphiti.features.create.CreateNodeFeatureFactory;
import net.enilink.komma.graphiti.features.create.IURIFactory;
import net.enilink.komma.graphiti.service.DiagramService;
import net.enilink.komma.graphiti.service.IDiagramService;
import net.enilink.komma.graphiti.service.ITypes;
import net.enilink.komma.graphiti.service.Types;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.vocab.visualization.layout.LAYOUT;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.ConfigurableFeatureProviderWrapper;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.impl.IIndependenceSolver;
import org.eclipse.graphiti.platform.IDiagramBehavior;
import org.eclipse.graphiti.platform.ga.IGraphicsAlgorithmRendererFactory;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaCreateService;
import org.eclipse.graphiti.services.IGaLayoutService;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;
import org.eclipse.graphiti.services.IPeLayoutService;
import org.eclipse.graphiti.services.IPeService;
import org.eclipse.graphiti.tb.IToolBehaviorProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;

public class KommaDiagramModule extends AbstractModule {
	private static class URIFactory implements IURIFactory {
		@Inject
		IModel model;

		@Override
		public synchronized URI createURI() {
			return model.getURI().appendLocalPart(
					"object_" + UUID.randomUUID().toString());
		}
	}

	final IProject project;
	final IDiagramTypeProvider typeProvider;

	public static class EditorSupport extends
			KommaEditorSupport<KommaDiagramEditor> implements
			IViewerMenuSupport {
		{
			disposeModelSet = false;
		}
		IModelSet modelSet;

		@Inject
		public EditorSupport(KommaDiagramEditor editor) {
			super(editor);
		}

		@Override
		public void createContextMenuFor(StructuredViewer viewer) {
			super.createContextMenuFor(viewer);
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

	public KommaDiagramModule(IProject project,
			IDiagramTypeProvider typeProvider) {
		this.project = project;
		this.typeProvider = typeProvider;
	}

	@Override
	protected void configure() {
		bind(IDiagramTypeProvider.class).toInstance(typeProvider);
		bind(IGraphicsAlgorithmRendererFactory.class).to(
				GraphicsAlgorithmRendererFactory.class).in(Singleton.class);
		Multibinder<IToolBehaviorProvider> toolBehaviors = Multibinder
				.newSetBinder(binder(), IToolBehaviorProvider.class);
		toolBehaviors.addBinding().to(ToolBehaviorProvider.class)
				.in(Singleton.class);

		install(new FactoryModuleBuilder().implement(CreateNodeFeature.class,
				CreateNodeFeature.class).build(CreateNodeFeatureFactory.class));

		// bind(CreateNodeFeatureFactory.class).toProvider(
		// FactoryProvider.newFactory(CreateNodeFeatureFactory.class,
		// CreateNodeFeature.class));

		bind(IURIFactory.class).to(URIFactory.class).in(Singleton.class);

		bind(KommaDiagramEditor.class).toInstance(
				(KommaDiagramEditor) typeProvider.getDiagramBehavior()
						.getDiagramContainer());
		bind(EditorSupport.class).in(Singleton.class);
		bind(new TypeLiteral<KommaEditorSupport<KommaDiagramEditor>>() {
		}).to(EditorSupport.class);
		bind(IViewerMenuSupport.class).to(EditorSupport.class);
		bind(IDiagramService.class).to(DiagramService.class);

		bind(ITypes.class).to(Types.class);

		// Graphiti services
		bind(IGaService.class).toInstance(Graphiti.getGaService());
		bind(IGaCreateService.class).toInstance(Graphiti.getGaCreateService());
		bind(IGaLayoutService.class).toInstance(Graphiti.getGaLayoutService());

		bind(IPeService.class).toInstance(Graphiti.getPeService());
		bind(IPeCreateService.class).toInstance(Graphiti.getPeCreateService());
		bind(IPeLayoutService.class).toInstance(Graphiti.getPeLayoutService());
	}

	@Provides
	protected IProxyService provideProxyService() {
		BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass())
				.getBundleContext();
		ServiceReference<IProxyService> serviceRef = bundleContext
				.getServiceReference(IProxyService.class);
		if (serviceRef != null) {
			return (IProxyService) bundleContext.getService(serviceRef);
		}
		return null;
	}

	@Provides
	protected ProjectModelSetManager provideModelSetManager(
			IDiagramTypeProvider diagramTypeProvider) {
		ProjectModelSetManager modelSetManager = ProjectModelSetManager
				.getSharedInstance(project);
		modelSetManager.addClient(diagramTypeProvider);
		return modelSetManager;
	}

	@Provides
	@Singleton
	protected IModelSet provideModelSet(ProjectModelSetManager modelSetManager) {
		return modelSetManager.getModelSet();
	}

	@Provides
	protected IDiagramBehavior provideDiagramBehavior(
			IDiagramTypeProvider diagramTypeProvider) {
		return diagramTypeProvider.getDiagramBehavior();
	}

	@Provides
	@Singleton
	protected IModel provideModel(IModelSet modelSet,
			IDiagramTypeProvider diagramTypeProvider) {
		org.eclipse.emf.common.util.URI diaURI = diagramTypeProvider
				.getDiagram().eResource().getURI();
		String ext = diaURI.fileExtension();
		if (ext != null) {
			ext = ext.replace("dia", "");
		}
		if (ext == null || "emf".equals(ext)) {
			ext = "owl";
		}
		URI modelUri = URIs.createURI(diaURI.trimFileExtension()
				.appendFileExtension(ext).toString());
		return loadModel(modelSet, modelUri);
	}

	@Provides
	@Singleton
	@Named("layout")
	protected IModel provideLayoutModel(IModelSet modelSet,
			IDiagramTypeProvider diagramTypeProvider) {
		URI modelUri = URIs.createURI(diagramTypeProvider.getDiagram()
				.eResource().getURI().toString());
		IModel model = loadModel(modelSet, modelUri);
		model.addImport(LAYOUT.NAMESPACE_URI.trimFragment(), "layout");
		return model;
	}

	protected IModel loadModel(IModelSet modelSet, URI modelUri) {
		IModel model = modelSet.getModel(modelUri, false);
		if (model == null) {
			model = modelSet.createModel(modelUri);
		}
		if (!model.isLoaded()) {
			Map<Object, Object> options = new HashMap<Object, Object>();
			String ext = modelUri.fileExtension();
			if (ext != null) {
				IContentType contentType = Platform
						.getContentTypeManager()
						.findContentTypeFor("example." + ext.replace("dia", ""));
				if (contentType != null) {
					options.put(IModel.OPTION_CONTENT_DESCRIPTION,
							contentType.getDefaultDescription());
				}
			}
			if (modelSet.getURIConverter().exists(modelUri, options)) {
				try {
					model.load(options);
				} catch (IOException e) {
					KommaGraphitiPlugin.INSTANCE.log(e);
				}
			}
		}
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
	protected IFeatureProvider provideFeatureProvider(Injector injector) {
		return new ConfigurableFeatureProviderWrapper(
				injector.getInstance(KommaDiagramFeatureProvider.class));
	}

	@Provides
	@Singleton
	protected ILabelProvider provideLabelProvider(IAdapterFactory adapterFactory) {
		return new AdapterFactoryLabelProvider(adapterFactory);
	}

	@Provides
	protected IAdapterFactory provideAdapterFactory(
			IEditingDomainProvider editingDomainProvider) {
		return ((AdapterFactoryEditingDomain) editingDomainProvider
				.getEditingDomain()).getAdapterFactory();
	}

	@Provides
	@Singleton
	protected IIndependenceSolver provideIndependenceSolver(final IModel model) {
		return new IIndependenceSolver() {
			@Override
			public String getKeyForBusinessObject(Object bo) {
				if (bo instanceof IStatement) {
					IStatement stmt = (IStatement) bo;
					return "[" + getKeyForBusinessObject(stmt.getSubject())
							+ ","
							+ getKeyForBusinessObject(stmt.getPredicate())
							+ "," + getKeyForBusinessObject(stmt.getObject())
							+ "]";
				} else if (bo instanceof IReference) {
					URI uri = ((IReference) bo).getURI();
					if (uri != null) {
						return uri.toString();
					}
				}
				// unknown object
				return null;

			}

			@Override
			public Object getBusinessObjectForKey(String key) {
				if (key.startsWith("[")) {
					Matcher matcher = Pattern.compile("\\[(.*),(.*),(.*)\\]")
							.matcher(key);
					if (matcher.matches()) {
						return new Statement(URIs.createURI(matcher.group(1)),
								URIs.createURI(matcher.group(2)),
								URIs.createURI(matcher.group(3)));
					}
					return null;
				}
				try {
					URI uri = URIs.createURI(key);
					return model.resolve(uri);
				} catch (Exception e) {
					return null;
				}
			}
		};
	}
};