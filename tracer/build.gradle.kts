plugins {
    id("dev.ancaria.coderpack")
}

version = "0.1.0"

sacred {
    id = "tracer"
    displayName = "Tracer"
    description = "Writes every event the loader receives to a separate log file for each run."
    entrypoint = "dev.ancaria.tracer.TracerMod"
    author("MairwunNx (Pavel Erokhin)")
    website = "https://ancaria.dev"
    repository = "https://github.com/ancaria-dev/mods"

    // No conflicts, and there cannot be any: every listener in here is a
    // MONITOR one, so the loader drops any rewrite it tried to make. A mod that
    // can only watch cannot disagree with anything.
    apiVersion = "0.1.0"
    installTo = layout.dir(providers.gradleProperty("sacredDir").map { file("$it/mods") })
}
