/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.reportportal.auth.integration.github;

import com.google.common.base.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Simple GitHub client
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */

public class GitHubClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubClient.class);

    private final String githubBaseUrl;

    private final RestTemplate restTemplate;


    private GitHubClient(String accessToken, String githubBaseUrl) {
        this.restTemplate = new RestTemplate();
        this.restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                String errorMessage = "Unable to load Github Data:" + new String(getResponseBody(response), Charsets.UTF_8);
                LOGGER.error(errorMessage);
                throw new AuthenticationServiceException(errorMessage);
            }
        });
        this.restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("Authorization", "bearer " + accessToken);
            return execution.execute(request, body);
        });
        this.githubBaseUrl=githubBaseUrl;
    }

    public static GitHubClient withAccessToken(String accessToken, String githubBaseUrl) {
        return new GitHubClient(accessToken, githubBaseUrl);
    }

    public UserResource getUser() {
        return this.restTemplate.getForObject(this.githubBaseUrl + "/user", UserResource.class);
    }

    public Map<String, Object> getUserAttributes() {
      return getForObject(this.githubBaseUrl + "/user", new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    public List<EmailResource> getUserEmails() {
        return getForObject(this.githubBaseUrl + "/user/emails", new ParameterizedTypeReference<List<EmailResource>>() {
        });
    }

    public List<OrganizationResource> getUserOrganizations(String user) {
        return getForObject(this.githubBaseUrl + "/user/orgs", new ParameterizedTypeReference<List<OrganizationResource>>() {
        }, user);
    }

    public List<OrganizationResource> getUserOrganizations(UserResource user) {
        return getForObject(user.getOrganizationsUrl(), new ParameterizedTypeReference<List<OrganizationResource>>() {
        });
    }

    public ResponseEntity<Resource> downloadResource(String url) {
        return this.restTemplate.getForEntity(url, Resource.class);
    }

    private <T> T getForObject(String url, ParameterizedTypeReference<T> type, Object... urlVars) {
        return this.restTemplate.exchange(url, HttpMethod.GET, null, type, urlVars).getBody();
    }
}
