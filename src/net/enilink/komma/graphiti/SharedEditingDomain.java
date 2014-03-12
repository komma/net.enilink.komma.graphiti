package net.enilink.komma.graphiti;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.emf.transaction.TransactionalEditingDomain;

public class SharedEditingDomain {
	protected static QualifiedName PROPERTY_INSTANCE = new QualifiedName(
			SharedEditingDomain.class.getName(), "instance");

	public static SharedEditingDomain getSharedInstance(IProject project) {
		SharedEditingDomain sharedEditingDomain = null;
		try {
			sharedEditingDomain = (SharedEditingDomain) project
					.getSessionProperty(PROPERTY_INSTANCE);
		} catch (CoreException e) {
			// ignore
		}
		if (sharedEditingDomain == null) {
			sharedEditingDomain = new SharedEditingDomain(project);
			try {
				project.setSessionProperty(PROPERTY_INSTANCE,
						sharedEditingDomain);
			} catch (CoreException e) {
				// ignore
			}
		}
		return sharedEditingDomain;
	}

	protected final Set<Object> clients = Collections
			.newSetFromMap(new WeakHashMap<Object, Boolean>());

	protected TransactionalEditingDomain editingDomain;

	protected final IProject project;

	protected SharedEditingDomain(IProject project) {
		this.project = project;
	}

	public void addClient(Object client) {
		clients.add(client);
	}

	public void setEditingDomain(TransactionalEditingDomain editingDomain) {
		this.editingDomain = editingDomain;
	}

	public TransactionalEditingDomain getEditingDomain() {
		return editingDomain;
	}

	public void removeClient(Object client) {
		clients.remove(client);
		if (clients.isEmpty()) {
			if (editingDomain != null) {
				editingDomain.dispose();
				editingDomain = null;
				// remove shared properties from project
				try {
					project.setSessionProperty(PROPERTY_INSTANCE, null);
				} catch (CoreException e) {
					// ignore
				}
			}
		}
	}
}
