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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;

import com.cloudbees.jenkins.plugins.gitea.hooks.GiteaOrgWebhook;
import hudson.console.HyperlinkNote;
import hudson.model.Action;
import hudson.model.Item;
import jenkins.scm.api.*;
import jenkins.scm.api.metadata.ObjectMetadataAction;
import jenkins.scm.impl.UncategorizedSCMSourceCategory;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.jenkins.plugins.gitea.api.GiteaApi;
import com.cloudbees.jenkins.plugins.gitea.api.GiteaRepository;
import com.cloudbees.jenkins.plugins.gitea.api.GiteaOrganization;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

public class GiteaSCMNavigator extends SCMNavigator {

    private final String repoOwner;
    private final String credentialsId;
    private final String checkoutCredentialsId;
    private String pattern = ".*";
    private boolean autoRegisterHooks = false;
    private String giteaServerUrl;
    private int sshPort = -1;

    /**
     * Gitea API client connector.
     */
    private transient GiteaApiConnector giteaConnector;

    @DataBoundConstructor 
    public GiteaSCMNavigator(String repoOwner, String credentialsId, String checkoutCredentialsId) {
        this.repoOwner = repoOwner;
        this.credentialsId = Util.fixEmpty(credentialsId);
        this.checkoutCredentialsId = checkoutCredentialsId;
    }

    @DataBoundSetter 
    public void setPattern(String pattern) {
        Pattern.compile(pattern);
        this.pattern = pattern;
    }

    @DataBoundSetter
    public void setAutoRegisterHooks(boolean autoRegisterHooks) {
        this.autoRegisterHooks = autoRegisterHooks;
    }

    public String getRepoOwner() {
        return repoOwner;
    }

    @CheckForNull
    public String getCredentialsId() {
        return credentialsId;
    }

    @CheckForNull
    public String getCheckoutCredentialsId() {
        return checkoutCredentialsId;
    }

    public String getPattern() {
        return pattern;
    }

    public boolean isAutoRegisterHooks() {
        return autoRegisterHooks;
    }

    public int getSshPort() {
        return sshPort;
    }

    @DataBoundSetter
    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    @DataBoundSetter
    public void setGiteaServerUrl(String url) {
        this.giteaServerUrl = Util.fixEmpty(url);
        if (this.giteaServerUrl != null) {
            // Remove a possible trailing slash
            this.giteaServerUrl = this.giteaServerUrl.replaceAll("/$", "");
        }
    }

    @CheckForNull
    public String getGiteaServerUrl() {
        return giteaServerUrl;
    }

    public void setGiteaConnector(@NonNull GiteaApiConnector giteaConnector) {
        this.giteaConnector = giteaConnector;
    }

