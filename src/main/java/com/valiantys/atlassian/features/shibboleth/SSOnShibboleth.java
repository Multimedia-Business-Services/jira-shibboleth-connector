package com.valiantys.atlassian.features.shibboleth;

import javax.servlet.http.HttpServletRequest;

public class SSOnShibboleth {

	public static SSOnShibboleth newInstance(HttpServletRequest request) {
		SSOnShibboleth ssoShibboleth = new SSOnShibboleth();
		if (request != null && request.getAttribute(UID_ATTRIBUTE_NAME) != null) {
			String uid = String.valueOf(request.getAttribute(UID_ATTRIBUTE_NAME));
			if (uid != null) {
				ssoShibboleth.setUid(uid.toLowerCase());
			}
		}
		return ssoShibboleth;
	}

	public String getUid() {
		return uid;
	}

	private void setUid(String uid) {
		this.uid = uid;
	}

	private String uid = null;
	private static final String UID_ATTRIBUTE_NAME = "uid";
}