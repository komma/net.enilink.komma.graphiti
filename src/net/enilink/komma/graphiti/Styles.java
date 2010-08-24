package net.enilink.komma.graphiti;

import java.util.Collection;

import org.eclipse.graphiti.mm.StyleContainer;
import org.eclipse.graphiti.mm.algorithms.styles.Style;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.util.ColorConstant;
import org.eclipse.graphiti.util.IColorConstant;
import org.eclipse.graphiti.util.PredefinedColoredAreas;

import com.google.inject.Inject;

public class Styles {

	private static final IColorConstant NODE_TEXT_FOREGROUND = new ColorConstant(
			51, 51, 153);

	private static final IColorConstant NODE_FOREGROUND = new ColorConstant(
			255, 102, 0);

	@Inject
	IGaService gaService;

	public Style getStyleForNode(Diagram diagram) {
		final String styleId = "NODE"; //$NON-NLS-1$

		Style style = findStyle(diagram, styleId);

		if (style == null) { // style not found - create new style
			style = gaService.createStyle(diagram, styleId);
			style.setForeground(gaService.manageColor(diagram, NODE_FOREGROUND));
			// gaService.setRenderingStyle(style,
			// TutorialColoredAreas.getLimeWhiteAdaptions());
			gaService.setRenderingStyle(style,
					PredefinedColoredAreas.getBlueWhiteGlossAdaptions());
			style.setLineWidth(2);
		}
		return style;
	}

	public Style getStyleForNodeText(Diagram diagram) {
		final String styleId = "NODE-TEXT"; //$NON-NLS-1$

		// this is a child style of the e-class-style
		Style parentStyle = getStyleForNode(diagram);
		Style style = findStyle(parentStyle, styleId);

		if (style == null) { // style not found - create new style
			style = gaService.createStyle(diagram, styleId);
			// "overwrites" values from parent style
			style.setForeground(gaService.manageColor(diagram,
					NODE_TEXT_FOREGROUND));
		}
		return style;
	}

	// find the style with a given id in the style-container, can return null
	private Style findStyle(StyleContainer styleContainer, String id) {
		// find and return style
		Collection<Style> styles = styleContainer.getStyles();
		if (styles != null) {
			for (Style style : styles) {
				if (id.equals(style.getId())) {
					return style;
				}
			}
		}
		return null;
	}
}