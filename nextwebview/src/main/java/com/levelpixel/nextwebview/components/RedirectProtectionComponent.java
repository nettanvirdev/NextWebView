package com.levelpixel.nextwebview.components;

import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import com.levelpixel.nextwebview.interfaces.OnAdBlockedListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Component responsible for redirect and popup protection
 */
public class RedirectProtectionComponent {
    private static final String TAG = "RedirectProtection";
    
    // Control flags
    private boolean popupBlockEnabled = true;
    private boolean redirectBlockEnabled = true;
    
    // Track navigation history for intelligent redirect detection
    private List<String> navigationHistory;
    private Map<String, Long> pageVisitTimes;
    private static final int MAX_HISTORY_SIZE = 20;
    private static final long SUSPICIOUS_REDIRECT_TIME_MS = 500; // Redirects faster than this are suspicious
    
    // Listener for notification
    private OnAdBlockedListener adBlockedListener;

    /**
     * Enhanced JavaScript code to prevent unwanted redirects.
     */
    private static final String ENHANCED_REDIRECT_HANDLER_JS =
            "window.nextwebview = window.nextwebview || {};" +
                    "nextwebview.lastClickTime = 0;" +
                    "nextwebview.originalHref = '';" +
                    "nextwebview.redirectAttempts = 0;" +
                    
                    // Track clicks more reliably
                    "document.addEventListener('mousedown', function(e) {" +
                    "  const target = e.target;" +
                    "  const closestLink = target.closest('a');" +
                    "  if (closestLink) {" +
                    "    nextwebview.originalHref = closestLink.href;" +
                    "    nextwebview.lastClickTime = Date.now();" +
                    "  }" +
                    "}, true);" +
                    
                    // Block window.open popup attempts
                    "const originalWindowOpen = window.open;" +
                    "window.open = function(url, name, features) {" +
                    "  const timeSinceClick = Date.now() - nextwebview.lastClickTime;" +
                    "  const isRecentClick = timeSinceClick < 1000;" +
                    "  if (!isRecentClick) {" +
                    "    console.log('Blocked popup: ' + url);" +
                    "    nextwebview.redirectAttempts++;" +
                    "    return null;" +
                    "  }" +
                    "  return originalWindowOpen.call(this, url, name, features);" +
                    "};" +
                    
                    // Block history API manipulation
                    "const originalPushState = history.pushState;" +
                    "history.pushState = function(state, title, url) {" +
                    "  const timeSinceClick = Date.now() - nextwebview.lastClickTime;" +
                    "  if (timeSinceClick > 2000 && url && url !== location.href) {" +
                    "    const isSuspicious = url.includes('redirect') || " +
                    "                          url.includes('track.php') || " +
                    "                          url.includes('click.php');" +
                    "    if (isSuspicious) {" +
                    "      console.log('Blocked redirect via history.pushState to: ' + url);" +
                    "      nextwebview.redirectAttempts++;" +
                    "      return;" +
                    "    }" +
                    "  }" +
                    "  return originalPushState.call(this, state, title, url);" +
                    "};" +
                    
                    // Block location changes
                    "const originalLocationAssign = location.assign;" +
                    "location.assign = function(url) {" +
                    "  const timeSinceClick = Date.now() - nextwebview.lastClickTime;" +
                    "  if (timeSinceClick > 1000) {" +
                    "    const isSuspicious = url.includes('redirect') || " +
                    "                          url.includes('track.php') || " +
                    "                          url.includes('click.php');" +
                    "    if (isSuspicious) {" +
                    "      console.log('Blocked redirect via location.assign to: ' + url);" +
                    "      nextwebview.redirectAttempts++;" +
                    "      return;" +
                    "    }" +
                    "  }" +
                    "  return originalLocationAssign.call(this, url);" +
                    "};" +
                    
                    // Prevent location.href changes
                    "let locationHrefDescriptor = Object.getOwnPropertyDescriptor(window.Location.prototype, 'href');" +
                    "if (locationHrefDescriptor && locationHrefDescriptor.configurable) {" +
                    "  Object.defineProperty(window.Location.prototype, 'href', {" +
                    "    set: function(url) {" +
                    "      const timeSinceClick = Date.now() - nextwebview.lastClickTime;" +
                    "      if (timeSinceClick > 1000) {" +
                    "        const isSuspicious = url.includes('redirect') || " +
                    "                            url.includes('track.php') || " +
                    "                            url.includes('click.php');" +
                    "        if (isSuspicious) {" +
                    "          console.log('Blocked redirect via location.href to: ' + url);" +
                    "          nextwebview.redirectAttempts++;" +
                    "          return;" +
                    "        }" +
                    "      }" +
                    "      locationHrefDescriptor.set.call(this, url);" +
                    "    }," +
                    "    get: locationHrefDescriptor.get" +
                    "  });" +
                    "}";
    
