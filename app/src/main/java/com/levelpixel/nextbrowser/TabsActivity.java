package com.levelpixel.nextbrowser;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity to manage browser tabs
 */
public class TabsActivity extends AppCompatActivity {
    
    private RecyclerView tabsRecyclerView;
    private FloatingActionButton fabAddTab;
    private TabAdapter tabAdapter;
    private List<TabItem> tabs;
    
    // Tab currently in use
    private int currentTabIndex = 0;
    
    // Request codes
    public static final int REQUEST_CODE = 1001;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabs);
        
        // Set up action bar with back button
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Tabs");
        }
        
        // Initialize tabs from Intent data
        tabs = new ArrayList<>();
        String[] tabTitles = getIntent().getStringArrayExtra("tab_titles");
        String[] tabUrls = getIntent().getStringArrayExtra("tab_urls");
        currentTabIndex = getIntent().getIntExtra("current_tab", 0);
        
        if (tabTitles != null && tabUrls != null && tabTitles.length == tabUrls.length) {
            for (int i = 0; i < tabTitles.length; i++) {
                TabItem tab = new TabItem();
                tab.title = tabTitles[i];
                tab.url = tabUrls[i];
                tabs.add(tab);
            }
        } else {
            // Add a default tab if no tabs were sent
            TabItem defaultTab = new TabItem();
            defaultTab.title = "New Tab";
            defaultTab.url = "https://www.google.com";
            tabs.add(defaultTab);
        }
        
        // Initialize views
        tabsRecyclerView = findViewById(R.id.tabs_recycler_view);
        fabAddTab = findViewById(R.id.fab_add_tab);
        
        // Set up RecyclerView
        tabsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tabAdapter = new TabAdapter();
        tabsRecyclerView.setAdapter(tabAdapter);
        
        // Set up FAB
        fabAddTab.setOnClickListener(v -> addNewTab());
    }
    
    /**
     * Add a new tab
     */
    private void addNewTab() {
        TabItem newTab = new TabItem();
        newTab.title = "New Tab";
        newTab.url = "https://www.google.com";
        tabs.add(newTab);
        tabAdapter.notifyItemInserted(tabs.size() - 1);
        
        // Return the new tab result
        currentTabIndex = tabs.size() - 1;
        setResultAndFinish();
    }
    
    /**
     * Remove a tab at specified position
     */
    private void removeTab(int position) {
        tabs.remove(position);
        tabAdapter.notifyItemRemoved(position);
        
        // If no tabs left, add one
        if (tabs.isEmpty()) {
            addNewTab();
        }
        
        // Fix currentTabIndex if needed
        if (currentTabIndex >= position) {
            currentTabIndex = Math.max(0, currentTabIndex - 1);
        }
    }
    
    /**
     * Set result and finish activity
     */
    private void setResultAndFinish() {
        // Convert tabs to arrays for intent
        String[] titles = new String[tabs.size()];
        String[] urls = new String[tabs.size()];
        
        for (int i = 0; i < tabs.size(); i++) {
            titles[i] = tabs.get(i).title;
            urls[i] = tabs.get(i).url;
        }
        
        Intent resultIntent = new Intent();
        resultIntent.putExtra("tab_titles", titles);
        resultIntent.putExtra("tab_urls", urls);
        resultIntent.putExtra("current_tab", currentTabIndex);
        setResult(RESULT_OK, resultIntent);
        
        finish();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResultAndFinish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        setResultAndFinish();
        super.onBackPressed();
    }
    
    /**
     * Tab data class
     */
    private static class TabItem {
        String title;
        String url;
    }
    
    /**
     * Adapter for tabs RecyclerView
     */
    private class TabAdapter extends RecyclerView.Adapter<TabViewHolder> {
        
        @NonNull
        @Override
        public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_tab, parent, false);
            return new TabViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
            // Store position in a final local variable to use in click listeners
            final int itemPosition = position;
            TabItem tab = tabs.get(itemPosition);
            holder.titleTextView.setText(tab.title);
            holder.urlTextView.setText(tab.url);
            
            // Highlight current tab
            holder.itemView.setSelected(itemPosition == currentTabIndex);
            
            // Click to switch to this tab
            holder.itemView.setOnClickListener(v -> {
                currentTabIndex = holder.getAdapterPosition();
                setResultAndFinish();
            });
            
            // Close tab
            holder.closeButton.setOnClickListener(v -> removeTab(holder.getAdapterPosition()));
        }
        
        @Override
        public int getItemCount() {
            return tabs.size();
        }
    }
    
    /**
     * ViewHolder for tabs
     */
    private static class TabViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView urlTextView;
        ImageButton closeButton;
        
        TabViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.text_tab_title);
            urlTextView = itemView.findViewById(R.id.text_tab_url);
            closeButton = itemView.findViewById(R.id.button_close_tab);
        }
    }
}
