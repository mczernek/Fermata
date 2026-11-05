## This fork (mczernek)
Personal build of Fermata for Android Auto, published to Google Play (internal testing track) as
`me.aap.fermata.mczernek.auto`. Fork-only changes live on the `mczernek` branch, rebased on top of
upstream `master`. The `master` branch is kept identical to upstream.

### Setting up a new machine
1. Install Android Studio (or the SDK command line tools) and JDK 17. In the SDK manager install
   **NDK 29.0.14206865** and **CMake 4.1.2** (the versions are defined in `gradle/libs.versions.toml`
   and `modules/whisper/build.gradle`).
2. Clone the fork and point the remotes at the fork and at upstream:
   ```bash
   git clone -b mczernek https://github.com/mczernek/Fermata.git
   cd Fermata
   git remote rename origin fork
   git remote add origin https://github.com/AndreyPavlenko/Fermata.git
   git fetch origin
   git branch --set-upstream-to=fork/mczernek mczernek
   git branch master origin/master   # master tracks upstream master
   ```
3. Copy the Google Play **upload key** from a safe place (password manager, encrypted backup -
   never commit it) to e.g. `~/Private/keys/Fermata/fermata.jks`. Its certificate is
   `CN=Michal Czernek`, SHA1 `E4:42:DC:BF:B1:AF:56:77:27:59:59:7F:1D:6D:E6:21:AB:2F:90:6C`.
