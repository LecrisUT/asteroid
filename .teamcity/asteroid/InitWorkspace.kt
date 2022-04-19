package asteroid

import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

object InitWorkspace : BuildType({
	name = "Initialize workspace"
	description = "Clone sources and initialize workspace"

	enablePersonalBuilds = false

	vcs {
		CoreVCS.attachVCS(this)
	}
})

fun initScript(buildStep: ScriptBuildStep, withSstate: Boolean = true) {
	// TODO: Change the hardcoded sturgeon to generic
	buildStep.name = "Prepare config files"
	val sstateMirror: String = if (withSstate)
		"""
			SSTATE_MIRRORS ?= " \\
			  file://.* %system.sstate.server.address%/sturgeon/sstate-cache/PATH;downloadfilename=PATH \\
			  file://.* %system.sstate.server.address%/armv7vehf-neon/sstate-cache/PATH;downloadfilename=PATH \\
			  file://.* %system.sstate.server.address%/allarch/sstate-cache/PATH;downloadfilename=PATH \\
			  file://.* %system.sstate.server.address%/other-sstate/sstate-cache/PATH;downloadfilename=PATH \\
			"
		""".trimStart().trimEnd()
	else
		"""
			SSTATE_MIRRORS ?= " \\
			  file://.* %system.sstate.server.address%/other-sstate/sstate-cache/PATH;downloadfilename=PATH \\
			"
		""".trimStart().trimEnd()
	// TODO: Add overrides of all dependent recipes
	val gitOverrides = ""
	buildStep.scriptContent = """
		mkdir -p build/conf
		cat > build/conf/local.conf <<-EOF
			DISTRO = "asteroid"
			MACHINE = "sturgeon"
			PACKAGE_CLASSES = "package_ipk"
			SRCDIR = "\${'$'}{@os.path.abspath(os.path.join("\${'$'}{TOPDIR}", "../src/"))}"
			PREMIRRORS:prepend = " \\
			  git://.*/.*/([^.]+).*   git://\${'$'}{SRCDIR}/\\1;protocol=file \\
			"
			$sstateMirror
		EOF
		cat > build/conf/bblayers.conf <<-EOF
			BBPATH = "\${'$'}{TOPDIR}"
			SRCDIR = "\${'$'}{@os.path.abspath(os.path.join("\${'$'}{TOPDIR}", "../src/"))}"
			
			BBLAYERS = " \\
			  \${'$'}{SRCDIR}/meta-qt5 \\
			  \${'$'}{SRCDIR}/oe-core/meta \\
			  \${'$'}{SRCDIR}/meta-asteroid \\
			  \${'$'}{SRCDIR}/meta-openembedded/meta-oe \\
			  \${'$'}{SRCDIR}/meta-openembedded/meta-multimedia \\
			  \${'$'}{SRCDIR}/meta-openembedded/meta-gnome \\
			  \${'$'}{SRCDIR}/meta-openembedded/meta-networking \\
			  \${'$'}{SRCDIR}/meta-smartphone/meta-android \\
			  \${'$'}{SRCDIR}/meta-openembedded/meta-python \\
			  \${'$'}{SRCDIR}/meta-openembedded/meta-filesystems \\
			  \${'$'}{SRCDIR}/meta-smartwatch/meta-sturgeon \\
			"
			$gitOverrides
		EOF
		
		# Try to initialize OE environment
		source ./src/oe-core/oe-init-build-env
		""".trimIndent()
}

