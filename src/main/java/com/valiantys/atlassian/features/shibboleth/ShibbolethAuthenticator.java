package com.valiantys.atlassian.features.shibboleth;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.crowd.exception.FailedAuthenticationException;
import com.atlassian.crowd.exception.runtime.CommunicationException;
import com.atlassian.crowd.exception.runtime.OperationFailedException;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.seraph.auth.AuthenticationErrorType;
import com.atlassian.seraph.auth.AuthenticatorException;
import com.atlassian.seraph.auth.DefaultAuthenticator;

public class ShibbolethAuthenticator extends DefaultAuthenticator {

	@Override
	public Principal getUser(HttpServletRequest request, HttpServletResponse response) {
		request.setAttribute("uid", "admin");

		if (!userIsAlreadyLogged(request) && !urlIsSkipped(request)) {
			SSOnShibboleth sso = SSOnShibboleth.newInstance(request);
			String username = sso.getUid();
			if (username != null) {
				Principal user = getUser(username);
				putPrincipalInSessionContext(request, user);
			}
		}
		return (Principal) request.getSession().getAttribute(LOGGED_IN_KEY);
	}

	@Override
	protected Principal getUser(String username) {
		return ComponentAccessor.getCrowdService().getUser(username);
	}

	@Override
	protected boolean authenticate(final Principal user, final String password) throws AuthenticatorException {
		try {
			ComponentAccessor.getCrowdService().authenticate(user.getName(), password);
			return true;
		} catch (FailedAuthenticationException e) {
			return false;
		} catch (CommunicationException ex) {
			throw new AuthenticatorException(AuthenticationErrorType.CommunicationError);
		} catch (OperationFailedException ex) {
			throw new AuthenticatorException(AuthenticationErrorType.UnknownError);
		}
	}

	private boolean urlIsSkipped(HttpServletRequest request) {
		return SKIPPED_URLS.contains(request.getRequestURL().toString());
	}

	private static boolean userIsAlreadyLogged(HttpServletRequest request) {
		return request.getSession().getAttribute(LOGGED_IN_KEY) != null;
	}

	private final static long serialVersionUID = 1492747625496115491L;
	private final static List<String> SKIPPED_URLS = Arrays.asList("security-tokens", "/ShibbolethLogout", "/logout");
}