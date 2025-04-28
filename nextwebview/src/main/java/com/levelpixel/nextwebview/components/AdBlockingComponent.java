package com.levelpixel.nextwebview.components;

import android.content.Context;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import androidx.annotation.Nullable;

import com.levelpixel.nextwebview.R;
import com.levelpixel.nextwebview.interfaces.OnAdBlockedListener;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Component responsible for ad blocking functionality
 */
public class AdBlockingComponent {
    private static final String TAG = "AdBlockingComponent";
    
    // Set to store domains that should be blocked
    private Set<String> adBlockList;
    
    // Pattern-based blocking for more sophisticated detection
    private List<Pattern> adUrlPatterns;
    
    // Tracking stats
    private int requestsBlocked = 0;
    private int elementsHidden = 0;
    
    // Control flags
    private boolean adBlockEnabled = true;
    private boolean aggressiveAdBlockMode = false;
    
    // Listener for ad blocking events
    private OnAdBlockedListener adBlockedListener;
    
    private Context context;
    
    /**
     * Enhanced JavaScript code to detect and remove unwanted overlay elements
     */
    private static final String ENHANCED_BLOCK_OVERLAY_JS =
            "function enhancedBlockUnwantedOverlays() {" +
                    // Track blocked elements for reporting
                    "  let blockedElements = 0;" +
                    
                    // More comprehensive selector targeting common ad containers
                    "  const elementsToCheck = document.querySelectorAll('div, iframe, span, aside, ins, section');" +
                    "  elementsToCheck.forEach(el => {" +
                    "    const style = window.getComputedStyle(el);" +
                    "    const rect = el.getBoundingClientRect();" +
                    "    const zIndex = parseInt(style.zIndex) || 0;" +
                    "    const opacity = parseFloat(style.opacity) || 1;" +
                    
                    // More sophisticated position detection
                    "    if ((style.position === 'fixed' || style.position === 'absolute' || style.position === 'sticky') && opacity > 0) {" +
                    
                    // Expanded keyword matching for better detection
                    "      const innerHTML = el.innerHTML.toLowerCase();" +
                    "      const hasAdKeywords = innerHTML.match(/(adsby|sponsored|advertisement|click here|you won|congratulation|lucky winner|pop-under|pop-up|banner|promo|offer|discount)/i);" +
                    
                    // Social buttons and sharing widgets are often fixed position but legitimate
                    "      const isSocialWidget = innerHTML.match(/(share|facebook|twitter|instagram|pinterest|linkedin)/i) && " +
                    "                            (rect.width < 100 && rect.height < 300);" +
                    
                    // Detect modal/overlay that covers most of the screen
                    "      const isFullScreenOverlay = (rect.width > window.innerWidth * 0.8 && rect.height > window.innerHeight * 0.8) && " +
                    "                                 zIndex > 100;" +
                    
                    // Corner ad detection with more precise positioning analysis
                    "      const isCornerAd = (rect.width < 400 && rect.height < 400) && " +
                    "                       ((rect.top < 10 && rect.left < 10) || " +
                    "                        (rect.top < 10 && rect.right > window.innerWidth - 10) || " +
                    "                        (rect.bottom > window.innerHeight - 10 && rect.left < 10) || " +
                    "                        (rect.bottom > window.innerHeight - 10 && rect.right > window.innerWidth - 10));" +
                    
                    // Floating boxes in the middle of content are often ads
                    "      const isFloatingBox = (rect.width < 600 && rect.height < 600) && " +
                    "                          (rect.left > 50 && rect.right < window.innerWidth - 50) && " +
                    "                          (rect.top > 100 && rect.bottom < window.innerHeight - 50) && " +
                    "                          zIndex > 10;" +
                    
                    // Check if element matches ad patterns but not social widgets
                    "      if ((hasAdKeywords || isCornerAd || (isFloatingBox && hasAdKeywords) || isFullScreenOverlay) && !isSocialWidget) {" +
                    "        const prevDisplay = el.style.display;" +
                    "        el.style.setProperty('display', 'none', 'important');" +
                    "        el.setAttribute('data-dune-blocked', 'true');" +
                    "        blockedElements++;" +
                    "      }" +
                    "    }" +
                    "  });" +
                    "  return blockedElements;" +
                    "}" +
                    
