package com.orange.jira.login;

import com.atlassian.crowd.embedded.api.Directory;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.spi.UserDao;
import com.atlassian.crowd.exception.*;
import com.atlassian.crowd.exception.runtime.CommunicationException;
import com.atlassian.crowd.exception.runtime.OperationFailedException;
import com.atlassian.crowd.manager.directory.DirectoryManager;
import com.atlassian.jira.bc.security.login.LoginInfo;
import com.atlassian.jira.bc.security.login.LoginService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.ofbiz.OfBizDelegator;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.seraph.auth.AuthenticationErrorType;
import com.atlassian.seraph.auth.AuthenticatorException;
import com.atlassian.seraph.auth.DefaultAuthenticator;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShibbolethAuthenticator extends DefaultAuthenticator {

	private final Logger log = Logger.getLogger(ShibbolethAuthenticator.class);

	private final static String USER_ATR_CRITERIA = "UserAttribute";
	private final static String USER_ID = "userId";
	private final static String DIRECTORY_ID = "directoryId";
	private final static String NAME = "name";
	private final static String VALUE = "value";
	private final static String AUTO_GROUPS_ATTR = "autoGroupsAdded";

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
			if (log.isDebugEnabled()) {
				log.debug("User " + username + " is already logged in. We check if default groups needs to be added");
			}
			// If no login has been yet it is most probably a SSO user
			// for which login count is not updated. We try to update user details via the directory configuration.
			tryToUpdateGroupMembership(username);
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

	private void tryToUpdateGroupMembership(String username) {
		boolean dbg = log.isDebugEnabled();
		ApplicationUser user = ComponentAccessor.getUserManager().getUserByName(username);
		if (user != null) {
			Map<String, Object> criteria = new HashMap<>();
			criteria.put(USER_ID, user.getId());
			criteria.put(NAME, AUTO_GROUPS_ATTR);
			List<GenericValue> userAttributes = ComponentAccessor.getComponentOfType(OfBizDelegator.class).findByAnd(USER_ATR_CRITERIA, criteria);
			if (userAttributes != null && userAttributes.size() > 0) {
				if (dbg) {
					log.debug("User " + username + " has already had the default groups assigned");
				}
				return;
			}
			DirectoryManager directoryManager = ComponentAccessor.getComponentOfType(DirectoryManager.class);
			if (directoryManager != null) {
				try {
					Directory directory = directoryManager.findDirectoryById(user.getDirectoryId());
					if (directory != null) {
						String groups = (directory.getAttributes() != null) ? directory.getAttributes().get("autoAddGroups") : null;
						if (dbg) {
							log.debug("Auto add Groups for the directory are : " + groups != null ? groups : "null");
						}
						if (groups != null) {
							for (String groupName : groups.split("\\|")) {
								Group group = ComponentAccessor.getGroupManager().getGroup(groupName);
								if (group != null) {
									try {
										ComponentAccessor.getGroupManager().addUserToGroup(user, group);
									} catch (GroupNotFoundException e) {
										log.error(e);
									} catch (OperationNotPermittedException e) {
										log.error(e);
									}
								}
							}
						}

						this.updateUserAttributes(user);

						if (dbg) {
							log.debug("User " + username + " has been updated given its directory's configuration");
						}
					} else {
						log.warn("The user directory could not be fetched. Aborting group update");
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

	private void updateUserAttributes(ApplicationUser user) {
		Map<String, Object> criteria = new HashMap<>();
		criteria.put(USER_ID, user.getId());
		Map<String, Object> attributeData = new HashMap<>();
		attributeData.put(DIRECTORY_ID, user.getDirectoryId());
		attributeData.put(USER_ID, user.getId());
		attributeData.put(NAME, AUTO_GROUPS_ATTR);
		attributeData.put(VALUE, true);
		ComponentAccessor.getComponentOfType(OfBizDelegator.class).createValue(USER_ATR_CRITERIA, attributeData);
	}
}