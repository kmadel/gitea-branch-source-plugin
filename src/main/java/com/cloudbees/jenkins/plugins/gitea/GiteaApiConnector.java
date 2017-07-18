/*
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.cloudbees.jenkins.plugins.gitea;

import java.io.IOException;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.cloudbees.jenkins.plugins.gitea.api.GiteaApi;
import com.cloudbees.jenkins.plugins.gitea.server.client.GiteaServerAPIClient;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import hudson.Util;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.scm.api.SCMSourceOwner;

public class GiteaApiConnector {

    private String serverUrl;

    public GiteaApiConnector() {
    }

    public GiteaApiConnector(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public GiteaApi create(String owner, String repository, StandardUsernamePasswordCredentials creds) {
        return new GiteaServerAPIClient(serverUrl, owner, repository, creds);
    }

    public GiteaApi create(String owner, StandardUsernamePasswordCredentials creds) {
        return new GiteaServerAPIClient(serverUrl, owner, creds);
    }

    public static @Nonnull
    GiteaApi connect(@CheckForNull String apiUri, @CheckForNull String owner, @CheckForNull StandardUsernamePasswordCredentials credentials) throws IOException {
        return new GiteaServerAPIClient(apiUri, owner, credentials);
    }

    public static @Nonnull
    GiteaApi connect(@CheckForNull String apiUri, @CheckForNull String owner, @CheckForNull String repositoryName, @CheckForNull StandardUsernamePasswordCredentials credentials) throws IOException {
        return new GiteaServerAPIClient(apiUri, owner, repositoryName, credentials);
    }

    @CheckForNull 
    public <T extends StandardCredentials> T lookupCredentials(@CheckForNull SCMSourceOwner context, @CheckForNull String id, Class<T> type) {
        if (Util.fixEmpty(id) == null) {
            return null;
        } else {
            if (id != null) {
                return CredentialsMatchers.firstOrNull(
                          CredentialsProvider.lookupCredentials(type, context, ACL.SYSTEM,
                          giteaDomainRequirements()),
                          CredentialsMatchers.allOf(
                              CredentialsMatchers.withId(id),
                              CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(type))));
            }
            return null;
        }
    }

    public static @CheckForNull StandardUsernamePasswordCredentials lookupScanCredentials(@CheckForNull SCMSourceOwner context, @CheckForNull String apiUri, @CheckForNull String scanCredentialsId) {
        if (Util.fixEmpty(scanCredentialsId) == null) {
            return null;
        } else {
            return CredentialsMatchers.firstOrNull(
                    CredentialsProvider.lookupCredentials(
                            StandardUsernamePasswordCredentials.class,
                            context,
                            ACL.SYSTEM,
                            giteaDomainRequirements(apiUri)
                    ),
                    CredentialsMatchers.allOf(CredentialsMatchers.withId(scanCredentialsId), giteaCredentialsMatcher())
            );
        }
    }

    /**
     * Resolves the specified scan credentials in the specified context for use against the specified API endpoint.
     *
     * @param context           the context.
     * @param apiUri            the API endpoint.
     * @param scanCredentialsId the credentials to resolve.
     * @return the {@link StandardCredentials} or {@code null}
     */
    @CheckForNull
    public static StandardUsernamePasswordCredentials lookupScanCredentials(@CheckForNull Item context,
                                                            @CheckForNull String apiUri,
                                                            @CheckForNull String scanCredentialsId) {
        if (Util.fixEmpty(scanCredentialsId) == null) {
            return null;
        } else {
            return CredentialsMatchers.firstOrNull(
                    CredentialsProvider.lookupCredentials(
                            StandardUsernamePasswordCredentials.class,
                            context,
                            context instanceof Queue.Task
                                    ? Tasks.getDefaultAuthenticationOf((Queue.Task) context)
                                    : ACL.SYSTEM,
                            giteaDomainRequirements(apiUri)
                    ),
                    CredentialsMatchers.allOf(CredentialsMatchers.withId(scanCredentialsId), giteaCredentialsMatcher())
            );
        }
    }

    public ListBoxModel fillCheckoutCredentials(StandardListBoxModel result, SCMSourceOwner context) {
        result.withMatching(giteaCheckoutCredentialsMatcher(), CredentialsProvider.lookupCredentials(
                StandardCredentials.class, context, ACL.SYSTEM, giteaDomainRequirements()));
        return result;
    }

    public ListBoxModel fillCredentials(StandardListBoxModel result, SCMSourceOwner context) {
        result.withMatching(giteaCredentialsMatcher(), CredentialsProvider.lookupCredentials(
                StandardUsernameCredentials.class, context, ACL.SYSTEM, giteaDomainRequirements()));
        return result;
    }

    /* package */ static CredentialsMatcher giteaCredentialsMatcher() {
        return CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));
    }

    /* package */ static CredentialsMatcher giteaCheckoutCredentialsMatcher() {
        return CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardCredentials.class));
    }

    /* package */ List<DomainRequirement> giteaDomainRequirements() {
        return URIRequirementBuilder.fromUri(serverUrl).build();
    }

    private static List<DomainRequirement> giteaDomainRequirements(String apiUri) {
        return URIRequirementBuilder.fromUri(apiUri).build();
    }

}