                    // Set up a MutationObserver to catch dynamically added ads
                    "let blockObserver = new MutationObserver(function(mutations) {" +
                    "  let shouldScan = false;" +
                    "  mutations.forEach(function(mutation) {" +
                    "    if (mutation.addedNodes.length > 0) shouldScan = true;" +
                    "  });" +
                    "  if (shouldScan) enhancedBlockUnwantedOverlays();" +
                    "});" +
                    
                    // Start blocking now and observe future DOM changes
                    "const blockedCount = enhancedBlockUnwantedOverlays();" +
                    "blockObserver.observe(document.body, { childList: true, subtree: true });" +
                    "return blockedCount;";

    public AdBlockingComponent(Context context) {
        this.context = context;
        this.adBlockList = new HashSet<>();
        this.adUrlPatterns = new ArrayList<>();
        initializeAdPatterns();
    }
    
    /**
     * Initialize regex patterns for advanced ad detection
     */
    private void initializeAdPatterns() {
        // Common ad server URL patterns
        String[] patternStrings = {
            ".*/ad[sx]?/.*",                // Matches paths containing /ad/, /ads/, or /adx/
            ".*/banner[sx]?/.*",            // Banner images and scripts
            ".*/pop(up|under).*",           // Popup or popunder scripts
            ".*[a-z0-9-]{1,50}.com/(ads|banners)/.*", // Common ad folder structure
            ".*/pixel\\.(gif|jpg|png).*",   // Tracking pixels
            ".*/tracking/.*",               // Tracking scripts
            ".*/analytics\\.js.*",          // Analytics scripts
            ".*/count\\.js.*",              // Counting/tracking scripts
            ".*/beacon\\.js.*",             // Tracking beacons
            ".*/affiliate/.*",              // Affiliate links
            ".*/(click|track|log)\\.php.*", // Click tracking scripts
            ".*/metrics/.*"                 // Metrics collection
        };
        
        for (String pattern : patternStrings) {
            adUrlPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
        }
    }
    
    /**
     * Check if a request should be blocked
     * 
     * @param request WebResourceRequest to check
     * @return WebResourceResponse with empty content if blocked, null otherwise
     */
    @Nullable
    public WebResourceResponse processRequest(WebResourceRequest request) {
        if (!adBlockEnabled) {
            return null;
        }
        
        String url = request.getUrl().toString().toLowerCase();
        String host = request.getUrl().getHost();
        
        // First check our domain blacklist
        if (host != null && adBlockList.contains(host)) {
            requestsBlocked++;
            if (adBlockedListener != null) {
                adBlockedListener.onAdBlocked(url, "Domain in blocklist");
                adBlockedListener.onAdBlockStats(requestsBlocked, elementsHidden);
            }
            return createEmptyResponse();
        }

        // Check for ad patterns in URL
        if (matchesAdPattern(url)) {
            requestsBlocked++;
            if (adBlockedListener != null) {
                adBlockedListener.onAdBlocked(url, "Matches ad pattern");
                adBlockedListener.onAdBlockStats(requestsBlocked, elementsHidden);
            }
            return createEmptyResponse();
        }
        
        // Additional checks for aggressive mode
        if (aggressiveAdBlockMode) {
            // Check for suspicious file types often used in ads
            if (url.endsWith(".gif") || url.contains("beacon") || url.contains("pixel")) {
                // Check dimensions - tracking pixels are usually small
                if (url.contains("1x1") || url.contains("pixel.gif")) {
                    requestsBlocked++;
                    if (adBlockedListener != null) {
                        adBlockedListener.onAdBlocked(url, "Tracking pixel detected");
                        adBlockedListener.onAdBlockStats(requestsBlocked, elementsHidden);
                    }
                    return createEmptyResponse();
                }
            }
        }
        
        return null; // Not blocked
    }
    
