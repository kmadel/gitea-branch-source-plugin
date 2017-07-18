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

import com.cloudbees.jenkins.plugins.gitea.api.GiteaApi;
import com.cloudbees.jenkins.plugins.gitea.api.GiteaBranch;
import com.cloudbees.jenkins.plugins.gitea.api.GiteaRepository;
import com.cloudbees.jenkins.plugins.gitea.server.client.repository.GiteaServerRepository;
import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.TaskListener;
import hudson.scm.SCM;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.*;
import jenkins.scm.api.metadata.ObjectMetadataAction;
import jenkins.scm.api.metadata.PrimaryInstanceMetadataAction;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.RefSpec;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * SCM source implementation for Gitea.
 * 
 * It provides a way to discover/retrieve branches and pull requests through the Gitea REST API
 * which is much more efficient than the plain Git SCM source implementation.
 */
public class GiteaSCMSource extends AbstractGitSCMSource {

    /**
     * Credentials used to access the Gitea REST API.
     */
    private String credentialsId;

    /**
     * Credentials used to clone the repository/repositories.
     */
    private String checkoutCredentialsId;

    /**
     * Repository owner.
     * Used to build the repository URL.
     */
    private final String repoOwner;

    /**
     * Repository name.
     * Used to build the repository URL.
     */
    private final String repository;

    /**
     * Ant match expression that indicates what branches to include in the retrieve process.
     */
    private String includes = "*";

    /**
     * Ant match expression that indicates what branches to exclude in the retrieve process.
     */
    private String excludes = "";

    /**
     * If true, a webhook will be auto-registered in the repository managed by this source.
     */
    private boolean autoRegisterHook = false;

    /**
     * Gitea Server URL.
     * An specific HTTP client is used if this field is not null.
     */
    private String giteaServerUrl;

    /**
     * Port used by Gitea Server for SSH clone.
     */
    private int sshPort = -1;

    /**
     * Label ID to use for Build Failure status issues.
     */
    private int buildFailureLabelId = -1;

    /**
     * Gitea API client connector.
     */
    private transient GiteaApiConnector giteaConnector;

    private static final Logger LOGGER = Logger.getLogger(GiteaSCMSource.class.getName());

    @DataBoundConstructor
    public GiteaSCMSource(String id, String repoOwner, String repository) {
        super(id);
        this.repoOwner = repoOwner;
        this.repository = repository;
    }

