package olle.test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import olle.test.domain.Issue;
import olle.test.domain.Person;

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
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.impl.util.FileUtils;
import org.neo4j.kernel.impl.util.StringLogger;

public class TestNeo {

	private static final String DB_PATH = "c:\\Development\\neo4j-community-1.8.2";

	GraphDatabaseService graphDb;
	Node current;
	Relationship relationship;

	// END SNIPPET: vars

	// START SNIPPET: createReltype
	private static enum RelTypes implements RelationshipType {
		KNOWS, LIKES, CONNECTED
	}

	public static void main(String[] args) throws Exception {
		TestNeo neo = new TestNeo();
		neo.createDb();
		neo.searchDb();
		neo.shutDown();
	}

	void shutDown() {
		System.out.println();
		System.out.println("Shutting down database ...");
		// START SNIPPET: shutdownServer
		graphDb.shutdown();
		// END SNIPPET: shutdownServer
	}

	void createDb() {
		clearDb();

		
		//graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
				
		graphDb = new GraphDatabaseFactory().
			    newEmbeddedDatabaseBuilder( DB_PATH ).
			    setConfig( GraphDatabaseSettings.node_keys_indexable, "UUID" ).
			    setConfig( GraphDatabaseSettings.node_auto_indexing, "true" ).
			    newGraphDatabase();
		//ExecutionResult result = engine.execute( "start n=node(7) return n, n.message" );
		
		TestNeo.registerShutdownHook(graphDb);
		// END SNIPPET: startDb

		// START SNIPPET: transaction
		Transaction tx = graphDb.beginTx();
		try {
			// Updating operations go here
			// END SNIPPET: transaction
			// START SNIPPET: addData
			Node A = graphDb.createNode();						
			A.setProperty("Type", "Issue");
			A.setProperty("Title", "A question");
			A.setProperty("UUID", "1");
			Node B = graphDb.createNode();
			B.setProperty("Type", "Person");
			B.setProperty("Name", "Olle Thalén");
			B.setProperty("UUID", "2");
			
			relationship = B.createRelationshipTo(A,
					RelTypes.LIKES);			
			Node C = graphDb.createNode();
			C.setProperty("Type", "Issue");
			C.setProperty("Title", "Question no 2");
			C.setProperty("UUID", "3");
			
			relationship = B.createRelationshipTo(C, RelTypes.LIKES);
			relationship = A.createRelationshipTo(C, RelTypes.CONNECTED);
			relationship.setProperty("Code", "111B");
			Node D = graphDb.createNode();
			D.setProperty("Type", "Person");
			D.setProperty("Name", "Kalle Kula");
			D.setProperty("UUID", "4");
			
			relationship = D.createRelationshipTo(C, RelTypes.LIKES);
			Node E = graphDb.createNode();
			E.setProperty("Type", "Issue");
			E.setProperty("Title", "Third question");
			E.setProperty("UUID", "5");
			
			relationship = B.createRelationshipTo(E, RelTypes.LIKES);
			relationship = E.createRelationshipTo(C, RelTypes.CONNECTED);
			relationship.setProperty("Code", "311B");
			this.current = A;
			
			tx.success();
		} finally {
			tx.finish();						
		}
		// END SNIPPET: transaction
	}
	
	void searchDb() {
		ExecutionEngine engine = new ExecutionEngine(graphDb, StringLogger.SYSTEM);
		ExecutionResult result = engine.execute( "start n=node(*) return n" );
		
		System.out.println(result.dumpToString());
		result = engine.execute("start n=node:node_auto_index(UUID='5') return n");
		System.out.println("result node By ID: " + result.dumpToString());
		
		String output = "";
		HashMap<String, Node> persons = new HashMap<String, Node>();
		
		Issue issue = new Issue(current);
		
		TraversalDescription td = Traversal.description()
	            .depthFirst()
	            .relationships( RelTypes.LIKES, Direction.BOTH )
	            .evaluator( Evaluators.excludeStartPosition()).evaluator(Evaluators.atDepth(1));
		for (Path shortPath : td.traverse(current)) {
			Node node = shortPath.endNode();
			Person person = new Person();			
			person.setName(node.getProperty("Name").toString());
			issue.getLikedBy().put(person.getName(), person);
		}
		
		td = Traversal.description()
	            .depthFirst()
	            .relationships( RelTypes.LIKES, Direction.BOTH )
	            .evaluator( Evaluators.excludeStartPosition());
		Traverser issueTraverser = td.traverse(current);
		for (Path position : issueTraverser) {			
			output += position + "\n";
			for (Node node : position.nodes()) {				
				if (node.getProperty("Type").toString().equals("Person")) {					
					persons.put(node.getProperty("Name").toString(), node);
				}
			}
		}
		
		System.out.println(output);
		td = Traversal.description()
	            .depthFirst()
	            .relationships( RelTypes.LIKES, Direction.BOTH )
	            .evaluator( Evaluators.excludeStartPosition()).evaluator(Evaluators.atDepth(1));
		
		Object rootKey = current.getProperty("UUID");
		
		for (Node person : persons.values()) {			
			for (Path curPath : td.traverse(person)) {
				Node likedIssue = curPath.endNode();
				if (likedIssue.getProperty("UUID").equals(rootKey)) {
					continue;
				}
				TraversalDescription td2 = Traversal.description()
			            .depthFirst()
			            .relationships( RelTypes.CONNECTED, Direction.BOTH )
			            .evaluator( Evaluators.excludeStartPosition());
				boolean connected = false;
				
				
				for (Path issuePath : td2.traverse(likedIssue)) {
					//this path leads back to the original issue => we are connected
					if (issuePath.endNode().getProperty("UUID").equals(rootKey)) {
						connected = true;
						break;
					}
				}
				
				String personName = person.getProperty("Name").toString();
				if (connected) {
					if (issue.getLikedBy().containsKey(personName)) {
						Person tmp = issue.getLikedBy().get(personName);
						Issue tmpIssue = new Issue(likedIssue);
						tmp.getLikedIssues().add(tmpIssue);
					} else {
						Person tmp = issue.getLikedIndirectlyBy().get(personName);
						if (tmp == null) {
							tmp = new Person();
							tmp.setName(personName);
							issue.getLikedIndirectlyBy().put(personName, tmp);							
						}
						Issue tmpIssue = new Issue(likedIssue);
						tmp.getLikedIssues().add(tmpIssue);
					}
				}
				output += curPath + "\n";
			}
		}
		System.out.println("\n\n");
		System.out.println(output);
		
	}

	private void clearDb() {
		try {
			FileUtils.deleteRecursively(new File(DB_PATH));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void registerShutdownHook(final GraphDatabaseService graphDb) {
		// Registers a shutdown hook for the Neo4j instance so that it
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
		// running example before it's completed)
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}
}
