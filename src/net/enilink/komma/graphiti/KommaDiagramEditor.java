package net.enilink.komma.graphiti;

import java.util.HashMap;
import java.util.Map;

import net.enilink.komma.common.ui.EclipseUtil;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.edit.ui.editor.ISupportedEditor;
import net.enilink.komma.graphiti.dnd.KommaDiagramDropTargetListener;
import net.enilink.komma.model.IModel;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.emf.common.command.CommandStack;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.ui.editor.DefaultUpdateBehavior;
import org.eclipse.graphiti.ui.editor.DiagramBehavior;
import org.eclipse.graphiti.ui.editor.DiagramEditor;
import org.eclipse.graphiti.ui.editor.DiagramEditorInput;
import org.eclipse.graphiti.ui.editor.IDiagramEditorInput;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;

public class KommaDiagramEditor extends DiagramEditor implements
		ISupportedEditor {
	/**
	 * The Constant DIAGRAM_EDITOR_ID.
	 */
	public static final String DIAGRAM_EDITOR_ID = KommaDiagramEditor.class
			.getName(); //$NON-NLS-1$

	public static final String DIAGRAM_PROVIDER_ID = "net.enilink.komma.graphiti.test.TestDiagramTypeProvider";

	protected boolean useNativeGraphitiFormat = true;

	protected IProject project;

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		if (input instanceof IFileEditorInput) {
			project = ((IFileEditorInput) input).getFile().getProject();
		} else if (input instanceof IDiagramEditorInput) {
			project = ResourcesPlugin
					.getWorkspace()
					.getRoot()
					.getProject(
							((IDiagramEditorInput) input).getUri().segment(1));
		} else {
			throw new PartInitException(
					"Unable to find related project for input.");
		}
		super.init(site, input);
	}

	@Override
	protected DiagramEditorInput convertToDiagramEditorInput(IEditorInput input)
			throws PartInitException {
		DiagramEditorInput deInput = super.convertToDiagramEditorInput(input);
		if (deInput.getProviderId() == null) {
			deInput = new DiagramEditorInput(deInput.getUri(),
					DIAGRAM_PROVIDER_ID);
		}
		return deInput;
	}

	@Override
	protected DiagramBehavior createDiagramBehavior() {
		return new DiagramBehavior(this) {
			@Override
			protected DefaultUpdateBehavior createUpdateBehavior() {
				return new DefaultUpdateBehavior(this) {
					SharedEditingDomain sharedEditingDomain = SharedEditingDomain
							.getSharedInstance(project);

					@Override
					protected void createEditingDomain() {
						sharedEditingDomain.addClient(this);
						TransactionalEditingDomain editingDomain = sharedEditingDomain
								.getEditingDomain();
						if (editingDomain == null) {
							super.createEditingDomain();
							sharedEditingDomain
									.setEditingDomain(getEditingDomain());
						} else {
							initializeEditingDomain(editingDomain);
						}
					}

					@Override
					protected void disposeEditingDomain() {
						sharedEditingDomain.removeClient(this);
					}

					@Override
					protected void handleChangedResources() {
						if (useNativeGraphitiFormat) {
							super.handleChangedResources();
						}
					}
				};
			}
		};
	}

	public IProject getProject() {
		return project;
	}

	@Override
	protected void setInput(IEditorInput input) {
		final IDiagramEditorInput deInput = (IDiagramEditorInput) input;
		useNativeGraphitiFormat = "diaemf".equals(deInput.getUri()
				.fileExtension());
		final ResourceSet resourceSet = getDiagramBehavior().getResourceSet();
		EObject diagram = null;
		try {
			diagram = resourceSet.getEObject(deInput.getUri(), true);
		} catch (Exception e) {
			// ignore
		}
		if (diagram == null) {
			final Diagram newDiagram = Graphiti.getPeService().createDiagram(
					DIAGRAM_PROVIDER_ID, "Diagram", false);

			// Create a resource for this file.
			final Resource newResource = resourceSet.getResource(deInput
					.getUri().trimFragment(), false);
			for (Resource r : resourceSet.getResources()) {
				System.out.println("Exists: " + r);
			}
			final CommandStack commandStack = getEditingDomain()
					.getCommandStack();
			commandStack.execute(new RecordingCommand(getEditingDomain()) {
				@Override
				protected void doExecute() {
					newResource.setTrackingModification(true);
					newResource.getContents().add(newDiagram);

					// PictogramLink link = PictogramsFactory.eINSTANCE
					// .createPictogramLink();
					// newDiagram.setLink(link);
					// newResource.getContents().add(link);
				}
			});
			input = new DiagramEditorInput(EcoreUtil.getURI(newDiagram),
					DIAGRAM_PROVIDER_ID);
		}
		super.setInput(input);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		monitor.beginTask("Saving models", 2);

		IRunnableWithProgress saveRunnable = new IRunnableWithProgress() {
			// This is the method that gets invoked when the operation runs.
			@Override
			public void run(IProgressMonitor monitor) {
				// Save only resources that have actually changed.
				Map<Object, Object> saveOptions = new HashMap<>();
				saveOptions.put(IModel.OPTION_SAVE_ONLY_IF_CHANGED,
						IModel.OPTION_SAVE_ONLY_IF_CHANGED_MEMORY_BUFFER);

				// Save the resources to the file system.
				IModel model = getDiagramTypeProvider().getModel();

				// if (model.isModified()) {
				model.getModelSet().getUnitOfWork().begin();
				try {
					model.save(saveOptions);
					if (!useNativeGraphitiFormat) {
						// Save the layout to the file system.
						IModel layoutModel = getDiagramTypeProvider()
								.getLayoutModel();
						saveOptions = new HashMap<>(saveOptions);
						String ext = layoutModel.getURI().fileExtension();
						if (ext != null) {
							IContentType contentType = Platform
									.getContentTypeManager()
									.findContentTypeFor(
											"example." + ext.replace("dia", ""));
							if (contentType != null) {
								saveOptions.put(
										IModel.OPTION_CONTENT_DESCRIPTION,
										contentType.getDefaultDescription());
							}
						}
						layoutModel.save(saveOptions);
					}
				} catch (Exception exception) {
					// ignore
					KommaEditUIPlugin.INSTANCE.log(exception);
				} finally {
					model.getModelSet().getUnitOfWork().end();
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
			KommaEditUIPlugin.INSTANCE.log(exception);
		}

		if (useNativeGraphitiFormat) {
			super.doSave(new SubProgressMonitor(monitor, 1));
		} else {
			getEditDomain().getCommandStack().flush();
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
	}

	@Override
	public KommaDiagramTypeProvider getDiagramTypeProvider() {
		return (KommaDiagramTypeProvider) super.getDiagramTypeProvider();
	}

	@Override
	public void initializeGraphicalViewer() {
		super.initializeGraphicalViewer();
		getGraphicalViewer().addDropTargetListener(
				new KommaDiagramDropTargetListener(getGraphicalViewer()));
	}

	@Override
	public void setFocus() {
		super.setFocus();
		// ensure that dirty flag is correctly updated
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	@Override
	public boolean isDirty() {
		if (useNativeGraphitiFormat) {
			return super.isDirty();
		}
		return getDiagramTypeProvider().getModel().isModified()
				|| getDiagramTypeProvider().getLayoutModel().isModified();
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

	final IEditingDomainProvider kommaEditingDomainProvider = new IEditingDomainProvider() {
		@Override
		public IEditingDomain getEditingDomain() {
			return getDiagramTypeProvider().getEditingDomain();
		}
	};

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class type) {
		if (getDiagramTypeProvider() != null) {
			if (IEditingDomainProvider.class.equals(type)) {
				return kommaEditingDomainProvider;
			} else if (IModel.class.equals(type)) {
				return getDiagramTypeProvider().getModel();
			}
		}
		return super.getAdapter(type);
	}
}
