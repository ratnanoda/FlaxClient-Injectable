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
2. The launcher is now fixed to **Minecraft 1.8.9**.
3. Click `Launch FlaxClient` to start with your Microsoft account or offline profile.

## Single-exe distribution
- The launcher can now embed `FlaxClient-Release.jar` at build time.
- Build the client first with `.\gradlew.bat build`, then build the launcher in `FlaxClientLauncher`.
- The resulting launcher exe can bootstrap FlaxClient by itself on another Windows machine.





