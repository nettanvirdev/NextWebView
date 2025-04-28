package com.levelpixel.nextbrowser;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for managing bookmarks
 */
public class BookmarkActivity extends AppCompatActivity {
    
    private RecyclerView bookmarksRecyclerView;
    private TextView noBookmarksText;
    private BookmarkAdapter adapter;
    private List<BookmarkItem> bookmarks;
    private SharedPreferences preferences;
    private FloatingActionButton fabAdd;
    
    // Request codes
    public static final int REQUEST_CODE = 1002;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmark);
        
        // Set up action bar with back button
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Bookmarks");
        }
        
        // Initialize views
        bookmarksRecyclerView = findViewById(R.id.bookmarks_recycler_view);
        noBookmarksText = findViewById(R.id.text_no_bookmarks);
        fabAdd = findViewById(R.id.fab_add_bookmark);
        
        // Initialize bookmarks list
        bookmarks = new ArrayList<>();
        
        // Initialize adapter
        adapter = new BookmarkAdapter();
        bookmarksRecyclerView.setAdapter(adapter);
        bookmarksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Initialize SharedPreferences
        preferences = getSharedPreferences("com.levelpixel.dunebrowser_preferences", MODE_PRIVATE);
        
        // Load saved bookmarks
        loadBookmarks();
        
        // Set up FAB click listener
        fabAdd.setOnClickListener(v -> showAddBookmarkDialog(null, null));
        
        // Check if we received a URL to add as bookmark from MainActivity
        String url = getIntent().getStringExtra("url");
        String title = getIntent().getStringExtra("title");
        
        if (url != null && !url.isEmpty()) {
            showAddBookmarkDialog(url, title);
        }
        
        // Update empty state visibility
        updateEmptyState();
    }
    
    /**
     * Load saved bookmarks from SharedPreferences
     */
    private void loadBookmarks() {
        String bookmarksJson = preferences.getString("bookmarks", "[]");
        bookmarks.clear();
        
        try {
            JSONArray jsonArray = new JSONArray(bookmarksJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                BookmarkItem item = new BookmarkItem();
                item.title = jsonObject.getString("title");
                item.url = jsonObject.getString("url");
                bookmarks.add(item);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }
    
    /**
     * Save bookmarks to SharedPreferences
     */
    private void saveBookmarks() {
        JSONArray jsonArray = new JSONArray();
        
        try {
            for (BookmarkItem item : bookmarks) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("title", item.title);
                jsonObject.put("url", item.url);
                jsonArray.put(jsonObject);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        preferences.edit().putString("bookmarks", jsonArray.toString()).apply();
    }
    
    /**
     * Update empty state message visibility
     */
    private void updateEmptyState() {
        if (bookmarks.isEmpty()) {
            noBookmarksText.setVisibility(View.VISIBLE);
            bookmarksRecyclerView.setVisibility(View.GONE);
        } else {
            noBookmarksText.setVisibility(View.GONE);
            bookmarksRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Show dialog to add a new bookmark
     */
    private void showAddBookmarkDialog(String url, String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Bookmark");
        
        View view = getLayoutInflater().inflate(R.layout.dialog_add_bookmark, null);
        EditText titleInput = view.findViewById(R.id.edit_bookmark_title);
        EditText urlInput = view.findViewById(R.id.edit_bookmark_url);
        
        if (url != null) {
            urlInput.setText(url);
        }
        
        if (title != null) {
            titleInput.setText(title);
        }
        
        builder.setView(view);
        
        builder.setPositiveButton("Add", (dialog, which) -> {
            String newTitle = titleInput.getText().toString();
            String newUrl = urlInput.getText().toString();
            
            if (newTitle.isEmpty() || newUrl.isEmpty()) {
                Toast.makeText(BookmarkActivity.this, "Title and URL cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Format URL if needed
            if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                newUrl = "https://" + newUrl;
            }
            
            BookmarkItem newBookmark = new BookmarkItem();
            newBookmark.title = newTitle;
            newBookmark.url = newUrl;
            
            bookmarks.add(newBookmark);
            saveBookmarks();
            adapter.notifyItemInserted(bookmarks.size() - 1);
            
            updateEmptyState();
            Toast.makeText(BookmarkActivity.this, "Bookmark added", Toast.LENGTH_SHORT).show();
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        
        builder.show();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Bookmark item data class
     */
    private static class BookmarkItem {
        String title;
        String url;
    }
    
    /**
     * Adapter for bookmarks RecyclerView
     */
    private class BookmarkAdapter extends RecyclerView.Adapter<BookmarkViewHolder> {
        
        @NonNull
        @Override
        public BookmarkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_bookmark, parent, false);
            return new BookmarkViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull BookmarkViewHolder holder, int position) {
            BookmarkItem item = bookmarks.get(position);
            holder.titleTextView.setText(item.title);
            holder.urlTextView.setText(item.url);
            
            holder.itemView.setOnClickListener(v -> {
                // Return result to MainActivity
                Intent resultIntent = new Intent();
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    resultIntent.putExtra("url", bookmarks.get(adapterPosition).url);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
            });
            
            holder.deleteButton.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    bookmarks.remove(adapterPosition);
                    notifyItemRemoved(adapterPosition);
                    saveBookmarks();
                    updateEmptyState();
                    Toast.makeText(BookmarkActivity.this, "Bookmark deleted", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        @Override
        public int getItemCount() {
            return bookmarks.size();
        }
    }
    
    /**
     * ViewHolder for bookmarks
     */
    private static class BookmarkViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView urlTextView;
        ImageButton deleteButton;
        
        public BookmarkViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.text_bookmark_title);
            urlTextView = itemView.findViewById(R.id.text_bookmark_url);
            deleteButton = itemView.findViewById(R.id.button_delete_bookmark);
        }
    }
}
