package com.levelpixel.nextwebview.components;

import android.webkit.WebView;

/**
 * Component for privacy enhancements like cookie banner blocking
 */
public class PrivacyEnhancementComponent {
    
    private boolean cookieBlockingEnabled = false;
    private boolean intelligentTrackingPrevention = true;
    
    /**
     * JavaScript to block cookie banners and consent popups
     */
    private static final String COOKIE_BANNER_BLOCKER_JS =
            "function blockCookieBanners() {" +
                    "  let removed = 0;" +
                    "  const selectors = [" +
                    "    '.cookie-banner', '.cookie-notice', '.cookie-policy', '.cookies-popup', '.cookie-consent'," +
                    "    '.cookie-alert', '#cookie-banner', '#cookie-notice', '#cookie-policy', '#cookies'," +
                    "    '.consent-banner', '.consent-popup', '.gdpr-banner', '.gdpr-consent', '.gdpr-popup'," +
                    "    '[data-cookie-notice]', '[data-gdpr]', '[aria-label*=\"cookie\"]', '[aria-label*=\"consent\"]'," +
                    "    'div[class*=\"cookie\"][class*=\"banner\"], div[class*=\"cookie\"][class*=\"notice\"]'," +
                    "    'div[class*=\"gdpr\"][class*=\"banner\"], div[class*=\"gdpr\"][class*=\"consent\"]'" +
                    "  ];" +
                    "  selectors.forEach(selector => {" +
                    "    try {" +
                    "      document.querySelectorAll(selector).forEach(el => {" +
                    "        if (el && el.style) {" +
                    "          el.style.setProperty('display', 'none', 'important');" +
                    "          el.setAttribute('data-dune-blocked-cookie', 'true');" +
                    "          removed++;" +
                    "        }" +
                    "      });" +
                    "    } catch (e) {}" +
                    "  });" +
                    
                    // Also accept cookies automatically to prevent recurring banners
                    "  ['accept', 'agree', 'continue', 'got it', 'ok', 'accept all', 'i agree', 'consent']" +
                    "    .forEach(text => {" +
                    "      document.querySelectorAll('button, a, .button').forEach(el => {" +
                    "        if (el.textContent.toLowerCase().includes(text)) {" +
                    "          if (el.textContent.toLowerCase().includes('cookie') || " +
                    "              el.textContent.toLowerCase().includes('consent') || " +
                    "              el.textContent.toLowerCase().includes('gdpr')) {" +
                    "            try { el.click(); removed++; } catch(e) {}" +
                    "          }" +
                    "        }" +
                    "      });" +
                    "    });" +
                    "  return removed;" +
                    "}" +
                    "blockCookieBanners();" +
                    "new MutationObserver(() => blockCookieBanners()).observe(document.body, { childList: true, subtree: true });";
    
    /**
     * Apply privacy enhancing scripts to a WebView
     */
    public void applyPrivacyProtections(WebView webView) {
        if (cookieBlockingEnabled) {
            webView.evaluateJavascript(COOKIE_BANNER_BLOCKER_JS, null);
        }
        
        if (intelligentTrackingPrevention) {
            // Apply third-party cookie blocking via WebView settings
            // This is handled by the main nextwebview class as it requires direct WebSettings access
        }
    }
    
    // Getters and setters
    public void setCookieBlockingEnabled(boolean enabled) {
        this.cookieBlockingEnabled = enabled;
    }
    
    public boolean isCookieBlockingEnabled() {
        return cookieBlockingEnabled;
    }
    
    public void setIntelligentTrackingPrevention(boolean enabled) {
        this.intelligentTrackingPrevention = enabled;
    }
    
    public boolean isIntelligentTrackingPrevention() {
        return intelligentTrackingPrevention;
    }
}
