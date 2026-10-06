package com.vihan.hospital;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/** Logs a sample staff user in through the real API and returns an "Authorization" header value. */
public final class TestLogin {

	private TestLogin() {
	}

	public static String bearer(MockMvc mvc, String username) throws Exception {
		String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, VihanIntegrationTest.SAMPLE_PASSWORD)))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		return "Bearer " + JsonPath.read(body, "$.accessToken");
	}

}
