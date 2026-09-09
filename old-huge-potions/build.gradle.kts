plugins {
    id("dev.ancaria.coderpack")
}

version = "0.99.1"

sacred {
    id = "old-huge-potions"
    displayName = "Old Huge Potions"
    description = "Restores Sacred’s original potion system by turning every potion you pick up into the full-size version of its kind."
    entrypoint = "dev.ancaria.potions.PotionsMod"
    author("MairwunNx (Pavel Erokhin)")
    website = "https://ancaria.dev"
    repository = "https://github.com/ancaria-dev/mods"
    apiVersion = libs.versions.coderpack.get()
    installTo = layout.dir(providers.gradleProperty("sacredDir").map { file("$it/mods") })
}
