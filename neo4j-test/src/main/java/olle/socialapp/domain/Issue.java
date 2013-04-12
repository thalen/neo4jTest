package olle.socialapp.domain;

import java.io.Serializable;
import java.util.List;

import org.neo4j.graphdb.Node;

public class Issue implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String title;	
	private List<Like> likes;
	
	public Issue(Node node) {
		this.title = node.getProperty("Description").toString();
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
		
	@Override
	public String toString() {
		return title + " liked by: " + likes;
	}

	public List<Like> getLikes() {
		return likes;
	}

	public void setLikes(List<Like> likes) {
		this.likes = likes;
	}
}
