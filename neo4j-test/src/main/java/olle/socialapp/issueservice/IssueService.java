package olle.socialapp.issueservice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import olle.server.NeoServer;
import olle.socialapp.domain.Issue;
import olle.socialapp.domain.Like;
import olle.socialapp.domain.Person;

import org.neo4j.graphdb.Node;

public class IssueService {

	private NeoServer neoServer;
	
	public IssueService(NeoServer neoServer) {
		this.neoServer = neoServer;
	}
	
	public List<Issue> getIssuesByUser(String personKey) {		
		Map<String, Node> likedIssues = neoServer.getLikedIssues(personKey);
		System.out.println("likedIssues: " + likedIssues);
		ArrayList<Issue> issues = new ArrayList<Issue>();		
		for (Node node : likedIssues.values()) {
			Issue issue = new Issue(node);
			issue.setLikes(new ArrayList<Like>());
			
			Map<String, Node> likedBy = neoServer.getLikedBy(node);
			for (Node person : likedBy.values()) {
				String comment = neoServer.getComment(node, person.getProperty("UUID").toString());
				issue.getLikes().add(new Like(new Person(person), comment));
			}
			issues.add(issue);
		}
		Node personNode = neoServer.getPerson(personKey);
		Like like = new Like(new Person(personNode), "");
		for (Issue issue : issues) {
			issue.getLikes().remove(like);
		}
		return issues;
	}
}
