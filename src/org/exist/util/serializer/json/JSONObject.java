package org.exist.util.serializer.json;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class JSONObject extends JSONNode {
	
	private JSONNode firstChild = null;
	private boolean asSimpleValue = false;
	
	public JSONObject(String name) {
		super(Type.OBJECT_TYPE, name);
	}
	
	public JSONObject(String name, boolean asSimpleValue) {
		super(Type.OBJECT_TYPE, name);
		this.asSimpleValue = asSimpleValue;
	}
	
	public void addObject(JSONNode node) {
		JSONNode childNode = findChild(node.getName());
		if (childNode == null) {
			childNode = getLastChild();
			if (childNode == null)
				firstChild = node;
			else
				childNode.setNext(node);
		} else
			childNode.setNextOfSame(node);
	}
	
	public JSONNode findChild(String nameToFind) {
		JSONNode nextNode = firstChild;
		while (nextNode != null) {
			if (nextNode.getName().equals(nameToFind))
				return nextNode;
			nextNode = nextNode.getNext();
		}
		return null;
	}
	
	public JSONNode getLastChild() {
		JSONNode nextNode = firstChild;
		JSONNode currentNode = null;
		while (nextNode != null) {
			currentNode = nextNode;
			nextNode = currentNode.getNext();
		}
		return currentNode;
	}
	
	public int getChildCount() {
		int count = 0;
		JSONNode nextNode = firstChild;
		while (nextNode != null) {
			count++;
			nextNode = nextNode.getNext();
		}
		return count;
	}
	
	public void serialize(Writer writer, boolean isRoot) throws IOException {
		if (!(isRoot || asSimpleValue)) {
			writer.write('"');
			writer.write(getName());
			writer.write("\" : ");
		}
		if (getNextOfSame() != null || getSerializationType() == SerializationType.AS_ARRAY) {
			writer.write("[");
			JSONNode next = this;
			while (next != null) {
				next.serializeContent(writer);
				next = next.getNextOfSame();
				if (next != null)
					writer.write(", ");
			}
			writer.write("]");
		} else
			serializeContent(writer);
	}
	
	public void serializeContent(Writer writer) throws IOException {
		if (firstChild == null)
			// an empty node gets a null value
			writer.write("null");
		else if (firstChild.getNext() == null && firstChild.getType() == Type.VALUE_TYPE)
			// if there's only one child and if it is text, it is serialized as simple value
			firstChild.serialize(writer, false);
		else {
			// complex object
			if (!asSimpleValue)
				writer.write("{ ");
			boolean allowText = false;
			JSONNode next = firstChild;
			while (next != null) {
				if (next.getType() == Type.VALUE_TYPE) {
					// if an element has attributes and text content, the text
					// node is serialized as property "#text". Text in mixed content nodes
					// is ignored though.
					if (allowText) {
						writer.write("\"#text\" : ");
						next.serialize(writer, false);
						allowText = false;
					}
				} else
					next.serialize(writer, false);
				if (next.getType() == Type.SIMPLE_PROPERTY_TYPE)
					allowText = true;
				next = next.getNext();
				if (next != null)
					writer.write(", ");
			}
			if (!asSimpleValue)
				writer.write("} ");
		}
	}

	@Override
	public String toString() {
		StringWriter writer = new StringWriter();
		try {
			serialize(writer, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return writer.toString();
	}
	
	
}
