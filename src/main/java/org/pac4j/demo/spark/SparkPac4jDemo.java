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

import org.pac4j.saml.client.SAML2Client;
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

	final CallbackRoute callback = new CallbackRoute(config, null, true);
	//callback.setRenewSession(false);	
	Spark.get("/callback", callback);
	Spark.post("/callback", callback);
	
	final SecurityFilter facebookFilter =
	    new SecurityFilter(config, "FacebookClient", "", "excludedPath");
	
	Spark.before("/facebook", facebookFilter);
	Spark.before("/facebook/*", facebookFilter);
	Spark.before("/facebookadmin",
		     new SecurityFilter(config, "FacebookClient", "admin"));
	Spark.before("/facebookcustom",
		     new SecurityFilter(config, "FacebookClient", "custom"));

	Spark.before("/protected", new SecurityFilter(config, null));


	Spark.get("/facebook", SparkPac4jDemo::protectedIndex, templateEngine);
        Spark.get("/facebook/notprotected", SparkPac4jDemo::protectedIndex, templateEngine);
	Spark.get("/facebookadmin", SparkPac4jDemo::protectedIndex, templateEngine);
	Spark.get("/facebookcustom", SparkPac4jDemo::protectedIndex, templateEngine);

	Spark.get("/protected", SparkPac4jDemo::protectedIndex, templateEngine);
	
	final LogoutRoute localLogout =
	    new LogoutRoute(config, "/?defaulturlafterlogout");
	localLogout.setDestroySession(true);
	Spark.get("/logout", localLogout);
	final LogoutRoute centralLogout = new LogoutRoute(config);
	centralLogout.setDefaultUrl("http://localhost:" + getPortAsString() +
				    "/?defaulturlafterlogoutafteridp");
	centralLogout.setLogoutUrlPattern("http://localhost:" +
					  getPortAsString() + "/.*");
	centralLogout.setLocalLogout(false);
	centralLogout.setCentralLogout(true);
	centralLogout.setDestroySession(true);
	Spark.get("/centralLogout", centralLogout);
	Spark.get("/forceLogin", (rq, rs) -> forceLogin(config, rq, rs));

	/*before("/body", (request, response) -> {
	  logger.debug("before /body");
	  });*/
	//before("/body", new SecurityFilter(config, "AnonymousClient"));

	Spark.before("/body", new SecurityFilter(config, "HeaderClient"));
	Spark.post("/body", (request, response) -> {
		logger.debug("Body: " + request.body());
		return "done: " + getProfiles(request, response);
	    });

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


    private static ModelAndView protectedIndex(final Request request, final Response response) {
	final Map<String,Object> map = new HashMap<String,Object>();
	map.put("profiles", getProfiles(request, response));
	return new ModelAndView(map, "protectedIndex.mustache");
    }

    private static List<CommonProfile> getProfiles(final Request request, final Response response) {
	final SparkWebContext context = new SparkWebContext(request, response);
	final ProfileManager<CommonProfile> manager = new ProfileManager<CommonProfile>(context);
	return manager.getAll(true);
    }

    private static ModelAndView forceLogin(final DemoConfig<Object,SparkWebContext> config, final Request request, final Response response) {
        final SparkWebContext context = new SparkWebContext(request, response);
        final String clientName = context.getRequestParameter(Clients.DEFAULT_CLIENT_NAME_PARAMETER);
	final Client client = config.getClients().findClient(clientName);
	HttpAction action;
	try {
	    action = client.redirect(context);
	} catch (final HttpAction e) {
	    action = e;
	}
	HttpActionAdapter<Object,SparkWebContext> haa = config.getHttpActionAdapter_FIXED();
	haa.adapt(action.getCode(), context);
	return null;
    }
}
