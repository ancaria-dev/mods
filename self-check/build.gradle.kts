plugins {
    id("dev.ancaria.coderpack")
}

version = "0.99.0"

dependencies {
    // JavaFX with the Windows classifier, which is the jar that carries the
    // native libraries as well as the classes. They travel inside the mod jar:
    // the zygote starts a plain JVM with no module path and no idea a window is
    // coming, so anything not in here does not exist.
    //
    // 21 rather than the newest: JavaFX 26 is compiled to class file 68, and the
    // loader targets Java 21, so `verifySacredMod` says so, correctly. A
    // player on exactly 21 could not load the window. The LTS line is built for
    // 17 and this window uses nothing newer than that.
    val fx = "21.0.9"
    implementation("org.openjfx:javafx-base:$fx:win")
    implementation("org.openjfx:javafx-graphics:$fx:win")
    implementation("org.openjfx:javafx-controls:$fx:win")
}

tasks.named<Jar>("shadowJar") {
    // All three JavaFX jars carry the same GraalVM native-image configuration.
    // The plugin packs duplicates rather than dropping them, which is right for
    // the service files it was aimed at and pointless here: nothing in a mod jar
    // is ever fed to native-image.
    exclude("META-INF/substrate/**")
}

sacred {
    id = "self-check"
    displayName = "Self Check"
    description = "Shows every event delivered by the loader and tests which events a mod can rewrite."
    entrypoint = "dev.ancaria.selfcheck.SelfCheckMod"
    author("MairwunNx (Pavel Erokhin)")
    website = "https://ancaria.dev"
    repository = "https://github.com/ancaria-dev/mods"

    // Both of these rewrite an item at pickup, and so does this one: it answers
    // every pickup with the type the item already had, to prove the verdict
    // travels. Whichever listener runs last decides, so beside either of them
    // this mod either reports a lie or undoes their work. A diagnostic belongs
    // on its own.
    conflictsWith("old-huge-potions")
    conflictsWith("all-my-runes")

    // The plugin adds the API as compileOnly at this version. The zygote has it
    // already, and a second copy inside the jar would be a different class with
    // the same name.
    apiVersion = libs.versions.coderpack.get()
    installTo = layout.dir(providers.gradleProperty("sacredDir").map { file("$it/mods") })
}
