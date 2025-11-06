# PolarPaper
#### Polar world format for Paper

> [!WARNING]  
> Not widely tested, possibly unstable. Backup your worlds before if you don't want to lose them!

Polar is a world format very similar to Slime, with the same advantages:
 - Small file sizes
 - Single file world
 - Immutable (worlds do not save until explicitly requested)
 - Store worlds wherever (whether as a file or in a database)

Polar is also a single plugin without requiring classloaders or a Paper fork

### [Download the latest jar](https://github.com/MinehubMC/PolarPaper/releases/latest)

Polar was originally developed for [Minestom](https://github.com/Minestom/Minestom), see the Minestom library [here](https://github.com/hollow-cube/polar)

[Support Discord](https://discord.gg/5MrPmKqS7p)

## Permissions
Permission nodes are simply `polarpaper.<subcommand>`, for example: `polarpaper.info` for `/polar info`

`polarpaper.version` for the root command (/polar)

## Custom gamerules
Polar provides a few custom gamerules that can be defined in the config:
| Name          | Type    | Description                                 |
| ------------- | ------- | ------------------------------------------- |
| blockPhysics  | Boolean | Controls block placement/interaction rules  |
| blockGravity  | Boolean | Allow gravity blocks to fall (sand, gravel) |
| liquidPhysics | Boolean | Allow lava/water to flow                    |
| coralDeath    | Boolean | Allow coral to die when not nearby water    |

## API
Remember to add `polarpaper` to your depend list in plugin.yml if using as a plugin/compileOnly
```yml
depend:
  - polarpaper
```

Add to Gradle:
```kts
repositories {
    maven("https://repo.minehub.live/releases")
}
dependencies {
    compileOnly("live.minehub:polarpaper:<latest version>")
}
```

Load a polar world
```java
// Manually
byte[] bytes = ...
PolarWorld polarWorld = PolarReader.read(bytes);
Polar.loadWorld(polarWorld, worldName);

// Manually using BytesPolarSource
byte[] bytes = ...
PolarSource source = new BytesPolarSource(bytes);
Polar.loadWorld(source, worldName);

// Using PolarSource
Path savePath = Path.of("./epic/world.polar");
// feel free to use your own PolarSource implementation
PolarSource source = new FilePolarSource(savePath);
Polar.loadWorld(source, worldName);

// Load world like /polar load
Polar.loadWorldFromFile("gamingworld");
```

Save a polar world
```java
// Manually
World bukkitWorld = player.getWorld();
Polar.updateWorld(bukkitWorld); // refresh chunks
PolarWorld polarWorld = PolarWorld.fromWorld(world);
byte[] bytes = PolarWriter.write(polarWorld);

// Manually using BytesPolarSource
World bukkitWorld = player.getWorld();
PolarSource source = new BytesPolarSource();
Polar.saveWorld(bukkitWorld, source);
byte[] bytes = source.bytes();

// Using PolarSource
World bukkitWorld = player.getWorld();
Path savePath = Path.of("./epic/world.polar");
// feel free to use your own PolarSource implementation
PolarSource source = new FilePolarSource(savePath);
Polar.saveWorld(bukkitWorld, source);

// Save world like /polar save
World bukkitWorld = player.getWorld();
Polar.saveWorldToFile(bukkitWorld);
```

Get the `PolarWorld` that a player is in
```java
PolarWorld polarWorld = PolarWorld.fromWorld(player.getWorld());
// (returns null if the world is not from PolarPaper)
```

Register events
```java
// If you're not using PolarPaper as a plugin and instead using it exclusively
// as a dependency (e.g. implementation instead of compileOnly), you do not need to
// add it to the depend list in your plugin.yml. However, you must manually register the plugin listeners:
PolarPaper.registerEvents();
```

### Versioning
`<mc version>.<our version>`

for example `1.21.4.1`
