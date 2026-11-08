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
| Web browser | `#4CB6F5` | |
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

1. Wordmark header.
2. **Streaming** — one tile per addon from `me.aap.fermata.addon.web.*`: solid service color,
   dark icon top-left, dark ExtraBold title bottom-left, 22–24dp corners.
3. **TV and your media** — two wide group rows: dim background, colored rounded icon box,
   cream title, subtitle, chevron (phone only). TV = `TvAddon`, My media = Folders.
4. **More** — small rows for other fragment addons and Voice (only if there are any).

Columns: 2 on the phone, 4 when the width is ≥ 600dp (group rows span half).
In the car and on wide, short screens (landscape) the tiles are sized to fill the screen and the
section titles are hidden. Focus (D-pad/rotary) shows a cream outline.

Settings, About, Exit are not on Home; they are in the Menu.

## Navigation

- Phone bottom bar (`NavBarMediator`): **Home, TV, My media, Menu**. Nothing else is shown.
- The app starts on Home. Home button and Back from a top-level screen go to Home; Back on Home
  exits (press twice).
- **My media** = Folders / Favorites / Playlists with a tab row on top (`MediaTabsView`); the
  selected tab is an orange pill, the "My media" nav item stays selected for all three.
- Floating nav panel (Home/Back/Menu, `NavPanel`): dark translucent pill with a `dc_line` outline.

## Player

Theme-driven: orange seek bar progress, cream thumb, orange loading/stream progress.

## Not restyled yet

List rows of TV and My media (only the theme colors/fonts apply), the control panel layout,
Settings screens.
