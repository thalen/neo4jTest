package olle.test.domain;

import java.util.ArrayList;
import java.util.List;

public class Person {
	private String name;
	private List<Issue> likedIssues = new ArrayList<Issue>();
	
	public List<Issue> getLikedIssues() {
		return likedIssues;
	}

	public void setLikedIssues(List<Issue> likedIssues) {
		this.likedIssues = likedIssues;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
		return this.name == null ? 0 : this.name.hashCode();
	}
	
}
