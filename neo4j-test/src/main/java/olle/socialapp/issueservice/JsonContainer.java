package olle.socialapp.issueservice;

import java.io.Serializable;
import java.util.List;

import olle.socialapp.domain.Issue;

public class JsonContainer implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2557607858119272513L;
	private List<Issue> issues;

	public List<Issue> getIssues() {
		return issues;
	}

	public void setIssues(List<Issue> issues) {
		this.issues = issues;
	}
}
