// The plugin and the API come from a repository, not from a sibling checkout,
// so this builds the same wherever it is cloned. Nothing has been released yet,
// which means only the first line here resolves anything today: run
// `publishToMavenLocal` in a checkout of `build` and one of `coderpack`. The
// plugin portal and Central are what those become after the first release.
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "mods"

// One Gradle build over every mod, so CI builds the lot with one command and
// the registry index is generated from what came out of one place. A mod is
// still a directory with its own build script and its own version: nothing
// here is shared but the wrapper.
include("self-check")
include("old-huge-potions")
include("all-my-runes")
include("tracer")
