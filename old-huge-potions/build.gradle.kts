plugins {
    id("dev.ancaria.coderpack")
}

version = "0.200.0"

sacred {
    id = "old-huge-potions"
    displayName = "Old Huge Potions"
    description = "Turns every small or medium potion you pick up into the largest one of its kind, in name and look."
    entrypoint = "dev.ancaria.potions.PotionsMod"
    author("MairwunNx (Pavel Erokhin)")
    website = "https://ancaria.dev"
    repository = "https://github.com/ancaria-dev/mods"
    apiVersion = libs.versions.api.get()
    installTo = layout.dir(providers.gradleProperty("sacredDir").map { file("$it/mods") })
}
