package com.roguecloud.server;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import com.roguecloud.RCSharedConstants;

/** 
 * Game clients may use this class to query whether a specific client API version is supported by the server.
 * 
 * Currently the logic to check compatibility is a simple equals comparison, but this may be updated in the future
 * if more loose backwards compatibility is required.
 * 
 **/
@Path("/apiVersion")
public class RsApiVersion {

	@GET
	@Path("/{apiVersion}/supported")
	public Response get(@PathParam("apiVersion") String apiVersion) {
		
		if(apiVersion == null) {
			// Bad Request
			return Response.status(400).build();
		}
		
		else if(!apiVersion.equals(RCSharedConstants.CLIENT_API_VERSION)) {
			// Unsupported version
			return Response.status(405).build();
		}
		
		// Supported version
		return Response.status(200).build();
		
	}
	
}
