package net.enilink.komma.graphiti.features;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.custom.AbstractCustomFeature;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.platform.IDiagramEditor;
import org.eclipse.graphiti.platform.IPlatformImageConstants;
import org.eclipse.graphiti.services.IPeService;
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

import net.enilink.komma.graphiti.SystemDiagramEditor;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;

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
				this.image = GraphitiUi.getImageService().getImageForId(
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
	public DrillDownFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public boolean canExecute(ICustomContext context) {
		PictogramElement[] pes = context.getPictogramElements();
		// first check, if one EClass is selected
		if (pes != null && pes.length == 1) {
			Object bo = getBusinessObjectForPictogramElement(pes[0]);
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
		final Collection<Diagram> possibleDiagramsList = getLinkedDiagrams(pe);

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

	protected Collection<Diagram> getLinkedDiagrams(PictogramElement pe) {
		final Collection<Diagram> diagrams = new HashSet<Diagram>();

		final Object[] businessObjectsForPictogramElement = getAllBusinessObjectsForPictogramElement(pe);
		URI firstUri = null;
		for (Object bo : businessObjectsForPictogramElement) {
			if (bo instanceof IReference) {
				URI uri = ((IReference) bo).getURI();
				if (uri != null) {
					firstUri = uri;
				}
			}
		}
		if (firstUri != null) {
			String diagramId = "diagram_" + firstUri.fragment();

			EObject linkedDiagram = null;
			for (TreeIterator<EObject> i = EcoreUtil.getAllProperContents(
					getDiagram().eResource().getContents(), false); i.hasNext();) {
				EObject eObject = i.next();
				if (eObject instanceof Diagram) {
					if (diagramId.equals(((Diagram) eObject).getName())) {
						linkedDiagram = eObject;
						break;
					}
				}
			}

			if (!(linkedDiagram instanceof Diagram)) {
				Diagram newDiagram = peService.createDiagram(getDiagram()
						.getDiagramTypeId(), diagramId, getDiagram()
						.isSnapToGrid());
				getDiagram().eResource().getContents().add(newDiagram);

				linkedDiagram = newDiagram;
			}

			if (!EcoreUtil.equals(getDiagram(), linkedDiagram)) {
				diagrams.add((Diagram) linkedDiagram);
			}
		}

		return diagrams;
	}

	@Override
	public String getName() {
		return "Open associated diagram"; //$NON-NLS-1$
	}

	protected TransactionalEditingDomain getTransActionalEditingDomainForNewDiagram(
			Diagram newDiagram) {
		return getDiagramEditor().getEditingDomain();
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
	public IDiagramEditor openDiagramEditor(Diagram diagram,
			TransactionalEditingDomain domain, String providerId,
			boolean disposeEditingDomain) {
		IDiagramEditor ret = null;
		DiagramEditorInput diagramEditorInput = DiagramEditorInput
				.createEditorInput(diagram, domain, providerId,
						disposeEditingDomain);
		IWorkbenchPage workbenchPage = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		try {
			IEditorPart editorPart = IDE.openEditor(workbenchPage,
					diagramEditorInput, SystemDiagramEditor.DIAGRAM_EDITOR_ID);
			if (editorPart instanceof IDiagramEditor) {
				ret = (IDiagramEditor) editorPart;
			}
		} catch (PartInitException e) {
			// $JL-EXC$
		}

		return ret;
	}
}
