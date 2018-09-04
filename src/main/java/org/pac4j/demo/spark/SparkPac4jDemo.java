package org.pac4j.demo.spark;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.http.client.indirect.FormClient;

import org.pac4j.core.http.HttpActionAdapter;

import org.pac4j.sparkjava.CallbackRoute;
import org.pac4j.sparkjava.LogoutRoute;
import org.pac4j.sparkjava.SecurityFilter;
import org.pac4j.sparkjava.SparkWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.mustache.MustacheTemplateEngine;

import spark.Spark;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.before;
import static spark.Spark.exception;


public class SparkPac4jDemo {

    private final static String SALT = "12345678901234567890123456789012";

    private final static Logger logger = LoggerFactory.getLogger(SparkPac4jDemo.class);

    private final static MustacheTemplateEngine templateEngine = new MustacheTemplateEngine();

    private static String PORT;
    
    static String getPortAsString() {
	if (PORT==null) {
	    PORT = Integer.toString(getHerokuAssignedPort());
	}
	return PORT;
    }
    
    static int getHerokuAssignedPort() {
	ProcessBuilder processBuilder = new ProcessBuilder();
	if (processBuilder.environment().get("PORT") != null) {
	    return Integer.parseInt(processBuilder.environment().get("PORT"));
	}
	return 8080; //return default port if heroku-port isn't set (i.e. on localhost)
    }

    /**

       return a HashMap with values of all the environment variables
       listed; print error message for each missing one, and exit if any
       of them is not defined.
    */
    
    public static HashMap<String,String>
	getNeededEnvVars(String [] neededEnvVars) {
	HashMap<String,String> envVars = new HashMap<String,String>();
	
	
	for (String k:neededEnvVars) {
	    String v = System.getenv(k);
	    envVars.put(k,v);
	}
	
	boolean error=false;
	for (String k:neededEnvVars) {
	    if (envVars.get(k)==null) {
		error = true;
		System.err.println("Error: Must define env variable " + k);
	    }
	}
	if (error) { System.exit(1); }
	
	return envVars;
    }

    public static void main(String[] args) {
	
	HashMap<String,String> envVars =
	    getNeededEnvVars(new String []{ "FACEBOOK_APP_ID",
					    "FACEBOOK_APP_SECRET",
					    "FACEBOOK_OAUTH_REDIRECT_URI",
					    "SALT",
					    "LOCALHOST_HTTPS"});

	Spark.port(getHerokuAssignedPort());	
	String keyStoreLocation = "keystore.jks";
	String keyStorePassword = "password";
	if (envVars.get("LOCALHOST_HTTPS").equals("true")) {
	    Spark.secure(keyStoreLocation, keyStorePassword, null, null);
	    System.out.println("Using https:");	    
	}
	

	final DemoConfig<Object,SparkWebContext> config =
	    new DemoConfigFactory(SALT, templateEngine, envVars).build();
	    
	Spark.get("/", SparkPac4jDemo::index, templateEngine);

	Spark.exception(Exception.class, (e, request, response) -> {
		logger.error("Unexpected exception", e);
		response.body(templateEngine.render(new ModelAndView(new HashMap<>(), "error500.mustache")));
	    });
    }

    private static ModelAndView index(final Request request, final Response response) {
	final Map<String,Object> map = new HashMap<String,Object>();
	map.put("profiles", getProfiles(request, response));
	final SparkWebContext ctx = new SparkWebContext(request, response);
	map.put("sessionId", ctx.getSessionIdentifier());
	return new ModelAndView(map, "index.mustache");
    }


    private static List<CommonProfile> getProfiles(final Request request, final Response response) {
	final SparkWebContext context = new SparkWebContext(request, response);
	final ProfileManager<CommonProfile> manager = new ProfileManager<CommonProfile>(context);
	return manager.getAll(true);
    }

}
