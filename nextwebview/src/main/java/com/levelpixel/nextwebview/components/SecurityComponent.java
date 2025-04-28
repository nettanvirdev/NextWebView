package com.levelpixel.nextwebview.components;

import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.levelpixel.nextwebview.interfaces.OnAdBlockedListener;
import com.levelpixel.nextwebview.interfaces.OnProgressChangedListener;

/**
 * Component for handling security features like blocking scam alerts
 */
public class SecurityComponent {
    
    private OnAdBlockedListener adBlockedListener;
    
    /**
     * Create a WebChromeClient that blocks suspicious JavaScript dialogs
     */
    public WebChromeClient createSecureWebChromeClient(final OnProgressChangedListener progressListener) {
        return new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (progressListener != null) {
                    progressListener.onProgressChanged(newProgress);
                }
            }
            
            // Block JavaScript alerts, confirms and prompts that are often used for scams
            @Override
            public boolean onJsAlert(WebView view, String url, String message, android.webkit.JsResult result) {
                // Only block alerts that look suspicious
                if (message != null && (
                    message.toLowerCase().contains("virus") ||
                    message.toLowerCase().contains("infected") ||
                    message.toLowerCase().contains("hacked") ||
                    message.toLowerCase().contains("call") ||
                    message.toLowerCase().contains("support") ||
                    message.toLowerCase().contains("error") ||
                    message.toLowerCase().contains("update") ||
                    message.toLowerCase().contains("prize") ||
                    message.toLowerCase().contains("winner"))) {
                    
                    result.cancel();
                    if (adBlockedListener != null) {
                        adBlockedListener.onAdBlocked(url, "Blocked scam alert: " + message);
                    }
                    return true;
                }
                return super.onJsAlert(view, url, message, result);
            }
            
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, android.webkit.JsResult result) {
                // Block suspicious confirms
                if (message != null && (
                    message.toLowerCase().contains("leave") ||
                    message.toLowerCase().contains("exit") ||
                    message.toLowerCase().contains("virus") ||
                    message.toLowerCase().contains("infected"))) {
                    
                    result.cancel();
                    if (adBlockedListener != null) {
                        adBlockedListener.onAdBlocked(url, "Blocked scam confirm: " + message);
                    }
                    return true;
                }
                return super.onJsConfirm(view, url, message, result);
            }
        };
    }
    
    // Listener management
    public void setAdBlockedListener(OnAdBlockedListener listener) {
        this.adBlockedListener = listener;
    }
}
