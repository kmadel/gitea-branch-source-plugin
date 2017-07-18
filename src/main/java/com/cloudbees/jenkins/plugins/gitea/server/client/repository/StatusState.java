package com.cloudbees.jenkins.plugins.gitea.server.client.repository;

/**
 * Created by kmadel on 6/14/17.
 */
public enum StatusState {
    PENDING("pending"),
    SUCCESS("success"),
    ERROR("error"),
    FAILURE("failure"),
    WARNING("warning");

    private String value;

    private StatusState(String value) {
        this.value = value;
    }

}
