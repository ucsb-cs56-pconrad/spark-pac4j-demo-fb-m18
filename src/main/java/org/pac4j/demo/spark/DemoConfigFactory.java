package org.pac4j.demo.spark;

import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.config.ConfigFactory;
import org.pac4j.core.matching.PathMatcher;
import org.pac4j.oauth.client.FacebookClient;

import org.pac4j.sparkjava.SparkWebContext;

import spark.TemplateEngine;

public class DemoConfigFactory  {

    private final String salt;

    private final TemplateEngine templateEngine;

    public DemoConfigFactory(final String salt,
			     final TemplateEngine templateEngine) {
        this.salt = salt;
        this.templateEngine = templateEngine;
    }

    public DemoConfig<Object,SparkWebContext> build(final Object... parameters) {

        final FacebookClient facebookClient =
	    new FacebookClient("145278422258960",
			       "be21409ba8f39b5dae2a7de525484da8");
	
        final Clients clients = new Clients("http://localhost:8080/callback",
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
