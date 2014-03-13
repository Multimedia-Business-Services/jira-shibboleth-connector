package com.valiantys.atlassian.features.shibboleth;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Category;

import com.atlassian.seraph.auth.Authenticator;
import com.atlassian.seraph.auth.AuthenticatorException;
import com.atlassian.seraph.config.SecurityConfig;
import com.atlassian.seraph.config.SecurityConfigFactory;
import com.atlassian.seraph.config.SecurityConfigImpl;

/**
 * Servelt which manage the jira deconnection. Here we are using a properties
 * file to have the possiblity of esay change the url value for different
 * environmen.
 * 
 * @author Maxime Cojan
 * 
 */
public class LogoutServlet extends HttpServlet {

	/**
	 * The key used to store the user object in the session
	 */
	public static final String LOGGED_IN_KEY = "seraph_defaultauthenticator_user";

	/**
	 * The key used to indicate that the user has logged out and session
	 * regarding of it containing a cookie is not logged in.
	 */
	public static final String LOGGED_OUT_KEY = "seraph_defaultauthenticator_logged_out_user";

	/**
	 * Logger.
	 */
	private static final Category LOG = Category.getInstance(LogoutServlet.class);

	private final static String CONFIG_FILE = "shibboleth-authenticator.properties";

	private String url_logout;

	private String url_login_jira;
	
	private String no_shib_context;
	
	private SecurityConfig securityConfig;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		try {
			Properties pluginSettings = initProperties(CONFIG_FILE);
			url_logout = pluginSettings.getProperty("logout.url");
			url_login_jira = pluginSettings.getProperty("loggin.jira");
			no_shib_context = pluginSettings.getProperty("context.noShib");
			LOG.debug("Url logout : " + url_logout);
			LOG.debug("Url login : " + url_login_jira);
			LOG.debug("Url no shib : " + no_shib_context);

			securityConfig = (SecurityConfig) config.getServletContext().getAttribute(SecurityConfigImpl.STORAGE_KEY);
		} catch (IOException e) {
			throw new ServletException("Error loading the plugin properties file.", e);
		}

	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doLogout(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doLogout(request, response);
	}

	protected void doLogout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession httpSession = request.getSession();
		if (logout(request, response)) {
			LOG.debug("The logout is done.");
		}
		if(request.getRequestURL().toString().indexOf(no_shib_context) > -1){
			response.sendRedirect(request.getContextPath()+url_login_jira);	
		}else{
			response.sendRedirect(url_logout);	
		}
		
	}

	public boolean logout(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		LOG.debug("logout requested.");
		// loginManager.logout(request, response);

		try {
			final Authenticator authenticator = getAuthenticator();
			authenticator.logout(request, response);
		} catch (final AuthenticatorException e) {
			throw new ServletException("Seraph authenticator couldn't log out", e);
		}

		request.getSession().setAttribute(LOGGED_IN_KEY, null);
		request.getSession().setAttribute(LOGGED_OUT_KEY, Boolean.TRUE);
		
		return true;
	}

	private Properties initProperties(String configurationFile) throws IOException {
		final ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Properties properties = new Properties();
		if (configurationFile != null && configurationFile.length() > 0) {
			properties.load(loader.getResourceAsStream(configurationFile));
		}
		return properties;
	}

	protected SecurityConfig getSecurityConfig() {
		return securityConfig;
	}

	protected Authenticator getAuthenticator() {
		return getSecurityConfig().getAuthenticator();
	}

}