4. Create `local.properties` in the project root (it's git-ignored):
   ```properties
   sdk.dir=/path/to/Android/sdk
   storeFile=/absolute/path/to/fermata.jks
   storePassword=...
   keyAlias=fermata
   keyPassword=...
   ```
   Do this **before** the first Gradle run. If `storeFile` is missing or doesn't point to an
   existing file, `settings.gradle` generates a new `res/fermata.jks` (`CN=Fermata`) and rewrites
   `local.properties` to use it - Google Play rejects bundles signed with that key.

### Building a release for Google Play
1. Bump `appVersionCode` (and `appVersionName` if needed) in `gradle/libs.versions.toml` - Play
   requires a new, higher version code for every upload.
2. Build the bundle. On macOS the native whisper build needs `ninja` from the SDK CMake on `PATH`:
   ```bash
   export PATH="$ANDROID_SDK_ROOT/cmake/4.1.2/bin:$PATH"
   ./gradlew bundleAutoRelease
   ```
   The bundle is `fermata/build/outputs/bundle/autoRelease/fermata-<version>-auto-release.aab`.
3. Check it's signed with the upload key (the SHA1 must match the one above):
   ```bash
   keytool -printcert -jarfile fermata/build/outputs/bundle/autoRelease/*.aab
   ```
4. Upload it in Play Console -> Fermata -> Testing -> Internal testing -> Create new release.

If the upload key is ever lost, it can be replaced in Play Console (Setup -> App signing ->
Request upload key reset); the app signing key itself is managed by Google.

### Fork changes
* Own application id (`APP_ID` in `build.gradle`), no `automotive` feature requirement, and fixes
  for content providers of on-demand feature modules, that crash Play installs.
* Car home screen with large tiles instead of the bottom navigation bar (Settings -> Interface ->
  Android Auto -> Home screen with tiles).
* "Fermata Full Screen" Android Auto app, showing the Fermata UI on the whole car screen.
  These apps are listed only, because `<uses name="template" />` is re-enabled in
  `fermata/src/auto/res/xml/automotive_app_desc.xml` (upstream disabled it, as it may conflict
  with the media service since AA 17.3). Android Auto must have "Unknown sources" enabled in its
  developer settings to list them.
* "Youtube Full Screen", "Netflix Full Screen", "TV Full Screen" and "Web browser" Android Auto
  apps - the same full screen UI, opened on another initial screen (`YoutubeCarService`,
  `NetflixCarService`, `TvCarService`, `WebBrowserCarService`). They are navigation apps, so they
  get the whole screen. All of them share one UI, so switching between them keeps the state. Home
  always opens the home screen with tiles. The car action strip (Home, Back, Menu) is shown over
  the content. In the split screen, tap the app icon in the Android Auto rail to get back to the
  full screen. The `web` and `tv` modules are delivered at install time, so they are always
  available.
* "Youtube", "Netflix" and "TV" Android Auto apps - the regular, projection based versions (not
  navigation apps), shown in the split screen like Fermata itself (`YoutubeCarActivityService`,
  `NetflixCarActivityService`, `TvCarActivityService`, `MainCarActivity.getInitialAddon()`).
* No floating button in the car. Its actions are on the car action strip of the full screen apps
  and on the Home/Back/Menu overlay of the projection based apps (`CarNavOverlay`), that stays
  visible in the video players too. TV: "Add M3U IPTV source" moved to the menu.
* YouTube: the automatic full screen on playback clicks the player's own full screen button
  (falls back to the full screen of the player, not the video element, which broke the aspect
  ratio). It can be disabled: YouTube settings -> Open videos in full screen.
* Netflix addon (`modules/web/.../netflix`): the mobile site has no player, so the web view poses
  as desktop Chrome, like Chrome's "Desktop site" mode (`NetflixWebView`): desktop Linux
  User-Agent with the web view's Chrome version, Chrome client hints instead of "Android WebView"
  (not mobile, Linux), no `X-Requested-With` header, `navigator.platform` "Linux x86_64" and the
  desktop viewport. Widevine (protected media) is granted by `FermataChromeClient`.
* Default TV channels: free, official streams in `modules/tv/src/main/res/raw/tv_*.m3u`, each
  added once as a TV source ("Polska", "World News", "Sport & Music"). The local copies are
  refreshed on every version code change, so edit these files and release to update the lists.
* Settings -> Other -> Version shows the version name and code.
* The debug-only `CarDisplayPreviewActivity` accepts `--es addon <addon class>` to preview them.

## !!!Be aware of scam!!! The sites like https :// fermata-auto . com  are 100% scam. This app is free and opensource!


## Fermata Media Player
[![Download the latest APK](https://img.shields.io/github/v/release/AndreyPavlenko/Fermata?label=Download%20APK&logo=github&style=for-the-badge)](https://github.com/AndreyPavlenko/Fermata/releases/latest)

## About
Fermata Media Player is a free, open source audio, video and TV player with a simple and intuitive interface. It is focused on playing media files organized in folders and playlists.

Supported features:

* Play media files organized in folders
* IPTV addon with support for XMLTV EPG and Catchup
* Remembers the last played track and position for each folder
* Support for favorites and playlists
* Support for CUE and M3U playlists
* Support for bookmarks
* Audio effects: Equalizer, Bass/Volume Boost and Virtualizer
* Configure audio effects for individual tracks and folders
* Configure playback speed for individual tracks and folders
* Customizable titles and subtitles
* Support for Android Auto
* Support for Android TV
* Show favorites and playlists on Android TV home screen
* Pluggable media engines: MediaPlayer, ExoPlayer and VLC
* Video player with support for subtitles and audio streams (VLC Engine only)

## Building the project
* Download and install the latest Android SDK or Android Studio from https://developer.android.com/studio/
* Set the environment variable ANDROID_SDK_ROOT pointing to the SDK directory
```bash
export ANDROID_SDK_ROOT=<path to android SDK>
```

### Clone the repository
```bash
git clone --recurse-submodules https://github.com/AndreyPavlenko/Fermata.git
cd Fermata
```

### Build AAB
```bash
./gradlew bundleAutoRelease -PAPP_ID_SFX=.type.your.pkg.sfx.here
find $PWD -name *.aab
```

### Build APK
```bash
./gradlew bundleAutoRelease -PAPP_ID_SFX=.type.your.pkg.sfx.here
find $PWD -name *.apk
```

### Building in docker
```bash
docker run -ti --name Fermata andreypavlenko/fermata
```
Enter the requested key alias and password, when prompted.
Build the required package using the above commands.
To copy the built package to the host machine, open a new terminal and run:
```bash
docker cp Fermata:/home/mobiledevops/Fermata/fermata/build/outputs/bundle/autoRelease/ .
```


## Donation
If you like the application, please consider making a donation:

[PayPal](https://www.paypal.com/donate/?hosted_button_id=NP5Q3YDSCJ98N)

[CloudTips](https://pay.cloudtips.ru/p/a03a73da)

[Yandex Money](https://money.yandex.ru/to/410014661137336)
