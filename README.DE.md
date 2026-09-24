<div align="center">

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org)
[![Gradle](https://img.shields.io/badge/Gradle-9.7.1-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org)
[![SRML](https://img.shields.io/badge/SRML-1-C8A45C?style=for-the-badge&labelColor=1C1410)](#srml)
[![License](https://img.shields.io/badge/License-MIT-4B5563?style=for-the-badge)](LICENSE)
[![Sacred](https://img.shields.io/badge/Sacred-Community-8B1A1A?style=for-the-badge&labelColor=1C1410)](https://ancaria.dev)

[Русский](README.md) · [English](README.EN.md)

</div>

# mods

Das offizielle Mod-Repository für Sacred Gold und die Quellen der vier Mods
darin.

Der Launcher bindet dieses Repository von selbst ein, du musst nichts
einrichten. Du wählst einen Mod aus, der Launcher lädt ihn herunter, prüft ihn
und legt ihn in `<Sacred Gold>/mods`.

Ein Mod-Repository ist ein ganz normales Git-Repository mit einem Index im
Wurzelverzeichnis. Für deine eigenen Mods kannst du ein eigenes anlegen, und
Spieler binden es über die URL ein.

## Erste Schritte

### Einen Mod installieren

1. Öffne den Launcher und geh zum Tab Available.
2. Drück beim gewünschten Mod auf Install.
3. Drück auf Play.

### Ein anderes Repository einbinden

Der Launcher nimmt eine HTTPS-Clone-URL mit der Endung `.git`. Für ein privates
Repository gibst du ein Zugriffstoken an.

### Deinen Mod teilen

Veröffentliche deinen Mod in einem eigenen Repository nach dem
[SRML](#srml)-Schema. Spieler können es sofort einbinden. Soll der Mod im
Katalog auf der Website erscheinen, drück auf
[ancaria.dev/mods](https://ancaria.dev/mods) auf Submit your mod.

## Mods

| Mod | Was er tut |
|---|---|
| [`self-check`](self-check) | Prüft, ob der Loader funktioniert. Er abonniert alle Ereignisse und öffnet ein Fenster mit 50 Szenarien, jedes mit einem Hinweis, wie du es bestehst. Weiß heißt noch nicht gesehen, Grün bestanden, Rot ein unerwartetes Ergebnis. Daneben zeigt ein Bereich die Live-Daten deines Helden. |
| [`tracer`](tracer) | Schreibt jedes Ereignis nach `<Sacred Gold>/logs/logs-<time>.txt`, eine Datei pro Start. Er sieht die endgültige Antwort aller Mods, kann sie aber nicht ändern. |
| [`old-huge-potions`](old-huge-potions) | Macht aus jedem aufgehobenen Trank die große Variante derselben Sorte. Nur der Typ ändert sich, Preis und Wirkung bleiben gleich. |
| [`all-my-runes`](all-my-runes) | Macht aus einer Rune einer fremden Klasse eine Kopie einer Rune deines Helden. Heb dafür zuerst mindestens eine eigene Rune auf. Unbekannte Runen lässt der Mod in Ruhe. |

Alle vier Mods laufen ohne Konflikte zusammen. Jeder sieht, was die Mods vor
ihm entschieden haben, und `self-check` hält sich raus, wenn ein anderer Mod
das Ereignis schon entschieden hat.

## SRML

SRML steht für Sacred Repository Mod Layout, Version 1. Jedes Git-Repository
mit `sacred.mods.repository.json` im Wurzelverzeichnis gilt als
Mod-Repository. Dieses hier sieht so aus:

```
sacred.mods.repository.json   der Index, generiert
registry.toml                 Angaben zum Repository, von Hand geschrieben
icon.png                      das Icon des Repositorys
<id>/                         ein Mod: Code, build.gradle.kts, icon.png
```

Jeder Mod bleibt ein normales Gradle-Projekt. Benenn seinen Ordner nach der
Mod-ID, dann trägt `coderpack index` die Pfade `source` und `icon` in den Index
ein. Bei einem anderen Namen fehlen diese Felder. In einem Repository mit nur
einem Mod dient das `icon.png` im Wurzelverzeichnis auch als Icon des Mods.

Bearbeite den Index nicht von Hand. Der Generator liest die Angaben zu jedem
Mod aus `META-INF/declaration.toml` im gebauten JAR: ID, Name, Version,
Beschreibung, Kompatibilität, Autoren, Website und Konflikte. Name, Website,
Icon und die URL-Vorlage für Releases nimmt er aus `registry.toml`. Dateiname,
Größe, SHA-256 und Download-URL berechnet er selbst.

```
coderpack index           Index neu erzeugen
coderpack index --check   prüfen, ohne zu schreiben
```

Dein eigenes Repository funktioniert genauso. Füll `registry.toml` aus, bau
deine Mods und führ `coderpack index` aus.

## Bauen

Du brauchst ein JDK im `PATH`. Die Mods werden für Java 21 kompiliert.

```
gradlew assembleSacredMod
```

Der Befehl baut alle vier Mods, prüft sie mit dem Linter und legt die JARs in
`<mod>/build/sacred-mod/`. So installierst du sie direkt ins Spiel:

```
gradlew installSacredMod -PsacredDir="C:/Games/Sacred Gold"
```

Das Plugin `dev.ancaria.coderpack` kommt aus dem Gradle Plugin Portal, die API
aus Maven Central. Beide Versionen stehen fest in `gradle/libs.versions.toml`.
Maven Local wird zuerst gefragt. Mit `publishToMavenLocal` in `build` oder
`coderpack` kannst du also eine noch nicht veröffentlichte Änderung
ausprobieren.

## Releases

Jeder Mod hat eigene Releases mit dem Tag `<id>-v<Version>`. Für ein Release
hebst du die Version in seinem `build.gradle.kts` an und pushst nach `master`.
Die CI baut die Mods, erzeugt und committet den Index und veröffentlicht ein
Release, wenn es den Tag noch nicht gibt.

Der Index beschreibt immer genau das JAR hinter der Download-URL. Vor der
Installation prüft der Launcher die SHA-256-Summe, liest den Deskriptor und
prüft die Kompatibilität. Eine Datei, die durchfällt, landet nie im
Mod-Ordner.

## Lizenz

MIT, siehe [LICENSE](LICENSE).
