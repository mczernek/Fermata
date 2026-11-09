# DashCinema design

The app is a **hub**: every streaming service has its own top-level tile, TV and the local
media are grouped. It looks the same on the phone and in the car (Android Auto), the car layout
only fits everything on one screen. Mockups: https://claude.ai/artifact/My6MWVMqn3we4iE6qV8RyG

Rules for changes:
- Do not add functionality as part of design work (no "continue watching", no recommendations).
- Keep one look for the phone and the car. Car-only differences: no section titles, tiles sized
  to fill the screen, no chevrons.
- Verify every visual change with screenshots: emulator (portrait and landscape) and the DHU.

## Colors

`fermata/src/main/res/values/dashcinema_colors.xml`. Night teal ground, cream text, orange for
the active state (the colors of the app icon).

| Token | Hex | Use |
|---|---|---|
| `dc_ground` | `#0A1417` | Background, dark icons/text on colored tiles (`dc_on_tile`) |
| `dc_bar` | `#0F1D21` | Tool bar, nav bar |
| `dc_surface` / `dc_surface_high` | `#15252B` / `#1A2C33` | Menus, tab row |
| `dc_line` | `#24383F` | Outlines, list item borders |
| `dc_cream` | `#F3EEE4` | Text, focus outline |
| `dc_muted` | `#A9BCBF` | Section titles, hints, inactive controls |
| `dc_orange` | `#F2A65A` | Active state: selected tab, seek bar, progress, ripple |

Each service has its own color: solid for its tile, a dim variant for group rows.

| Service | Color | Dim |
|---|---|---|
| YouTube | `#FF5A4F` | |
| Disney+ | `#6F8FFF` | |
| CANAL+ online | `#B08CFF` | |
| TVP VOD | `#FF7FB0` | |
| player.pl | `#FFC94D` | |
| Prime Video | `#00A8E1` | |
| Web browser | `#4CB6F5` | `#0F2A3A` |
| TV | `#2BC4A0` | `#10302B` |
| My media | `#F2A65A` | `#2C2213` |
| Other addons, Voice | `#C9E46A` | `#252B12` |

A new streaming addon needs a color here and a mapping in `CarHomeFragment.serviceColor()`.

## Typography

- **Barlow Condensed Bold**: the wordmark only ("Dash" cream, "Cinema" orange).
- **Plus Jakarta Sans** (400/600/800): everything else. Tile titles and selected tabs are
  ExtraBold, section titles ExtraBold, uppercase, letter-spaced.
- Fonts are bundled in `res/font/`, licenses (OFL) in `fonts/`.

## Theme

`AppTheme.DashCinema` in `res/values/theme_dashcinema.xml`, `THEME_DASHCINEMA` in
`MainActivityPrefs`, the default for both the phone and the car. Other themes stay selectable in
Settings.

## Home (`CarHomeFragment`)

A `RecyclerView` grid, used on the phone and in the car:

1. Wordmark header with round **Settings** and **Menu** buttons on the right.
2. **Streaming** — one tile per addon from `me.aap.fermata.addon.web.*`: solid service color,
   dark icon top-left, dark ExtraBold title bottom-left, 22–24dp corners.
