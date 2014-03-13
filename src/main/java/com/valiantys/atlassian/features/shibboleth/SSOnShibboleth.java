package com.valiantys.atlassian.features.shibboleth;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

/**
 * SSO for Shibboleth - business class.
 * @author Maxime Cojan
 *
 */
public class SSOnShibboleth {

	/**
	 * Logger.
	 */
	private final static Logger LOG = Logger.getLogger(SSOnShibboleth.class);

	/**
	 * User Id form shibboleth.
	 */
	private String userId = null;

	/**
	 * Get the ssoSibboleth object.
	 * 
	 * @param request
	 * @return
	 */
	public static SSOnShibboleth getSSOShibboleth(HttpServletRequest request) {
		SSOnShibboleth ssoShibboleth = new SSOnShibboleth();
		// Since they aren't logged in, get the user name from the REMOTE_USER
		// header
		if(request != null){
			if(request.getAttribute("uid") != null){
				String remoteUser = String.valueOf(request.getAttribute("uid"));
				LOG.info("The remote user from the header is : " + remoteUser);

				if(remoteUser!=null && remoteUser.length()>0){
					// Convert username to all lowercase
					ssoShibboleth.setUserId(remoteUser.toLowerCase());
				}else{
					LOG.debug("The remoteUser is null");
				}
			}else{
				LOG.debug("The uid attribute is null");
			}
		}else{
			LOG.debug("The request is null");
		}
		return ssoShibboleth;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
}
