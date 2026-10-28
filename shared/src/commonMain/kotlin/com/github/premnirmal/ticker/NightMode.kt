package com.github.premnirmal.ticker

/**
 * Platform-neutral description of the effective night mode, derived from the user's theme
 * preference ([UserPreferences.themePref]).
 *
 * This replaces the former Android-only `@AppCompatDelegate.NightMode Int` that lived on
 * `AppPreferences`: the decision (light / dark / follow-system / battery-saver) is shared, while the
 * mapping onto the platform API stays on the platform — Android maps it to `AppCompatDelegate`'s
 * `MODE_NIGHT_*` constants, iOS interprets it for its own theming.
 */
enum class NightMode {
  LIGHT,
  DARK,
  FOLLOW_SYSTEM,
  AUTO_BATTERY,
}

/**
 * Whether the platform should follow the system night mode (vs. fall back to a battery-saver dark
 * mode) when the user picks "follow system". Android gates this on the OS version / manufacturer;
 * iOS always supports it.
 */
internal expect fun supportsSystemNightMode(): Boolean
