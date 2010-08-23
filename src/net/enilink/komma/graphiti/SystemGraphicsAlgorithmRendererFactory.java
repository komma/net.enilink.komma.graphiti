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

import net.enilink.komma.concepts.IClass;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.graphiti.graphical.NodelFigure;
import net.enilink.komma.graphiti.service.IDiagramService;

public class SystemGraphicsAlgorithmRendererFactory implements
		IGraphicsAlgorithmRendererFactory {
	public final static String NODE_FIGURE = "net.enilink.komma.graphiti.Node";

	@Inject
	IFeatureProvider fp;

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

	private NodelFigure createNodeFigure(GraphicsAlgorithm ga) {
		URL url = null;

		Object bo = diagramService.getRootBusinessObject(ga);
		if (bo instanceof IResource) {
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

		return new NodelFigure(url);
	}
}
