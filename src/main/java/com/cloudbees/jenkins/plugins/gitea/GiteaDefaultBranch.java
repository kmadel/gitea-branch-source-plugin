package com.cloudbees.jenkins.plugins.gitea;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.InvisibleAction;
import java.io.Serializable;

/**
 * @author Kurt Madel
 */
public class GiteaDefaultBranch extends InvisibleAction implements Serializable {
    private static final long serialVersionUID = 1L;
    @NonNull
    private final String repoOwner;
    @NonNull
    private final String repository;
    @NonNull
    private final String defaultBranch;

    public GiteaDefaultBranch(@NonNull String repoOwner, @NonNull String repository, @NonNull String defaultBranch) {
        this.repoOwner = repoOwner;
        this.repository = repository;
        this.defaultBranch = defaultBranch;
    }

    @NonNull
    public String getRepoOwner() {
        return repoOwner;
    }

    @NonNull
    public String getRepository() {
        return repository;
    }

    @NonNull
    public String getDefaultBranch() {
        return defaultBranch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GiteaDefaultBranch that = (GiteaDefaultBranch) o;

        if (!repoOwner.equals(that.repoOwner)) {
            return false;
        }
        if (!repository.equals(that.repository)) {
            return false;
        }
        return defaultBranch.equals(that.defaultBranch);
    }

    @Override
    public int hashCode() {
        int result = repoOwner.hashCode();
        result = 31 * result + repository.hashCode();
        result = 31 * result + defaultBranch.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "GiteaDefaultBranch{" +
                "repoOwner='" + repoOwner + '\'' +
                ", repository='" + repository + '\'' +
                ", defaultBranch='" + defaultBranch + '\'' +
                '}';
    }

}
