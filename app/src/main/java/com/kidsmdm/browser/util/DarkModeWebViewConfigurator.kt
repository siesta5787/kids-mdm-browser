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
 * that doesn't support it.
 */
object DarkModeWebViewConfigurator {
    fun apply(settings: WebSettings, isSystemDark: Boolean) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, isSystemDark)
        }
    }
}
