package net.enilink.komma.graphiti;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.emf.common.command.CommandStack;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.ui.editor.DiagramEditor;
import org.eclipse.graphiti.ui.editor.DiagramEditorFactory;
import org.eclipse.graphiti.ui.editor.DiagramEditorInput;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;

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

	protected boolean useNativeGraphitiFormat = true;

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		// Eclipse may call us with an IFileEditorInput when a file is to be
		// opened. Try to convert this to a diagram input.
		if (!(input instanceof DiagramEditorInput)) {
			if (input instanceof IFileEditorInput) {
				final IFileEditorInput fileInput = (IFileEditorInput) input;
				final IFile file = fileInput.getFile();
				if (file.toString().endsWith(".diagram.layout.owl")) {
					useNativeGraphitiFormat = false;

					String providerId = "de.fhg.iwu.komma.graphiti.test.TestDiagramTypeProvider";
					URI fileURI = URI.createPlatformResourceURI(file
							.getFullPath().toString(), true);

					final TransactionalEditingDomain domain = new DiagramEditorFactory()
							.createResourceSetAndEditingDomain();

					final Diagram newDiagram = Graphiti.getPeService()
							.createDiagram(providerId, "Diagram", false);

					final ResourceSet resourceSet = domain.getResourceSet();
					// Create a resource for this file.
					final Resource resource = resourceSet
							.createResource(fileURI.trimFileExtension()
									.trimFileExtension());

					final CommandStack commandStack = domain.getCommandStack();
					commandStack.execute(new RecordingCommand(domain) {
						@Override
						protected void doExecute() {
							resource.setTrackingModification(true);
							resource.getContents().add(newDiagram);

						}
					});

					input = DiagramEditorInput.createEditorInput(newDiagram,
							domain, providerId, true);
				}
			}
		}
		super.init(site, input);
	}

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

				// Save the layout to the file system.
				IModel layoutModel = ((SystemDiagramTypeProvider) getDiagramTypeProvider())
						.getLayoutModel();

				// if (model.isModified()) {
				try {
					model.save(saveOptions);
					layoutModel.save(saveOptions);
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

		if (useNativeGraphitiFormat) {
			super.doSave(new SubProgressMonitor(monitor, 1));
		} else {
			getEditDomain().getCommandStack().flush();
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
	}

	@Override
	protected void initializeGraphicalViewer() {
		super.initializeGraphicalViewer();

		getGraphicalViewer().addDropTargetListener(
				new SystemDiagramDropTargetListener(getGraphicalViewer()));
	}

	@Override
	public boolean isDirty() {
		if (useNativeGraphitiFormat && super.isDirty()) {
			return true;
		}
		return ((SystemDiagramTypeProvider) getDiagramTypeProvider())
				.getModel().isModified()
				|| ((SystemDiagramTypeProvider) getDiagramTypeProvider())
						.getLayoutModel().isModified();
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
