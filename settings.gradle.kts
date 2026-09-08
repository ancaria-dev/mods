// The plugin and the API come from a repository, not from a sibling checkout,
// so this builds the same wherever it is cloned: the plugin from the Gradle
// Plugin Portal, the API from Maven Central. mavenLocal() stays first so a
// `publishToMavenLocal` run in `build` or `coderpack` still overrides them
// for local testing of an unreleased change.
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
