package com.cloudbees.jenkins.plugins.gitea;

import hudson.model.Action;

import java.net.URL;

/**
 * Link to Gitea
 *
 * @author Kurt Madel
 */
public class GiteaLink implements Action {
    /**
     * Maps to the directory name under webapp/images
     */
    private final String image;

    /**
     * Target of the hyperlink to take the user to.
     */
    private final String url;

    /*package*/ GiteaLink(String image, String url) {
        this.image = image;
        this.url = url;
    }

    /*package*/ GiteaLink(String image, URL url) {
        this(image,url.toExternalForm());
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/gitea-branch-source/images/"+ image +"/24x24.png";
    }

    @Override
    public String getDisplayName() {
        return "Gitea";
    }

    @Override
    public String getUrlName() {
        return url;
    }
}