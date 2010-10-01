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

import java.util.LinkedList;
import java.util.Queue;

import org.eclipse.graphiti.datatypes.IDimension;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ILayoutContext;
import org.eclipse.graphiti.features.impl.AbstractLayoutFeature;
import org.eclipse.graphiti.mm.algorithms.AbstractText;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.PlatformGraphicsAlgorithm;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.IGaService;

import com.google.inject.Inject;

import net.enilink.komma.concepts.IResource;
import net.enilink.komma.graphiti.SystemGraphicsAlgorithmRendererFactory;
import net.enilink.komma.graphiti.service.ITypes;

/**
 * The Class LayoutPoolFeature.
 */
public class LayoutNodeFeature extends AbstractLayoutFeature {
	private static final int MIN_HEIGHT = 40;

	private static final int MIN_WIDTH = 25;

	private static final int IMAGE_PADDING = 3;

	@Inject
	IGaService gaService;

	@Inject
	ITypes types;

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

		int textHeight = 0;
		for (Shape shape : containerShape.getChildren()) {
			GraphicsAlgorithm graphicsAlgorithm = shape.getGraphicsAlgorithm();
			if (graphicsAlgorithm instanceof AbstractText) {
				String value = ((AbstractText) graphicsAlgorithm).getValue();
				if (value == null)
					continue;
				textHeight = ((AbstractText) graphicsAlgorithm).getValue()
						.split("\r?\n").length * 20;
			}
		}
		int textY = containerHeight - textHeight;

		// resize all child GAs
		Queue<GraphicsAlgorithm> queue = new LinkedList<GraphicsAlgorithm>(
				containerGa.getGraphicsAlgorithmChildren());
		while (!queue.isEmpty()) {
			GraphicsAlgorithm childGa = queue.remove();

			if (childGa instanceof PlatformGraphicsAlgorithm
					&& ((PlatformGraphicsAlgorithm) childGa).getId().equals(
							SystemGraphicsAlgorithmRendererFactory.NODE_FIGURE)) {
				// scale and center image
				int origWidth = containerWidth - 2 * IMAGE_PADDING;
				int origHeight = textY - 2 * IMAGE_PADDING;

				int length = Math.min(origWidth, origHeight);

				childGa.setX(IMAGE_PADDING + (origWidth - length) / 2);
				childGa.setY(IMAGE_PADDING + (origHeight - length) / 2);

				childGa.setWidth(length);
				childGa.setHeight(length);
			} else {
				childGa.setX(0);
				childGa.setY(0);

				childGa.setWidth(containerWidth);
				childGa.setHeight(containerHeight);

				queue.addAll(childGa.getGraphicsAlgorithmChildren());
			}
		}

		// GA of the container shape, we need it to determine the valid range
		// for connectors to be placed
		GraphicsAlgorithm cga = containerShape.getGraphicsAlgorithm();

		for (Shape shape : containerShape.getChildren()) {
			GraphicsAlgorithm graphicsAlgorithm = shape.getGraphicsAlgorithm();
			if (types.isInterface(shape)) {
				// ensure that connectors are placed at object borders
				int x, y, cw, ch;
				x = graphicsAlgorithm.getX();
				y = graphicsAlgorithm.getY();
				cw = cga.getWidth();
				ch = textY;

				// put the item into the valid range
				if (x < 5)
					graphicsAlgorithm.setX(5);
				if (x > (cw - 15))
					graphicsAlgorithm.setX(cga.getWidth() - 15);
				if (y < 5)
					graphicsAlgorithm.setY(5);
				if (y > (ch - 15))
					graphicsAlgorithm.setY(textY - 15);

				// update these...
				x = graphicsAlgorithm.getX();
				y = graphicsAlgorithm.getY();

				boolean needsAlignment = true;

				// check whether the connector needs alignment, i.e. it is not
				// clamped to one border
				if (x == 5)
					needsAlignment = false;
				if (x == (cw - 15))
					needsAlignment = false;
				if (y == 5)
					needsAlignment = false;
				if (y == (ch - 15))
					needsAlignment = false;

				if (needsAlignment) {
					// determine the closest border
					int dl, dt, dr, db;
					dl = x;
					dt = y;
					dr = cw - (x + 10);
					db = ch - (y + 10);

					int minDist = Math.min(Math.min(Math.min(dl, dt), dr), db);

					if (minDist == dl)// clamp to left
						graphicsAlgorithm.setX(5);
					if (minDist == dt)// clamp to top
						graphicsAlgorithm.setY(5);
					if (minDist == dr)
						graphicsAlgorithm.setX(cw - 15);
					if (minDist == db)
						graphicsAlgorithm.setY(ch - 15);
				}
			} else if (graphicsAlgorithm instanceof AbstractText) {
				IDimension size = gaService.calculateSize(graphicsAlgorithm);
				if (containerWidth != size.getWidth()) {
					gaService.setWidth(graphicsAlgorithm, containerWidth);
					changed = true;
				}

				if (textY != graphicsAlgorithm.getY()) {
					graphicsAlgorithm.setHeight(textHeight);
					graphicsAlgorithm.setY(textY);
					changed = true;
				}
			}
		}
		return changed;
	}
}
