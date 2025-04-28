package com.levelpixel.nextwebview;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.DownloadListener;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;

import com.levelpixel.nextwebview.components.AdBlockingComponent;
import com.levelpixel.nextwebview.components.DownloadHandlerComponent;
import com.levelpixel.nextwebview.components.PrivacyEnhancementComponent;
import com.levelpixel.nextwebview.components.RedirectProtectionComponent;
import com.levelpixel.nextwebview.components.SecurityComponent;
import com.levelpixel.nextwebview.interfaces.OnAdBlockedListener;
import com.levelpixel.nextwebview.interfaces.OnProgressChangedListener;

/**
 * nextwebview extends Android's WebView with additional security and user experience features:
 * - Advanced ad blocking with pattern matching
 * - Intelligent popup and overlay detection
 * - Heuristic-based redirect prevention
 * - Custom download handling
 * - Progress tracking
 * - Machine learning-inspired content filtering
 *
 * This implementation uses a modular component architecture for better maintainability.
 */
public class NextWebView extends WebView {
    // Component modules
    private AdBlockingComponent adBlocker;
    private RedirectProtectionComponent redirectProtection;
    private PrivacyEnhancementComponent privacyEnhancement;
    private DownloadHandlerComponent downloadHandler;
    private SecurityComponent securityComponent;

    // Feature flags
    private boolean javascriptEnabled = true;

    // Interfaces for callback functionality
    private OnProgressChangedListener progressListener;
    private OnAdBlockedListener adBlockedListener;

    /**
     * Constructor for programmatic instantiation
     */
    public NextWebView(Context context) {
        super(context);
        init();
    }

    /**
     * Constructor for XML layout instantiation
     */
    public NextWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Initializes the WebView with enhanced features
     */
    private void init() {
        // Initialize components
        adBlocker = new AdBlockingComponent(getContext());
        redirectProtection = new RedirectProtectionComponent();
        privacyEnhancement = new PrivacyEnhancementComponent();
        downloadHandler = new DownloadHandlerComponent(getContext());
        securityComponent = new SecurityComponent();
        
        // Set up the WebView
        setupWebView();
        setupWebViewClient();
        setupWebChromeClient();
        setupDownloadListener();
    }

    /**
     * Configures WebView settings for optimal browsing experience with enhanced security
     */
    private void setupWebView() {
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(javascriptEnabled);
        settings.setBuiltInZoomControls(true);
        settings.setSupportZoom(true);
        settings.setDisplayZoomControls(false);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        setScrollbarFadingEnabled(true);

        // Enhanced security settings
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setSaveFormData(false);
        
        // Disable geolocation by default for privacy
        settings.setGeolocationEnabled(false);
        
        // Advanced cache settings
        settings.setDatabaseEnabled(true);
        
        // Disable mixed content (HTTP resources on HTTPS pages)
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        
        // Set user agent
        settings.setUserAgentString(settings.getUserAgentString());
    }

