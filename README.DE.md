<div align="center">

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org)
[![Gradle](https://img.shields.io/badge/Gradle-9.7.1-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org)
[![SRML](https://img.shields.io/badge/SRML-1-C8A45C?style=for-the-badge&labelColor=1C1410)](#srml)
[![License](https://img.shields.io/badge/License-MIT-4B5563?style=for-the-badge)](LICENSE)
[![Sacred](https://img.shields.io/badge/Sacred-Community-8B1A1A?style=for-the-badge&labelColor=1C1410)](https://ancaria.dev)

[Русский](README.md) · [English](README.EN.md)

</div>

# mods

Dies ist das offizielle Mod-Repository für Sacred Gold und der Quellcode von
vier Mods. Der Launcher kennt seine Adresse bereits. Im Reiter `Available`
genügt ein Klick auf `Install`, danach liegt die geprüfte JAR-Datei unter
`<Sacred Gold>/mods`. Manuelle Downloads und Kopieren entfallen.

Weitere Repositories lassen sich hinzufügen. Der Launcher akzeptiert
HTTPS-Clone-URLs mit der Endung `.git` und liest
`sacred.mods.repository.json` aus dem Stammverzeichnis. Für private
Repositories kann ein Zugriffstoken hinterlegt werden.

## Was hier liegt

| Mod | Was er tut |
|---|---|
| [`self-check`](self-check) | Abonniert alle Loader-Ereignisse und öffnet ein Fenster mit 50 Szenarien, jedes mit einer Zeile dazu, wie es sich erfüllen lässt, und einer Tafel mit Live-Daten des Helden, gelesen über die API. Weiß steht für noch nicht gesehen, Grün für bestanden und Rot für ein unerwartetes Ergebnis. Bei Ereignissen mit Antwort prüft die Mod den gesamten Weg vom Spiel zur JVM und zurück. |
| [`tracer`](tracer) | Schreibt jedes Ereignis nach `<Sacred Gold>/logs/logs-<time>.txt`, mit einer Datei pro Start. Alle Listener verwenden die Priorität `MONITOR`. Tracer zeichnet daher die endgültige Antwort auf und kann sie nicht verändern. |
| [`old-huge-potions`](old-huge-potions) | Ersetzt jeden vom Spieler aufgehobenen Trank durch den größten Typ derselben Art. Die Zuordnung entsteht aus den Typnamen des laufenden Spiels. Nur der Typ ändert sich, daher behält der Trank seinen ursprünglichen Preis und seine Wirkung. |
| [`all-my-runes`](all-my-runes) | Ersetzt eine Rune für eine fremde Klasse durch die Kopie einer bereits gesehenen Rune des Helden. Zuerst muss eine eigene Rune aufgehoben werden. Unbekannte Runen bleiben unverändert. |

Alle vier Mods arbeiten zusammen, und keiner davon meldet einen Konflikt.

Früher führte `self-check` die beiden anderen als Konflikte auf. Alle drei
schrieben beim Aufheben den Typ des Gegenstands, es galt der zuletzt
ausgeführte Listener, und `self-check` lief zuletzt: Ein Trank kam mit seinem
ursprünglichen Typ statt dem großen zurück, und eine Rune mit ihrem
ursprünglichen Typ, aber mit Preis, Stufe und Modifikatoren der kopierten.
Jetzt sieht ein Listener, was vor ihm entschieden wurde, und `self-check`
tritt zurück, sobald ein anderer Mod das Ereignis bereits entschieden hat.
Damit bleibt nichts zu melden.

## Bauen

Ein JDK muss über `PATH` erreichbar sein. Das Plugin kompiliert die Mods für
Java 21.

```
gradlew assembleSacredMod
```

Dieser Befehl baut alle vier Mods und prüft jede davon mit dem Linter. Die
fertigen JAR-Dateien liegen anschließend unter `<mod>/build/sacred-mod/`.
Alternativ lassen sich die Mods direkt ins Spiel installieren:

```
gradlew installSacredMod -PsacredDir="C:/Games/Sacred Gold"
```

Das Plugin `dev.ancaria.coderpack` und die API werden als veröffentlichte
Abhängigkeiten aufgelöst, nicht über benachbarte Verzeichnisse: das Plugin aus
dem Gradle Plugin Portal, die API aus Maven Central.
`gradle/libs.versions.toml` legt Plugin und API beide auf `0.200.0` fest,
die Mod-API 3. Maven Local wird zuerst geprüft, daher lässt sich mit
`publishToMavenLocal` in `build` oder `coderpack` eine noch unveröffentlichte
Änderung testen.

## SRML

SRML steht für Sacred Repository Mod Layout. In Version 1 kennzeichnet
`sacred.mods.repository.json` im Stammverzeichnis ein solches Repository.
Dieses Repository hat folgende Struktur:

```
sacred.mods.repository.json   Repository-Index, generiert
registry.toml                 Repository-Metadaten, von Hand geschrieben
icon.png                      Repository-Symbol
<id>/                         Mod-Code, build.gradle.kts und icon.png
```

Jede Mod bleibt ein gewöhnliches Gradle-Projekt. Bei einem Repository mit
mehreren Mods trägt ihr Verzeichnis üblicherweise die Mod-ID. `coderpack index`
ergänzt dann relative Pfade für `source` und `icon`. Bei einem anderen
Verzeichnisnamen fehlen diese Felder. In einem Repository mit nur einer Mod
kann `icon.png` im Stammverzeichnis zugleich ihr Symbol sein.

Der Index wird nicht von Hand bearbeitet. Mod-ID, Name, Version, Beschreibung,
API- und Loader-Bereiche, Autoren, Website und Konflikte stammen aus
`META-INF/declaration.toml` in jeder gebauten JAR-Datei. Optionale Felder
fehlen, wenn Deskriptor oder Verzeichnisstruktur sie nicht liefern. Name,
Beschreibung, Website, Symbol und Release-URL-Vorlage des Repositories kommen
aus `registry.toml`. Dateiname, Byte-Größe, SHA-256-Prüfsumme und Download-URL
werden aus dem Artefakt und der Vorlage berechnet.

```
coderpack index           neu erzeugen
coderpack index --check   prüfen, ohne etwas zu schreiben
```

Die Version anzuheben genügt. Auf `master` schreibt die CI den Index und
committet ihn, sodass niemand an den Befehl denken muss. Eine bereits
veröffentlichte Mod wird aus der JAR-Datei ihres Releases indiziert, eine
Version ohne Tag aus der soeben gebauten, die gleich veröffentlicht wird. So
beschreibt die Prüfsumme im Index immer genau die Datei, die hinter der
Download-URL liegt, und genau die prüft ein Launcher vor der Installation.

Aus einem Pull Request darf nichts gepusht werden, also wird der Index dort nur
erzeugt und nicht verglichen. Das Erzeugen scheitert weiterhin an einer
JAR-Datei, die bei der Mod-Prüfung durchfällt, an einer unlesbaren
`registry.toml` und an zwei Mods mit derselben Kennung.

## Releases

Jede Mod erhält einen eigenen GitHub-Release mit einem Tag im Format
`<id>-v<version>`. Dieser Release enthält ihre JAR-Datei. Auf `master` baut der
Workflow alle vier Mods, erzeugt und committet den Index neu und erstellt einen
Release nur dann, wenn der zugehörige Tag noch fehlt. Bei unveränderten
Versionen wird nichts veröffentlicht.

URL, Größe und SHA-256-Prüfsumme jeder JAR-Datei stehen im Index. Vor der
Installation prüft der Launcher den Hash, liest den Deskriptor aus der
heruntergeladenen JAR-Datei und kontrolliert ID sowie Loader-Kompatibilität.
Eine fehlgeschlagene Datei bleibt nicht im Mod-Verzeichnis liegen.

Ein eigenes Mod-Repository wird auf dieselbe Weise eingerichtet. Es benötigt
eine `registry.toml` mit Name, Website und Release-URL-Vorlage. Danach werden
die JAR-Dateien gebaut und mit `coderpack index` erfasst. Spieler tragen die
HTTPS-Clone-URL anschließend in den Launcher ein.
