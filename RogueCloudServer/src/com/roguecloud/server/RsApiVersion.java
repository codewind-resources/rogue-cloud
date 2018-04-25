package com.roguecloud.server;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import com.roguecloud.RCSharedConstants;

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
