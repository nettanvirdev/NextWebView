package com.levelpixel.nextwebview.interfaces;

/**
 * Interface for ad blocking statistics
 */
public interface OnAdBlockedListener {
    void onAdBlocked(String url, String reason);
    void onAdBlockStats(int requestsBlocked, int elementsHidden);
}