    private GiteaApiConnector getGiteaConnector() {
        if (giteaConnector == null) {
            giteaConnector = new GiteaApiConnector(giteaServerUrl);
        }
        return giteaConnector;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public List<Action> retrieveActions(@NonNull SCMNavigatorOwner owner,
                                        @CheckForNull SCMNavigatorEvent event,
                                        @NonNull TaskListener listener) throws IOException, InterruptedException {
        // TODO when we have support for trusted events, use the details from event if event was from trusted source
        listener.getLogger().printf("Looking up details of %s...%n", getRepoOwner());
        List<Action> result = new ArrayList<>();
        StandardUsernamePasswordCredentials credentials =
                getGiteaConnector().lookupScanCredentials((Item) owner, giteaServerUrl, checkoutCredentialsId);

        GiteaApi giteaApi = getGiteaConnector().create(repoOwner, credentials);

        GiteaOrganization org = giteaApi.getOrganization();
        String objectUrl = org.getHtmlUrl() == null ? null : org.getHtmlUrl().toExternalForm();
        result.add(new ObjectMetadataAction(
                Util.fixEmpty(org.getName()),
                null,
                objectUrl)
        );
        result.add(new GiteaOrgMetadataAction(org));
        result.add(new GiteaLink("icon-gitea-logo", org.getHtmlUrl()));
        if (objectUrl == null) {
            listener.getLogger().println("Organization URL: unspecified");
        } else {
            listener.getLogger().printf("Organization URL: %s%n",
                    HyperlinkNote.encodeTo(objectUrl, StringUtils.defaultIfBlank(org.getName(), objectUrl)));
        }
        return result;
    }


    @NonNull
    @Override
    protected String id() {
        return giteaServerUrl + "::" + repoOwner;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterSave(@NonNull SCMNavigatorOwner owner) {
        try {
            StandardUsernamePasswordCredentials credentials =
                    getGiteaConnector().lookupScanCredentials((Item) owner, giteaServerUrl, checkoutCredentialsId);

            GiteaApi giteaApi = getGiteaConnector().create(repoOwner, credentials);
            GiteaOrgWebhook.register(giteaApi, repoOwner);
        } catch (IOException e) {
            DescriptorImpl.LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
    }

    @Override
    public void visitSources(SCMSourceObserver observer) throws IOException, InterruptedException {
        TaskListener listener = observer.getListener();

        if (StringUtils.isBlank(repoOwner)) {
            listener.getLogger().format("Must specify a repository owner%n");
            return;
        }
        StandardUsernamePasswordCredentials credentials = getGiteaConnector().lookupCredentials(observer.getContext(),
                credentialsId, StandardUsernamePasswordCredentials.class);

        if (credentials == null) {
            listener.getLogger().format("Connecting to %s with no credentials, anonymous access%n", giteaServerUrl == null ? "https://gitea.org" : giteaServerUrl);
        } else {
            listener.getLogger().format("Connecting to %s using %s%n", giteaServerUrl == null ? "https://gitea.org" : giteaServerUrl, CredentialsNameProvider.name(credentials));
        }
        List<? extends GiteaRepository> repositories;
        GiteaApi gitea = getGiteaConnector().create(repoOwner, credentials);
        GiteaOrganization organization = gitea.getOrganization();
        if (organization != null) {
            // Navigate repositories of the team
            listener.getLogger().format("Looking up repositories of organization %s%n", repoOwner);
            for (GiteaRepository repo : gitea.getOrgRepositories(organization)) {
                add(listener, observer, repo);
            }
        }

        // Navigate the repositories of the repoOwner as a user
        listener.getLogger().format("Looking up repositories of user %s%n", repoOwner);
        repositories = gitea.getRepositories();

        for (GiteaRepository repo : repositories) {
            add(listener, observer, repo);
        }
    }

    private void add(TaskListener listener, SCMSourceObserver observer, GiteaRepository repo) throws InterruptedException, IOException {
        String name = repo.getRepositoryName();
        if (!Pattern.compile(pattern).matcher(name).matches()) {
            listener.getLogger().format("Ignoring %s%n", name);
            return;
        }
        listener.getLogger().format("Proposing %s%n", name);
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        SCMSourceObserver.ProjectObserver projectObserver = observer.observe(name);
        GiteaSCMSource scmSource = new GiteaSCMSource(null, repoOwner, name);
        scmSource.setGiteaConnector(getGiteaConnector());
        scmSource.setCredentialsId(credentialsId);
        scmSource.setCheckoutCredentialsId(checkoutCredentialsId);
        scmSource.setAutoRegisterHook(isAutoRegisterHooks());
        scmSource.setGiteaServerUrl(giteaServerUrl);
        scmSource.setSshPort(sshPort);
        projectObserver.addSource(scmSource);
        projectObserver.complete();
    }

    @Extension 
    public static class DescriptorImpl extends SCMNavigatorDescriptor {

        private static final Logger LOGGER = Logger.getLogger(DescriptorImpl.class.getName());

        public static final String ANONYMOUS = GiteaSCMSource.DescriptorImpl.ANONYMOUS;
        public static final String SAME = GiteaSCMSource.DescriptorImpl.SAME;

        /**
         * {@inheritDoc}
         */
        @Override
        public String getPronoun() {
            return Messages.GiteaSCMNavigator_Pronoun();
        }

        @Override
        public String getDisplayName() {
            return Messages.GiteaSCMNavigator_DisplayName();
        }

        @Override
        public String getDescription() {
            return Messages.GiteaSCMNavigator_Description();
        }

        @Override
        public String getIconFilePathPattern() {
            return "plugin/gitea-branch-source/images/:size/gitea-scmnavigator.png";
        }

        @NonNull
        @Override
        protected SCMSourceCategory[] createCategories() {
            return new SCMSourceCategory[]{
                    new UncategorizedSCMSourceCategory(Messages._GiteaSCMNavigator_UncategorizedCategory())
            };
        }

        @Override
        public SCMNavigator newInstance(String name) {
            return new GiteaSCMNavigator(name, "", GiteaSCMSource.DescriptorImpl.SAME);
        }

        public FormValidation doCheckCredentialsId(@QueryParameter String value) {
            if (!value.isEmpty()) {
                return FormValidation.ok();
            } else {
                return FormValidation.warning("Credentials are required for build notifications");
            }
        }

        public FormValidation doCheckGiteaServerUrl(@QueryParameter String giteaServerUrl) {
            return GiteaSCMSource.DescriptorImpl.doCheckGiteaServerUrl(giteaServerUrl);
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath SCMSourceOwner context, @QueryParameter String giteaServerUrl) {
            StandardListBoxModel result = new StandardListBoxModel();
            result.withEmptySelection();
            new GiteaApiConnector(giteaServerUrl).fillCredentials(result, context);
            return result;
        }

        public ListBoxModel doFillCheckoutCredentialsIdItems(@AncestorInPath SCMSourceOwner context, @QueryParameter String giteaServerUrl) {
            StandardListBoxModel result = new StandardListBoxModel();
            result.add("- same as scan credentials -", GiteaSCMSource.DescriptorImpl.SAME);
            result.add("- anonymous -", GiteaSCMSource.DescriptorImpl.ANONYMOUS);
            new GiteaApiConnector(giteaServerUrl).fillCheckoutCredentials(result, context);
            return result;
        }

    }
}