    @CheckForNull
    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = Util.fixEmpty(credentialsId);
    }

    @CheckForNull
    public String getCheckoutCredentialsId() {
        return checkoutCredentialsId;
    }

    @DataBoundSetter
    public void setCheckoutCredentialsId(String checkoutCredentialsId) {
        this.checkoutCredentialsId = checkoutCredentialsId;
    }

    public String getIncludes() {
        return includes;
    }

    @DataBoundSetter
    public void setIncludes(@NonNull String includes) {
        Pattern.compile(getPattern(includes));
        this.includes = includes;
    }

    public String getExcludes() {
        return excludes;
    }

    @DataBoundSetter
    public void setExcludes(@NonNull String excludes) {
        Pattern.compile(getPattern(excludes));
        this.excludes = excludes;
    }

    public String getRepoOwner() {
        return repoOwner;
    }

    public String getRepository() {
        return repository;
    }

    @DataBoundSetter
    public void setAutoRegisterHook(boolean autoRegisterHook) {
        this.autoRegisterHook = autoRegisterHook;
    }

    public boolean isAutoRegisterHook() {
        return autoRegisterHook;
    }

    public int getSshPort() {
        return sshPort;
    }

    @DataBoundSetter
    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    public int getBuildFailureLabelId() {
        return buildFailureLabelId;
    }

    @DataBoundSetter
    public void setBuildFailureLabelId(int buildFailureLabelId) {
        this.buildFailureLabelId = buildFailureLabelId;
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

    @Override
    public String getRemote() {
        return getUriResolver().getRepositoryUri(giteaServerUrl, repoOwner, repository);
    }


    public GiteaApi buildGiteaClient() {
        return getGiteaConnector().create(repoOwner, repository, getScanCredentials());
    }

    @Override
    protected final void retrieve(@CheckForNull SCMSourceCriteria criteria,
                                  @NonNull SCMHeadObserver observer,
                                  @CheckForNull SCMHeadEvent<?> event,
                                  @NonNull final TaskListener listener) throws IOException, InterruptedException {

        StandardUsernamePasswordCredentials scanCredentials = getScanCredentials();
        if (scanCredentials == null) {
            listener.getLogger().format("Connecting to %s with no credentials, anonymous access%n",giteaServerUrl == null ? "https://gitea.org" : giteaServerUrl);
        } else {
            listener.getLogger().format("Connecting to %s using %s%n", giteaServerUrl == null ? "https://gitea.org" : giteaServerUrl, CredentialsNameProvider.name(scanCredentials));
        }

        // Search branches
        retrieveBranches(criteria, observer, listener);
    }

    private void retrieveBranches(SCMSourceCriteria criteria, @NonNull final SCMHeadObserver observer, @NonNull TaskListener listener)
            throws IOException, InterruptedException {
        String fullName = repoOwner + "/" + repository;
        listener.getLogger().println("Looking up " + fullName + " for branches");

        final GiteaApi giteaApi = getGiteaConnector().create(repoOwner, repository, getScanCredentials());
        List<? extends GiteaBranch> branches = giteaApi.getBranches();
        for (GiteaBranch branch : branches) {
            listener.getLogger().println("Checking branch " + branch.getName() + " from " + fullName);
            final String branchName = branch.getName();
            if (isExcluded(branchName)) {
                continue;
            }
            if (criteria != null) {
                SCMHead head = new BranchSCMHead(branchName);
                SCMRevision hash = new SCMRevisionImpl(head, branch.getCommit().getHash());
                SCMProbe probe = createProbe(head, hash);
                if (criteria.isHead(probe, listener)) {
                    listener.getLogger().format("    Met criteria%n");
                } else {
                    listener.getLogger().format("    Does not meet criteria%n");
                    continue;
                }
            }
            SCMHead head = new SCMHead(branchName);
            SCMRevision hash = new AbstractGitSCMSource.SCMRevisionImpl(head, branch.getCommit().getHash());
            observer.observe(head, hash);

        }
    }

    @NonNull
    @Override
    protected SCMProbe createProbe(@NonNull SCMHead head, @CheckForNull final SCMRevision revision) throws IOException {
        // Gitea client and validation
        GiteaApi giteaApi = getGiteaConnector().create(repoOwner, repository, getScanCredentials());
        return new GiteaSCMProbe(giteaApi, head);
    }

    @Override
    protected SCMRevision retrieve(SCMHead head, TaskListener listener) throws IOException, InterruptedException {
        GiteaApi giteaApi = getGiteaConnector().create(repoOwner, repository, getScanCredentials());
        GiteaBranch branch = giteaApi.getBranch(head.getName());
        listener.getLogger().println("Retrieving HEAD for " + branch.getName() + " branch");
        if (branch.getCommit() != null) {
            return new AbstractGitSCMSource.SCMRevisionImpl(head, branch.getCommit().getHash());
        }
        LOGGER.warning("No branch found in " + repoOwner + "/" + repository + " with name [" + head.getName() + "]");
        return null;
    }

    @Override
    public SCM build(SCMHead head, SCMRevision revision) {
        LOGGER.info("Build HEAD for " + head.getName() + " branch");
        if (revision == null) {
            // TODO will this work sanely for PRs? Branch.scm seems to be used only as a fallback for SCMBinder/SCMVar where they would perhaps better just report an error.
            return super.build(head, null);
        } else {
            return super.build(head, /* casting just as an assertion */(AbstractGitSCMSource.SCMRevisionImpl) revision);
        }
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    protected List<Action> retrieveActions(@NonNull SCMHead head,
                                           @CheckForNull SCMHeadEvent event,
                                           @NonNull TaskListener listener) throws IOException, InterruptedException {
        // TODO when we have support for trusted events, use the details from event if event was from trusted source
        List<Action> result = new ArrayList<>();
        SCMSourceOwner owner = getOwner();
        if (owner instanceof Actionable) {
            GiteaLink repoLink = ((Actionable) owner).getAction(GiteaLink.class);
            if (repoLink != null) {
                String url;
                ObjectMetadataAction metadataAction = null;
                // branch in this repository
                url = repoLink.getUrl() + "/tree/" + head.getName();
                metadataAction = new ObjectMetadataAction(head.getName(), null, url);
                result.add(new GiteaLink("icon-gitea-branch", url));
                result.add(metadataAction);
            }
            if (head instanceof BranchSCMHead) {
                for (GiteaDefaultBranch p : ((Actionable) owner).getActions(GiteaDefaultBranch.class)) {
                    if (StringUtils.equals(getRepoOwner(), p.getRepoOwner())
                            && StringUtils.equals(repository, p.getRepository())
                            && StringUtils.equals(p.getDefaultBranch(), head.getName())) {
                        result.add(new PrimaryInstanceMetadataAction());
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    protected List<Action> retrieveActions(@CheckForNull SCMSourceEvent event,
                                           @NonNull TaskListener listener) throws IOException {
        // TODO when we have support for trusted events, use the details from event if event was from trusted source
        List<Action> result = new ArrayList<>();
        result.add(new GiteaRepoMetadataAction());
        GiteaApi giteaApi = getGiteaConnector().create(repoOwner, repository, getScanCredentials());
        GiteaRepository giteaRepository = giteaApi.getRepository();

            result.add(new ObjectMetadataAction(null, giteaRepository.getDescription(), Util.fixEmpty(giteaRepository.getWebsite())));
            result.add(new GiteaLink("icon-gitea-repo", giteaRepository.getHtmlUrl()));
            if (StringUtils.isNotBlank(giteaRepository.getDefaultBranch())) {
                result.add(new GiteaDefaultBranch(getRepoOwner(), repository, giteaRepository.getDefaultBranch()));
            }
            return result;

    }

    @Override
    protected List<RefSpec> getRefSpecs() {
        return new ArrayList<>(Arrays.asList(new RefSpec("+refs/heads/*:refs/remotes/origin/*"),
                // For PRs we check out the head, then perhaps merge with the base branch.
                new RefSpec("+refs/pull/*/head:refs/remotes/origin/pr/*")));
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @CheckForNull
    /* package */ StandardUsernamePasswordCredentials getScanCredentials() {
        return getGiteaConnector().lookupCredentials(getOwner(), credentialsId, StandardUsernamePasswordCredentials.class);
    }

    private StandardCredentials getCheckoutCredentials() {
        return getGiteaConnector().lookupCredentials(getOwner(), getCheckoutEffectiveCredentials(), StandardCredentials.class);
    }

    public String getRemoteName() {
      return "origin";
    }

    /**
     * Returns the pattern corresponding to the branches containing wildcards. 
     * 
     * @param branches space separated list of expressions. 
     *        For example "*" which would match all branches and branch* would match branch1, branch2, etc.
     * @return pattern corresponding to the branches containing wildcards (ready to be used by {@link Pattern})
     */
    private String getPattern(String branches) {
        StringBuilder quotedBranches = new StringBuilder();
        for (String wildcard : branches.split(" ")) {
            StringBuilder quotedBranch = new StringBuilder();
            for (String branch : wildcard.split("\\*")) {
                if (wildcard.startsWith("*") || quotedBranches.length() > 0) {
                    quotedBranch.append(".*");
                }
                quotedBranch.append(Pattern.quote(branch));
            }
            if (wildcard.endsWith("*")) {
                quotedBranch.append(".*");
            }
            if (quotedBranches.length() > 0) {
                quotedBranches.append("|");
            }
            quotedBranches.append(quotedBranch);
        }
        return quotedBranches.toString();
    }

    /**
     * Returns a {@link RepositoryUriResolver} according to credentials configuration.
     *
     * @return a {@link RepositoryUriResolver}
     */
    public RepositoryUriResolver getUriResolver() {
        String credentialsId = getCredentialsId();
        if (credentialsId == null) {
            return new HttpsRepositoryUriResolver();
        } else {
            if (getCredentials(StandardCredentials.class, credentialsId) instanceof SSHUserPrivateKey) {
                return new SshRepositoryUriResolver();
            } else {
                // Defaults to HTTP/HTTPS
                return new HttpsRepositoryUriResolver();
            }
        }
    }


    /**
     * Returns a credentials by type and identifier.
     *
     * @param type Type that we are looking for
     * @param credentialsId Identifier of credentials
     * @return The credentials or null if it does not exists
     */
    private <T extends StandardCredentials> T getCredentials(@Nonnull Class<T> type, @Nonnull String credentialsId) {
        return CredentialsMatchers.firstOrNull(CredentialsProvider.lookupCredentials(
                type, getOwner(), ACL.SYSTEM,
                Collections.<DomainRequirement> emptyList()), CredentialsMatchers.allOf(
                CredentialsMatchers.withId(credentialsId),
                CredentialsMatchers.instanceOf(type)));
    }

    private String getCheckoutEffectiveCredentials() {
        if (DescriptorImpl.ANONYMOUS.equals(checkoutCredentialsId)) {
            return null;
        } else if (DescriptorImpl.SAME.equals(checkoutCredentialsId)) {
            return credentialsId;
        } else {
            return checkoutCredentialsId;
        }
    }

    @Extension
    public static class DescriptorImpl extends SCMSourceDescriptor {

        public static final String ANONYMOUS = "ANONYMOUS";
        public static final String SAME = "SAME";

        @Override
        public String getDisplayName() {
            return "Gitea";
        }

        public FormValidation doCheckCredentialsId(@QueryParameter String value) {
            if (!value.isEmpty()) {
                return FormValidation.ok();
            } else {
                return FormValidation.warning("Credentials are required for notifications");
            }
        }

        public static FormValidation doCheckGiteaServerUrl(@QueryParameter String giteaServerUrl) {
            String url = Util.fixEmpty(giteaServerUrl);
            if (url == null) {
                return FormValidation.ok();
            }
            try {
                new URL(giteaServerUrl);
            } catch (MalformedURLException e) {
                return FormValidation.error("Invalid URL: " +  e.getMessage());
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath SCMSourceOwner context, @QueryParameter String giteaServerUrl) {
            StandardListBoxModel result = new StandardListBoxModel();
            result.withEmptySelection();
            new GiteaApiConnector(giteaServerUrl).fillCredentials(result, context);
            return result;
        }

        public ListBoxModel doFillCheckoutCredentialsIdItems(@AncestorInPath SCMSourceOwner context, @QueryParameter String giteaServerUrl) {
            StandardListBoxModel result = new StandardListBoxModel();
            result.add("- same as scan credentials -", SAME);
            result.add("- anonymous -", ANONYMOUS);
            new GiteaApiConnector(giteaServerUrl).fillCheckoutCredentials(result, context);
            return result;
        }

    }
}