    /**
     * Injects ad blocking scripts into the WebView after page load
     */
    public void injectAdBlockingScripts(WebView webView) {
        if (!adBlockEnabled) return;
        
        webView.evaluateJavascript(ENHANCED_BLOCK_OVERLAY_JS, value -> {
            try {
                // Parse the number of blocked elements
                int newlyBlocked = Integer.parseInt(value);
                elementsHidden += newlyBlocked;
                
                if (adBlockedListener != null && newlyBlocked > 0) {
                    adBlockedListener.onAdBlockStats(requestsBlocked, elementsHidden);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing blocked elements count", e);
            }
        });
    }
    
    /**
     * Load ad block rules from a raw resource file
     * Format: One domain per line
     * @param useDefaultHosts whether to use the default hosts file
     * @param resourceId custom resource ID, can be null if useDefaultHosts is true
     */
    public void loadAdBlockListFromResource(boolean useDefaultHosts, @Nullable Integer resourceId) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                InputStream fis = (useDefaultHosts || resourceId == null)
                        ? context.getResources().openRawResource(R.raw.adblockserverlist)
                        : context.getResources().openRawResource(resourceId);

                try (BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
                    Set<String> tempSet = new HashSet<>();

                    br.lines()
                            .map(String::trim)
                            .filter(line -> !line.isEmpty())
                            .map(String::toLowerCase)
                            .forEach(tempSet::add);

                    // synchronized to prevent concurrent modification
                    synchronized (adBlockList) {
                        adBlockList.addAll(tempSet);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error loading ad block list", e);
            } finally {
                executor.shutdown();
            }
        });
    }
    
    /**
     * Check if a URL matches any of our ad patterns
     * @param url URL to check
     * @return true if URL matches ad pattern
     */
    private boolean matchesAdPattern(String url) {
        // First check basic patterns
        if (url.contains("/ad/") ||
                url.contains("/ads/") ||
                url.contains("pop-under") ||
                url.contains("popunder") ||
                url.contains("click.php") ||
                url.contains("track.php") ||
                url.contains("banner.") ||
                url.contains("analytics.") ||
                url.contains("tracker.")) {
            return true;
        }
        
        // Then check regex patterns
        for (Pattern pattern : adUrlPatterns) {
            if (pattern.matcher(url).matches()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Creates an empty response for blocked requests
     */
    private WebResourceResponse createEmptyResponse() {
        return new WebResourceResponse("text/plain", "utf-8",
                new ByteArrayInputStream("".getBytes()));
    }

    /**
     * Add a domain to the block list
     * @param domain domain to block
     */
    public void addCustomBlockedDomain(String domain) {
        adBlockList.add(domain.toLowerCase());
    }

    /**
     * Remove a domain from the block list
     * @param domain domain to unblock
     */
    public void removeBlockedDomain(String domain) {
        adBlockList.remove(domain.toLowerCase());
    }

    /**
     * Clear all domains from the block list
     */
    public void clearBlocklist() {
        adBlockList.clear();
    }

    /**
     * Get the current size of the block list
     * @return number of domains in the blocklist
     */
    public int getBlocklistSize() {
        return adBlockList.size();
    }

    /**
     * Check if a domain is currently blocked
     * @param domain domain to check
     * @return true if domain is blocked
     */
    public boolean isBlockedDomain(String domain) {
        return adBlockList.contains(domain.toLowerCase());
    }
    
    /**
     * Add a custom ad URL pattern for regex-based blocking
     * @param pattern regex pattern to match against URLs
     */
    public void addCustomAdPattern(String pattern) {
        adUrlPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
    }
    
    /**
     * Reset ad blocking statistics
     */
    public void resetBlockStats() {
        requestsBlocked = 0;
        elementsHidden = 0;
    }
    
    // Getters for stats
    public int getBlockedRequestCount() {
        return requestsBlocked;
    }
    
    public int getHiddenElementCount() {
        return elementsHidden;
    }
    
    // Enable/disable functionality
    public void setAdBlockEnabled(boolean enabled) {
        this.adBlockEnabled = enabled;
    }
    
    public void setAggressiveAdBlockMode(boolean enabled) {
        this.aggressiveAdBlockMode = enabled;
    }
    
    public boolean isAdBlockEnabled() {
        return adBlockEnabled;
    }
    
    // Listener management
    public void setAdBlockListener(OnAdBlockedListener listener) {
        this.adBlockedListener = listener;
    }
}
