package net.enilink.komma.graphiti.model;

import java.net.URL;
import java.util.Collection;

import org.eclipse.core.resources.IProject;

import com.google.inject.Inject;
import com.google.inject.Injector;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.command.EditingDomainCommandStack;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.provider.ComposedAdapterFactory;
import net.enilink.komma.edit.provider.ReflectiveItemProviderAdapterFactory;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelCore;
import net.enilink.komma.model.base.ModelSetFactory;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.util.KommaUtil;
import net.enilink.komma.workbench.IProjectModelSet;
import net.enilink.komma.workbench.ProjectModelSetSupport;

public class ModelSetManager {
	private ComposedAdapterFactory ownedAdapterFactory;

	public IModelSet createModelSet(IProject project) {
		KommaModule module = ModelCore.createModelSetModule(getClass()
				.getClassLoader());
		module.addConcept(IProjectModelSet.class);
		module.addBehaviour(ProjectModelSetSupport.class);

		IModelSet modelSet = new ModelSetFactory(module,
				URIImpl.createURI(MODELS.NAMESPACE +
				// "MemoryModelSet" //
						"OwlimModelSet" //
						// "AGModelSet" //
				), URIImpl.createURI(MODELS.NAMESPACE + "ProjectModelSet") //
		).createModelSet();

		if (modelSet instanceof IProjectModelSet && project != null) {
			((IProjectModelSet) modelSet).setProject(project);
		}

		for (URL url : KommaUtil
				.getBundleMetaInfLocations("net.enilink.vocab.systems")) {
			modelSet.getModule().addLibrary(url);
		}

		for (URL url : KommaUtil
				.getBundleMetaInfLocations("net.enilink.modeling.bpmn2")) {
			modelSet.getModule().addLibrary(url);
		}

		initializeEditingDomain(modelSet);
		return modelSet;
	}

	@Inject
	protected Injector injector;

	protected void initializeEditingDomain(IModelSet modelSet) {
		// Create an adapter factory that yields item providers.
		ownedAdapterFactory = new ComposedAdapterFactory(
				ComposedAdapterFactory.IDescriptor.IRegistry.INSTANCE) {
			/**
			 * Default adapter factory for all namespaces
			 */
			class DefaultItemProviderAdapterFactory extends
					ReflectiveItemProviderAdapterFactory {
				public DefaultItemProviderAdapterFactory() {
					super(KommaEditPlugin.getPlugin());
				}

				@Override
				public Object adapt(Object object, Object type) {
					if (object instanceof IClass) {
						// do not override the adapter for classes
						return null;
					}
					return super.adapt(object, type);
				}

				@Override
				public boolean isFactoryForType(Object type) {
					return type instanceof URI || supportedTypes.contains(type);
				}
			}

			DefaultItemProviderAdapterFactory defaultAdapterFactory;
			{
				defaultAdapterFactory = new DefaultItemProviderAdapterFactory();
				defaultAdapterFactory.setParentAdapterFactory(this);
			}

			@Override
			protected IAdapterFactory delegatedGetFactoryForTypes(
					Collection<?> types) {
				// provide a default adapter factory as fallback if no
				// specific adapter factory was found
				return defaultAdapterFactory;
			}
		};

		if (injector != null) {
			injector.injectMembers(ownedAdapterFactory);
		}

		// Create the command stack that will notify this editor as commands
		// are executed.
		EditingDomainCommandStack commandStack = new EditingDomainCommandStack();

		AdapterFactoryEditingDomain editingDomain = new AdapterFactoryEditingDomain(
				ownedAdapterFactory, commandStack, modelSet);
		commandStack.setEditingDomain(editingDomain);
		editingDomain
				.setModelToReadOnlyMap(new java.util.WeakHashMap<IModel, Boolean>());
	}
}
