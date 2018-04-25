/*
 * Copyright 2018 IBM Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
*/

package com.roguecloud.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.roguecloud.db.DatabaseInstance;
import com.roguecloud.db.DatabaseUtils;
import com.roguecloud.db.DbLeaderboardEntry;
import com.roguecloud.db.DbUser;
import com.roguecloud.db.MemoryDatabase;
import com.roguecloud.json.client.JsonUserRequest;
import com.roguecloud.json.client.JsonUserRequestResponse;
import com.roguecloud.utils.Logger;
import com.roguecloud.utils.ServerUtil;

/**
 * This is a JAX-RS class which serves both HTML pages (for display in the browser) and serves HTTP requests
 * from the Rogue Cloud agent client API. 
 * 
 * HTTP @GET methods annotated with @Produces("text/html") are returned to the browser, while the rest handle
 * agent client requests.  
 * 
 */
@Path("/")
public class RsDatabase {

	private static final Logger log = Logger.getInstance();
	
	@Context
	HttpServletRequest request;
	
	@Context
	HttpServletResponse response;

	@GET
	@Path("/round/recent")
	@Produces("text/html")
	public Response getRecentRoundResults() {
		
		if(isRedirectToRootNeeded()) {
			return null;
		}
		
		// Previous 4 days worth of rounds
		List<DbLeaderboardEntry> l = DatabaseInstance.get().getBestPreviousXRoundsOfLeaderboardEntries(960);
		
		DatabaseUtils.sortDescendingByScore(l);
				
		DatabasePage dp = new DatabasePage("Top 200 scores for recent rounds", 4);
		
		dp.getEntries().add(new ArrayList<String>(Arrays.asList("Rank", "Name", "Round #", "Score"))); 
		
		for(int rank = 0; rank < l.size() && rank < 200; rank++) {
			
			List<String> row = new ArrayList<>();
			
			DbLeaderboardEntry dle = l.get(rank);
			
			DbUser user = DatabaseInstance.get().getUserById(dle.getUserId());
			if(user == null) {
				log.severe("Unable to find yser with ID "+dle.getUserId()+" from dle with round id "+dle.getRoundId(), null);
				continue;
			}
			
			row.add((rank+1)+". ");
			row.add("<a href='../../user/"+user.getUserId()+"/'>"+user.getUsername()+"</a>");
			row.add("<a href='../"+dle.getRoundId()+"/'>"+dle.getRoundId()+"</a>");
			row.add(""+dle.getScore());
			
			dp.getEntries().add(row);
		}
		
		try {
			request.setAttribute("page", dp);
			
			request.getRequestDispatcher("/Table.jsp").forward(request, response);
		} catch (ServletException | IOException e) {
			e.printStackTrace();
			log.severe("Error from JSP page of get all round results", null);
		}
		
		return null;
				
	}

	
	@GET
	@Path("/round/all")
	@Produces("text/html")
	public Response getAllRoundResults() {
		
		if(isRedirectToRootNeeded()) {
			return null;
		}
				
		List<DbLeaderboardEntry> l = DatabaseInstance.get().getBestOverallLeaderboardEntries();
		
		DatabaseUtils.sortDescendingByScore(l);
				
		DatabasePage dp = new DatabasePage("Top 200 Scores for all rounds", 4);
		
		dp.getEntries().add(new ArrayList<String>(Arrays.asList("Rank", "Name", "Round #", "Score"))); 
		
		for(int rank = 0; rank < l.size() && rank < 200; rank++) {
			
			List<String> row = new ArrayList<>();
			
			DbLeaderboardEntry dle = l.get(rank);
			
			DbUser user = DatabaseInstance.get().getUserById(dle.getUserId());
			if(user == null) {
				log.severe("Unable to find yser with ID "+dle.getUserId()+" from dle with round id "+dle.getRoundId(), null);
				continue;
			}
			
			row.add((rank+1)+". ");
			row.add("<a href='../../user/"+user.getUserId()+"/'>"+user.getUsername()+"</a>");
			row.add("<a href='../"+dle.getRoundId()+"/'>"+dle.getRoundId()+"</a>");
			row.add(""+dle.getScore());
			
			dp.getEntries().add(row);
		}
		
		try {
			request.setAttribute("page", dp);
			
			request.getRequestDispatcher("/Table.jsp").forward(request, response);
		} catch (ServletException | IOException e) {
			e.printStackTrace();
			log.severe("Error from JSP page of get all round results", null);
		}
		
		return null;
				
	}
	
