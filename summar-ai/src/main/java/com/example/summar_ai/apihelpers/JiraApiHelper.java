package com.example.summar_ai.apihelpers;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class JiraApiHelper {

    private final RestTemplate restTemplate = new RestTemplate();

    public String getUserIssuesForSite(String accessToken, String siteUrl, String cloudId, LocalDate start, LocalDate end) {
        System.out.println("Getting Issues from site (cloudId=" + cloudId + ")");

        // Step 1: Get user's accountId
        String accountId = getAccountId(accessToken, cloudId);

        // Step 2: Build raw JQL
        String jql = String.format(
                "(creator = 'accountid:%s' OR assignee = 'accountid:%s' OR reporter = 'accountid:%s') " +
                        "AND ((created >= '%s' AND created <= '%s') " +
                        "OR (updated >= '%s' AND updated <= '%s') " +
                        "OR (resolved >= '%s' AND resolved <= '%s'))",
                accountId, accountId, accountId,
                start, end,
                start, end,
                start, end
        );

        System.out.println("Raw JQL: " + jql);

        // Step 3: URL-encode the JQL manually
        String encodedJql = UriUtils.encodeQuery(jql, StandardCharsets.UTF_8);

        // Step 4: Build full URL using encoded JQL
        String url = UriComponentsBuilder
                .fromUriString("https://api.atlassian.com/ex/jira/" + cloudId + "/rest/api/3/search")
                .queryParam("jql", encodedJql)
                .build(false)  // Don't re-encode query params
                .toUriString();

        System.out.println("Final Jira API URL: " + url);

        // Step 5: Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Step 6: Make request and parse results
        ResponseEntity<Map> response = restTemplate.exchange(URI.create(url), HttpMethod.GET, entity, Map.class);

        StringBuilder issuesString = new StringBuilder();

        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> body = response.getBody();
            List<Map<String, Object>> issues = (List<Map<String, Object>>) body.get("issues");

            for (Map<String, Object> issue : issues) {
                issuesString.append("Issue Key: ").append(issue.get("key")).append("\n");

                Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
                issuesString.append("Summary: ").append(fields.get("summary")).append("\n");
                issuesString.append("Description: ").append(fields.get("description")).append("\n");
                issuesString.append("\n-----\n");
            }
        } else {
            System.out.println("Failed to fetch issues: " + response.getStatusCode());
        }

        return issuesString.toString();
    }

    // Reused as-is
    public String getAccountId(String accessToken, String cloudId) {
        String url = "https://api.atlassian.com/ex/jira/" + cloudId + "/rest/api/3/myself";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> body = response.getBody();
            return (String) body.get("accountId");
        } else {
            throw new RuntimeException("Failed to get account ID from Jira: " + response.getStatusCode());
        }
    }

    // Reused as-is
    public List<Map<String, Object>> getAccessibleJiraSites(String accessToken) {
        System.out.println("Getting Accessible Jira Sites");
        String url = "https://api.atlassian.com/oauth/token/accessible-resources";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
        return response.getBody();
    }
}
