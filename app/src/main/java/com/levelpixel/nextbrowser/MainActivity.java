package com.levelpixel.nextbrowser;

// Import statement section only
import com.levelpixel.nextwebview.interfaces.OnAdBlockedListener;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.text.TextWatcher;
import android.text.Editable;

import com.levelpixel.nextbrowser.databinding.ActivityMainBinding;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements OnAdBlockedListener {
    
    // Default URL to load
    private static final String DEFAULT_URL = "https://www.google.com";
    private static final int REQUEST_BOOKMARK = 1001;
    private static final int REQUEST_TABS = 1002;
    
    private ActivityMainBinding binding;
    private SharedPreferences preferences;
    
    // Stats counters
    private int requestsBlocked = 0;
    private int elementsHidden = 0;
    
    // Tab management
    private List<Tab> tabs = new ArrayList<>();
    private int currentTabIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 
        // Enable edge-to-edge display but maintain system bars

        // Get preferences
        preferences = getSharedPreferences("com.levelpixel.dunebrowser_preferences", MODE_PRIVATE);
        
        // Inflate the binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        // Initialize all views and listeners
        initializeViews();
        setupSwipeRefresh();
        setupListeners();
        
        // Load stats from preferences
        requestsBlocked = preferences.getInt("requests_blocked", 0);
        elementsHidden = preferences.getInt("elements_hidden", 0);
    }

    /**
     * Initializes all UI components and configures nextwebview settings.
     */
    private void initializeViews() {
        // Configure nextwebview with security and privacy settings from preferences
        binding.nextwebview.setAdBlockEnabled(preferences.getBoolean("ad_block_enabled", true));
        binding.nextwebview.setAggressiveAdBlockMode(preferences.getBoolean("aggressive_ad_block", false));
        binding.nextwebview.setPopupBlockEnabled(preferences.getBoolean("popup_block_enabled", true));
        binding.nextwebview.setRedirectBlockEnabled(preferences.getBoolean("redirect_block_enabled", true));
        binding.nextwebview.setUseSystemDownloader(preferences.getBoolean("use_system_downloader", true));
        binding.nextwebview.setJavaScriptEnabled(preferences.getBoolean("javascript_enabled", true));
        binding.nextwebview.setCookieBlockingEnabled(preferences.getBoolean("cookie_block_enabled", false));
        binding.nextwebview.setIntelligentTrackingPrevention(preferences.getBoolean("tracking_prevention", true));

        // Set ad block listener to track stats
        binding.nextwebview.setAdBlockListener(this);
        
        // Load default ad blocklist
        binding.nextwebview.loadAdBlockListFromResource(true, null);

        // Set up progress view listener to track page loading
        binding.nextwebview.setProgressListener(progress -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.progressBar.setProgress(progress);
            if (progress == 100) {
                binding.progressBar.setVisibility(View.INVISIBLE);
            }
            binding.urlInput.setText(binding.nextwebview.getUrl());
        });

        // Load the default URL in nextwebview
        binding.nextwebview.loadUrl(DEFAULT_URL);
    }

    /**
     * Configures SwipeRefreshLayout to refresh the page on swipe gesture.
     */
    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
             binding.nextwebview.reload();
            new Handler().postDelayed(() ->  binding.swipeRefreshLayout.setRefreshing(false), 500);
        });

        // Set color scheme for refresh indicator based on app theme attributes
        int colorPrimary = getColorFromAttr(this, R.attr.backgroundColor);
        int backgroundColor = getColorFromAttr(this, R.attr.backgroundColor);
        int colorSecondary = getColorFromAttr(this, R.attr.colorTextSecondary);

        binding.swipeRefreshLayout.setColorSchemeColors(colorPrimary, colorSecondary, backgroundColor);
    }

    /**
     * Utility method to fetch color attribute from the app's theme.
     *
     * @param context The application context.
     * @param attr The attribute to fetch.
     * @return The color value of the specified attribute.
     */
    private static int getColorFromAttr(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        TypedArray typedArray = context.obtainStyledAttributes(typedValue.data, new int[]{attr});
        int color = typedArray.getColor(0, 0);
        typedArray.recycle();
        return color;
    }

    /**
     * Sets up button listeners and handles user interactions.
     */
    private void setupListeners() {
        // Show 'Go' button only if there's input text
        binding.urlInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.goButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.goButton.setOnClickListener(v -> {
            loadUrl();
            hideKeyboard();
        });

        binding.backButton.setOnClickListener(v -> {
            if (binding.nextwebview.canGoBack()) {
                binding.nextwebview.goBack();
            } else {
                binding.nextwebview.loadUrl(DEFAULT_URL);
            }
        });

        binding.forwardButton.setOnClickListener(v -> {
            if (binding.nextwebview.canGoForward()) {
                binding.nextwebview.goForward();
            }
        });

        binding.refreshButton.setOnClickListener(v -> binding.nextwebview.reload());

        binding.settingsButton.setOnClickListener(v -> {
            // Open settings activity
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
        
        // Share button - Share current URL
        binding.shareButton.setOnClickListener(v -> {
            String url = binding.nextwebview.getUrl();
            if (url != null && !url.isEmpty()) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, url);
                startActivity(Intent.createChooser(shareIntent, "Share via"));
            }
        });
        
        // Home button
        binding.homeButton.setOnClickListener(v -> binding.nextwebview.loadUrl(DEFAULT_URL));
        
        // Tabs button
        binding.tabsButton.setOnClickListener(v -> showTabsMenu());
        
        // Menu button
        binding.menuButton.setOnClickListener(v -> showBrowserMenu());

        binding.urlInput.setOnEditorActionListener((v, actionId, event) -> {
            loadUrl();
            hideKeyboard();
            return true;
        });

        // Update security indicator when page loads
        binding.nextwebview.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                super.onPageFinished(view, url);
                updateSecurityIndicator(url);
                updateNavigationButtonStates();
            }
        });
    }
    
    /**
     * Shows the browser menu with options
     */
    private void showBrowserMenu() {
        PopupMenu popup = new PopupMenu(this, binding.menuButton);
        popup.getMenuInflater().inflate(R.menu.browser_menu, popup.getMenu());
        
        // Update desktop site checkbox status
        boolean isDesktopMode = preferences.getBoolean("desktop_site", false);
        MenuItem desktopItem = popup.getMenu().findItem(R.id.menu_desktop_site);
        desktopItem.setChecked(isDesktopMode);
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.menu_new_tab) {
                // Create a new tab
                openNewTab();
                return true;
            } else if (itemId == R.id.menu_incognito) {
                // Open incognito tab
                openIncognitoTab();
                return true;
            } else if (itemId == R.id.menu_bookmarks) {
                // Show bookmarks
                showBookmarks();
                return true;
            } else if (itemId == R.id.menu_find_in_page) {
                // Find in page
                showFindDialog();
                return true;
            } else if (itemId == R.id.menu_desktop_site) {
                // Toggle desktop site mode
                boolean newValue = !item.isChecked();
                item.setChecked(newValue);
                preferences.edit().putBoolean("desktop_site", newValue).apply();
                binding.nextwebview.setDesktopMode(newValue);
                return true;
            } else if (itemId == R.id.menu_settings) {
                // Settings already handled in main buttons
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            }
            return false;
        });
        
        popup.show();
    }
    
    /**
     * Shows the tabs menu
     */
    private void showTabsMenu() {
        Intent intent = new Intent(this, TabsActivity.class);
        
        // Prepare tab data to send
        if (tabs.isEmpty()) {
            Tab currentTab = new Tab(
                binding.nextwebview.getTitle() != null ? binding.nextwebview.getTitle() : "New Tab",
                binding.nextwebview.getUrl() != null ? binding.nextwebview.getUrl() : DEFAULT_URL
            );
            tabs.add(currentTab);
            currentTabIndex = 0;
        }
        
        String[] titles = new String[tabs.size()];
        String[] urls = new String[tabs.size()];
        
        for (int i = 0; i < tabs.size(); i++) {
            titles[i] = tabs.get(i).title;
            urls[i] = tabs.get(i).url;
        }
        
        intent.putExtra("tab_titles", titles);
        intent.putExtra("tab_urls", urls);
        intent.putExtra("current_tab", currentTabIndex);
        
        startActivityForResult(intent, REQUEST_TABS);
    }
    
    /**
     * Shows the bookmarks interface
     */
    private void showBookmarks() {
        Intent intent = new Intent(this, BookmarkActivity.class);
        startActivityForResult(intent, REQUEST_BOOKMARK);
    }
    
    /**
     * Add current page to bookmarks
     */
    private void addToBookmarks() {
        Intent intent = new Intent(this, BookmarkActivity.class);
        intent.putExtra("url", binding.nextwebview.getUrl());
        intent.putExtra("title", binding.nextwebview.getTitle());
        startActivityForResult(intent, REQUEST_BOOKMARK);
    }
    
    /**
     * Opens a new tab
     */
    private void openNewTab() {
        // Save current tab state
        saveCurrentTab();
        
        // Create and add new tab
        Tab newTab = new Tab("New Tab", DEFAULT_URL);
        tabs.add(newTab);
        currentTabIndex = tabs.size() - 1;
        
        // Load the new tab
        binding.nextwebview.loadUrl(DEFAULT_URL);
        Toast.makeText(this, "New Tab opened", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Save current tab state
     */
    private void saveCurrentTab() {
        if (currentTabIndex >= 0 && currentTabIndex < tabs.size()) {
            tabs.get(currentTabIndex).url = binding.nextwebview.getUrl();
            tabs.get(currentTabIndex).title = binding.nextwebview.getTitle();
        } else if (binding.nextwebview.getUrl() != null) {
            // Create new tab entry if none exists
            Tab currentTab = new Tab(
                binding.nextwebview.getTitle() != null ? binding.nextwebview.getTitle() : "Tab",
                binding.nextwebview.getUrl()
            );
            tabs.add(currentTab);
            currentTabIndex = tabs.size() - 1;
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_BOOKMARK && data != null) {
                // Handle bookmark selection
                String url = data.getStringExtra("url");
                if (url != null && !url.isEmpty()) {
                    binding.nextwebview.loadUrl(url);
                }
            } else if (requestCode == REQUEST_TABS && data != null) {
                // Handle tabs result
                String[] tabTitles = data.getStringArrayExtra("tab_titles");
                String[] tabUrls = data.getStringArrayExtra("tab_urls");
                int newCurrentTabIndex = data.getIntExtra("current_tab", 0);
                
                if (tabTitles != null && tabUrls != null && tabTitles.length == tabUrls.length) {
                    // Update tabs list
                    tabs.clear();
                    for (int i = 0; i < tabTitles.length; i++) {
                        Tab tab = new Tab(tabTitles[i], tabUrls[i]);
                        tabs.add(tab);
                    }
                    
                    // Switch to selected tab
                    currentTabIndex = newCurrentTabIndex;
                    if (currentTabIndex >= 0 && currentTabIndex < tabs.size()) {
                        binding.nextwebview.loadUrl(tabs.get(currentTabIndex).url);
                    }
                }
            }
        }
    }
    
    /**
     * Opens an incognito tab (simplified implementation)
     */
    private void openIncognitoTab() {
        binding.nextwebview.clearHistory();
        binding.nextwebview.clearCache(true);
        binding.nextwebview.loadUrl(DEFAULT_URL);
        Toast.makeText(this, "Incognito mode activated", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Tab data class
     */
    private static class Tab {
        String title;
        String url;
        
        Tab(String title, String url) {
            this.title = title;
            this.url = url;
        }
    }
    
    /**
     * Update the state of navigation buttons based on WebView state
     */
    private void updateNavigationButtonStates() {
        binding.backButton.setAlpha(binding.nextwebview.canGoBack() ? 1.0f : 0.5f);
        binding.forwardButton.setAlpha(binding.nextwebview.canGoForward() ? 1.0f : 0.5f);
    }

    @Override
    public void onBackPressed() {
        if (binding.nextwebview.canGoBack()) {
            binding.nextwebview.goBack();
        } else {
            super.onBackPressed();
        }
    }
    
    /**
     * Update the security indicator based on URL
     */
    private void updateSecurityIndicator(String url) {
        if (url != null && url.startsWith("https://")) {
            binding.secureIcon.setImageResource(R.drawable.ic_lock);
        } else {
            binding.secureIcon.setImageResource(R.drawable.ic_info);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Apply any settings that might have changed
        applySettingsChanges();
    }
    
    /**
     * Apply settings changes when returning from settings screen
     */
    private void applySettingsChanges() {
        binding.nextwebview.setAdBlockEnabled(preferences.getBoolean("ad_block_enabled", true));
        binding.nextwebview.setAggressiveAdBlockMode(preferences.getBoolean("aggressive_ad_block", false));
        binding.nextwebview.setPopupBlockEnabled(preferences.getBoolean("popup_block_enabled", true));
        binding.nextwebview.setRedirectBlockEnabled(preferences.getBoolean("redirect_block_enabled", true));
        binding.nextwebview.setUseSystemDownloader(preferences.getBoolean("use_system_downloader", true));
        binding.nextwebview.setJavaScriptEnabled(preferences.getBoolean("javascript_enabled", true));
        binding.nextwebview.setCookieBlockingEnabled(preferences.getBoolean("cookie_block_enabled", false));
        binding.nextwebview.setIntelligentTrackingPrevention(preferences.getBoolean("tracking_prevention", true));
        
        // Apply protection scripts immediately
        binding.nextwebview.applyProtectionScripts();
    }

    /**
     * Hides the keyboard and clears focus from the URL input field.
     */
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            clearUrlInputFocus();
        }
    }

    /**
     * Clears focus from the URL input field and focuses on the WebView.
     */
    private void clearUrlInputFocus() {
        binding.urlInput.clearFocus();
         binding.nextwebview.requestFocus();
    }

    // URL processing configurations
    private static final String[] URL_PROTOCOLS = {"http://", "https://", "ftp://", "file://"};
    private static final String[] COMMON_DOMAINS = {".com", ".org", ".net", ".edu", ".gov", ".io", ".co", ".me"};
    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(([\\w-]+\\.)+[\\w-]+|localhost)(:[0-9]+)?(/.*)?$"
    );

    /**
     * Loads a URL or search query from the URL input field.
     */
    private void loadUrl() {
        String input =  binding.urlInput.getText().toString().trim();

        if (input.isEmpty()) {
            showError("Please enter a URL or search term");
            return;
        }

        String processedUrl = processInput(input);
         binding.nextwebview.loadUrl(processedUrl);
    }

    /**
     * Processes the input text and determines whether it's a URL or search term.
     *
     * @param input The user input from the URL field.
     * @return A URL with protocol or a search URL.
     */
    private String processInput(String input) {
        String cleanInput = input.trim().toLowerCase();

        if (hasValidProtocol(cleanInput)) {
            return input;
        }

        if (looksLikeUrl(cleanInput)) {
            return "https://" + input;
        }

        return buildSearchUrl(input);
    }

    /**
     * Checks if the input has a valid URL protocol.
     */
    private boolean hasValidProtocol(String url) {
        for (String protocol : URL_PROTOCOLS) {
            if (url.startsWith(protocol)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the input resembles a URL by checking domain patterns.
     */
    private boolean looksLikeUrl(String input) {
        String domainPart = input.split("/")[0].split("\\?")[0];

        if (IP_ADDRESS_PATTERN.matcher(domainPart).matches()) {
            return true;
        }

        for (String domain : COMMON_DOMAINS) {
            if (input.contains(domain)) {
                return true;
            }
        }

        return URL_PATTERN.matcher(domainPart).matches() || (input.contains(".") && !input.contains(" ") && !input.contains("@") && input.length() >= 3);
    }

    /**
     * Constructs a search URL for a given search query.
     */
    private String buildSearchUrl(String query) {
        try {
            return "https://www.google.com/search?q=" + URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e("MainActivity", "Error encoding search query", e);
            return "https://www.google.com/search?q=" + query.replace(" ", "+");
        }
    }

    /**
     * Displays a toast message for errors.
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Called when an ad is blocked
     */
    @Override
    public void onAdBlocked(String url, String reason) {
        // Optionally show a notification or log for debugging
        Log.d("DuneBrowser", "Blocked: " + url + " - Reason: " + reason);
    }
    
    /**
     * Called with ad blocking statistics
     */
    @Override
    public void onAdBlockStats(int requests, int elements) {
        // Update our counters
        this.requestsBlocked = requests;
        this.elementsHidden = elements;
        
        // Save to preferences for the settings screen
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("requests_blocked", requests);
        editor.putInt("elements_hidden", elements);
        editor.apply();
    }

    /**
     * Shows dialog for finding text in page
     */
    private void showFindDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Find in page");
        
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        input.setHint("Enter text to find");
        int paddingPx = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        builder.setView(input);
        
        builder.setPositiveButton("Find", (dialog, which) -> {
            String query = input.getText().toString();
            if (!query.isEmpty()) {
                binding.nextwebview.findInPage(query);
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            binding.nextwebview.stopFindInPage();
            dialog.cancel();
        });
        
        android.app.AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(d -> binding.nextwebview.stopFindInPage());
        dialog.show();
    }
}
