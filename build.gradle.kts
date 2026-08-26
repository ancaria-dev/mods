// Declared once and applied by each mod, which is the only thing the root
// project does. Every `sacred { }` block is written out in full in the mod that
// owns it rather than shared from here: these are the examples a mod author
// reads, and a block assembled out of two files teaches the wrong shape.
plugins {
    id("dev.ancaria.coderpack") version "0.1.0" apply false
}
