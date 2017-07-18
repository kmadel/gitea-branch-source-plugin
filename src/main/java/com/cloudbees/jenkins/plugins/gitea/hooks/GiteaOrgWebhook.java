package com.cloudbees.jenkins.plugins.gitea.hooks;

import com.cloudbees.jenkins.plugins.gitea.api.GiteaApi;
import com.cloudbees.jenkins.plugins.gitea.api.GiteaWebHook;
import com.cloudbees.jenkins.plugins.gitea.server.client.repository.GiteaHook;
import com.cloudbees.jenkins.plugins.gitea.server.client.repository.HookConfig;
import jenkins.model.Jenkins;
import jenkins.util.io.FileBoolean;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Created by kmadel on 6/15/17.
 */
public class GiteaOrgWebhook {

    private static final Logger LOGGER = Logger.getLogger(GiteaOrgWebhook.class.getName());

    public static void register(GiteaApi giteaApi, String orgName) throws IOException {
        String rootUrl = Jenkins.getActiveInstance().getRootUrl();
        if (rootUrl == null) {
            return;
        }
        FileBoolean orghook = new FileBoolean(getTrackingFile(orgName));
        if (orghook.isOff()) {
            giteaApi.createOrgWebHook(getHook());
        }
    }

    private static File getTrackingFile(String orgName) {
        return new File(Jenkins.getActiveInstance().getRootDir(), "gitea-webhooks/GiteaOrgHook." + orgName);
    }

    private static GiteaWebHook getHook() {
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

}
