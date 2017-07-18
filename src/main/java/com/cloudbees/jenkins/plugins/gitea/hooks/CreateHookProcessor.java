package com.cloudbees.jenkins.plugins.gitea.hooks;

import com.cloudbees.jenkins.plugins.gitea.server.client.GiteaWebhookPayload;
import com.cloudbees.jenkins.plugins.gitea.server.client.events.GiteaPushEvent;

import java.util.logging.Logger;

public class CreateHookProcessor extends HookProcessor {

    private static final Logger LOGGER = Logger.getLogger(CreateHookProcessor.class.getName());

    //TODO currently the same as PushHookProcessor
    @Override
    public void process(String payload) {
        if (payload != null) {
            // TODO: update to create
            GiteaPushEvent push = GiteaWebhookPayload.pushEventFromPayload(payload);
            if (push != null) {
                String owner = push.getRepository().getOwner().getUsername();
                String repository = push.getRepository().getName();

                LOGGER.info(String.format("Received hook from Gitea. Processing create event on %s/%s", owner, repository));
                scmSourceReIndex(owner, repository);
            }
        }
    }
}
