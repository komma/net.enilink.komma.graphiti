package net.enilink.komma.graphiti.features;

import java.util.Collection;

import net.enilink.komma.core.IReference;
import net.enilink.komma.graphiti.IKommaDiagramImages;
import net.enilink.komma.graphiti.KommaDiagramEditor;
import net.enilink.komma.graphiti.service.IDiagramService;

import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.custom.AbstractCustomFeature;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.platform.IPlatformImageConstants;
import org.eclipse.graphiti.services.IPeService;
import org.eclipse.graphiti.ui.editor.DiagramEditor;
import org.eclipse.graphiti.ui.editor.DiagramEditorInput;
import org.eclipse.graphiti.ui.services.GraphitiUi;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.ide.IDE;

import com.google.inject.Inject;

public class DrillDownFeature extends AbstractCustomFeature {
	private class DiagramLabelProvider extends LabelProvider {

		Image image;

		/**
		 * Instantiates a new diagram label provider.
		 */
		public DiagramLabelProvider() {
			super();
		}

		@Override
		public Image getImage(Object element) {
			if (this.image == null) {
				this.image = GraphitiUi.getImageService()
						.getPlatformImageForId(
								IPlatformImageConstants.IMG_DIAGRAM);
			}
			return this.image;
		}

		@Override
		public String getText(Object o) {
			String ret = null;
			if (o instanceof Diagram) {
				Diagram diagram = (Diagram) o;
				ret = diagram.getName()
						+ " (" + diagram.getDiagramTypeId() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			return ret;
		}

	}

	@Inject
	IPeService peService;

	@Inject
	IDiagramService diagramService;

	@Inject
	public DrillDownFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public boolean canExecute(ICustomContext context) {
		PictogramElement[] pes = context.getPictogramElements();
		// first check, if one EClass is selected
		if (pes != null && pes.length == 1) {
			Object bo = getBusinessObjectForPictogramElement(diagramService
					.getRootOrFirstElementWithBO(pes[0]));
			if (bo instanceof IReference) {
				// // then forward to super-implementation, which checks if
				// // this EClass is associated with other diagrams
				// return super.canExecute(context);
				return true;
			}
		}
		return false;
	}

	public void execute(ICustomContext context) {
		final PictogramElement pe = context.getPictogramElements()[0];
		final Collection<Diagram> possibleDiagramsList = diagramService
				.getLinkedDiagrams(pe, true);

		if (!possibleDiagramsList.isEmpty()) {
			final Diagram[] possibleDiagrams = possibleDiagramsList
					.toArray(new Diagram[0]);
			if (possibleDiagramsList.size() == 1) {
				final Diagram diagram = possibleDiagrams[0];
				openDiagram(context, diagram);
			} else {
				ListDialog dialog = new ListDialog(PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getShell());
				dialog.setContentProvider(new IStructuredContentProvider() {

					@Override
					public void dispose() {
					}

					@Override
					public Object[] getElements(Object inputElement) {
						return possibleDiagramsList.toArray();
					}

					@Override
					public void inputChanged(Viewer viewer, Object oldInput,
							Object newInput) {
					}
				});
				dialog.setTitle("Choose Diagram");
				dialog.setMessage("Choose Diagram to open");
				dialog.setInitialSelections(new Diagram[] { possibleDiagrams[0] });
				dialog.setLabelProvider(new DiagramLabelProvider());
				dialog.setAddCancelButton(true);
				dialog.setHelpAvailable(false);
				dialog.setInput(new Object());
				dialog.open();
				Object[] result = dialog.getResult();
				if (result != null) {
					for (int i = 0; i < result.length; i++) {
						Diagram diagram = (Diagram) result[i];
						openDiagram(context, diagram);
					}
				}
			}
		}
	}

	protected void openDiagram(ICustomContext context, Diagram diagram) {
		openDiagramEditor(diagram,
				getTransActionalEditingDomainForNewDiagram(diagram),
				getFeatureProvider().getDiagramTypeProvider().getProviderId(),
				false);
	}

	@Override
	public String getDescription() {
		return "Open the diagram associated with this node"; //$NON-NLS-1$
	}

	@Override
	public String getName() {
		return "Open associated diagram"; //$NON-NLS-1$
	}

	protected TransactionalEditingDomain getTransActionalEditingDomainForNewDiagram(
			Diagram newDiagram) {
		return getDiagramBehavior().getEditingDomain();
	}

	/**
	 * Opens the given diagram in the diagram editor.
	 * 
	 * @param diagram
	 *            which should be opened
	 * @param domain
	 * @param providerId
	 *            the unique provider id of a diagram type provider which will
	 *            be used by the editor.
	 * @return the editor instance
	 */
	public DiagramEditor openDiagramEditor(Diagram diagram,
			TransactionalEditingDomain domain, String providerId,
			boolean disposeEditingDomain) {
		DiagramEditor ret = null;
		DiagramEditorInput diagramEditorInput = DiagramEditorInput
				.createEditorInput(diagram, providerId);
		IWorkbenchPage workbenchPage = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		try {
			IEditorPart editorPart = IDE.openEditor(workbenchPage,
					diagramEditorInput, KommaDiagramEditor.DIAGRAM_EDITOR_ID);
			if (editorPart instanceof DiagramEditor) {
				ret = (DiagramEditor) editorPart;
			}
		} catch (PartInitException e) {
			// $JL-EXC$
		}

		return ret;
	}

	@Override
	public String getImageId() {
		return IKommaDiagramImages.LINK_IMG;
	}
}
