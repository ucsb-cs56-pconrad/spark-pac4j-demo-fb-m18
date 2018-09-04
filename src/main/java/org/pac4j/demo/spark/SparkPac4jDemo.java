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

import org.pac4j.jwt.config.signature.SecretSignatureConfiguration;
import org.pac4j.jwt.profile.JwtGenerator;
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

    private final static String JWT_SALT = "12345678901234567890123456789012";

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
    
    public static void main(String[] args) {

	    
	Spark.port(getHerokuAssignedPort());
	final Config config =
	    new DemoConfigFactory(JWT_SALT, templateEngine).build();
	    
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
	Spark.before("/twitter",
		     new SecurityFilter(config, "TwitterClient,FacebookClient"));
	Spark.before("/form",
		     new SecurityFilter(config, "FormClient"));
	Spark.before("/basicauth",
		     new SecurityFilter(config, "IndirectBasicAuthClient"));
	Spark.before("/cas", new SecurityFilter(config, "CasClient"));
	Spark.before("/saml2", new SecurityFilter(config, "SAML2Client"));
	Spark.before("/oidc", new SecurityFilter(config, "OidcClient"));
	Spark.before("/protected", new SecurityFilter(config, null));
	Spark.before("/dba", new SecurityFilter(config, "DirectBasicAuthClient,ParameterClient"));
	Spark.before("/rest-jwt",
		     new SecurityFilter(config, "ParameterClient"));
	Spark.get("/facebook", SparkPac4jDemo::protectedIndex, templateEngine);
        Spark.get("/facebook/notprotected", SparkPac4jDemo::protectedIndex, templateEngine);
	Spark.get("/facebookadmin", SparkPac4jDemo::protectedIndex, templateEngine);
	Spark.get("/facebookcustom", SparkPac4jDemo::protectedIndex, templateEngine);
	Spark.get("/twitter", SparkPac4jDemo::protectedIndex, templateEngine);
	Spark.get("/form", SparkPac4jDemo::protectedIndex, templateEngine);
	Spark.get("/basicauth", SparkPac4jDemo::protectedIndex, templateEngine);
	Spark.get("/cas", SparkPac4jDemo::protectedIndex, templateEngine);
	Spark.get("/saml2", SparkPac4jDemo::protectedIndex, templateEngine);
	Spark.get("/saml2-metadata", (rq, rs) -> {
		SAML2Client samlclient = config.getClients().findClient(SAML2Client.class);
		samlclient.init(new SparkWebContext(rq, rs));
		return samlclient.getServiceProviderMetadataResolver().getMetadata();
	    });
	Spark.get("/jwt", SparkPac4jDemo::jwt, templateEngine);
	Spark.get("/oidc", SparkPac4jDemo::protectedIndex, templateEngine);
	Spark.get("/protected", SparkPac4jDemo::protectedIndex, templateEngine);
	Spark.get("/dba", SparkPac4jDemo::protectedIndex, templateEngine);
	Spark.get("/rest-jwt", SparkPac4jDemo::protectedIndex, templateEngine);
	Spark.get("/loginForm", (rq, rs) -> form(config), templateEngine);
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
	final Map map = new HashMap();
	map.put("profiles", getProfiles(request, response));
	final SparkWebContext ctx = new SparkWebContext(request, response);
	map.put("sessionId", ctx.getSessionIdentifier());
	return new ModelAndView(map, "index.mustache");
    }

    private static ModelAndView jwt(final Request request, final Response response) {
	final SparkWebContext context = new SparkWebContext(request, response);
	final ProfileManager manager = new ProfileManager(context);
	final Optional<CommonProfile> profile = manager.get(true);
	String token = "";
	if (profile.isPresent()) {
	    JwtGenerator generator = new JwtGenerator(new SecretSignatureConfiguration(JWT_SALT));
	    token = generator.generate(profile.get());
	}
	final Map map = new HashMap();
	map.put("token", token);
	return new ModelAndView(map, "jwt.mustache");
    }

    private static ModelAndView form(final Config config) {
	final Map map = new HashMap();
	final FormClient formClient = config.getClients().findClient(FormClient.class);
	map.put("callbackUrl", formClient.getCallbackUrl());
	return new ModelAndView(map, "loginForm.mustache");
    }

    private static ModelAndView protectedIndex(final Request request, final Response response) {
	final Map map = new HashMap();
	map.put("profiles", getProfiles(request, response));
	return new ModelAndView(map, "protectedIndex.mustache");
    }

    private static List<CommonProfile> getProfiles(final Request request, final Response response) {
	final SparkWebContext context = new SparkWebContext(request, response);
	final ProfileManager manager = new ProfileManager(context);
	return manager.getAll(true);
    }

    private static ModelAndView forceLogin(final Config config, final Request request, final Response response) {
        final SparkWebContext context = new SparkWebContext(request, response);
        final String clientName = context.getRequestParameter(Clients.DEFAULT_CLIENT_NAME_PARAMETER);
	final Client client = config.getClients().findClient(clientName);
	HttpAction action;
	try {
	    action = client.redirect(context);
	} catch (final HttpAction e) {
	    action = e;
	}
	config.getHttpActionAdapter().adapt(action.getCode(), context);
	return null;
    }
}