3. **TV and your media** — group rows (TV, My media, Web browser): dim background, colored rounded icon box,
   cream title, subtitle, chevron (phone only). TV = `TvAddon`, My media = Folders, Web browser = `WebBrowserAddon` (not a streaming
   service, so it's not a tile).
4. **More** — small rows for other fragment addons and Voice (only if there are any).

Columns: 2 on the phone (group rows full width), 4 when the width is ≥ 600dp (all the group rows
share one row).
In the car and on wide, short screens (landscape) the tiles are sized to fill the screen and the
section titles are hidden. There the services are balanced between the rows, so every row is full
(5 services: 3 + 2, never 4 + 1); very wide screens (width ≥ 2.4 × height) take up to 5 in a row.
Focus (D-pad/rotary) shows a cream outline.

In the car only the tiles chosen in Settings -> **Android Auto home screen** (on the phone, a top
level item right after Interface) are shown: a checkbox per addon tile, My media and Voice
(`MainActivityPrefs.carHomeItemPref()`, all on by default). The car home reloads as soon as they
change. The phone home always shows everything.

About and Exit are in the Menu; Settings is both in the header and in the Menu.

## Navigation

- No bottom navigation bar on the phone (`MainActivityDelegate.isNavBarHidden()`): Home, TV and
  My media are on Home, Settings and Menu are in the Home header and the floating nav panel.
  In the car the bar (**Home, TV, My media, Menu**, `NavBarMediator`) is shown only if the car
  home is disabled. The nav bar position/size settings are only in the Android Auto settings.
- The app starts on Home. Home button and Back from a top-level screen go to Home; Back on Home
  exits (press twice).
- **My media** = Folders / Favorites / Playlists with a tab row on top (`MediaTabsView`); the
  selected tab is an orange pill, the "My media" nav item stays selected for all three.
- Floating nav panel (Home/Back/Menu, `NavPanel`): dark translucent pill with a `dc_line` outline.
  In a top corner it is placed below the screen header (`R.id.dc_header`: Home, TV, Settings),
  so it never covers the title, back or the header buttons. In the full screen car apps the
  top end position is to the left of Android Auto's round Menu button, the headers also leave
  that corner free (`FermataActivity.getTopEndInsetDp`).

## Screen header (`DcViews`)

TV and Settings have no tool bar; they use `DcViews.header()`: an optional round back button,
a Barlow Condensed title (34sp, 28sp with the back button) and actions on the right (round 48dp
icon buttons or pill buttons on `dc_surface`). Chips: 48dp pills, the selected one filled with the
section color. Anything tappable is at least 48dp and shows the cream focus outline.

## TV (`TvFragment`)

- Header: **TV** + **Add playlist** pill (teal plus). Below it a chip row of the playlists; the
  selected (remembered) or the first playlist is opened directly, the list of playlists is never
  shown. Long press on a chip: Edit / Delete (centered dialog). Back from a playlist goes Home.
- Inside a group: the header shows the group name with a back button, no chips.
- Channels are cards (`MediaItemView.setCardStyle`): `dc_card` background, 18dp corners, a 56dp
  icon box (the logo on `dc_surface_high`, or the default icon on a color picked by the name),
  ExtraBold title, `dc_tv_text` subtitle (hidden when empty), teal EPG progress. The playing
  channel has the `dc_tv_dim` background.
- Car: the chips are next to the title, the channels are a grid of tiles (`setForcedGrid`), only
  the name and the progress. The visible height is divided into whole rows
  (`MediaItemListView.getTileHeight`), so no row is cut under the header.
- No empty list: without playlists a hint is shown under the header.

## Settings (`SettingsFragment`)

Header with the title of the current section (back button inside a section). Rows are cards
(`AppTheme.DashCinema.PreferenceStyle`, `dc_pref_item_bg`): `dc_card`, 18dp corners, 16dp side
margins, ExtraBold 17sp titles, `dc_muted` subtitles, orange controls.

## Menus and dialogs

There are no small popups. All overlay menus (Menu, context menu, tool and control menus) and the
dialogs built on them are centered, up to 560dp wide, with 24dp corners, 60dp rows and a dimmed
backdrop (`dc_scrim`, tap outside closes). Dialog buttons are at least 56dp tall.

## Player

Full screen video of a web site (all the streaming addons and the Web browser) has no overlay of
ours: no control panel, no title, no navigation panel. The site's player has its own controls
(quality, captions, ...) and gets all the touches; Back or its own button leaves the full screen.
The DashCinema control panel and the navigation panel are only for the media we play ourselves
(My media, TV).

Video keeps its aspect ratio. VLC doesn't report the video layout for streams: the surface gets
the ratio of the video track, 16:9 until the track is known.

Theme-driven: orange seek bar progress, cream thumb, orange loading/stream progress.

## Android Auto app icons

Each full screen car app (`*CarService`, the FS mirror) uses the icon of its projection
counterpart with a full screen badge (`car_app_fs_badge`): two diagonal arrows, dark on a white
disc with a dark ring, bottom-right inside the visible circle. The icons are `*_fs` adaptive icons
in `fermata/src/auto/res/drawable`.

## Not restyled yet

List rows of My media (only the theme colors/fonts apply), the control panel layout.
