package net.enilink.komma.graphiti;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.graphiti.platform.IDiagramEditor;

public interface IDiagramEditorExt extends IDiagramEditor {
	GraphicalViewer getGraphicalViewer();
}
