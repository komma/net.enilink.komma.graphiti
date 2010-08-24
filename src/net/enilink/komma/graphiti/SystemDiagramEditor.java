package net.enilink.komma.graphiti;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.graphiti.ui.editor.DiagramEditor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IEditorInput;

import net.enilink.komma.common.ui.EclipseUtil;
import net.enilink.komma.edit.ui.editor.ISupportedEditor;
import net.enilink.komma.graphiti.dnd.SystemDiagramDropTargetListener;
import net.enilink.komma.model.IModel;

public class SystemDiagramEditor extends DiagramEditor implements
		ISupportedEditor, IDiagramEditorExt {
	/**
	 * The Constant DIAGRAM_EDITOR_ID.
	 */
	public static final String DIAGRAM_EDITOR_ID = "de.fhg.iwu.komma.graphiti.test.SystemDiagramEditor"; //$NON-NLS-1$

	@Override
	public void doSave(IProgressMonitor monitor) {
		monitor.beginTask("Saving models", 2);

		// Save only resources that have actually changed.
		final Map<Object, Object> saveOptions = new HashMap<Object, Object>();
		saveOptions.put(IModel.OPTION_SAVE_ONLY_IF_CHANGED,
				IModel.OPTION_SAVE_ONLY_IF_CHANGED_MEMORY_BUFFER);

		IRunnableWithProgress saveRunnable = new IRunnableWithProgress() {
			// This is the method that gets invoked when the operation runs.
			@Override
			public void run(IProgressMonitor monitor) {
				// Save the resources to the file system.
				IModel model = ((SystemDiagramTypeProvider) getDiagramTypeProvider())
						.getModel();
				// if (model.isModified()) {
				try {
					model.save(saveOptions);
				} catch (Exception exception) {
					// ignore
				}
				// }
			}
		};

		// Do the work within an operation because this is a long running
		// activity that modifies the workbench.
		saveRunnable = EclipseUtil.createWorkspaceModifyOperation(saveRunnable);

		try {
			// This runs the options, and shows progress.
			new ProgressMonitorDialog(getSite().getShell()).run(true, false,
					saveRunnable);

			// Refresh the necessary state.
			// ((BasicCommandStack)
			// editingDomain.getCommandStack()).saveIsDone();
		} catch (Exception exception) {
			// Something went wrong that shouldn't.
			// KommaEditUIPlugin.INSTANCE.log(exception);
		}

		super.doSave(new SubProgressMonitor(monitor, 1));
	}

	@Override
	protected void initializeGraphicalViewer() {
		super.initializeGraphicalViewer();

		getGraphicalViewer().addDropTargetListener(
				new SystemDiagramDropTargetListener(getGraphicalViewer()));
	}

	@Override
	public void firePropertyChange(int property) {
		super.firePropertyChange(property);
	}

	@Override
	public void setPartName(String partName) {
		super.setPartName(partName);
	}

	public void setInputWithNotify(IEditorInput input) {
		super.setInputWithNotify(input);
	}

	@Override
	public GraphicalViewer getGraphicalViewer() {
		return super.getGraphicalViewer();
	}
}
