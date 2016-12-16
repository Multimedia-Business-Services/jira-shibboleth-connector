package com.orange.jira.login;

import com.atlassian.crowd.directory.DelegatedAuthenticationDirectory;
import com.atlassian.crowd.directory.RemoteDirectory;
import com.atlassian.crowd.directory.loader.DirectoryInstanceLoader;
import com.atlassian.crowd.embedded.api.Directory;
import com.atlassian.crowd.exception.DirectoryNotFoundException;
import com.atlassian.crowd.exception.FailedAuthenticationException;
import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.crowd.exception.runtime.CommunicationException;
import com.atlassian.crowd.exception.runtime.OperationFailedException;
import com.atlassian.crowd.manager.directory.DirectoryManager;
import com.atlassian.jira.bc.security.login.LoginInfo;
import com.atlassian.jira.bc.security.login.LoginService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.login.LoginStore;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.seraph.auth.AuthenticationErrorType;
import com.atlassian.seraph.auth.AuthenticatorException;
import com.atlassian.seraph.auth.DefaultAuthenticator;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;

public class ShibbolethAuthenticator extends DefaultAuthenticator {

	private final Logger log = Logger.getLogger(ShibbolethAuthenticator.class);

	@Override
	public Principal getUser(HttpServletRequest request, HttpServletResponse response) {
		String username = request.getRemoteUser();
		if (!userIsAlreadyLogged(request) && !urlIsSkipped(request)) {
			if (username != null) {
				Principal user = getUser(username);
				putPrincipalInSessionContext(request, user);
			}
		} else {
			// User is already logged in. We check whether a connection has already been detected by JIRA.
			if (this.isFirstLogin(request.getRemoteUser())) {
				// If no login has been yet. It is either the very first connection of our user, or it is a SSO user
				// for which login count is not updated. We try to update user details via the directory configuration.
				tryToUpdateGroupMembership(username);
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
		for (String skippedUrl : SKIPPED_URLS) {
			if (request.getRequestURL().toString().contains(skippedUrl)) {
				return true;
			}
		}
		return false;
	}

	private static boolean userIsAlreadyLogged(HttpServletRequest request) {
		return request.getSession().getAttribute(LOGGED_IN_KEY) != null;
	}

	private final static long serialVersionUID = 1492747625496115491L;
	private final static List<String> SKIPPED_URLS = Arrays.asList("security-tokens", "/Shibboleth.sso/Logout", "/logout");

	private boolean isFirstLogin(String username) {
		if (StringUtils.isEmpty(username)) {
			return false;
		}
		LoginService loginService = ComponentAccessor.getComponentOfType(LoginService.class);
		if (loginService != null) {
			LoginInfo info = loginService.getLoginInfo(username);
			if (log.isDebugEnabled() && info != null) {
				log.debug("User " + username + " connection count is at : " + info.getLoginCount());
			}
			return info == null || info.getLoginCount() == null || info.getLoginCount() == 0;
		} else {
			log.error("LoginService is not available");
		}

		return false;
	}

	private void tryToUpdateGroupMembership(String username) {
		ApplicationUser user = ComponentAccessor.getUserManager().getUserByName(username);
		if (user != null) {
			DirectoryManager directoryManager = ComponentAccessor.getComponentOfType(DirectoryManager.class);
			if (directoryManager != null) {
				try {
					Directory directory = directoryManager.findDirectoryById(user.getDirectoryId());
					DirectoryInstanceLoader directoryInstanceLoader =  ComponentAccessor.getComponentOfType(DirectoryInstanceLoader.class);
					if (directoryInstanceLoader != null) {
						RemoteDirectory remoteDirectory = directoryInstanceLoader.getDirectory(directory);
						if (remoteDirectory instanceof DelegatedAuthenticationDirectory) {
							((DelegatedAuthenticationDirectory) remoteDirectory).addOrUpdateLdapUser(username);
							// Finally we record a successful login so as to update the groups only once per user.
							LoginStore loginStore = ComponentAccessor.getComponentOfType(LoginStore.class);
							loginStore.recordLoginAttempt(user.getDirectoryUser(), true);
						}
					}
				} catch (DirectoryNotFoundException e) {
					log.error("Directory with ID " + user.getDirectoryId() + " has not been found"); // Should not happen since user is supposed to be already logged in
				} catch (UserNotFoundException e) {
					// Should not happen. ALl checks have been made previously
					log.error("UserNotFoundException detected !", e); // Should not happen since user is supposed to be already logged in
				} catch (com.atlassian.crowd.exception.OperationFailedException e) {
					log.warn("Could not update user details for " + username, e);
				}
			} else {
				log.error("Directory Manager is not available : we cannot proceed with group assignment");
			}
		} else {
			log.warn("Unable to find user for name : " + username + " eventhough he is already logged in !");
		}
	}
}