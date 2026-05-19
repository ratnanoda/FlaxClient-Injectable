<p align="center">
  <img src="/.github/assets/GlideClientLogo.png" alt="Glide Client. An updated version of Soar client" title="GlideClient">
</p>

## More features, fixes and quality of life.
Download now on our website! https://glideclient.com/download
### Join our discord! https://glideclient.com/discord
<br>


## License 
Glide uses [GPL v3](https://github.com/GlideClient/client/blob/main/LICENSE)

## Issues
Please either contact us on discord or make a github issues if there is any issues you need to bring to our attention

## building (assuming intelij idea)
```
gradlew setupDecompWorkspace
```
```
gradlew genIntellijRuns
```
```
gradlew build
```

## Platform support
windows (working well) <br>
linux (working but some issues noticed that can be fixed by using the fps limit mod) <br>
mac os (working with some limitations)

## Launch with FlaxClientLauncher
1. Run `FlaxClientLauncher.bat` in the repository root.
2. Select **1.8.9** or **1.21.11** from the version dropdown.
3. Click `Launch` (or `Launch Offline`) to start FlaxClient.

## Build 1.21.11 Fabric mod
Requires Java 21+:
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.11"
cd FlaxClient-1.21.11
.\gradlew.bat build
```





