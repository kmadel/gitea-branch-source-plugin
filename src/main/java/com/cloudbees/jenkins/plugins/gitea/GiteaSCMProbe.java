package com.cloudbees.jenkins.plugins.gitea;

import com.cloudbees.jenkins.plugins.gitea.api.GiteaApi;
import com.cloudbees.jenkins.plugins.gitea.api.GiteaRepository;
import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.*;

import java.io.IOException;

/**
 * Created by kmadel on 6/19/17.
 */
public class GiteaSCMProbe extends SCMProbe {

    private static final long serialVersionUID = 1L;
    private final transient GiteaApi giteaApi;
    private final String ref;
    private final String name;

    public GiteaSCMProbe(GiteaApi giteaApi, SCMHead head) {
        this.giteaApi = giteaApi;
        this.name = head.getName();
        //TODO update for pull requests
        this.ref = "refs/heads/" + head.getName();
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public long lastModified() {
        return 0;
    }

    @NonNull
    @Override
    public SCMProbeStat stat(@NonNull String path) throws IOException {
        boolean exists = giteaApi.checkPathExists(name, path);
        if(exists) {
            return SCMProbeStat.fromType(SCMFile.Type.REGULAR_FILE);
        }
        return SCMProbeStat.fromType(SCMFile.Type.NONEXISTENT);
    }

    @Override
    public void close() throws IOException {

    }
}
