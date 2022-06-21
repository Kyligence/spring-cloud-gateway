package io.kyligence.kap.gateway.utils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MdxAuthenticationUtilsTest {

	@Mock
	private ServerHttpRequest request;

	@Test
	public void getProject() {
		Assert.assertNull(MdxAuthenticationUtils.getProject("/dataset/list"));
		Assert.assertEquals("AdventureWorks", MdxAuthenticationUtils.getProject("/mdx/xmla/AdventureWorks"));
		Assert.assertEquals("AdventureWorks", MdxAuthenticationUtils.getProject("/mdx/xmla_server/AdventureWorks"));
		Assert.assertEquals("AdventureWorks", MdxAuthenticationUtils.getProject("/mdx/xmla_server/AdventureWorks/clearCache"));
		Assert.assertNull(MdxAuthenticationUtils.getProject("/mdx/xmla_server/clearCache"));
	}

	@Test
	public void getUsername() {
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
		HttpHeaders httpHeaders = new HttpHeaders();
		MultiValueMap<String, HttpCookie> httpCookies = new LinkedMultiValueMap<>();
		when(request.getQueryParams()).thenReturn(queryParams);
		when(request.getHeaders()).thenReturn(httpHeaders);
		when(request.getCookies()).thenReturn(httpCookies);
		{
			Assert.assertNull(MdxAuthenticationUtils.getUsername(request));
		}
		{
			queryParams.add("EXECUTE_AS_USER_ID", "ADMIN1");
			Assert.assertEquals("ADMIN1", MdxAuthenticationUtils.getUsername(request));
			queryParams.remove("EXECUTE_AS_USER_ID");
		}
		{
			queryParams.add("username", "ADMIN2");
			Assert.assertEquals("ADMIN2", MdxAuthenticationUtils.getUsername(request));
			queryParams.remove("username");
		}
		{
			httpHeaders.setBasicAuth("ADMIN3", "PASSWORD");
			Assert.assertEquals("ADMIN3", MdxAuthenticationUtils.getUsername(request));
			httpHeaders.remove("Authorization");
		}
		{
			httpHeaders.set("GatewayAuth", "Basic QURNSU40OktZTElOQDEyMw==");
			Assert.assertEquals("ADMIN4", MdxAuthenticationUtils.getUsername(request));
			httpHeaders.remove("GatewayAuth");
		}
		{
			httpCookies.add("mdx_session_7fb21c59", new HttpCookie("mdx_session_7fb21c59", "elBuQ014dEh1TzpBRE1JTjphMjQ5NzRhMDAwYzhmYTI5MDAwYzhhMWY1NmRmOWMwMjoxNjU1NzQyODk4"));
			Assert.assertEquals("ADMIN", MdxAuthenticationUtils.getUsername(request));
		}
	}

}