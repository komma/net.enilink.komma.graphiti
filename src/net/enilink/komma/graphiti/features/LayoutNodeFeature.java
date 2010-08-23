/*******************************************************************************
 * <copyright>
 *
 * Copyright (c) 2005, 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API, implementation and documentation
 *
 * </copyright>
 *
 *******************************************************************************/
package net.enilink.komma.graphiti.features;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import org.eclipse.graphiti.datatypes.IDimension;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ILayoutContext;
import org.eclipse.graphiti.features.impl.AbstractLayoutFeature;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.PlatformGraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;

import com.google.inject.Inject;

import net.enilink.komma.concepts.IResource;
import net.enilink.komma.graphiti.SystemGraphicsAlgorithmRendererFactory;

/**
 * The Class LayoutPoolFeature.
 */
public class LayoutNodeFeature extends AbstractLayoutFeature {
	private static final int MIN_HEIGHT = 40;

	private static final int MIN_WIDTH = 25;

	private static final int IMAGE_PADDING = 3;

	@Inject
	public LayoutNodeFeature(IFeatureProvider fp) {
		super(fp);
	}

	public boolean canLayout(ILayoutContext context) {
		if (!(context.getPictogramElement() instanceof ContainerShape)) {
			return false;
		}

		// return true, if linked business object is a Class
		Object bo = getBusinessObjectForPictogramElement(context
				.getPictogramElement());
		return bo instanceof IResource;
	}

	public boolean layout(ILayoutContext context) {
		boolean changed = false;
		ContainerShape containerShape = (ContainerShape) context
				.getPictogramElement();
		GraphicsAlgorithm containerGa = containerShape.getGraphicsAlgorithm();

		// height
		int containerHeight = containerGa.getHeight();
		if (containerHeight < MIN_HEIGHT) {
			containerGa.setHeight(MIN_HEIGHT);
			changed = true;
		}

		// width
		int containerWidth = containerGa.getWidth();

		if (containerWidth < MIN_WIDTH) {
			containerGa.setWidth(MIN_WIDTH);
			changed = true;
		}

		int textY = containerHeight - 20;

		// resize all child GAs
		Queue<GraphicsAlgorithm> queue = new LinkedList<GraphicsAlgorithm>(
				containerGa.getGraphicsAlgorithmChildren());
		while (!queue.isEmpty()) {
			GraphicsAlgorithm childGa = queue.remove();

			if (childGa instanceof PlatformGraphicsAlgorithm
					&& ((PlatformGraphicsAlgorithm) childGa).getId().equals(
							SystemGraphicsAlgorithmRendererFactory.NODE_FIGURE)) {
				childGa.setX(IMAGE_PADDING);
				childGa.setY(IMAGE_PADDING);

				childGa.setWidth(containerWidth - 2 * IMAGE_PADDING);
				childGa.setHeight(textY - 2 * IMAGE_PADDING);
			} else {
				childGa.setX(0);
				childGa.setY(0);

				childGa.setWidth(containerWidth);
				childGa.setHeight(textY);

				queue.addAll(childGa.getGraphicsAlgorithmChildren());
			}
		}

		Collection<Shape> children = containerShape.getChildren();
		for (Shape shape : children) {
			GraphicsAlgorithm graphicsAlgorithm = shape.getGraphicsAlgorithm();
			IGaService gaService = Graphiti.getGaService();
			IDimension size = gaService.calculateSize(graphicsAlgorithm);
			if (containerWidth != size.getWidth()) {
				gaService.setWidth(graphicsAlgorithm, containerWidth);
				changed = true;
			}
			if (graphicsAlgorithm instanceof Text
					&& textY != graphicsAlgorithm.getY()) {
				graphicsAlgorithm.setHeight(20);
				graphicsAlgorithm.setY(textY);
				changed = true;
			}
		}
		return changed;
	}
}
