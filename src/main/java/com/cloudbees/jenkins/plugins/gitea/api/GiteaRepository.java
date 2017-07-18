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

/**
 * Represents a Gitea repository.
 */
public interface GiteaRepository {

    /**
     * @return full repositoty name, which is owner/name (where owner could be a user or an organization)
     */
    String getFullName();

    /**
     * @return repository owner (could be a user or an organization)
     */
    GiteaUser getOwner();

    /**
     * @return {@link #getOwner()}'s name
     */
    String getOwnerName();

    /**
     * @return the repository name (as extracted from {@link #getFullName()})
     */
    String getRepositoryName();

    /**
     * @return the repository description
     */
    String getDescription();

    /**
     * @return return true if the repository is a private one (false otherwise).
     */
    boolean isPrivate();

    /**
     * @return the repository html url
     */
    String getHtmlUrl();

    /**
     * @return the repository default branch
     */
    String getDefaultBranch();

    /**
     * @return the repository website
     */
    String getWebsite();
}