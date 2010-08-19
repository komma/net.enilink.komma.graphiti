package net.enilink.komma.graphiti.model;

import java.net.URL;
import java.util.Collection;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.command.EditingDomainCommandStack;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
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

public class ModelSetManager {
	private IModelSet modelSet;
	private ComposedAdapterFactory ownedAdapterFactory;

	protected IModelSet createModelSet() {
		KommaModule module = ModelCore.createModelSetModule(getClass()
				.getClassLoader());
		// module.addBehaviour(SessionModelSetSupport.class, MODELS.NAMESPACE
		// + "OwlimModelSet");

		IModelSet modelSet = new ModelSetFactory(module,
				URIImpl.createURI(MODELS.NAMESPACE +
				// "MemoryModelSet" //
						"OwlimModelSet" //
				)).createModelSet();

		for (URL url : KommaUtil
				.getBundleMetaInfLocations("net.enilink.vocab.systems")) {
			modelSet.getModule().addLibrary(url);
		}
		return modelSet;
	}

	public synchronized IModelSet getModelSet() {
		if (modelSet == null) {
			modelSet = createModelSet();
			initializeEditingDomain();
		}
		return modelSet;
	}

	protected void initializeEditingDomain() {
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

		// Create the command stack that will notify this editor as commands
		// are executed.
		EditingDomainCommandStack commandStack = new EditingDomainCommandStack();

		AdapterFactoryEditingDomain editingDomain = new AdapterFactoryEditingDomain(
				ownedAdapterFactory, commandStack, modelSet);
		commandStack.setEditingDomain(editingDomain);
		editingDomain
				.setModelToReadOnlyMap(new java.util.WeakHashMap<IModel, Boolean>());
	}

	public IEditingDomainProvider getEditingDomainProvider() {
		return (IEditingDomainProvider) getModelSet().adapters().getAdapter(
				IEditingDomainProvider.class);
	}
}
