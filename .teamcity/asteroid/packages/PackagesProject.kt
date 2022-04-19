package asteroid.packages

import asteroid.*
import asteroid.devices.DevicesProject
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.FailureAction
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.sequential
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.schedule
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object PackagesProject : Project({
	id("Packages")
	name = "Packages"
	description = "Package projects"

	subProject(AsteroidAppsProject)
	subProject(CommunityAppsProject)

	buildType(BuildAll)

	sequential {
		parallel {
			for (pkg in AsteroidAppsProject.packages)
				buildType(pkg.buildPackage)
		}
		buildType(BuildAll) {
			onDependencyFailure = FailureAction.CANCEL
		}
	}
})

object BuildAll : BuildType({
	id("Packages_BuildAll")
	name = "Build all Packages"
	description = "Build all packages"

	vcs {
		CoreVCS.attachVCS(this)
		root(CoreVCS.Asteroid, "+:.=>asteroid")
		root(CoreVCS.MetaAsteroid, "+:.=>src/meta-asteroid")
	}

	triggers {
		schedule {
//			schedulingPolicy = weekly{
//				dayOfWeek = ScheduleTrigger.DAY.Sunday
//			}
			schedulingPolicy = daily{
				hour = 12
			}
			triggerRules = """
				+:root=${CoreVCS.MetaOpenEmbedded.id}:/**
				+:root=${CoreVCS.MetaQt5.id}:/**
				+:root=${CoreVCS.MetaSmartphone.id}:/**
				+:root=${CoreVCS.OpenEmbeddedCore.id}:/**
				+:root=${CoreVCS.Bitbake.id}:/**
			""".trimIndent()
			branchFilter = "+:<default>"
		}
		vcs {
			enabled = false
			watchChangesInDependencies = true
			triggerRules = """
				+:/**
				-:root=${DevicesProject.vcs.id}:/**
				-:root=${CoreVCS.MetaAsteroid.id}:/**
			""".trimIndent()

			branchFilter = "+:<default>"
		}
		vcs {
			watchChangesInDependencies = true
			triggerRules = """
				+:root=${CoreVCS.MetaAsteroid.id};comment=^(?!\[NoBuild\]:).+:/**
				-:root=${CoreVCS.MetaAsteroid.id}:/recipes-asteroid-apps/**
				-:root=${CoreVCS.MetaAsteroid.id}:/recipes-asteroid/**
			""".trimIndent()

			branchFilter = """
				+:<default>
				+:pull/*
			""".trimIndent()
		}
		vcs {
			triggerRules = """
				+:root=${CoreVCS.Asteroid.id}:/.teamcity/*
				+:root=${CoreVCS.Asteroid.id}:/.teamcity/packages/**
			""".trimIndent()

			branchFilter = """
				+:<default>
			""".trimIndent()
		}
	}
	features {
		var gitChecker: GitAPIChecker?
		if (Settings.pullRequests) {
			gitChecker = GitAPIChecker.Create(CoreVCS.MetaAsteroid.url!!, Settings.GithubTokenID)
			if (gitChecker?.checkPR() == true)
				pullRequests {
					vcsRootExtId = "${CoreVCS.MetaAsteroid.id}"
					when (gitChecker!!.hubType) {
						GitRepoHubType.Github -> {
							provider = github {
								authType = token {
									token = Settings.GithubTokenID
								}
								filterAuthorRole = PullRequests.GitHubRoleFilter.MEMBER_OR_COLLABORATOR
							}
						}
					}
				}
			gitChecker = GitAPIChecker.Create(CoreVCS.Asteroid.url!!, Settings.GithubTokenID)
			if (gitChecker?.checkPR() == true)
				pullRequests {
					enabled = false
					vcsRootExtId = "${CoreVCS.Asteroid.id}"
					when (gitChecker!!.hubType) {
						GitRepoHubType.Github -> {
							provider = github {
								authType = token {
									token = Settings.GithubTokenID
								}
								filterAuthorRole = PullRequests.GitHubRoleFilter.MEMBER_OR_COLLABORATOR
							}
						}
					}
				}
		}
		if (Settings.commitStatus) {
			gitChecker = GitAPIChecker.Create(CoreVCS.MetaAsteroid.url!!, Settings.GithubTokenID)
			if (gitChecker?.checkCommitStatus() == true)
				commitStatusPublisher {
					vcsRootExtId = "${CoreVCS.MetaAsteroid.id}"
					when (gitChecker!!.hubType) {
						GitRepoHubType.Github -> {
							publisher = github {
								githubUrl = "https://api.github.com"
								authType = personalToken {
									token = Settings.GithubTokenID
								}
							}
							param("github_oauth_user", gitChecker!!.commitUser)
						}
					}
				}
			gitChecker = GitAPIChecker.Create(CoreVCS.Asteroid.url!!, Settings.GithubTokenID)
			if (gitChecker?.checkCommitStatus() == true)
				commitStatusPublisher {
					enabled = false
					vcsRootExtId = "${CoreVCS.Asteroid.id}"
					when (gitChecker!!.hubType) {
						GitRepoHubType.Github -> {
							publisher = github {
								githubUrl = "https://api.github.com"
								authType = personalToken {
									token = Settings.GithubTokenID
								}
							}
							param("github_oauth_user", gitChecker!!.commitUser)
						}
					}
				}
		}
	}
})