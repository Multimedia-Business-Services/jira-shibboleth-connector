package com.valiantys.atlassian.features.shibboleth;

import static com.atlassian.seraph.auth.DefaultAuthenticator.LOGGED_IN_KEY;
import static com.atlassian.seraph.auth.DefaultAuthenticator.LOGGED_OUT_KEY;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.atlassian.seraph.auth.AuthenticatorException;
import com.atlassian.seraph.config.SecurityConfig;
import com.atlassian.seraph.config.SecurityConfigImpl;

public class LogoutServlet extends HttpServlet {

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		security = (SecurityConfig) config.getServletContext().getAttribute(SecurityConfigImpl.STORAGE_KEY);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		logout(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logout(request, response);
	}

	private void logout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			security.getAuthenticator().logout(request, response);
			removePrincipalFromSessionContext(request);
			response.sendRedirect("/Shibboleth.sso/Logout");
		} catch (final AuthenticatorException e) {
			throw new ServletException("Seraph authenticator couldn't log out", e);
		}
	}

	private void removePrincipalFromSessionContext(final HttpServletRequest httpServletRequest) {
		final HttpSession httpSession = httpServletRequest.getSession();
		httpSession.setAttribute(LOGGED_IN_KEY, null);
		httpSession.setAttribute(LOGGED_OUT_KEY, Boolean.TRUE);
	}

	private SecurityConfig security;
	private static final long serialVersionUID = 1L;

}