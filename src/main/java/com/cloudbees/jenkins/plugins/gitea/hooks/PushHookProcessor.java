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

import java.util.logging.Logger;

import com.cloudbees.jenkins.plugins.gitea.server.client.GiteaWebhookPayload;
import com.cloudbees.jenkins.plugins.gitea.server.client.events.GiteaPushEvent;

public class PushHookProcessor extends HookProcessor {

    private static final Logger LOGGER = Logger.getLogger(PushHookProcessor.class.getName());

    @Override
    public void process(String payload) {
        if (payload != null) {
            // TODO: generalize this for BB server
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