fun initScript(buildStep: ScriptBuildStep, recipe: String, recipeVCS: GitVcsRoot,  withSstate: Boolean = true) {
	// TODO: Change the hardcoded sturgeon to generic
	buildStep.name = "Prepare config files"
	val sstateMirror: String = if (withSstate)
		"""
			SSTATE_MIRRORS ?= " \\
			  file://.* %system.sstate.server.address%/sturgeon/sstate-cache/PATH;downloadfilename=PATH \\
			  file://.* %system.sstate.server.address%/armv7vehf-neon/sstate-cache/PATH;downloadfilename=PATH \\
			  file://.* %system.sstate.server.address%/allarch/sstate-cache/PATH;downloadfilename=PATH \\
			  file://.* %system.sstate.server.address%/other-sstate/sstate-cache/PATH;downloadfilename=PATH \\
			"
		""".trimStart().trimEnd()
	else
		"""
			SSTATE_MIRRORS ?= " \\
			  file://.* %system.sstate.server.address%/other-sstate/sstate-cache/PATH;downloadfilename=PATH \\
			"
		""".trimStart().trimEnd()
	val gitOverrides = """
		SRCREV:pn-${recipe} = "%build.vcs.number.${recipeVCS.id}%"
		SRCBRANCH:pn-${recipe} = "%teamcity.build.vcs.branch.${recipeVCS.id}%"
	""".trimStart().trimEnd()
	buildStep.scriptContent = """
		mkdir -p build/conf
		cat > build/conf/local.conf <<-EOF
			DISTRO = "asteroid"
			MACHINE = "sturgeon"
			PACKAGE_CLASSES = "package_ipk"
			SRCDIR = "\${'$'}{@os.path.abspath(os.path.join("\${'$'}{TOPDIR}", "../src/"))}"
			PREMIRRORS:prepend = " \\
			  git://.*/.*/([^.]+).*   git://\${'$'}{SRCDIR}/\\1;protocol=file \\
			"
			$sstateMirror
		EOF
		cat > build/conf/bblayers.conf <<-EOF
			BBPATH = "\${'$'}{TOPDIR}"
			SRCDIR = "\${'$'}{@os.path.abspath(os.path.join("\${'$'}{TOPDIR}", "../src/"))}"
			
			BBLAYERS = " \\
			  \${'$'}{SRCDIR}/meta-qt5 \\
			  \${'$'}{SRCDIR}/oe-core/meta \\
			  \${'$'}{SRCDIR}/meta-asteroid \\
			  \${'$'}{SRCDIR}/meta-openembedded/meta-oe \\
			  \${'$'}{SRCDIR}/meta-openembedded/meta-multimedia \\
			  \${'$'}{SRCDIR}/meta-openembedded/meta-gnome \\
			  \${'$'}{SRCDIR}/meta-openembedded/meta-networking \\
			  \${'$'}{SRCDIR}/meta-smartphone/meta-android \\
			  \${'$'}{SRCDIR}/meta-openembedded/meta-python \\
			  \${'$'}{SRCDIR}/meta-openembedded/meta-filesystems \\
			  \${'$'}{SRCDIR}/meta-smartwatch/meta-sturgeon \\
			"
			$gitOverrides
		EOF
		
		
		# Try to initialize OE environment
		source ./src/oe-core/oe-init-build-env
		""".trimIndent()
}

fun initScript(
	buildStep: ScriptBuildStep,
	device: String,
	architecture: String = "armv7vehf-neon",
	meta: String = device,
	withSstate: Boolean = true
) {
	buildStep.name = "Prepare config files"
	val sstateMirror: String = if (withSstate)
		"""
			SSTATE_MIRRORS ?= " \\
			  file://.* %system.sstate.server.address%/${device}/sstate-cache/PATH;downloadfilename=PATH \\
			  file://.* %system.sstate.server.address%/${architecture}/sstate-cache/PATH;downloadfilename=PATH \\
			  file://.* %system.sstate.server.address%/allarch/sstate-cache/PATH;downloadfilename=PATH \\
			  file://.* %system.sstate.server.address%/other-sstate/sstate-cache/PATH;downloadfilename=PATH \\
			"
		""".trimStart().trimEnd()
	else
		"""
			SSTATE_MIRRORS ?= " \\
			  file://.* %system.sstate.server.address%/other-sstate/sstate-cache/PATH;downloadfilename=PATH \\
			"
		""".trimStart().trimEnd()
	buildStep.scriptContent = """
		mkdir -p build/conf
		cat > build/conf/local.conf <<-EOF
			DISTRO = "asteroid"
			MACHINE = "$device"
			PACKAGE_CLASSES = "package_ipk"
			SRCDIR = "\${'$'}{@os.path.abspath(os.path.join("\${'$'}{TOPDIR}", "../src/"))}"
			PREMIRRORS:prepend = " \\
			  git://.*/.*/([^.]+).*   git://\${'$'}{SRCDIR}/\\1;protocol=file \\
			"
			$sstateMirror
		EOF
		cat > build/conf/bblayers.conf <<-EOF
			BBPATH = "\${'$'}{TOPDIR}"
			SRCDIR = "\${'$'}{@os.path.abspath(os.path.join("\${'$'}{TOPDIR}", "../src/"))}"
			
			BBLAYERS = " \\
			  \${'$'}{SRCDIR}/meta-qt5 \\
			  \${'$'}{SRCDIR}/oe-core/meta \\
			  \${'$'}{SRCDIR}/meta-asteroid \\
			  \${'$'}{SRCDIR}/meta-openembedded/meta-oe \\
			  \${'$'}{SRCDIR}/meta-openembedded/meta-multimedia \\
			  \${'$'}{SRCDIR}/meta-openembedded/meta-gnome \\
			  \${'$'}{SRCDIR}/meta-openembedded/meta-networking \\
			  \${'$'}{SRCDIR}/meta-smartphone/meta-android \\
			  \${'$'}{SRCDIR}/meta-openembedded/meta-python \\
			  \${'$'}{SRCDIR}/meta-openembedded/meta-filesystems \\
			  \${'$'}{SRCDIR}/meta-smartwatch/meta-$meta \\
			  \${'$'}{TOPDIR}/CI-layer \\
			"
		EOF
		mkdir -p build/CI-layer/conf
		cat > build/CI-layer/conf/layer.conf <<-EOF
			BBPATH =. "\${'$'}{LAYERDIR}:"
			BBFILES += "\${'$'}{LAYERDIR}/appends/*.bbappend"
			BBFILE_COLLECTIONS += "CI-layer"
			BBFILE_PATTERN_CI-layer = "^\${'$'}{LAYERDIR}/"
			BBFILE_PATTERN_IGNORE_EMPTY_CI-layer = "1"
			BBFILE_PRIORITY_CI-layer = "8"
			LAYERSERIES_COMPAT_CI-layer = "\${'$'}{LAYERSERIES_COMPAT_core}"
		EOF
		
		# Try to initialize OE environment
		source ./src/oe-core/oe-init-build-env
		""".trimIndent()
}

