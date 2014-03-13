package com.valiantys.atlassian.features.shibboleth;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.exception.FailedAuthenticationException;
import com.atlassian.crowd.exception.runtime.CommunicationException;
import com.atlassian.crowd.exception.runtime.OperationFailedException;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.user.util.OSUserConverter;
import com.atlassian.seraph.auth.AuthenticationErrorType;
import com.atlassian.seraph.auth.AuthenticatorException;
import com.atlassian.seraph.auth.DefaultAuthenticator;

/**
 * Authenticator custom dedicated to shibboleth integration.
 * 
 * @author Maxime Cojan
 * 
 */
public class ShibbolethAuthenticator extends DefaultAuthenticator {

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = 1492747625496115491L;

	/**
	 * Logger
	 */
	private final static Logger LOG = Logger.getLogger(ShibbolethAuthenticator.class);

	private Properties pluginSettings;
	
	private final static String CONFIG_FILE = "shibboleth-authenticator.properties";
	

	private List<String> urlToPass;
	
	@Override
	public Principal getUser(HttpServletRequest request, HttpServletResponse response) {
		return getUserFromShibbolethAuthentication(request, response);
	}

	/**
	 * Returns the currently Shibboleth logged in user.
	 * 
	 * @param request
	 *            : HttpRequest.
	 * @param response
	 *            : HttpResponse.
	 * @return
	 */
	public Principal getUserFromShibbolethAuthentication(HttpServletRequest request, HttpServletResponse response) {
		LOG.debug("Request made to " + request.getRequestURL() + " triggered this Authentication check");

		HttpSession httpSession = request.getSession();
		Principal user = null;
		
	
		// Check if the user is already logged in
		if (httpSession.getAttribute(LOGGED_IN_KEY) != null) {
			LOG.info("Session found; user already logged in");
			user = (Principal) httpSession.getAttribute(LOGGED_IN_KEY);
			return user;
		} else if(passThroughUrlFilter(request.getRequestURL().toString())){ 
			LOG.debug("Session is empty found; no user logged in");
			
			SSOnShibboleth ssoShibboleth = SSOnShibboleth.getSSOShibboleth(request);
			LOG.info("Here the SSOnShibboleth we have got : " + ssoShibboleth);
			if (ssoShibboleth != null) {
				// Seamless login from intranet
				LOG.info("Trying seamless Single Sign-on...");
				String username = ssoShibboleth.getUserId();
				if (username != null) {
					LOG.debug("The user name form shibboleth : " + username);
					user = getUser(username);
					LOG.info("Logged in via SSO, with JIRA User " + user);
					request.getSession().setAttribute(DefaultAuthenticator.LOGGED_IN_KEY, user);
					request.getSession().setAttribute(DefaultAuthenticator.LOGGED_OUT_KEY, null);
				} else {
					LOG.debug("Remote user was null or empty, can not perform authentication");
				}
			} else {
				LOG.info("ssoShibboleth is null; redirecting");
				// user was not found, or not currently valid
				return null;
			}
		}
		return user;
	}

	@Override
	protected boolean authenticate(final Principal user, final String password) throws AuthenticatorException {
		LOG.debug("method authenticate usr : " + user);
		LOG.debug("method authenticate passwd : " + password);

		try {
			getCrowdService().authenticate(user.getName(), password);
			return true;
		} catch (FailedAuthenticationException e) {
			return false;
		} catch (CommunicationException ex) {
			throw new AuthenticatorException(AuthenticationErrorType.CommunicationError);
		} catch (OperationFailedException ex) {
			// Unexpected error - log the stacktrace.
			LOG.error("Error occurred while trying to authenticate user '" + user.getName() + "'.", ex);
			throw new AuthenticatorException(AuthenticationErrorType.UnknownError);
		}
	}

	/**
	 * Uses OSUser to retrieve a Principal for a given username. Returns null if
	 * no user exists.
	 */
	protected Principal getUser(String username) {
		return OSUserConverter.convertToOSUser(getCrowdService().getUser(username));
	}

	/**
	 * Get a fresh version of the Crowd Read Write service from Pico Container.
	 * 
	 * @return fresh version of the Crowd Read Write service from Pico
	 *         Container.
	 */
	private CrowdService getCrowdService() {
		return ComponentManager.getComponent(CrowdService.class);
	}

	private Properties initProperties(String configurationFile) throws IOException {
		final ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Properties properties = new Properties();
		if (configurationFile != null && configurationFile.length() > 0) {
			properties.load(loader.getResourceAsStream(configurationFile));
		}
		return properties;
	}

	private boolean passThroughUrlFilter(String url){
		if(url == null){
			return false;
		}
		boolean passFilter = true;
		if(pluginSettings == null){
			try {
				pluginSettings = initProperties(CONFIG_FILE);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(urlToPass == null){
			String url_to_skip = pluginSettings.getProperty("skipped.url");
			LOG.debug("url_to_skip : " + url_to_skip);
			
			StringTokenizer st = new StringTokenizer(url_to_skip, ";");
			while (st.hasMoreTokens()) {
				if(urlToPass == null){
					urlToPass = new ArrayList<String>();
				}
				urlToPass.add(st.nextToken());
			}
		}
		
		if(urlToPass != null){
			for (String urlConfigured : urlToPass) {
				if(url.indexOf(urlConfigured)>-1){
					passFilter = false;
				}
			}
		}
		return passFilter;
	}
	
}


