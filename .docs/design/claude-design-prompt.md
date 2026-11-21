# Prompt: Pagecast Design System

Paste everything below the line into Claude Design, and attach every image in [screens](screens).

---

Create a design system for **Pagecast**, an Android app that plays narrated articles as podcasts. While the narration plays, the article's charts, diagrams, and photos appear on screen in sync. You listen with the phone in your pocket, and look only when a chime says there's something to see.

The app works today, but it uses stock Material 3 with the default purple palette. It has no identity of its own. I want a system that makes it feel like a calm, focused listening tool, and that I can implement directly in Jetpack Compose.

## Who uses it

- People who read long technical and research articles, and would rather listen on a commute or a walk.
- They glance at the screen for a few seconds at a time, often in motion, sometimes in sunlight.
- Sessions run 10 to 60 minutes per episode.

## Constraints

- **Dark only.** The app is always dark, so the system bars stay light. Design one dark theme, done well. Don't design a light theme.
- **Images are the content.** In the player, charts and diagrams from the article must stay readable. Nothing in the chrome should compete with them: no strong tints over images, and no busy backgrounds.
- **Compose and Material 3.** Express the system as a Material 3 `ColorScheme`, `Typography`, and `Shapes`, plus any extra tokens. Components should map to Material 3 components (`ListItem`, `FilterChip`, `ModalBottomSheet`, `Slider`, `TopAppBar`, `AlertDialog`) restyled, not custom-drawn.
- **Unbranded content.** The first feed narrates Anthropic articles, but Pagecast isn't affiliated with Anthropic. Don't use Anthropic's branding, colors, or type.
- **Accessibility.** WCAG AA contrast for all text, including the small overline text on list items. Touch targets of at least 48 dp. Must work at 200% font scale.

## The screens

The attached screenshots are the current app, at 1080 × 2400.

| File | Screen | State |
| --- | --- | --- |
| `01-library-first-run` | Library | Fresh install, recommended feed loaded, nothing downloaded |
| `02-library-downloading` | Library | One episode downloading, with a progress ring |
| `03-library-filter-area` | Library | Filtered to the Science area |
| `04-library-filter-downloaded` | Library | Filtered to downloaded episodes |
| `05-library-no-matches` | Library | Filters match nothing |
| `06-library-sort-menu` | Library | Sort menu open: Newest, Oldest, Title |
| `07-add-feed-dialog` | Add podcast | Dialog for a feed URL |
| `08-settings` | Settings | Chime and recommended feed switches |
| `09-library-no-feeds` | Library | No feeds at all |
| `10-player-image` | Player | Playing, controls hidden, chart on screen |
| `11-player-controls` | Player | Controls shown over the image |
| `12-player-speed-sheet` | Player | Playback speed sheet |
| `13-player-chapters` | Player | Chapter list with thumbnails |
| `14-library-continue-listening` | Library | Continue listening section with progress |
| `15-library-error-dialog` | Library | Feed failed to load |

How the main screens work:

- **Library.** An optional Continue listening section, then Episodes with a sort menu and filter chips: Downloaded, 1d, 1w, 1m, then one chip per area, such as Science. Each item has an overline ("Anthropic · Science · Nov 17, 2026 · 31 min"), a title, and either a download button or delete and play buttons.
- **Player.** Full screen, system bars hidden. The current image fills the screen and GIFs animate. A tap shows controls for 4 seconds: back and chapters at the top, back 30 s, play/pause and forward 30 s in the middle, and at the bottom a seek bar, elapsed time, speed, stop, and duration.
- **Sheets.** Speed has −/+ in 0.1 steps, the time left, and preset chips. Chapters lists a thumbnail, title, and start time for each.

## What's weak today

- No identity: default purple accents and stock typography.
- In the player, the 50% black overlay dims the chart the listener is trying to read. Large black bars above and below wide images waste the screen.
- The player header repeats the title when the chapter has the same name.
- Delete sits right next to play on every downloaded item, so it's easy to hit by accident.
- Continue listening items also appear again in the list below, with no visual distinction.
- Empty states and the error dialog are bare text. The error shows a raw exception message.
- Filter chips scroll off-screen, and nothing hints that more areas exist.

## What I want back

1. **Foundations:** a color palette with roles, mapped to every Material 3 `ColorScheme` slot. Also a type scale for Material 3 `Typography`, with one font family (free and on Google Fonts), plus spacing, shapes and corner radii, elevation or tonal surfaces, and motion durations and easing.
2. **Components:** episode list item (not downloaded, downloading, downloaded, in progress), section header, filter chip and sort control, Continue listening card, player control overlay, seek bar, bottom sheets, dialog, empty state, and error state.
3. **Redesigned screens:** Library (first run, filtered, with Continue listening), Player (controls hidden and shown), Speed sheet, Chapters sheet, and Settings. Fix the weaknesses above.
4. **App icon and wordmark** direction for "Pagecast".
5. **Tokens as code:** a Kotlin `ColorScheme`, `Typography`, and `Shapes` I can drop into `MaterialTheme`, with any extra tokens as a small Kotlin object.

Keep it restrained. Pagecast is for listening. The screen should feel quiet until there's something worth looking at.
