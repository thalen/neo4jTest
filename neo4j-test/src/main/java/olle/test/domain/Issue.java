package olle.test.domain;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Node;

public class Issue {
	private String title;
	private Map<String, Person> likedBy = new HashMap<String, Person>();
	private Map<String, Person> likedIndirectlyBy = new HashMap<String, Person>();
	
	public Issue(Node node) {
		this.title = node.getProperty("Title").toString();
	}
	
	public Map<String, Person> getLikedBy() {
		return likedBy;
	}

	public void setLikedBy(Map<String, Person> likedBy) {
		this.likedBy = likedBy;
	}

	public Map<String, Person> getLikedIndirectlyBy() {
		return likedIndirectlyBy;
	}

	public void setLikedIndirectlyBy(Map<String, Person> likedIndirectlyBy) {
		this.likedIndirectlyBy = likedIndirectlyBy;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Issue)) {
			return false;
		}
		Issue other = (Issue) obj;
		return this.title == null ? false : this.title.equals(other.title);
	}
}
