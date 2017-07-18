package com.cloudbees.jenkins.plugins.gitea.notification;

import com.cloudbees.jenkins.plugins.gitea.GiteaApiConnector;
import com.cloudbees.jenkins.plugins.gitea.GiteaSCMSource;
import com.cloudbees.jenkins.plugins.gitea.Messages;
import com.cloudbees.jenkins.plugins.gitea.api.GiteaApi;
import com.cloudbees.jenkins.plugins.gitea.api.GiteaRepository;
import com.cloudbees.jenkins.plugins.gitea.server.client.repository.StatusOptions;
import com.cloudbees.jenkins.plugins.gitea.server.client.repository.StatusState;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.*;
import hudson.model.queue.QueueListener;
import hudson.model.listeners.RunListener;
import hudson.model.listeners.SCMListener;
import hudson.scm.SCM;
import hudson.scm.SCMRevisionState;
import jenkins.model.Jenkins;
import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.SCMHead;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Manages Gitea build status.
 *
 * Job (associated to a PR) scheduled: PENDING
 * Build doing a checkout: PENDING
 * Build done: SUCCESS, FAILURE, WARNING or ERROR
 *
 */
public class GiteaBuildStatusNotification {

    private static final Logger LOGGER = Logger.getLogger(GiteaBuildStatusNotification.class.getName());

    private static StatusOptions createCommitStatus(@Nonnull GiteaRepository repo, @Nonnull StatusState state, @Nonnull String url, @Nonnull String message, @Nonnull Job<?, ?> job, int buildFailureLabelId) throws IOException {
        StatusOptions statusOptions = new StatusOptions();

        statusOptions.setTarget_url(url);
        statusOptions.setState(state);
        statusOptions.setDescription(message);
        statusOptions.setContext("continuous-integration/jenkins/branch");

        return statusOptions;
    }

    @SuppressWarnings("deprecation") // Run.getAbsoluteUrl appropriate here
    private static void createBuildCommitStatus(Run<?,?> build, TaskListener listener) {
        try {
            SCMSourceOwner scmSourceOwner = getSCMSourceOwner(build.getParent());
            //no need to continue if there is no SCMSourceOwner
            if(scmSourceOwner != null) {
                GiteaSCMSource source = getSCMSource(scmSourceOwner);
                int buildFailureLabelId = source.getBuildFailureLabelId();

                GiteaApi giteaApi = GiteaApiConnector.connect(source.getGiteaServerUrl(), source.getRepoOwner(), source.getRepository(), GiteaApiConnector.lookupScanCredentials
                        (scmSourceOwner, null, source.getCredentialsId()));

                GiteaRepository repo = giteaApi.getRepository();
                if (repo != null) {
                    List<Cause> causes = build.getCauses();
                    for (Cause cause : causes) {
                        LOGGER.info(cause.getClass().getName() + " cause short desc: " + cause.getShortDescription());
                    }

                    SCMRevisionAction action = build.getAction(SCMRevisionAction.class);
                    if (action != null) {
                        SCMRevision revision = action.getRevision();
                        String url;
                        try {
                            url = build.getAbsoluteUrl();
                        } catch (IllegalStateException ise) {
                            url = "http://unconfigured-jenkins-location/" + build.getUrl();
                        }
                        boolean ignoreError = false;
                        try {
                            Result result = build.getResult();
                            String revisionToNotify = resolveHeadCommit(revision);
                            Job<?, ?> job = build.getParent();
                            StatusOptions statusOptions = null;
                            if(Result.SUCCESS.equals(result)) {
                                statusOptions = createCommitStatus(repo, StatusState.SUCCESS, url, Messages.GiteaBuildStatusNotification_CommitStatus_Unstable(), job, buildFailureLabelId);
                            } else if (Result.UNSTABLE.equals(result)) {
                                statusOptions = createCommitStatus(repo, StatusState.WARNING, url, Messages.GiteaBuildStatusNotification_CommitStatus_Unstable(), job, buildFailureLabelId);
                            } else if (Result.FAILURE.equals(result)) {
                                statusOptions = createCommitStatus(repo, StatusState.FAILURE, url, Messages.GiteaBuildStatusNotification_CommitStatus_Failure(), job, buildFailureLabelId);
                            } else if (!Result.SUCCESS.equals(result) && result != null) { // ABORTED etc.
                                statusOptions = createCommitStatus(repo, StatusState.ERROR, url, Messages.GiteaBuildStatusNotification_CommitStatus_Other(), job, buildFailureLabelId);
                            } else {
                                ignoreError = true;
                                statusOptions = createCommitStatus(repo, StatusState.PENDING, url, Messages.GiteaBuildStatusNotification_CommitStatus_Other(), job, buildFailureLabelId);
                            }
                            if (statusOptions != null && revisionToNotify != null) {
                                LOGGER.info("create status for sha: " + revisionToNotify);
                                giteaApi.createStatus(statusOptions, revisionToNotify);
                            }
                            if (result != null) {
                                listener.getLogger().format("%n" + Messages.GiteaBuildStatusNotification_CommitStatusSet() + "%n%n");
                            }
                        } catch (FileNotFoundException fnfe) {
                            if (!ignoreError) {
                                listener.getLogger().format("%nCould not update commit status, please check if your scan " +
                                        "credentials belong to a member of the organization or a collaborator of the " +
                                        "repository and repo:status scope is selected%n%n");
                                LOGGER.log(Level.FINE, null, fnfe);
                            }
                        }
                    }
                }
            }
        } catch (IOException ioe) {
            listener.getLogger().format("%nCould not update commit status. Message: %s%n%n", ioe.getMessage());
            LOGGER.log(Level.FINE, "Could not update commit status", ioe);
        }

    }