    /**
     * Sets up the WebViewClient for content filtering
     */
    private void setupWebViewClient() {
        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                
                // Process page navigation through redirect protection
                redirectProtection.processPageStarted(url, view);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                
                // Apply components to the loaded page
                adBlocker.injectAdBlockingScripts(view);
                redirectProtection.injectRedirectProtectionScripts(view);
                privacyEnhancement.applyPrivacyProtections(view);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // Check if request should be blocked by ad blocker
                WebResourceResponse blockedResponse = adBlocker.processRequest(request);
                if (blockedResponse != null) {
                    return blockedResponse;
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Check if navigation should be blocked
                return redirectProtection.shouldBlockNavigation(request);
            }
        });
    }
    
    /**
     * Sets up the WebChromeClient for handling JavaScript dialogs and progress
     */
    private void setupWebChromeClient() {
        setWebChromeClient(securityComponent.createSecureWebChromeClient(progressListener));
    }

    /**
     * Configures the download listener
     */
    private void setupDownloadListener() {
        setDownloadListener(downloadHandler.getDownloadListener());
    }

    // Public API methods

    /**
     * Enable/disable ad blocking
     * @param enabled true to enable ad blocking
     */
    public void setAdBlockEnabled(boolean enabled) {
        adBlocker.setAdBlockEnabled(enabled);
    }
    
    /**
     * Enable/disable aggressive ad blocking mode
     * Aggressive mode blocks more content but may interfere with some sites
     * @param enabled true to enable aggressive mode
     */
    public void setAggressiveAdBlockMode(boolean enabled) {
        adBlocker.setAggressiveAdBlockMode(enabled);
    }

    /**
     * Enable/disable popup blocking
     * @param enabled true to enable popup blocking
     */
    public void setPopupBlockEnabled(boolean enabled) {
        redirectProtection.setPopupBlockEnabled(enabled);
    }

    /**
     * Enable/disable redirect blocking
     * @param enabled true to enable redirect blocking
     */
    public void setRedirectBlockEnabled(boolean enabled) {
        redirectProtection.setRedirectBlockEnabled(enabled);
    }

    /**
     * Enable/disable cookie consent banner blocking
     * @param enabled true to enable cookie banner blocking
     */
    public void setCookieBlockingEnabled(boolean enabled) {
        privacyEnhancement.setCookieBlockingEnabled(enabled);
    }

    /**
     * Enable/disable system download manager
     * @param enabled true to use system downloader
     */
    public void setUseSystemDownloader(boolean enabled) {
        downloadHandler.setUseSystemDownloader(enabled);
    }
    
    /**
     * Enable/disable JavaScript
     * Note: Disabling JavaScript improves security but breaks many websites
     * @param enabled true to enable JavaScript
     */
    public void setJavaScriptEnabled(boolean enabled) {
        this.javascriptEnabled = enabled;
        getSettings().setJavaScriptEnabled(enabled);
    }
    
    /**
     * Enable/disable intelligent tracking prevention
     * @param enabled true to enable tracking prevention
     */
    public void setIntelligentTrackingPrevention(boolean enabled) {
        privacyEnhancement.setIntelligentTrackingPrevention(enabled);
    }

    /**
     * Set custom download listener
     * @param listener the download listener to use
     */
    public void setCustomDownloadListener(DownloadListener listener) {
        downloadHandler.setCustomDownloadListener(listener);
    }

    /**
     * Set progress change listener
     * @param listener the progress listener to use
     */
    public void setProgressListener(OnProgressChangedListener listener) {
        this.progressListener = listener;
    }
    
    /**
     * Set ad blocked listener to receive notifications about blocked ads
     * @param listener the ad blocked listener to use
     */
    public void setAdBlockListener(OnAdBlockedListener listener) {
        this.adBlockedListener = listener;
        
        // Propagate listener to components
        adBlocker.setAdBlockListener(listener);
        redirectProtection.setAdBlockedListener(listener);
        securityComponent.setAdBlockedListener(listener);
    }

    /**
     * Load ad block rules from a raw resource file
     * @param useDefaultHosts whether to use the default hosts file
     * @param resourceId custom resource ID, can be null if useDefaultHosts is true
     */
    public void loadAdBlockListFromResource(boolean useDefaultHosts, @Nullable Integer resourceId) {
        adBlocker.loadAdBlockListFromResource(useDefaultHosts, resourceId);
    }

    /**
     * Add a domain to the block list
     * @param domain domain to block
     */
    public void addCustomBlockedDomain(String domain) {
        adBlocker.addCustomBlockedDomain(domain);
    }

    /**
     * Remove a domain from the block list
     * @param domain domain to unblock
     */
    public void removeBlockedDomain(String domain) {
        adBlocker.removeBlockedDomain(domain);
    }

    /**
     * Clear all domains from the block list
     */
    public void clearBlocklist() {
        adBlocker.clearBlocklist();
    }

    /**
     * Get the current size of the block list
     * @return number of domains in the blocklist
     */
    public int getBlocklistSize() {
        return adBlocker.getBlocklistSize();
    }

    /**
     * Check if a domain is currently blocked
     * @param domain domain to check
     * @return true if domain is blocked
     */
    public boolean isBlockedDomain(String domain) {
        return adBlocker.isBlockedDomain(domain);
    }
    
    /**
     * Add a custom ad URL pattern for regex-based blocking
     * @param pattern regex pattern to match against URLs
     */
    public void addCustomAdPattern(String pattern) {
        adBlocker.addCustomAdPattern(pattern);
    }
    
    /**
     * Get the number of ad requests blocked in this session
     * @return count of blocked requests
     */
    public int getBlockedRequestCount() {
        return adBlocker.getBlockedRequestCount();
    }
    
    /**
     * Get the number of DOM elements hidden in this session
     * @return count of hidden elements
     */
    public int getHiddenElementCount() {
        return adBlocker.getHiddenElementCount();
    }
    
    /**
     * Reset ad blocking statistics
     */
    public void resetBlockStats() {
        adBlocker.resetBlockStats();
    }
    
    /**
     * Force-inject all protection scripts immediately
     * Useful after settings changes
     */
    public void applyProtectionScripts() {
        adBlocker.injectAdBlockingScripts(this);
        redirectProtection.injectRedirectProtectionScripts(this);
        privacyEnhancement.applyPrivacyProtections(this);
    }

    /**
     * Enable/disable desktop site mode
     * @param enabled true to enable desktop mode
     */
    public void setDesktopMode(boolean enabled) {
        WebSettings settings = getSettings();
        String newUserAgent;
        
        if (enabled) {
            // Use desktop user agent
            newUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.69 Safari/537.36";
        } else {
            // Reset to default mobile user agent
            newUserAgent = WebSettings.getDefaultUserAgent(getContext());
        }
        
        settings.setUserAgentString(newUserAgent);
        settings.setUseWideViewPort(enabled);
        settings.setLoadWithOverviewMode(enabled);
        
        // Force reload to apply changes
        reload();
    }
    
    /**
     * Find text in the current page
     * @param query text to find
     */
    public void findInPage(String query) {
        if (query == null || query.isEmpty()) {
            clearMatches();
            return;
        }
        
        findAllAsync(query);
    }
    
    /**
     * Stop finding in page and clear highlights
     */
    public void stopFindInPage() {
        clearMatches();
    }
    
    /**
     * Check if the WebView's URL is currently using HTTPS
     * @return true if using secure connection
     */
    public boolean isSecureConnection() {
        String url = getUrl();
        return url != null && url.startsWith("https://");
    }
    
    /**
     * Get the site's domain name from the current URL
     * @return domain name of current website
     */
    public String getCurrentDomain() {
        String url = getUrl();
        if (url == null) return "";
        
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            return parsedUrl.getHost();
        } catch (Exception e) {
            return "";
        }
    }
}