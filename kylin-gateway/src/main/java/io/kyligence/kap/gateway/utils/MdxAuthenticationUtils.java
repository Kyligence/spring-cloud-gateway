/*
 * Copyright (C) 2020 Kyligence Inc. All rights reserved.
 *
 * http://kyligence.io
 *
 * This software is the confidential and proprietary information of
 * Kyligence Inc. ("Confidential Information"). You shall not disclose
 * such Confidential Information and shall use it only in accordance
 * with the terms of the license agreement you entered into with
 * Kyligence Inc.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package io.kyligence.kap.gateway.utils;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MdxAuthenticationUtils {

	private static final Pattern URI_PATTERN_0 = Pattern.compile("/mdx/xmla/(.+)");

	private static final Pattern URI_PATTERN_1 = Pattern.compile("/mdx/xmla_server/(.+)");

	private static final String MDX_AUTH = "MDXAUTH";

	private static final String HEADER_NAME_GATEWAY = "GatewayAuth";

	private static final String EXECUTE_AS_USER_ID = "EXECUTE_AS_USER_ID";

	private static final String USERNAME = "username";

	private static final String AUTHORIZATION = "Authorization";

	private static final int SALT_LENGTH = 5;

	private MdxAuthenticationUtils() {
	}

	public static String getProjectContext(String contextPath) {
		String projectContext = "";
		Matcher uriMatcher = URI_PATTERN_0.matcher(contextPath);
		if (uriMatcher.find()) {
			projectContext = uriMatcher.group(1);
		} else {
			uriMatcher = URI_PATTERN_1.matcher(contextPath);
			if (uriMatcher.find()) {
				projectContext = uriMatcher.group(1);
			}
		}
		return projectContext;
	}

	public static String getProjectName(String projectContext) {
		String project = "";
		int clearCacheFlagIdx = projectContext.indexOf("clearCache");
		int deprecateCacheFlagIdx = projectContext.indexOf("/clearCache");
		if (deprecateCacheFlagIdx != -1 && "".equals(projectContext.substring(0, deprecateCacheFlagIdx))) {
			// etc "/mdx/xmla//clearCache"
		} else if (deprecateCacheFlagIdx != -1 && !"".equals(projectContext.substring(0, deprecateCacheFlagIdx))) {
			// etc "/mdx/xmla/learn_kylin/clearCache"
			project = projectContext.substring(0, deprecateCacheFlagIdx);
		} else if (clearCacheFlagIdx != -1) {
			// etc "/mdx/xmla/clearCache"
		} else {
			// etc "/mdx/xmla/learn_kylin"
			project = projectContext;
		}
		return project;
	}

	public static String getUsername(ServerHttpRequest request) {
		String username;

		// Delegate user, get user from 'EXECUTE_AS_USER_ID' parameter
		username = request.getQueryParams().getFirst(EXECUTE_AS_USER_ID);
		if (StringUtils.isNotBlank(username)) {
			return username;
		}

		// Get user from 'username' parameter
		username = request.getQueryParams().getFirst(USERNAME);
		if (StringUtils.isNotBlank(username)) {
			return username;
		}

		HttpHeaders headers = request.getHeaders();
		// Get username from 'authorization' header
		List<String> authList = headers.get(AUTHORIZATION);
		if (authList != null) {
			username = parseAuthInfo(authList.get(0));
			return username;
		}

		// Get username from cookie
		HttpCookie mdxAuthCookie = getSessionAuthCookie(request);
		if (mdxAuthCookie != null) {
			String cookieValue = mdxAuthCookie.getValue();
			String decodeTxt = decodeTxt(SALT_LENGTH, cookieValue);
			String[] authInfos = decodeTxt.split(":");
			username = authInfos[0];
			return username;
		}

		// Get username from 'GatewayAuth' header
		List<String> gatewayAuthList = headers.get(HEADER_NAME_GATEWAY);
		if (gatewayAuthList != null && gatewayAuthList.get(0) != null) {
			return parseAuthInfo(gatewayAuthList.get(0));
		}

		return username;
	}

	private static String parseAuthInfo(String authorization) {
		String[] basicAuthInfos = authorization.split("\\s");
		if (basicAuthInfos.length < 2) {
			return null;
		} else {
			String basicAuth = new String(Base64.decode(basicAuthInfos[1]));
			String[] authInfos = basicAuth.split(":", 2);
			if (authInfos.length < 2) {
				return null;
			} else {
				return authInfos[0];
			}
		}
	}

	private static HttpCookie getSessionAuthCookie(ServerHttpRequest request) {
		MultiValueMap<String, HttpCookie> cs = request.getCookies();
		List<HttpCookie> httpCookieList = cs.get(MDX_AUTH);
		if (httpCookieList == null) {
			return null;
		}
		for (HttpCookie cookie : httpCookieList) {
			if (MDX_AUTH.equals(cookie.getName())) {
				return cookie;
			}
		}
		return null;
	}

	public static String decodeTxt(int length, String encodedTxt) {
		String decodeUserPwd = new String(Base64.decode(encodedTxt), StandardCharsets.UTF_8);
		return decodeUserPwd.substring(length);
	}

	public static String getUserFromParameter(HttpHeaders headers, String parameter) {
		String username = "";
		List<String> valueList = headers.get(parameter);
		if (valueList != null) {
			username = valueList.get(0);
			if (StringUtils.isNotBlank(username)) {
				return username;
			}
		}
		return username;
	}

}
