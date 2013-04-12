package olle.socialapp.domain;

import java.io.Serializable;

import org.neo4j.graphdb.Node;

public class Person implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8456752962989671550L;
	private String name;
	private String unid;
	
	public Person(Node node) {
		this.name = node.getProperty("Name").toString();
		this.unid = node.getProperty("UUID").toString();
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getUnid() {
		return unid;
	}
	
	public void setUnid(String unid) {
		this.unid = unid;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Person)) {
			return false;
		}
		Person other = (Person) obj;
		return this.name == null ? false : this.name.equals(other.name);
	}
	
	@Override
	public int hashCode() {
		if (this.name == null) {
			return 0;
		}
		return this.name.hashCode();
	}
}
