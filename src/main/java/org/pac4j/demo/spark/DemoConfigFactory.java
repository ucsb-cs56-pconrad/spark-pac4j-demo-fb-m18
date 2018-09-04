package org.pac4j.demo.spark;

import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.config.ConfigFactory;
import org.pac4j.core.matching.PathMatcher;
import org.pac4j.oauth.client.FacebookClient;

import org.pac4j.sparkjava.SparkWebContext;

import spark.TemplateEngine;
import java.util.HashMap;

public class DemoConfigFactory  {

    private final String salt;

    private final TemplateEngine templateEngine;

    private final HashMap<String,String> envVars;
    
    public DemoConfigFactory(final String salt,
			     final TemplateEngine templateEngine,
			     final HashMap<String,String> envVars) {
        this.salt = salt;
        this.templateEngine = templateEngine;
	this.envVars = envVars;
    }

    public DemoConfig<Object,SparkWebContext> build(final Object... parameters) {

        final FacebookClient facebookClient =
	    new FacebookClient(envVars.get("FACEBOOK_APP_ID"),
			       envVars.get("FACEBOOK_APP_SECRET"));
	
	final Clients clients =
	    new Clients(envVars.get("FACEBOOK_OAUTH_REDIRECT_URI"),
			facebookClient);
	
        final DemoConfig<Object,SparkWebContext> config =
	    new DemoConfig<Object,SparkWebContext>(clients);
	
        config.addAuthorizer("admin",
			     new RequireAnyRoleAuthorizer("ROLE_ADMIN"));
        config.addAuthorizer("custom",
			     new CustomAuthorizer());
        config.addMatcher("excludedPath",
			  new PathMatcher().excludeRegex("^/facebook/notprotected$"));
        config.setHttpActionAdapter_FIXED(new DemoHttpActionAdapter(templateEngine));
        return config;

    }
}
