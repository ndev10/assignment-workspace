package com.oauth;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@WebAppConfiguration
@SpringBootTest(classes = OauthServiceApplication.class)
public class OauthGenerateTokenTests {

    @Value("${oauth.client.clientId}")
    private String clientId;

    @Value("${oauth.client.clientSecret}")
    private String clientSecret;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    private MockMvc mockMvc;

    private static final String CONTENT_TYPE = "application/json;charset=UTF-8";

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).addFilter(springSecurityFilterChain).build();
    }

    @Test
    public void given_right_credentials_then_generate_token() throws Exception {
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "password");
        params.add("client_id", clientId);
        params.add("username", "john");
        params.add("password", "password");


        ResultActions result = mockMvc.perform(post("/oauth/token")
                .params(params)
                .with(httpBasic(clientId, clientSecret))
                .accept(CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(CONTENT_TYPE));

        String resultString = result.andReturn().getResponse().getContentAsString();

        JacksonJsonParser jsonParser = new JacksonJsonParser();
        String token =  jsonParser.parseMap(resultString).get("access_token").toString();
        Assert.assertNotNull(token);
    }

    @Test
    public void given_invalid_password_then_error() throws Exception {
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "password");
        params.add("username",  "john");
        params.add("password", "invalidPassword");


        ResultActions result = mockMvc.perform(post("/oauth/token")
                .params(params)
                .with(httpBasic(clientId, clientSecret))
                .accept(CONTENT_TYPE))
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(CONTENT_TYPE));


        MockHttpServletResponse response = result.andReturn().getResponse();
        String resultString = response.getContentAsString();

        JacksonJsonParser jsonParser = new JacksonJsonParser();
        String error= jsonParser.parseMap(resultString).get("error").toString();

        Assert.assertEquals(response.getStatus(), 400);
        Assert.assertEquals(error,"invalid_grant");
    }

    @Test
    public void given_invalid_client_id_then_gives_error() throws Exception {
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "password");
        params.add("username",  "john");
        params.add("password", "wrongpassword");


        ResultActions result = mockMvc.perform(post("/oauth/token")
                .params(params)
                .with(httpBasic("InvalidClient", clientSecret))
                .accept(CONTENT_TYPE))
                .andExpect(status().is4xxClientError());


        MockHttpServletResponse response = result.andReturn().getResponse();
        Assert.assertEquals(response.getStatus(), 401);

    }

}
