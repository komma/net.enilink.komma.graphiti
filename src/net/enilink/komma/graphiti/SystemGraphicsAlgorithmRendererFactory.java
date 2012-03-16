package net.enilink.komma.graphiti;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.platform.ga.IGraphicsAlgorithmRenderer;
import org.eclipse.graphiti.platform.ga.IGraphicsAlgorithmRendererFactory;
import org.eclipse.graphiti.platform.ga.IRendererContext;

import com.google.inject.Inject;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.graphiti.graphical.IGraphitiProvider;
import net.enilink.komma.graphiti.graphical.NodeFigure;
import net.enilink.komma.graphiti.service.IDiagramService;

public class SystemGraphicsAlgorithmRendererFactory implements
		IGraphicsAlgorithmRendererFactory {
	public final static String NODE_FIGURE = "net.enilink.komma.graphiti.Node";

	@Inject
	IFeatureProvider fp;

	@Inject
	IAdapterFactory adapterFactory;

	@Inject
	IDiagramService diagramService;

	@Inject(optional = true)
	IProxyService proxyService;

	@Override
	public IGraphicsAlgorithmRenderer createGraphicsAlgorithmRenderer(
			IRendererContext rendererContext) {
		IGraphicsAlgorithmRenderer renderer = null;
		if (NODE_FIGURE.equals(rendererContext.getPlatformGraphicsAlgorithm()
				.getId())) {
			renderer = createNodeFigure(rendererContext.getGraphicsAlgorithm());
		}
		return renderer;
	}

	private NodeFigure createNodeFigure(GraphicsAlgorithm ga) {
		URL url = null;

		Object bo = diagramService.getFirstBusinessObject(ga);
		if (bo instanceof IResource) {
			// get an IGraphitiNodeFigureProvider from the adapter factory
			IGraphitiProvider graphitiProvider = (IGraphitiProvider) adapterFactory
					.adapt(bo, IGraphitiProvider.class);

			// return if graphitiProvider exists and returns a node figure
			if (graphitiProvider != null) {
				NodeFigure figure = graphitiProvider.getNodeFigure((IResource) bo);
				if (figure != null)
					return figure;
			}

			// otherwise try to find image annotation in the objects classes
			Set<Object> seen = new HashSet<Object>();
			Queue<IClass> queue = new LinkedList<IClass>(((IResource) bo)
					.getDirectNamedClasses().toList());

			java.net.URI imageURI = null;
			while (!queue.isEmpty()) {
				IClass type = queue.remove();
				if (seen.add(type)) {
					imageURI = type.getImage();
					if (imageURI != null) {
						break;
					}

					queue.addAll(type.getDirectNamedSuperClasses().toList());
				}
			}

			if (imageURI != null) {
				// FIXME: remove proxy settings here
				if (proxyService != null) {
					IProxyData[] proxyDataForHost = proxyService
							.select(imageURI);

					for (IProxyData data : proxyDataForHost) {
						if (data.getHost() != null) {
							System.setProperty("http.proxySet", "true");
							System.setProperty("http.proxyHost", data.getHost());
						}
						if (data.getHost() != null) {
							System.setProperty("http.proxyPort",
									String.valueOf(data.getPort()));
						}
					}
				}

				try {
					url = imageURI.toURL();
				} catch (MalformedURLException e) {
					// ignore
				}
			}
		}

		if (url == null) {
			url = FileLocator.find(KommaGraphitiPlugin.getPlugin().getBundle(),
					new Path("/figures/node.svg"), null);
		}

		return new NodeFigure(url);
	}
}
