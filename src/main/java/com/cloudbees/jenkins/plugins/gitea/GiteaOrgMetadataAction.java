package com.cloudbees.jenkins.plugins.gitea;

import com.cloudbees.jenkins.plugins.gitea.api.GiteaOrganization;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Util;
import hudson.model.Hudson;
import jenkins.scm.api.metadata.AvatarMetadataAction;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.Stapler;

import java.io.IOException;
import java.io.ObjectStreamException;

/**
 * Invisible {@link AvatarMetadataAction} property that retains information about Gitea organization.
 *
 * @author Kurt Madel
 *
 */
public class GiteaOrgMetadataAction extends AvatarMetadataAction {
    @CheckForNull
    private final String avatar;

    public GiteaOrgMetadataAction(@NonNull GiteaOrganization org) throws IOException {
        this(org.getAvatarUrl());
    }

    public GiteaOrgMetadataAction(@CheckForNull String avatar) {
        this.avatar = Util.fixEmpty(avatar);
    }

    public GiteaOrgMetadataAction(@NonNull GiteaOrgMetadataAction that) {
        this(that.getAvatar());
    }

    private Object readResolve() throws ObjectStreamException {
        if (avatar != null && StringUtils.isBlank(avatar))
            return new GiteaOrgMetadataAction(this);
        return this;
    }

    @CheckForNull
    public String getAvatar() {
        return Util.fixEmpty(avatar);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAvatarImageOf(String size) {
        if (avatar == null) {
            // fall back to the generic gitea org icon
            String image = avatarIconClassNameImageOf(getAvatarIconClassName(), size);
            return image != null
                    ? image
                    : (Stapler.getCurrentRequest().getContextPath() + Hudson.RESOURCE_PATH
                    + "/plugin/github-branch-source/images/" + size + "/gitea-logo.png");
        } else {
            String[] xy = size.split("x");
            if (xy.length == 0) return avatar;
            if (avatar.contains("?")) return avatar + "&s=" + xy[0];
            else return avatar + "?s=" + xy[0];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAvatarIconClassName() {
        return avatar == null ? "icon-gitea-logo" : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAvatarDescription() {
        return Messages.GiteaOrgMetadataAction_IconDescription();
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

        GiteaOrgMetadataAction that = (GiteaOrgMetadataAction) o;

        return avatar != null ? avatar.equals(that.avatar) : that.avatar == null;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return (avatar != null ? avatar.hashCode() : 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "GiteaOrgMetadataAction{" +
                ", avatar='" + avatar + '\'' +
                "}";
    }

}