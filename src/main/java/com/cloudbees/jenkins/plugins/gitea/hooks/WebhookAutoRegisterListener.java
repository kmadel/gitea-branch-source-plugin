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
package com.cloudbees.jenkins.plugins.gitea.hooks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cloudbees.jenkins.plugins.gitea.GiteaSCMSource;
import com.cloudbees.jenkins.plugins.gitea.api.GiteaApi;
import com.cloudbees.jenkins.plugins.gitea.api.GiteaWebHook;
import com.cloudbees.jenkins.plugins.gitea.server.client.repository.GiteaHook;

import com.cloudbees.jenkins.plugins.gitea.server.client.repository.HookConfig;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;
import hudson.triggers.SafeTimerTask;
import hudson.util.DaemonThreadFactory;
import hudson.util.NamingThreadFactory;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.SCMSourceOwners;

/**
 * {@link SCMSourceOwner} item listener that traverse the list of {@link SCMSource} and register
 * a webhook for every {@link GiteaSCMSource} found.
 */
@Extension
public class WebhookAutoRegisterListener extends ItemListener {

    private static final Logger LOGGER = Logger.getLogger(WebhookAutoRegisterListener.class.getName());

    private static ExecutorService executorService;

    @Override
    public void onCreated(Item item) {
        if (!isApplicable(item)) {
            return;
        }
        registerHooksAsync((SCMSourceOwner) item);
    }

    @Override
    public void onDeleted(Item item) {
        if (!isApplicable(item)) {
            return;
        }
        removeHooksAsync((SCMSourceOwner) item);
    }

    @Override
    public void onUpdated(Item item) {
        if (!isApplicable(item)) {
            return;
        }
        registerHooksAsync((SCMSourceOwner) item);
    }

    private boolean isApplicable(Item item) {
        return item instanceof SCMSourceOwner;
    }

    private void registerHooksAsync(final SCMSourceOwner owner) {
        getExecutorService().submit(new SafeTimerTask() {
            @Override
            public void doRun() {
                registerHooks(owner);
            }
        });
    }

    private void removeHooksAsync(final SCMSourceOwner owner) {
        getExecutorService().submit(new SafeTimerTask() {
            @Override
            public void doRun() {
                removeHooks(owner);
            }
        });
    }

    // synchronized just to avoid duplicated webhooks in case SCMSourceOwner is updated repeatedly and quickly
    private synchronized void registerHooks(SCMSourceOwner owner) {
        List<GiteaSCMSource> sources = getGiteaSCMSources(owner);
        for (GiteaSCMSource source : sources) {
            if (source.isAutoRegisterHook()) {
                GiteaApi giteaApi = source.buildGiteaClient();
                List<? extends GiteaWebHook> existent = giteaApi.getWebHooks();
                boolean exists = false;
                for (GiteaWebHook hook : existent) {
                    // Check if there is a hook pointing to us already
                    if (hook.getConfig().getUrl().equals(Jenkins.getActiveInstance().getRootUrl() + GiteaSCMWebHook.FULL_PATH)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    String rootUrl = Jenkins.getActiveInstance().getRootUrl();
                    if (rootUrl != null) {
                        LOGGER.info(String.format("Registering hook for %s/%s", source.getRepoOwner(), source.getRepository()));
                        giteaApi.registerCommitWebHook(getHook());
                    } else {
                        LOGGER.warning(String.format("Can not register hook. Jenkins root URL is not valid: %s", rootUrl));
                    }
                }
            }
        }
    }

    private void removeHooks(SCMSourceOwner owner) {
        List<GiteaSCMSource> sources = getGiteaSCMSources(owner);
        for (GiteaSCMSource source : sources) {
            if (source.isAutoRegisterHook()) {
                GiteaApi giteaApi = source.buildGiteaClient();
                List<? extends GiteaWebHook> existent = giteaApi.getWebHooks();
                GiteaWebHook hook = null;
                for (GiteaWebHook h : existent) {
                    // Check if there is a hook pointing to us
                    if (h.getConfig().getUrl().equals(Jenkins.getActiveInstance().getRootUrl() + GiteaSCMWebHook.FULL_PATH)) {
                        hook = h;
                        break;
                    }
                }
                if (hook != null && !isUsedSomewhereElse(owner, source.getRepoOwner(), source.getRepository())) {
                    LOGGER.info(String.format("Removing hook for %s/%s", source.getRepoOwner(), source.getRepository()));
                    giteaApi.removeCommitWebHook(hook);
                } else {
                    LOGGER.log(Level.FINE, String.format("NOT removing hook for %s/%s because does not exists or its used in other project", 
                            source.getRepoOwner(), source.getRepository()));
                }
            }
        }
    }

    private boolean isUsedSomewhereElse(SCMSourceOwner owner, String repoOwner, String repoName) {
        Iterable<SCMSourceOwner> all = SCMSourceOwners.all();
        for (SCMSourceOwner other : all) {
            if (owner != other) {
                for(SCMSource otherSource : other.getSCMSources()) {
                    if (otherSource instanceof GiteaSCMSource
                            && ((GiteaSCMSource) otherSource).getRepoOwner().equals(repoOwner)
                            && ((GiteaSCMSource) otherSource).getRepository().equals(repoName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<GiteaSCMSource> getGiteaSCMSources(SCMSourceOwner owner) {
        List<GiteaSCMSource> sources = new ArrayList<GiteaSCMSource>();
        for (SCMSource source : owner.getSCMSources()) {
            if (source instanceof GiteaSCMSource) {
                sources.add((GiteaSCMSource) source);
            }
        }
        return sources;
    }

    private GiteaWebHook getHook() {
        GiteaHook hook = new GiteaHook();
        hook.setActive(true);
        hook.setType("gitea");
        HookConfig config = new HookConfig();
        config.setUrl(Jenkins.getActiveInstance().getRootUrl() + GiteaSCMWebHook.FULL_PATH);
        config.setContent_type("json");
        hook.setConfig(config);
        //set hook for all event types
        hook.setEvents(Arrays.asList(HookEventType.PUSH.getKey(), HookEventType.CREATE.getKey(), HookEventType.PULL_REQUEST.getKey()));
        return hook;
    }

    /**
     * We need a single thread executor to run webhooks operations in background but in order.
     * Registrations and removals need to be done in the same order than they were called by the item listener.
     */
    private static synchronized ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor(new NamingThreadFactory(new DaemonThreadFactory(), WebhookAutoRegisterListener.class.getName()));
        }
        return executorService;
    }

}
