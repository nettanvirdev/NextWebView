package com.levelpixel.nextwebview.components;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.widget.Toast;

/**
 * Component for handling file downloads
 */
public class DownloadHandlerComponent {
    
    private Context context;
    private boolean useSystemDownloader = true;
    private DownloadListener customDownloadListener;
    
    public DownloadHandlerComponent(Context context) {
        this.context = context;
    }
    
    /**
     * Get the download listener to attach to the WebView
     */
    public DownloadListener getDownloadListener() {
        return (url, userAgent, contentDisposition, mimeType, contentLength) -> {
            // First check if we have a custom download listener
            if (customDownloadListener != null) {
                customDownloadListener.onDownloadStart(url, userAgent, contentDisposition,
                        mimeType, contentLength);
                return;
            }

            // Security check for potentially dangerous file types
            boolean isPotentiallyDangerous = 
                    contentDisposition != null && (
                    contentDisposition.toLowerCase().contains(".exe") ||
                    contentDisposition.toLowerCase().contains(".msi") ||
                    contentDisposition.toLowerCase().contains(".apk") ||
                    contentDisposition.toLowerCase().contains(".bat") ||
                    contentDisposition.toLowerCase().contains(".cmd") ||
                    contentDisposition.toLowerCase().contains(".sh") ||
                    contentDisposition.toLowerCase().contains(".jar"));
            
            if (isPotentiallyDangerous) {
                Toast.makeText(context, "Warning: Downloading executable files may be unsafe", 
                        Toast.LENGTH_LONG).show();
                // Still allow the download but show a warning
            }

            // Use system download manager 
            if (useSystemDownloader) {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                // Set request headers for authentication and tracking
                request.setMimeType(mimeType);
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);

                // Configure download with better descriptions
                String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
                request.setDescription("Downloading: " + fileName);
                request.setTitle(fileName);
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

                // Start download with enhanced error handling
                try {
                    DownloadManager dm = (DownloadManager) context
                            .getSystemService(Context.DOWNLOAD_SERVICE);
                    dm.enqueue(request);
                    Toast.makeText(context, "Downloading: " + fileName, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(context, 
                            "Download failed: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                }
            }
        };
    }
    
    // Getters and setters
    public void setUseSystemDownloader(boolean enabled) {
        this.useSystemDownloader = enabled;
    }
    
    public boolean isUseSystemDownloader() {
        return useSystemDownloader;
    }
    
    public void setCustomDownloadListener(DownloadListener listener) {
        this.customDownloadListener = listener;
    }
    
    public DownloadListener getCustomDownloadListener() {
        return customDownloadListener;
    }
}
