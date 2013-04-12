package olle.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import olle.socialapp.domain.Issue;
import olle.socialapp.issueservice.IssueService;
import olle.socialapp.issueservice.JsonContainer;

import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphdb.Node;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class EmbeddedHttp {
	private NeoServer neoServer;

	public void initNeo() {
		this.neoServer = new NeoServer();
	}

	public void initHttp() {
		HttpServer server;
		try {
			server = HttpServer.create(new InetSocketAddress(8085), 0);
			System.out.println("server created");
			server.createContext("/neo4j/addLike", new MyHandler(this,
					neoServer));
			server.createContext("/neo4j/getLikesByPerson", new GetLikes(
					neoServer));
			server.createContext("/neo4j/followPerson", new FollowPerson(this,
					neoServer));
			server.createContext("/neo4j/dontFollowPerson",
					new DontFollowPerson(this, neoServer));
			server.createContext("/neo4j/addFollowersToIssues",
					new AddFollowers(this, neoServer));
			server.createContext("/neo4j/getFollowers",
					new GetFollowers(this, neoServer));
			server.setExecutor(null); // creates a default executor
			server.start();

			System.out.println("server started");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected Map<String, String> getInput(String decodedString) {
		HashMap<String, String> input = new HashMap<String, String>();
		for (String entry : decodedString.split("&")) {
			String[] keyvalue = entry.split("=");
			input.put(keyvalue[0], keyvalue[1]);
		}
		return input;
	}

	public static void main(String[] args) {
		EmbeddedHttp mainServer = new EmbeddedHttp();
		mainServer.initNeo();
		mainServer.initHttp();
	}

	static class MyHandler implements HttpHandler {

		private NeoServer neoServer;
		private EmbeddedHttp mainServer;

		public MyHandler(EmbeddedHttp mainServer, NeoServer neoServer) {
			this.mainServer = mainServer;
			this.neoServer = neoServer;
		}

		@Override
		public void handle(HttpExchange t) throws IOException {
			InputStream is = t.getRequestBody();
			BufferedReader in = new BufferedReader(new InputStreamReader(is));
			String inp = null;
			while ((inp = in.readLine()) != null) {

				String decoded = URLDecoder.decode(inp, "utf-8");
				Map<String, String> input = mainServer.getInput(decoded);
				System.out.println("parsed input: " + input);
				String action = input.remove("action");
				if (action.equals("CreateLike")) {
					try {
						Node node = neoServer.addLike(input);
						neoServer.printDb();
						neoServer.printLikesFromNode(node);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}
			String response = "This is the response";
			t.sendResponseHeaders(200, response.length());
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}

	}

	static class GetLikes implements HttpHandler {

		private NeoServer neoServer;

		public GetLikes(NeoServer neoServer) {
			this.neoServer = neoServer;
		}

		@Override
		public void handle(HttpExchange t) throws IOException {
			System.out.println("query: " + t.getRequestURI().getQuery());
			String unid = t.getRequestURI().getQuery().split("=")[1];
			System.out.println("unid: " + unid);
			IssueService issueService = new IssueService(neoServer);
			List<Issue> issues = issueService.getIssuesByUser(unid);
			System.out.println("issues found: " + issues);
			Headers h = t.getResponseHeaders();
			h.set("Content-Type", "application/json;charset=utf-8");
			h.set("Cache-Control", "no-cache");

			t.sendResponseHeaders(200, 0);

			OutputStream os = t.getResponseBody();
			ObjectMapper mapper = new ObjectMapper();
			JsonContainer container = new JsonContainer();
			container.setIssues(issues);
			mapper.writeValue(os, container);

			os.close();

		}

	}

	static class FollowPerson implements HttpHandler {

		private NeoServer neoServer;
		private EmbeddedHttp mainServer;

		public FollowPerson(EmbeddedHttp mainServer, NeoServer neoServer) {
			this.mainServer = mainServer;
			this.neoServer = neoServer;
		}

		@Override
		public void handle(HttpExchange t) throws IOException {
			InputStream is = t.getRequestBody();
			BufferedReader in = new BufferedReader(new InputStreamReader(is));
			String inp = in.readLine();
			String decoded = URLDecoder.decode(inp, "utf-8");
			Map<String, String> input = mainServer.getInput(decoded);
			System.out.println("incoming data: " + input);
			try {
				Node source = neoServer.addFollower(input);
				neoServer.printDb();
				neoServer.printFollowersFromNode(source);
			} catch (Exception e) {
				e.printStackTrace();
			}
			t.sendResponseHeaders(200, "".length());
			OutputStream os = t.getResponseBody();
			os.write("".getBytes());
			os.close();
		}

	}

	static class DontFollowPerson implements HttpHandler {

		private NeoServer neoServer;
		private EmbeddedHttp mainServer;

		public DontFollowPerson(EmbeddedHttp mainServer, NeoServer neoServer) {
			this.mainServer = mainServer;
			this.neoServer = neoServer;
		}

		@Override
		public void handle(HttpExchange t) throws IOException {
			InputStream is = t.getRequestBody();
			BufferedReader in = new BufferedReader(new InputStreamReader(is));
			String inp = in.readLine();
			String decoded = URLDecoder.decode(inp, "utf-8");
			Map<String, String> input = mainServer.getInput(decoded);
			System.out.println("incoming data: " + input);
			try {
				Node source = neoServer.removeFollower(input);
				neoServer.printDb();
				neoServer.printFollowersFromNode(source);
			} catch (Exception e) {
				e.printStackTrace();
			}
			t.sendResponseHeaders(200, "".length());
			OutputStream os = t.getResponseBody();
			os.write("".getBytes());
			os.close();
		}

	}

	static class AddFollowers implements HttpHandler {

		@SuppressWarnings("unused")
		private EmbeddedHttp mainServer;
		private NeoServer neoServer;

		public AddFollowers(EmbeddedHttp mainServer, NeoServer neoServer) {
			this.mainServer = mainServer;
			this.neoServer = neoServer;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void handle(HttpExchange t) throws IOException {
			try {
				InputStream is = t.getRequestBody();
				BufferedReader in = new BufferedReader(new InputStreamReader(is));
				String inp = in.readLine();
				String decoded = URLDecoder.decode(inp, "utf-8");
				System.out.println("input json: " + decoded);
				
				ObjectMapper mapper = new ObjectMapper();
				
				Map<String, Object> userData = mapper.readValue(decoded, Map.class);
				String currentUser = (String) userData.get("user");
				List<Map<String, Object>> data = (List<Map<String, Object>>) userData
						.get("data");
				Set<String> followers = neoServer.getFollowers(currentUser);
				for (Map<String, Object> jsonObj : data) {
					Map<String, Object> responsible = (Map<String, Object>) jsonObj
							.get("responsible");
					boolean following = followers.contains(responsible
							.get("personId"));
					responsible.put("following", following);
				}

				Headers h = t.getResponseHeaders();
				h.set("Content-Type", "application/json;charset=utf-8");
				h.set("Cache-Control", "no-cache");

				t.sendResponseHeaders(200, 0);

				OutputStream os = t.getResponseBody();
				mapper.writeValue(os, data);
				
			} catch (Exception e) {
				e.printStackTrace();
				if (e instanceof IOException) {
					throw (IOException) e;
				}
			}
		}
	}
	
	static class GetFollowers implements HttpHandler {

		@SuppressWarnings("unused")
		private EmbeddedHttp mainServer;
		private NeoServer neoServer;

		public GetFollowers(EmbeddedHttp mainServer, NeoServer neoServer) {
			this.mainServer = mainServer;
			this.neoServer = neoServer;
		}
		
		@Override
		public void handle(HttpExchange t) throws IOException {
			try {
				String unid = t.getRequestURI().getQuery().split("=")[1];
				Set<String> followers = neoServer.getFollowers(unid);				
				Map<String, Object> data = new HashMap<String, Object>();
				data.put("followers", followers);
				ObjectMapper mapper = new ObjectMapper();
								
				Headers h = t.getResponseHeaders();
				h.set("Content-Type", "application/json;charset=utf-8");
				h.set("Cache-Control", "no-cache");

				t.sendResponseHeaders(200, 0);

				OutputStream os = t.getResponseBody();
				mapper.writeValue(os, data);
				
			} catch (Exception e) {
				e.printStackTrace();
				if (e instanceof IOException) {
					throw (IOException) e;
				}
			}			
		}
		
	}
}
