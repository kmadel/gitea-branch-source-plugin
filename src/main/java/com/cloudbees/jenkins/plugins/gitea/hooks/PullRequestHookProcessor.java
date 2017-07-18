package com.cloudbees.jenkins.plugins.gitea.hooks;

import com.cloudbees.jenkins.plugins.gitea.server.client.GiteaWebhookPayload;
import com.cloudbees.jenkins.plugins.gitea.server.client.events.GiteaPushEvent;

import java.util.logging.Logger;

public class PullRequestHookProcessor extends HookProcessor {

    private static final Logger LOGGER = Logger.getLogger(PushHookProcessor.class.getName());

    @Override
    public void process(String payload) {
        if (payload != null) {
            GiteaPushEvent push = GiteaWebhookPayload.pushEventFromPayload(payload);
            if (push != null) {
                String owner = push.getRepository().getOwner().getUsername();
                String repository = push.getRepository().getName();

                LOGGER.info(String.format("Received hook from Gitea. Processing push event on %s/%s", owner, repository));
                scmSourceReIndex(owner, repository);
            }
        }
    }

}
