package com.levelpixel.nextbrowser;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.levelpixel.nextbrowser.databinding.ActivitySettingsBinding;

/**
 * SettingsActivity provides a user interface for configuring the nextwebview browser.
 * Users can customize privacy, security, and browsing experience options.
 */
public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SharedPreferences preferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize view binding
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up action bar with back button
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Browser Settings");
        }

        // Initialize SharedPreferences
        preferences = getSharedPreferences("com.levelpixel.dunebrowser_preferences", MODE_PRIVATE);
        
        // Load saved settings
        loadSavedSettings();
        
        // Set up switch listeners
        setupSwitchListeners();
        
        // Reset stats button
        binding.resetStatsButton.setOnClickListener(v -> {
            // Reset stats in shared preferences
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("requests_blocked", 0);
            editor.putInt("elements_hidden", 0);
            editor.apply();
            
            updateStats(0, 0);
            Toast.makeText(this, "Statistics reset", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Load saved settings from SharedPreferences
     */
    private void loadSavedSettings() {
        // Load all switch states
        binding.switchAdBlock.setChecked(preferences.getBoolean("ad_block_enabled", true));
        binding.switchAggressiveAdBlock.setChecked(preferences.getBoolean("aggressive_ad_block", false));
        binding.switchPopupBlock.setChecked(preferences.getBoolean("popup_block_enabled", true));
        binding.switchRedirectBlock.setChecked(preferences.getBoolean("redirect_block_enabled", true));
        binding.switchCookieBlock.setChecked(preferences.getBoolean("cookie_block_enabled", false));
        binding.switchJavascript.setChecked(preferences.getBoolean("javascript_enabled", true));
        binding.switchSystemDownloader.setChecked(preferences.getBoolean("use_system_downloader", true));
        binding.switchTrackingPrevention.setChecked(preferences.getBoolean("tracking_prevention", true));
        
        // Load stats
        int requestsBlocked = preferences.getInt("requests_blocked", 0);
        int elementsHidden = preferences.getInt("elements_hidden", 0);
        updateStats(requestsBlocked, elementsHidden);
    }
    
    /**
     * Update the statistics display
     */
    private void updateStats(int requestsBlocked, int elementsHidden) {
        binding.statsRequestsBlocked.setText(String.valueOf(requestsBlocked));
        binding.statsElementsHidden.setText(String.valueOf(elementsHidden));
        binding.statsTotalBlocked.setText(String.valueOf(requestsBlocked + elementsHidden));
    }
    
    /**
     * Set up listeners for all setting switches
     */
    private void setupSwitchListeners() {
        // Ad blocking
        binding.switchAdBlock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("ad_block_enabled", isChecked);
            
            // Show/hide aggressive mode option based on ad blocking being enabled
            binding.layoutAggressiveAdBlock.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        
        // Aggressive ad blocking
        binding.switchAggressiveAdBlock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("aggressive_ad_block", isChecked);
        });
        
        // Popup blocking
        binding.switchPopupBlock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("popup_block_enabled", isChecked);
        });
        
        // Redirect blocking
        binding.switchRedirectBlock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("redirect_block_enabled", isChecked);
        });
        
        // Cookie banner blocking
        binding.switchCookieBlock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("cookie_block_enabled", isChecked);
        });
        
        // JavaScript
        binding.switchJavascript.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("javascript_enabled", isChecked);
            if (!isChecked) {
                Toast.makeText(this, "Warning: Disabling JavaScript may break many websites", 
                               Toast.LENGTH_LONG).show();
            }
        });
        
        // System downloader
        binding.switchSystemDownloader.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("use_system_downloader", isChecked);
        });
        
        // Tracking prevention
        binding.switchTrackingPrevention.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("tracking_prevention", isChecked);
        });
        
        // Initial visibility setup for dependent options
        binding.layoutAggressiveAdBlock.setVisibility(binding.switchAdBlock.isChecked() ? 
                                                      View.VISIBLE : View.GONE);
    }
    
    /**
     * Save a boolean preference
     */
    private void savePreference(String key, boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }
    
    /**
     * Update statistics in SharedPreferences
     */
    public void updateBlockingStats(int requestsBlocked, int elementsHidden) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("requests_blocked", requestsBlocked);
        editor.putInt("elements_hidden", elementsHidden);
        editor.apply();
        
        updateStats(requestsBlocked, elementsHidden);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
