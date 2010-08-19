package net.enilink.komma.graphiti;

import net.enilink.komma.common.AbstractKommaPlugin;
import net.enilink.komma.common.ui.EclipseUIPlugin;
import net.enilink.komma.common.util.IResourceLocator;

/**
 * The activator class controls the plug-in life cycle
 */
public class KommaGraphitiPlugin extends AbstractKommaPlugin {
	public static final String PLUGIN_ID = "net.enilink.komma.graphiti";

	/**
	 * The singleton instance of the plugin.
	 */
	public static final KommaGraphitiPlugin INSTANCE = new KommaGraphitiPlugin();

	/**
	 * The one instance of this class.
	 */
	private static Implementation plugin;

	/**
	 * Creates the singleton instance.
	 */
	private KommaGraphitiPlugin() {
		super(new IResourceLocator[] {});
	}

	/*
	 * Javadoc copied from base class.
	 */
	@Override
	public IResourceLocator getBundleResourceLocator() {
		return plugin;
	}

	/**
	 * Returns the singleton instance of the Eclipse plugin.
	 * 
	 * @return the singleton instance.
	 */
	public static Implementation getPlugin() {
		return plugin;
	}

	/**
	 * The actual implementation of the Eclipse <b>Plugin</b>.
	 */
	public static class Implementation extends EclipseUIPlugin {
		/**
		 * Creates an instance.
		 */
		public Implementation() {
			super();

			// Remember the static instance.
			//
			plugin = this;
		}
	}
}
