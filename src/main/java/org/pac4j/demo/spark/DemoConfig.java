package org.pac4j.demo.spark;


import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.CallbackLogic;
import org.pac4j.core.engine.LogoutLogic;
import org.pac4j.core.engine.SecurityLogic;
import org.pac4j.core.http.HttpActionAdapter;
import org.pac4j.core.matching.Matcher;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.util.CommonHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


public class DemoConfig<R, C extends WebContext>
    extends org.pac4j.core.config.Config {

    public DemoConfig(final Clients clients) {
	super(clients);
    }
    
    public DemoConfig (final String callbackUrl, final Client... clients) {
	super(callbackUrl,clients);
    }
    
    public DemoConfig (final Client... clients) {
	super(clients);
    }

    public DemoConfig (final String callbackUrl, final Client client) {
	super(callbackUrl,client);
    }
    
    public DemoConfig (final Client client) {
	super(client);
    }

    
    private HttpActionAdapter<R,C> haa;
    
    public HttpActionAdapter<R,C> getHttpActionAdapter_FIXED() {
	return this.haa;
    }

    public void setHttpActionAdapter_FIXED(final HttpActionAdapter<R,C> haa) {
	this.haa = haa;
	setHttpActionAdapter(haa);
    }

}
