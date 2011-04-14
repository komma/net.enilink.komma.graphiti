package net.enilink.komma.graphiti.concepts;

public class Connector {
	Object businessObject;

	public Connector(Object businessObject) {
		this.businessObject = businessObject;
	}

	public Object getBusinessObject() {
		return businessObject;
	}
}
