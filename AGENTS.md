# Agents

If you learn things about making the Gradle build work in the sandbox, document it under the Sandbox section.

# Various Notes

* This is a Minecraft Forge 1.7.10 mod called Electrical Age (Eln). It uses an updated GTNH Gradle build system.
* We are aiming to move to 100% Kotlin. Where possible, use Kotlin for new code.
* Java source files belong in the `src/main/java` directory. Kotlin source files belong in the `src/main/kotlin` directory.
* Do not update the Gradle configuration or wrapper without asking first. They are from an upstream repository.
* If you create documentation, please add it do the `docs` directory.
* Do not move the `id` or `subId` of existing registered items or blocks unless explicitly asked. Those values are world-save sensitive and changing unrelated ones breaks existing saves.
* **i18n (internationalization) workflow**:
  - Wrap all user-facing strings with `tr()` and item/block names with `TR_NAME()`.
  - Avoid dynamically constructing strings; keep them as complete phrases so translators can work with them.
  - Use positional parameters (`%1$`, `%2$`, …) for runtime values instead of string concatenation or direct variable embedding — this allows translators to reorder arguments for their language.
  - After adding new items or translatable strings, run `./gradlew generateLangFiles` to update the language files. Do not manually edit language files unless fixing an existing translation.
* The javac deprecation/unchecked warnings reported from `build/rfg/minecraft-src/java` were already investigated. They come from generated/decompiled patched Minecraft/Forge sources, not Electrical Age source. Running `compilePatchedMcJava` with `-Xlint:deprecation` and `-Xlint:unchecked` shows details such as deprecated Forge progress APIs and raw collection usage in Minecraft classes. There is not a meaningful durable fix in this repository because `build/rfg/minecraft-src` is generated from an external source; suppress, isolate, or leave diagnostic lint opt-in rather than trying to patch those generated files.

# Sandbox

* When `./gradlew` fails inside the sandbox claiming it “Could not determine a usable wildcard IP”, rerun with `GRADLE_USER_HOME=$PWD/.gradle` and, if needed, request escalation—the build otherwise succeeds once it can write its cache and bind to an IP.
* The first `port-1.20.1` build downloads and decompiles Minecraft (a few minutes). Maven Central can answer `429 Too Many Requests` during the first dependency download; rerun with `--max-workers=2`, already downloaded files stay cached.
* `port-1.20.1` needs Java 17; Gradle provisions it through the foojay toolchain resolver when only a newer JDK is installed.
* To look up a 1.20.1 class or method signature, run `javap -cp <jar> <class>` on `~/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.20.1-<forge>_mapped_official_1.20.1/*.jar` (no decompiled sources are generated).
* `pkill -f <pattern>` kills the calling shell when the pattern appears in its own command line; stop dev servers with `kill $(pgrep -x java)` instead.

# 1.20.1 port

* The Forge 1.20.1 port is a separate Gradle project in `port-1.20.1/`. See `docs/port-1.20.1.md` for layout, conventions and milestones. Its build files are ours, not upstream's, so they may be changed.
