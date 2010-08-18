package de.fhg.iwu.komma.graphiti;

import java.util.Set;
import java.util.UUID;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.Platform;
import org.eclipse.gef.EditPart;
import org.eclipse.graphiti.dt.AbstractDiagramTypeProvider;
import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.ConfigurableFeatureProviderWrapper;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.platform.IDiagramEditor;
import org.eclipse.graphiti.platform.ga.IGraphicsAlgorithmRendererFactory;
import org.eclipse.graphiti.tb.IToolBehaviorProvider;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryProvider;
import com.google.inject.multibindings.Multibinder;

import de.fhg.iwu.komma.graphiti.features.create.CreateNodeFeature;
import de.fhg.iwu.komma.graphiti.features.create.CreateNodeFeatureFactory;
import de.fhg.iwu.komma.graphiti.features.create.IURIFactory;
import de.fhg.iwu.komma.graphiti.model.ModelSetManager;
import net.enilink.vocab.systems.SYSTEMS;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

public class SystemDiagramTypeProvider extends AbstractDiagramTypeProvider
		implements IEditingDomainProvider {
	private static class URIFactory implements IURIFactory {
		@Inject
		IModel model;

		@Override
		public synchronized URI createURI() {
			return model.getURI().namespace()
					.appendFragment("object_" + UUID.randomUUID().toString());
		}
	}

	class SystemDiagramModule extends AbstractModule {
		ModelSetManager modelSetManager;

		@Override
		protected void configure() {
			modelSetManager = new ModelSetManager();

			bind(IDiagramTypeProvider.class).toInstance(
					SystemDiagramTypeProvider.this);
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
		}

		@Provides
		@Singleton
		protected IModelSet provideModelSet() {
			return modelSetManager.getModelSet();
		}

		@Provides
		@Singleton
		protected IModel provideModel(IModelSet modelSet) {
			URI modelUri = URIImpl.createURI(getDiagram().eResource().getURI()
					.appendFileExtension("owl").toString());
			IModel model = modelSet.getModel(modelUri, true);
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
				SystemFeatureProvider systemFeatureProvider) {
			return new ConfigurableFeatureProviderWrapper(systemFeatureProvider);
		}
	};

	private Injector injector;
	private IAdapterFactory platformAdapterFactory;

	@Override
	public void init(Diagram diagram, IDiagramEditor diagramEditor) {
		super.init(diagram, diagramEditor);

		injector = Guice.createInjector(new SystemDiagramModule());

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
		Platform.getAdapterManager().unregisterAdapters(platformAdapterFactory);
		platformAdapterFactory = null;

		super.dispose();
	}

	@Override
	public IEditingDomain getEditingDomain() {
		return injector.getInstance(IEditingDomainProvider.class)
				.getEditingDomain();
	}
}
