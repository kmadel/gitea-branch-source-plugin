package com.cloudbees.jenkins.plugins.gitea.server.client.issues;

import com.cloudbees.jenkins.plugins.gitea.api.GiteaIssue;

import java.util.List;

/**
 * Created by kmadel on 8/2/16.
 */
public class GiteaServerIssue implements GiteaIssue {

    private String title;

    private String body;

    private String assignee;

    private List<Integer> labels;

    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public List<Integer> getLabels() {
        return labels;
    }

    public void setLabels(List<Integer> labels) {
        this.labels = labels;
    }
}
