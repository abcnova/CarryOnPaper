![CarryOnPaper](assets/project-icon.webp)

# CarryOnPaper

**CarryOnPaper** is a server-side Carry On plugin for Paper and compatible Bukkit servers. Players sneak and right-click to carry configured blocks or living mobs; container contents are preserved when enabled.

[Modrinth](https://modrinth.com/plugin/carryonpaper) · [CurseForge](https://www.curseforge.com/minecraft/bukkit-plugins/carryonpaper) · [SpigotMC](https://www.spigotmc.org/resources/carryonspigot.135984/) · [Hangar](https://hangar.papermc.io/ABC_Nova/CarryOnPaper) · [GitHub source and releases](https://github.com/ABC_Nova/CarryOnPaper) · [Report an issue](https://github.com/ABC_Nova/CarryOnPaper/issues)

## Features

- Carry chests, barrels, shulker boxes, furnaces, anvils and other configured blocks.
- Preserve container inventories by default; configure block allow and deny lists.
- Carry living mobs with a configurable deny list. New entity types are allowed by default; players are always excluded for session safety.
- English, German, French and Spanish messages, plus configurable sounds.
- `/carryon reload` and `/carryon language <language>`.
- Optional Modrinth update checks; only server operators and the console receive notices.

## Downloads

Use the Paper artifact for Paper, Purpur and compatible Paper forks. The Spigot artifact has the name `CarryOnSpigot`; it is the same plugin and codebase built with Spigot API metadata.

- [Modrinth releases](https://modrinth.com/plugin/carryonpaper/versions)
- [GitHub releases](https://github.com/ABC_Nova/CarryOnPaper/releases)

## Compatibility

The plugin declares Bukkit API 1.21 as its minimum and is built for Java 21 bytecode. Paper 26.1+ servers require Java 25 to run, while 1.21.x servers require Java 21. Actual server builds must still be smoke-tested; a version tag alone is not runtime proof.

## Configuration

`plugins/CarryOnPaper/config.yml` and `sounds.yml` contain English comments for every setting. Built-in language files are under `plugins/CarryOnPaper/lang/`. Existing configs migrate to schema 7 while retaining unrelated custom values.

The update checker requests the public Modrinth project version endpoint at startup and every 12 hours. It sends no player names, server address, configuration, or other server data.

## Building

Requires JDK 25 and Maven 3.9+.

```powershell
mvn clean package
mvn clean package -Pspigot
```

Output: `target/CarryOnPaper-1.0.2.jar` and `target/CarryOnSpigot-1.0.2.jar`.

## Credits and license

CarryOnPaper is an independent server plugin inspired by the Carry On gameplay concept. It is not affiliated with the original mod. This project retains the Zlib license used by the existing release.
