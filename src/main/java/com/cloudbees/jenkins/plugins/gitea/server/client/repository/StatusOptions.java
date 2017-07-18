package com.cloudbees.jenkins.plugins.gitea.server.client.repository;

import com.cloudbees.jenkins.plugins.gitea.api.GiteaStatusOptions;

/**
 * Created by kmadel on 6/14/17.
 */
public class StatusOptions implements GiteaStatusOptions {

    private StatusState state;
    private String target_url;
    private String description;
    private String context;

    public StatusState getState() {
        return state;
    }

    public void setState(StatusState state) {
        this.state = state;
    }

    public String getTarget_url() {
        return target_url;
    }

    public void setTarget_url(String target_url) {
        this.target_url = target_url;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setContext(String context) {
        this.context = context;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getContext() {
        return null;
    }
}
