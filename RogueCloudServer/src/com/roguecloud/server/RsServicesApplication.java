package com.roguecloud.server;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/** JAX-RS application class, containing the RsApiVersion service. */
@ApplicationPath("/services")
public class RsServicesApplication extends Application {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Set<Class<?>> getClasses() {
		HashSet hs = new HashSet();
		hs.add(RsApiVersion.class);
		return hs;
	}

}