	@GET
	@Path("/round/{roundId}")
	public void getSpecificRoundResult(@PathParam("roundId") long roundId) {
		
		if(isRedirectToRootNeeded()) {
			return;
		}

		
		List<DbLeaderboardEntry> l = DatabaseInstance.get().getLeaderboardEntriesForARound(roundId);
		
		// Sort descending by score
		l.sort ((a, b) -> {
			return (int)(b.getScore() - a.getScore());
		});
		
		DatabasePage dp = new DatabasePage("Round "+roundId, 4);
		
		dp.getEntries().add(new ArrayList<String>(Arrays.asList("Rank", "Name", "Score"))); 
		
		for(int rank = 0; rank < l.size(); rank++) {
			
			List<String> row = new ArrayList<>();
			
			DbLeaderboardEntry dle = l.get(rank);
			
			DbUser user = DatabaseInstance.get().getUserById(dle.getUserId());
			if(user == null) {
				log.severe("Unable to find user id "+dle.getUserId()+" from dle with round id "+dle.getRoundId(), null);
				continue;
			}
			
			row.add((rank+1)+". ");
			row.add("<a href='../../user/"+user.getUserId()+"'>"+user.getUsername()+"</a>");
			row.add(""+dle.getScore());
			
			dp.getEntries().add(row);
		}
		
		try {
			request.setAttribute("page", dp);
			
			request.getRequestDispatcher("/Table.jsp").forward(request, response);
		} catch (ServletException | IOException e) {
			e.printStackTrace();
			log.severe("Error from JSP page of specific round result", null);
		}

		
	}
	
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/user/name/{username}")
	public Response createOrUpdateUser(JsonUserRequest r) {
		
		try {
			
			// Prevent password brute forcing by delaying every response by 500 msecs
//			Thread.sleep(500);
			
			MemoryDatabase db = DatabaseInstance.get();
			
			DbUser user = db.getUserByUsername(r.getUsername());
			if(user == null) {
				String passwordField = ServerUtil.SHA_256_FIELD+ServerUtil.oneWayFunction(r.getPassword());

				// User doesn't exist yet, so create them
				long userId = db.createUser(new DbUser(DbUser.NEW_USER_ID, r.getUsername().toLowerCase(), passwordField ));
				JsonUserRequestResponse reqResponse = new JsonUserRequestResponse();
				reqResponse.setUserId(userId);
				return Response.ok(reqResponse).build();
			}
			
			if(!user.getUsername().equalsIgnoreCase(r.getUsername())) {				
				return Response.status(Status.FORBIDDEN).entity("Username did not match what is in the database: "+user.getUsername()+" / "+r.getUsername()).type(MediaType.TEXT_PLAIN_TYPE).build();
			}
			
			if(!ServerUtil.computeAndCompareEqualPasswordHashes(user, r.getPassword())) {
				// If the password doesn't match... (note that password is case insensitive)
				return Response.status(Status.FORBIDDEN).entity("Provided password does not match what is in our database. Provide the correct password, or specify a new username.").type(MediaType.TEXT_PLAIN_TYPE).build();				
			}
			
			JsonUserRequestResponse reqResponse = new JsonUserRequestResponse();
			reqResponse.setUserId(user.getUserId());
			
			return Response.ok(reqResponse, MediaType.APPLICATION_JSON_TYPE).build();
		
		} catch(Exception e) {
			e.printStackTrace();
			return wrapException(e);
		}
	}
	
	
	
