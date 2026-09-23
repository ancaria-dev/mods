plugins {
    id("dev.ancaria.coderpack")
}

version = "0.200.0"

sacred {
    id = "all-my-runes"
    displayName = "All My Runes"
    description = "Turns runes for other classes into runes for your class. Pick up one of your own first so the mod can copy it."
    entrypoint = "dev.ancaria.runes.RunesMod"
    author("MairwunNx (Pavel Erokhin)")
    website = "https://ancaria.dev"
    repository = "https://github.com/ancaria-dev/mods"
    apiVersion = libs.versions.api.get()
    installTo = layout.dir(providers.gradleProperty("sacredDir").map { file("$it/mods") })
}
