package olle.server;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.impl.util.FileUtils;
import org.neo4j.kernel.impl.util.StringLogger;
import org.neo4j.tooling.GlobalGraphOperations;

public class NeoServer {

	private static final String DB_PATH = "c:\\Development\\neo4j-community-1.8.2";
	private GraphDatabaseService graphDb;

	private static enum RelTypes implements RelationshipType {
		KNOWS, LIKES, FOLLOWING
	}
	
	public NeoServer() {
		clearDb();
		graphDb = new GraphDatabaseFactory()
				.newEmbeddedDatabaseBuilder(DB_PATH)
				.setConfig(GraphDatabaseSettings.node_keys_indexable, "UUID")
				.setConfig(GraphDatabaseSettings.node_auto_indexing, "true")
				.newGraphDatabase();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});

	}

	private void clearDb() {
		try {
			FileUtils.deleteRecursively(new File(DB_PATH));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Node addLike(Map<String, String> properties) {
		ReadableIndex<Node> autoNodeIndex = graphDb.index()
				.getNodeAutoIndexer().getAutoIndex();
		String issueKey = properties.get("IssueKey");
		Node issueNode = autoNodeIndex.get("UUID", issueKey).getSingle();
		String personKey = properties.get("PersonKey");
		Node personNode = autoNodeIndex.get("UUID", personKey).getSingle();
		
		if(issueNode != null && personNode != null) {
			TraversalDescription td = Traversal.description()
		            .depthFirst()
		            .relationships( RelTypes.LIKES, Direction.BOTH )
		            .evaluator( Evaluators.excludeStartPosition());
			for (Path shortPath : td.traverse(issueNode)) {
				if (shortPath.endNode().getProperty("UUID").equals(personKey)) {
					//can't add the same relation twice
					return issueNode;
				}
			}
		}
		if (issueNode == null) {
			HashMap<String, String> props = new HashMap<String, String>();
			props.put("Type", "Issue");
			props.put("UUID", issueKey);
			props.put("Description", properties.get("IssueTitle"));
			issueNode = addNode(props);
		}
		if (personNode == null) {
			HashMap<String, String> props = new HashMap<String, String>();
			props.put("Type", "Person");
			props.put("UUID", personKey);
			props.put("Name", properties.get("PersonName"));
			personNode = addNode(props);
		}
		Transaction tx = graphDb.beginTx();
		try {
			Relationship rel = personNode.createRelationshipTo(issueNode,
					RelTypes.LIKES);
			System.out.println("comment: " + properties.get("Comment"));
			rel.setProperty("comment", properties.get("Comment"));
			tx.success();
		} finally {
			tx.finish();
		}
		return issueNode;
		
	}	
	
	public Node addFollower(Map<String, String> properties) {
		ReadableIndex<Node> autoNodeIndex = graphDb.index()
				.getNodeAutoIndexer().getAutoIndex();
		String sourceKey = properties.get("source[id]");
		Node source = autoNodeIndex.get("UUID", sourceKey).getSingle();
		if (source == null) {
			HashMap<String, String> props = new HashMap<String, String>();
			props.put("Type", "Person");
			props.put("UUID", sourceKey);
			props.put("Name", properties.get("source[name]"));
			source = addNode(props);
		}
		String targetKey = properties.get("target[id]");
		Node target = autoNodeIndex.get("UUID", targetKey).getSingle();
		if (target == null) {
			HashMap<String, String> props = new HashMap<String, String>();
			props.put("Type", "Person");
			props.put("UUID", targetKey);
			props.put("Name", properties.get("target[name]"));
			target = addNode(props);
		}
		Transaction tx = graphDb.beginTx();
		try {
			source.createRelationshipTo(target,
					RelTypes.FOLLOWING);			
			tx.success();
		} finally {
			tx.finish();
		}		
		return source;
	}
	
	public Node removeFollower(Map<String, String> properties) {
		ReadableIndex<Node> autoNodeIndex = graphDb.index()
				.getNodeAutoIndexer().getAutoIndex();
		String sourceKey = properties.get("source[id]");
		Node source = autoNodeIndex.get("UUID", sourceKey).getSingle();
		
		String targetKey = properties.get("target[id]");		
		for (Relationship rel : source.getRelationships()) {
			if (targetKey.equals(rel.getEndNode().getProperty("UUID"))) {
				Transaction tx = graphDb.beginTx();
				try {
					System.out.println("removing relation");
					rel.delete();
					tx.success();
				} finally {
					tx.finish();
				}
			}
		}	
		return source;
	}
	
	public void printLikesFromNode(Node node) {		
		TraversalDescription td = Traversal.description()
	            .depthFirst()
	            .relationships( RelTypes.LIKES, Direction.BOTH )
	            .evaluator( Evaluators.excludeStartPosition());
		for (Path shortPath : td.traverse(node)) {						
			System.out.println(shortPath);
		}
	}
	
	public Set<String> getFollowers(String personKey) {
		TraversalDescription td = Traversal.description()
	            .depthFirst()
	            .relationships( RelTypes.FOLLOWING, Direction.BOTH )
	            .evaluator( Evaluators.excludeStartPosition()).evaluator(Evaluators.atDepth(1));
		
		ReadableIndex<Node> autoNodeIndex = graphDb.index()
				.getNodeAutoIndexer().getAutoIndex();		
		Node node = autoNodeIndex.get("UUID", personKey).getSingle();
		if (node == null) {
			return Collections.emptySet();
		}
		HashSet<String> result = new HashSet<String>();
		for (Path shortPath : td.traverse(node)) {									
			Node follower = shortPath.endNode();
			result.add(follower.getProperty("UUID").toString());
			
		}
		return result;
	}
	
	public void printFollowersFromNode(Node node) {		
		TraversalDescription td = Traversal.description()
	            .depthFirst()
	            .relationships( RelTypes.FOLLOWING, Direction.BOTH )
	            .evaluator( Evaluators.excludeStartPosition());
		for (Path shortPath : td.traverse(node)) {						
			System.out.println(shortPath);
		}
	}
	
	public Node addNode(Map<String, String> properties) {
		Transaction tx = graphDb.beginTx();
		try {
			Node node = graphDb.createNode();
			for (Map.Entry<String, String> entry : properties.entrySet()) {
				node.setProperty(entry.getKey(), entry.getValue());
			}

			tx.success();
			return node;
		} finally {
			tx.finish();
		}
	}

	public void printDb() {
		ExecutionEngine engine = new ExecutionEngine(graphDb,
				StringLogger.SYSTEM);
		ExecutionResult result = engine.execute("start n=node(*) return n");
		System.out.println("all nodes: " + result.dumpToString());
		System.out.println("relationship data:");
		for (Relationship rel : GlobalGraphOperations.at(graphDb).getAllRelationships()) {
			HashMap<String, Object> props = new HashMap<String, Object>();
			for (String str : rel.getPropertyKeys()) {
				props.put(str, rel.getProperty(str));
			}
			System.out.println(rel.getType() + ": " + props);
		}
		
	}
	
	public Map<String, Node> getLikedIssues(String personKey) {
		ReadableIndex<Node> autoNodeIndex = graphDb.index()
				.getNodeAutoIndexer().getAutoIndex();
		Node personNode = autoNodeIndex.get("UUID", personKey).getSingle();
		TraversalDescription td = Traversal.description()
	            .depthFirst()
	            .relationships( RelTypes.LIKES, Direction.BOTH )
	            .evaluator( Evaluators.excludeStartPosition()).evaluator(Evaluators.atDepth(1));
		
		HashMap<String, Node> nodes = new HashMap<String, Node>();
		for (Path shortPath : td.traverse(personNode)) {
			Node endNode = shortPath.endNode();
			if (endNode.hasProperty("UUID")) {
				nodes.put(endNode.getProperty("UUID").toString(), endNode);
			}
		}
		return nodes;
	}
	
	public String getComment(Node issueNode, String personKey) {
		System.out.println("issueNode: " + issueNode);
		for (Relationship relation : issueNode.getRelationships(RelTypes.LIKES)) {
			System.out.println("rel: " + relation);
			System.out.println("startNode key: " + relation.getStartNode().getProperty("UUID"));
			System.out.println("endNode key: " + relation.getEndNode().getProperty("UUID"));
			
			if (relation.getStartNode().getProperty("UUID").toString().equals(personKey)) {
				return relation.getProperty("comment").toString();
			}
		}
		return "";
	}
	
	public Node getPerson(String personKey) {
		ReadableIndex<Node> autoNodeIndex = graphDb.index()
				.getNodeAutoIndexer().getAutoIndex();
		Node personNode = autoNodeIndex.get("UUID", personKey).getSingle();
		return personNode;
	}
	
	public Map<String, Node> getLikedBy(Node issueNode) {
		TraversalDescription td = Traversal.description()
	            .depthFirst()
	            .relationships( RelTypes.LIKES, Direction.BOTH )
	            .evaluator( Evaluators.excludeStartPosition()).evaluator(Evaluators.atDepth(1));
		HashMap<String, Node> nodes = new HashMap<String, Node>();
		for (Path shortPath : td.traverse(issueNode)) {
			Node endNode = shortPath.endNode();
			if (endNode.hasProperty("UUID")) {
				nodes.put(endNode.getProperty("UUID").toString(), endNode);
			}			
		}
		return nodes;
	}
}