fun bitbakeBuild(buildStep: ScriptBuildStep, recipe: String? = null) {
	buildStep.scriptContent = """
		source ./src/oe-core/oe-init-build-env > /dev/null
		echo "Starting bitbake"
		bitbake --ui=teamcity ${recipe ?: "asteroid-image%system.image.dev-suffix%"}
	""".trimIndent()
}

fun updateSstate(buildStep: ScriptBuildStep, cleanServer: Boolean = false) {
	// TODO: Change the hardcoded sturgeon to generic
	buildStep.name = "Upload sstate-cache"
	val cleanCommand = if (cleanServer)
		"""
			# Clean destination
			mkdir dummy_empty
			rsync --delete \
				./dummy_empty ${'$'}{ServerAddr}
		""".trimStart().trimEnd()
	else
		""
	buildStep.scriptContent = """
		Opts="-a --prune-empty-dirs --remove-source-files \
			--checksum --progress"
		ServerAddr="%system.sstate.server.user%@%system.sstate.server.upload_address%:%system.sstate.server.location%"
		
		$cleanCommand
		rsync ${'$'}{Opts} \
			build/sstate-cache/fedora-35 ${'$'}{ServerAddr}/other-sstate/sstate-cache
		rsync ${'$'}{Opts} \
			--include '*/' --include '*:*:*:*:*::*' --exclude '*' \
			build/sstate-cache ${'$'}{ServerAddr}/other-sstate
		rsync ${'$'}{Opts} \
			--include '*/' --include '*:*:*:*:*:sturgeon:*' --exclude '*' \
			build/sstate-cache ${'$'}{ServerAddr}/sturgeon
		rsync ${'$'}{Opts} \
			--include '*/' --include '*:*:*:*:*:armv7vehf-neon:*' --exclude '*' \
			build/sstate-cache ${'$'}{ServerAddr}/armv7vehf-neon
		rsync ${'$'}{Opts} \
			--include '*/' --include '*:*:*:*:*:allarch:*' --exclude '*' \
			build/sstate-cache ${'$'}{ServerAddr}/allarch
		rsync ${'$'}{Opts} \
			build/sstate-cache ${'$'}{ServerAddr}/others/sturgeon
		""".trimIndent()
}

fun updateSstate(buildStep: ScriptBuildStep, device: String, architecture: String, cleanServer: Boolean = false) {
	buildStep.name = "Upload sstate-cache"
	buildStep.scriptContent = """
		Opts="-a --prune-empty-dirs --remove-source-files \
			--checksum --progress"
		ServerAddr="%system.sstate.server.user%@%system.sstate.server.upload_address%:%system.sstate.server.location%"
		
		rsync ${'$'}{Opts} \
			build/sstate-cache/fedora-35 ${'$'}{ServerAddr}/other-sstate/sstate-cache
		rsync ${'$'}{Opts} \
			--include '*/' --include '*:*:*:*:*::*' --exclude '*' \
			build/sstate-cache ${'$'}{ServerAddr}/other-sstate
		rsync ${'$'}{Opts} \
			${if (cleanServer) "--delete" else ""} \
			--include '*/' --include '*:*:*:*:*:$device:*' --exclude '*' \
			build/sstate-cache ${'$'}{ServerAddr}/$device
		rsync ${'$'}{Opts} \
			--include '*/' --include '*:*:*:*:*:$architecture:*' --exclude '*' \
			build/sstate-cache ${'$'}{ServerAddr}/$architecture
		rsync ${'$'}{Opts} \
			--include '*/' --include '*:*:*:*:*:allarch:*' --exclude '*' \
			build/sstate-cache ${'$'}{ServerAddr}/allarch
		rsync ${'$'}{Opts} \
			build/sstate-cache ${'$'}{ServerAddr}/others/$device
		""".trimIndent()
}