    /**
     * Returns the SCMSourceOwner associated to a Job.
     *
     * @param job A {@link Job}
     * @return A {@link GiteaApi} or null, either if a scan credentials was not provided, or a GitHubSCMSource was not defined.
     * @throws IOException
     */
    private static @CheckForNull
    SCMSourceOwner getSCMSourceOwner(@Nonnull Job<?,?> job) throws IOException {
        ItemGroup<?> multiBranchProject = job.getParent();
        if (multiBranchProject instanceof SCMSourceOwner) {
            return (SCMSourceOwner) multiBranchProject;
        }
        return null;
    }

    /**
     * It is possible having more than one SCMSource in our MultiBranchProject.
     * TODO: Does it make sense having more than one of the same type?
     *
     * @param scmSourceOwner An {@link Item} that owns {@link SCMSource} instances.
     * @return A {@link GiteaSCMSource} or null
     */
    @CheckForNull
    private static GiteaSCMSource getSCMSource(final SCMSourceOwner scmSourceOwner) {
        for (SCMSource scmSource : scmSourceOwner.getSCMSources()) {
            if (scmSource instanceof GiteaSCMSource) {
                return (GiteaSCMSource) scmSource;
            }
        }
        return null;
    }

    /**
     * With this listener one notifies to Gitea when a Job has been scheduled.
     * Sends: GHCommitState.PENDING
     */
    @Extension
    public static class JobScheduledListener extends QueueListener {

        /**
         * Manages the Gitea Commit Pending Status.
         */
        @Override
        public void onEnterWaiting(Queue.WaitingItem wi) {
            if (!(wi.task instanceof Job)) {
                return;
            }
            final long taskId = wi.getId();
            final Job<?,?> job = (Job) wi.task;
            final GiteaSCMSource source = (GiteaSCMSource) SCMSource.SourceByItem.findSource(job);

            final SCMHead head = SCMHead.HeadByItem.findHead(job);

            // prevent delays in the queue when updating gitea
            Computer.threadPoolForRemoting.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        String hash = resolveHeadCommit(source.fetch(head, null));

                        GiteaApi giteaApi = GiteaApiConnector.connect(source.getGiteaServerUrl(), source.getRepoOwner(), source.getRepository(), GiteaApiConnector.lookupScanCredentials
                                (source.getOwner(), null, source.getCredentialsId()));

                        GiteaRepository repo = giteaApi.getRepository();
                        if (repo != null) {
                            String url = DisplayURLProvider.get().getJobURL(job);
                            // The submitter might push another commit before this build even starts.
                            if (Jenkins.getActiveInstance().getQueue().getItem(taskId) instanceof Queue.LeftItem) {
                                // we took too long and the item has left the queue, no longer valid to apply pending

                                // status. JobCheckOutListener is now responsible for setting the pending status.
                                return;
                            }
                            int buildFailureLabelId = source.getBuildFailureLabelId();
                            StatusOptions statusOptions = createCommitStatus(repo, StatusState.PENDING, url, Messages.GiteaBuildStatusNotification_CommitStatus_Other(), job, buildFailureLabelId);
                            if (statusOptions != null && hash != null) {
                                LOGGER.info("create status for sha: " + hash);
                                giteaApi.createStatus(statusOptions, hash);
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING,
                                "Could not update commit status to PENDING. Message: " + e.getMessage(),
                                LOGGER.isLoggable(Level.FINE) ? e : (Throwable) null);
                    }catch(InterruptedException e) {
                        LOGGER.log(Level.WARNING,
                                "Could not update commit status to PENDING. Rate limit exhausted",
                                LOGGER.isLoggable(Level.FINE) ? e : (Throwable) null);
                        LOGGER.log(Level.FINE, null, e);
                    }

                }
            });
        }

    }

    /**
     * With this listener one notifies to Gitea when the SCM checkout process has started.
     * Possible option: GHCommitState.PENDING
     */
    @Extension public static class JobCheckOutListener extends SCMListener {

        @Override public void onCheckout(Run<?, ?> build, SCM scm, FilePath workspace, TaskListener listener, File changelogFile, SCMRevisionState pollingBaseline) throws Exception {
            createBuildCommitStatus(build, listener);
        }

    }

    /**
     * With this listener one notifies to Gitea the build result.
     * Possible options: GHCommitState.SUCCESS, GHCommitState.ERROR or GHCommitState.FAILURE
     */
    @Extension public static class JobCompletedListener extends RunListener<Run<?,?>> {

        @Override public void onCompleted(Run<?, ?> build, TaskListener listener) {
            createBuildCommitStatus(build, listener);
        }

    }

    private static String resolveHeadCommit(SCMRevision revision) throws IllegalArgumentException {
        if (revision instanceof SCMRevisionImpl) {
            return ((SCMRevisionImpl) revision).getHash();
        } else {
            throw new IllegalArgumentException("did not recognize " + revision);
        }
    }

    private GiteaBuildStatusNotification() {}

    public enum GiteaCommitState {
        PENDING, SUCCESS, ERROR, FAILURE
    }

}