# TeamCity CI setup

This setup was constructed so that it can be easily ported by anyone who want to contribute and develop on their own
forks. Just follow these steps to get your CI server up and running (order of opperations is important):

1. [Install a local TeamCity server](https://www.jetbrains.com/help/teamcity/2021.2/install-and-start-teamcity-server.html)
2. Create a new project named `Asteroid` pointing to this repository and the specific branch you wish to track and/or
   push these settings to
3. Configure the [Context Parameters](#context-parameters)
4. (optional) Change the settings repository to point to the project one.

### Context Parameters

The project will create subprojects and enable features according to the `Context Parameters` defined here. For a full
list of available `Context Parameters` check `.teamcity/$Project/Settings.kt`.

#### Basic parameters

- `Fork`: [AsteroidOS]<br/>
  The fork that should be attached to the current CI project.
- `Devices`: [sturgeon,catfish]<br/>
  A comma-separated list of devices you wish to manage.
- `Packages`: [asteroid-launcher]<br/>
  A comma-separated list of Asteroid packages you wish to manage.
- (optional) `CommunityPackages`: []<br/>
  A comma-separated list of Asteroid packages you wish to manage.
- (optional) `Upstream`: [AsteroidOS]<br/>
  The upstream repository of all the other parent/child projects.
- (optional) `CleanBuilds`: [false]<br/>
  Enable building images from scratch.
- (optional) `WithSstate`: [true]<br/>
  Enable the default builders using an `sstate-server`.

#### Sstate-server parameters (`WithSstate == true`)

- (optional) `SstateServerURL`: [<https://sstate.asteroid.org>]<br/>
  Sstate-server url from where to download the `sstate-cache`.
- (optional) `DeploySstate`: [false]<br/>
  Enables uploading to the `sstate-cache` server. Must also setup `Sstate Server Key`
  in [Other configurations](#other-configurations).
- (optional) `SstateServerBackendURL`: [sstate.asteroid.org]<br/>
  Sstate-server url where to upload the new `sstate-cache`.
- (optional) `SstateServerUser`: [asteroidos]<br/>
  User used to `rsync` to `SstateServerBackendURL`.
- (optional) `SstateServerLocation`: []<br/>
  Path prefix where to upload `sstate-cache` on the `SstateServerBackendURL`. Consider setting
  up [`rrsync`](https://www.guyrutenberg.com/2014/01/14/restricting-ssh-access-to-rsync/) for the SSH key used here

#### Github features

- (optional) `GithubToken`: [credentialsJSON:0b803d82-f0a8-42ee-b8f9-0fca109a14ab]<br/>
  Github token used for further integrations. In the `credentialsJSON` format above (can use any other random UUID) add
  the actual token value in `$Project/Versioned Settings/Tokens`.
- (optional) `PullRequests`: [false]<br/>
  Enables tracking pull requests on the repositories.
- (optional) `CommitStatus`: [false]<br/>
  Enables pushing commit status to the repositories.
- (optional) `CommitUser`: [`Fork`]<br/>
  The commit user pushing commit status.

### Other configurations

- (optional) `$Project/SSH Keys/Sstate Server Key`<br/>
  The SSH key used to upload to your own `sstate-cache` server.
  *Required if `DeploySstate == true`*.
- (optional) `file://.teamcity/overrides.json`<br/>
  Configure some additional overrides to the automatic project creation, e.g. pointing to different branches. Consult
  the Kotlin source code about the usage of `Settings.overrides` for the available overrides.
- (optional | BE CAREFUL!) `Administration/Diagnostics/Internal Properties`<br/>
  Additional settable properties if you have admin privileges to the server. Be careful what you trust in
  the `.teamcity` code and this guide
	- `teamcity.dsl.sandbox.disabled=Asteroid`<br/>
	  Disable sandboxing for the Asteroid project and its subprojects. Used for doing http requests to check the
	  validity of the `GithubToken` for each repository it interacts. If
	  issue [TW-66716](https://youtrack.jetbrains.com/issue/TW-66716) is resolved, this will be disabled.
	- `teamcity.versionedSettings.configsGeneratorTimeoutSeconds=1200`<br/>
	  Increase the allowed compilation time of the project. Only relevant for the above setting due to the "slow" HTTP
	  requests.

### Other Notes