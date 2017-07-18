Gitea Branch Source
=====================
# Coming Soon...
Provides Jenkins SCMSource and Jenkins SCMNavigator for [Gitea: Git with a cup of tea](https://gitea.io/en-US/)

**[Pipeline as Code](https://go.cloudbees.com/docs/cloudbees-documentation/cookbook/ch19.html#ch19_pipeline-as-code) for Gitea: Git with a cup of tea!**

A Jenkins Plugin that provides SCMSource (i.e. [Pipeline Multibranch](https://wiki.jenkins-ci.org/display/JENKINS/Pipeline+Multibranch+Plugin)) and SCMNavigator for [Gitea - Git with a cup of tea](https://github.com/gogits/gogs).

## Features

- Supports Pipeline Multibranch
- Auto creation of repository webhooks for `push` and `create` events
- Supports SCMNavigator (Gitea Organization Scanning) functionality (i.e. org scanning per [GitHub Branch Source plugin for Jenins](https://wiki.jenkins-ci.org/display/JENKINS/GitHub+Branch+Source+Plugin))
- Supports commit status update via Gitea statuses
- Partial support for Gitea Organization avatar - Gitea does not support dynamically sized avatar images?

### Missing Features

- Does not support Gitea Pull Requests - coming soon (with limitations)
- Does not support auto-creation of Organization webhooks - coming soon
- Does not support auto-cleanup of removed repositories from an organization

#### Set up a Gitea Organization Folder
- On jenkins-team-1, upload the gogs-branch-source plugin from https://github.com/kmadel/gogs-branch-source-plugin/releases/download/v0.1-alpha/gogs-branch-source-0.1-alpha.hpi
- On jenkins-team-1 update branch-api plugin to version 1.10
- On jenkins-team-1, add credentials for Gogs user: Username/Password credentials
  - user: beedemo-user
  - password: admin
  - id: gogs-beedemo-user
  - desc: Gogs credentials for beedemo-user
- On jenkins-team-1 create a 'Gogs Organization Folder' project named 'beedemo': repo url=http://192.168.99.100:10080, creds=gogs-beedemo-user

Note: your network full_name may vary, especially on windows (`docker network ls`), also your Gogs URL/IP may vary based on your Docker Machine (check `docker-machine ip {machine-full_name}`)

#### Tested Against

- Gitea: v1.1.2

### Why Gitea and not Gogs?

Basically it comes down to the rate of new features being added to the project.

- Commit Status
- Org webhook creation
- Better pull request support