	@GET
	@Path("/user/{userid}")
	public Response getAllResultsForUser(@PathParam("userid") long userId) {
		
		if(isRedirectToRootNeeded()) {
			return null;
		}
		
		MemoryDatabase db = DatabaseInstance.get();
		
		DbUser user = db.getUserById(userId);
		if(user == null) {
			log.err("User not found: "+userId, null);
			return Response.status(404).build();
		}
		
		List<DbLeaderboardEntry> entries = db.getAllLeaderboardEntriesForUser(userId);
		DatabaseUtils.sortDescendingByScore(entries);

		
		HashMap<Long /* round id*/ , Long /* user position in round*/> map = new HashMap<>();
		
		for(DbLeaderboardEntry dle : entries) {
			
			List<DbLeaderboardEntry> roundResults = db.getLeaderboardEntriesForARound(dle.getRoundId());
			if(roundResults == null) {
				log.severe("Unable to locate round results for round: "+dle.getRoundId(), null);
				continue;
			}
			
			int placeMatch = -1;
			
			DatabaseUtils.sortDescendingByScore(roundResults);
			place_for: for(int place = 0; place < roundResults.size(); place++) {
				DbLeaderboardEntry e = roundResults.get(place);
				if(e.getUserId() == userId) {
					placeMatch = place+1;
					break place_for;
				}
			}
			
			if(placeMatch == -1) {
				log.severe("Unable to find a place match for "+dle.getRoundId()+ " "+dle.getUserId(), null);
			} else {
				map.put(dle.getRoundId(), (long)placeMatch);
				
			}
		}

		
		DatabasePage dp = new DatabasePage("High Scores for User "+user.getUsername(), 4);
		
		dp.getEntries().add(new ArrayList<String>(Arrays.asList("Round", "Rank", "Score"))); 
		
		for(DbLeaderboardEntry dle : entries) {
			List<String> row = new ArrayList<>();
			
			Long roundRank = map.get(dle.getRoundId());
			if(roundRank == null) { continue; /*Note: this shouldn't happen, and has been logged as severe above.*/ }
			
			row.add("<a href='../../round/"+dle.getRoundId()+"'>"+dle.getRoundId()+"</a>");
			row.add(roundRank != null ? "#"+roundRank : "N/A");
			row.add(""+dle.getScore());
			
			dp.getEntries().add(row);			
			
		}
		
		try {
			request.setAttribute("page", dp);
			
			request.getRequestDispatcher("/Table.jsp").forward(request, response);
		} catch (ServletException | IOException e) {
			e.printStackTrace();
			log.severe("Error from JSP page of get all results for user", null);
		}
		
		return null;
		
	}
	
//	@GET
//	@Path("/user/{userId}/round/{roundId}")
//	public Response getUserRoundData(@PathParam("userid") long userId, @PathParam("roundId") long roundId) {
//		
//		JsonDbLeaderboardList result = new JsonDbLeaderboardList();
//		
//		MemoryDatabase db = DatabaseInstance.get();
//		List<DbLeaderboardEntry> dbl = db.getAllLeaderboardEntriesForUserAndRound(userId, roundId);
//		
//		System.out.println(userId+" "+roundId);
//		
//		return Response.ok().build();
//	}

	private boolean isRedirectToRootNeeded() {
		if(!request.getRequestURI().endsWith("/")) {
			
			try {
				String newUrl = request.getRequestURI()+"/";
				while(newUrl.endsWith("//") ) {
					newUrl = newUrl.substring(newUrl.length()-1);
				}
				response.sendRedirect(newUrl);
			} catch (IOException e) {
				e.printStackTrace();
				log.severe("This shouldn't happen", e, null);
			}
			return true;
		}
		
		return false;
	}
	
	private static Response wrapException(Exception e) {
		String result = e.getMessage();
		
		return Response.serverError().entity(result).type(MediaType.TEXT_PLAIN_TYPE).build();
	}
	
	
	/** This class is used to generate the high score tables which are served to the browser AS HTML. A DatabasePage
	 * object is converted into HTML by Table.jsp.*/
	public static class DatabasePage {
		
		/** Each entry in the list is a row of a table, the contents of each entry are the columns of the able.*/
		private final List<List<String>> entries = new ArrayList<List<String>>();
		
		/** The page title */
		private final String title;
		
		private final int width;
		
		public DatabasePage(String title, int width) {
			this.width = width;
			this.title = title;
		}
		
		public List<List<String>> getEntries() {
			return entries;
		}
		
		public String getTitle() {
			return title;
		}
		
		public int getWidth() {
			return width;
		}
	}
}
