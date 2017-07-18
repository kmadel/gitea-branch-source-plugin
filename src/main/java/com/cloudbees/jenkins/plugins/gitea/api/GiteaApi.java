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
package com.cloudbees.jenkins.plugins.gitea.api;

import java.util.List;

import com.cloudbees.jenkins.plugins.gitea.server.client.repository.StatusOptions;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * Provides access to a specific repository.
 * One API object needs to be created for each repository you want to work with.
 */
public interface GiteaApi {

    /**
     * @return the repository owner name.
     */
    String getOwner();

    /**
     * @return the repository name.
     */
    String getRepositoryName();

    /**
     * @return the repository specified by {@link #getOwner()}/{@link #getRepositoryName()} 
     *      (or null if repositoryName is not set)
     */
    @CheckForNull
    GiteaRepository getRepository();

    /**
     * @return the list of branches in the repository.
     */
    List<? extends GiteaBranch> getBranches();

    /**
     * @return the get branch in repository.
     */
    GiteaBranch getBranch(String name);

    /**
     * Register a webhook on the repository.
     *
     * @param hook the webhook object
     */
    void registerCommitWebHook(GiteaWebHook hook);

    /**
     * Create issue on repository.
     *
     * @param issue the issue object
     */
    void createIssue(GiteaIssue issue);

    /**
     * Create issue on repository.
     *
     * @param statusOptions the status options object
     * @param sha the commit sha to create status for
     */
    void createStatus(StatusOptions statusOptions, String sha);

    /**
     * Remove the webhook (ID field required) from the repository.
     *
     * @param hook the webhook object
     */
    void removeCommitWebHook(GiteaWebHook hook);

    /**
     * @return the list of webhooks registered in the repository.
     */
    List<? extends GiteaWebHook> getWebHooks();

    /**
     * @return the organization of the current owner, or null if {@link #getOwner()} is not an organization ID.
     */
    @CheckForNull
    GiteaOrganization getOrganization();

    /**
     * Register a webhook on the organization.
     *
     * @param hook the webhook object
     */
    void createOrgWebHook(GiteaWebHook hook);

    /**
     * @return Gitea user.
     */
    @CheckForNull
    GiteaUser getUser(String username);

    /**
     * @return Authenticated Gitea user.
     */
    @CheckForNull
    GiteaUser getAuthenticatedUser();

    /**
     * Returns all the repositories for the current owner (even if it's a regular user or an organization).
     *
     * @return all repositories for the current {@link #getOwner()}
     */
    List<? extends GiteaRepository> getRepositories();

    /**
     * Returns all the repositories for the provided organization.
     *
     * @return all repositories for the provided organization}
     */
    List<? extends GiteaRepository> getOrgRepositories(GiteaOrganization organization);

    /**
     * @return true if the repository ({@link #getOwner()}/{@link #getRepositoryName()}) is private, false otherwise
     *          (if it's public or does not exists).
     */
    boolean isPrivate();


    /**
     * @return true if the path exists for repository branch.
     */
    boolean checkPathExists(String branch, String path);

}