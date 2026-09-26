# Game Libraries

These JAR files are required to build EtherMenu but are **not included** in the repository for copyright reasons.

You must extract them from your own Project Zomboid installation.

## Required Files

| File | Description |
|------|-------------|
| `zombie.jar` | Project Zomboid game classes (`zombie.*` packages). Extract from `projectzomboid.jar` in the game directory. |
| `Kahlua.jar` | Kahlua2 Lua interpreter used by PZ |
| `fmod.jar` | FMOD audio library bindings |
| `org.jar` | `org.*` utility packages used by PZ |

## How to Extract

From your PZ installation folder (e.g. `C:\Steam\steamapps\common\ProjectZomboid`):

1. **zombie.jar**: Copy `projectzomboid.jar` and rename it, or use it directly as `zombie.jar`
2. **Kahlua.jar**, **fmod.jar**, **org.jar**: These can be found inside `projectzomboid.jar` or in the game's classpath. Extract the relevant packages into separate JARs.

Alternatively, you can use a tool like 7-Zip to extract the needed `.class` files from `projectzomboid.jar` and repackage them.
