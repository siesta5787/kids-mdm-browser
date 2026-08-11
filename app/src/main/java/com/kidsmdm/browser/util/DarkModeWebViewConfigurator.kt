package com.kidsmdm.browser.util

import android.webkit.WebSettings
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/**
 * Makes web *content* dark mode track the same system-dark signal the app chrome uses (see
 * ui/BrowserTheme.kt) - without this, WebView content stays light even when the rest of the app
 * is dark, which reads as broken. `ALGORITHMIC_DARKENING` lets WebView darken pages that don't
 * support `prefers-color-scheme` themselves; pages that do declare it are unaffected either way.
 * Feature-gated since it's provider/version-dependent - a no-op (not a crash) on a WebView build
 * that doesn't support it. `FORCE_DARK_STRATEGY` additionally forces algorithmic darkening ahead
 * of a page's own (possibly broken/partial) declared theme.
 *
 * Verified via logging that every documented signal here is correctly set - feature-supported,
 * the settings flag itself reading back true, the forced strategy, and the WebView's own
 * Configuration night-mode bit (see WebViewFactory.darkAwareContext) - and web content still
 * doesn't visually darken on a stock AVD emulator WebView build. That appears to be a genuine
 * WebView-implementation limitation for this WebView version/provider, not a gap in this code;
 * re-test on real GrapheneOS/Vanadium hardware before assuming otherwise, since a different
 * WebView provider can behave differently here.
 */
object DarkModeWebViewConfigurator {
    fun apply(settings: WebSettings, isSystemDark: Boolean) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, isSystemDark)
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
            WebSettingsCompat.setForceDarkStrategy(
                settings,
                WebSettingsCompat.DARK_STRATEGY_USER_AGENT_DARKENING_ONLY,
            )
        }
    }
}
