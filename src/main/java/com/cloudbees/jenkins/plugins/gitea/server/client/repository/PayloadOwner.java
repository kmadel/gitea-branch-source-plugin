package com.cloudbees.jenkins.plugins.gitea.server.client.repository;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Created by kmadel on 8/1/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayloadOwner {

    private long id;
    private String full_name;
    private String email;
    private String username;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFull_name() {
        return full_name;
    }

    public PayloadOwner setFull_name(String full_name) {
        this.full_name = full_name;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public PayloadOwner setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public PayloadOwner setUsername(String username) {
        this.username = username;
        return this;
    }
}