    public RedirectProtectionComponent() {
        navigationHistory = new ArrayList<>();
        pageVisitTimes = new HashMap<>();
    }
    
    /**
     * Process a new page navigation
     * 
     * @param url The URL being navigated to
     * @param webView WebView instance for potential redirection
     * @return true if navigation should be intercepted
     */
    public boolean processPageStarted(String url, WebView webView) {
        if (!redirectBlockEnabled) return false;
        
        // Record navigation for redirect detection
        if (!navigationHistory.isEmpty() && url.equals(navigationHistory.get(0))) {
            // Already processed this URL
            return false;
        }
        
        // Add to navigation history
        navigationHistory.add(0, url); // Add to front
        pageVisitTimes.put(url, System.currentTimeMillis());
        
        // Keep history at a reasonable size
        if (navigationHistory.size() > MAX_HISTORY_SIZE) {
            String oldUrl = navigationHistory.remove(navigationHistory.size() - 1);
            pageVisitTimes.remove(oldUrl);
        }
        
        // Check for suspicious rapid redirects
        int redirectCount = 0;
        if (navigationHistory.size() > 1) {
            String previousUrl = navigationHistory.get(1);
            Long lastTime = pageVisitTimes.get(previousUrl);
            if (lastTime != null) {
                long timeDiff = System.currentTimeMillis() - lastTime;
                if (timeDiff < SUSPICIOUS_REDIRECT_TIME_MS) {
                    redirectCount++;
                    // If we detect too many rapid redirects, go back to a safe page
                    if (redirectCount >= 3 && navigationHistory.size() >= 3) {
                        String safeUrl = navigationHistory.get(2); // Go back two pages
                        if (webView != null) {
                            webView.loadUrl(safeUrl);
                        }
                        if (adBlockedListener != null) {
                            adBlockedListener.onAdBlocked(url, "Excessive redirects detected");
                        }
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Inject redirect protection scripts into the WebView
     */
    public void injectRedirectProtectionScripts(WebView webView) {
        if (!redirectBlockEnabled) return;
        webView.evaluateJavascript(ENHANCED_REDIRECT_HANDLER_JS, null);
    }
    
    /**
     * Check if a URL load should be overridden (blocked)
     * 
     * @param request The WebResourceRequest being loaded
     * @return true if the request should be blocked
     */
    public boolean shouldBlockNavigation(WebResourceRequest request) {
        String url = request.getUrl().toString().toLowerCase();
        
        // Check for popup patterns
        if (popupBlockEnabled && isProbablePopupUrl(url)) {
            if (adBlockedListener != null) {
                adBlockedListener.onAdBlocked(url, "Popup blocked");
            }
            return true; // Block the navigation
        }
        
        // Check for suspicious redirects
        if (redirectBlockEnabled && isProbableRedirectUrl(url)) {
            if (adBlockedListener != null) {
                adBlockedListener.onAdBlocked(url, "Suspicious redirect blocked");
            }
            return true; // Block the navigation
        }
        
        return false; // Allow normal navigation
    }
    
    /**
     * Advanced detection for popup URLs using multiple heuristics
     * @param url URL to check
     * @return true if the URL is likely a popup
     */
    private boolean isProbablePopupUrl(String url) {
        return url.contains("popup") ||
                url.contains("click.php") ||
                url.contains("window=") ||
                url.contains("/pop/") ||
                url.contains("popads") ||
                url.contains("popcash") ||
                url.contains("popunder") ||
                url.contains("pophit") ||
                url.contains("exit-ad");
    }

    /**
     * Enhanced redirect detection using URL patterns and heuristics
     * @param url URL to check
     * @return true if the URL is likely a malicious redirect
     */
    private boolean isProbableRedirectUrl(String url) {
        return url.contains("redirect") ||
                url.contains("track.php") ||
                url.contains("tracking.php") ||
                url.contains("goto=") ||
                url.contains("clickthrough") ||
                url.contains("go.php") ||
                url.contains("exit=") ||
                url.contains("counter.php") || 
                url.contains("out.php");
    }
    
    // Enable/disable functionality
    public void setPopupBlockEnabled(boolean enabled) {
        this.popupBlockEnabled = enabled;
    }
    
    public void setRedirectBlockEnabled(boolean enabled) {
        this.redirectBlockEnabled = enabled;
    }
    
    public boolean isPopupBlockEnabled() {
        return popupBlockEnabled;
    }
    
    public boolean isRedirectBlockEnabled() {
        return redirectBlockEnabled;
    }
    
    // Listener management
    public void setAdBlockedListener(OnAdBlockedListener listener) {
        this.adBlockedListener = listener;
    }
}
