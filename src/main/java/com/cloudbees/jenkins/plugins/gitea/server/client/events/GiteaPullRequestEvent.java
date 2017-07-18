package com.cloudbees.jenkins.plugins.gitea.server.client.events;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Created by kmadel on 6/16/17.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class GiteaPullRequestEvent {

    private String action;


}
