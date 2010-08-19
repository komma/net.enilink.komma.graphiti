package net.enilink.komma.graphiti.dnd;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.dnd.AbstractTransferDropTargetListener;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.dnd.DND;

import net.enilink.komma.edit.ui.dnd.LocalTransfer;

public class SystemDiagramDropTargetListener extends AbstractTransferDropTargetListener {
	public SystemDiagramDropTargetListener(EditPartViewer viewer) {
		super(viewer, LocalTransfer.getInstance());
	}

	@Override
	protected void handleDrop() {
		super.handleDrop();
		if (getCurrentEvent().detail == DND.DROP_MOVE) {
			getCurrentEvent().detail = DND.DROP_COPY;
		}
	}

	@Override
	protected void updateTargetRequest() {
		((CreateRequest) getTargetRequest()).setLocation(getDropLocation());
	}

	@Override
	protected Request createTargetRequest() {
		CreateRequest request = new CreateRequest();

		request.setFactory(new DropTargetCreationFactory());
		request.setLocation(getDropLocation());
		return request;
	}

	@Override
	protected void handleDragOver() {
		super.handleDragOver();

		Command command = getCommand();
		if (command != null && command.canExecute()) {
			getCurrentEvent().detail = DND.DROP_COPY;
		}
	}

	private class DropTargetCreationFactory implements CreationFactory {
		public Object getNewObject() {
			return LocalTransfer.getInstance().nativeToJava(
					getCurrentEvent().currentDataType);
		}

		public Object getObjectType() {
			return ISelection.class;
		}
	}

}
