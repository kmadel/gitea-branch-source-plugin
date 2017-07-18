package com.cloudbees.jenkins.plugins.gitea;

import jenkins.scm.api.metadata.AvatarMetadataAction;

/**
 * Created by kmadel on 6/20/17.
 */
public class GiteaRepoMetadataAction  extends AvatarMetadataAction {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAvatarIconClassName() {
        return "icon-gitea-repo";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAvatarDescription() {
        return Messages.GiteaRepoMetadataAction_IconDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return true;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "GiteaRepoMetadataAction{}";
    }
}
