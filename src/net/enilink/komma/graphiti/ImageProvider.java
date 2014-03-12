package net.enilink.komma.graphiti;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.eclipse.graphiti.ui.platform.AbstractImageProvider;
import org.eclipse.graphiti.ui.platform.IImageProvider;

public class ImageProvider extends AbstractImageProvider implements
		IImageProvider {
	@Override
	protected void addAvailableImages() {
		for (Field f : IKommaDiagramImages.class.getDeclaredFields()) {
			if (String.class.equals(f.getType())
					&& Modifier.isStatic(f.getModifiers())) {
				try {
					String image = (String) f.get(null);
					addImageFilePath(image, image);
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}
}
