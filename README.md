# NextWebView

<p align="center">
  <img src="images/logo.png" alt="NextWebView Logo" width="200">
</p>

<p align="center">
  🚀 A secure, enhanced WebView component for Android with built-in ad blocking and security features.
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-Level%20Pixel-blue.svg" alt="License"></a>
  <img src="https://img.shields.io/badge/platform-Android-green.svg" alt="Platform">
  <img src="https://img.shields.io/badge/API-21%2B-brightgreen.svg" alt="API">
</p>

## 📋 Table of Contents

- [✨ Key Features](#-key-features)
- [📦 Installation](#-installation)
- [🎯 Basic Usage](#-basic-usage)
- [🛠️ Advanced Configuration](#️-advanced-configuration)
- [📱 Required Permissions](#-required-permissions)
- [🔧 Customization Options](#-customization-options)
- [🗺️ Future Plans](#️-future-plans)
- [🤝 Contributing](#-contributing)
- [📄 License](#-license)
- [📞 Support](#-support)
- [📸 Screenshots](#-screenshots)

## ✨ Key Features

### 🛡️ **Security & Privacy**

- **Ad Blocking**: Built-in filter for common ad servers and domains
- **Popup Blocking**: Prevention of unwanted popup windows
- **Redirect Protection**: Protection from unwanted URL redirects
- **Cookie Banner Blocking**: Automatic handling of cookie consent dialogs

### 📱 **User Experience**

- **Download Management**: Flexible file download handling
- **Progress Tracking**: Real-time page load progress indicator
- **Desktop Mode**: Option to request desktop versions of websites
- **Find in Page**: Text search within web pages

### ⚙️ **Developer Experience**

- **Simple Integration**: Drop-in replacement for Android's WebView
- **Customizable Security**: Control which protection features to enable
- **Performance Optimized**: Efficient rendering and blocking mechanisms
- **Java & Kotlin Support**: Full compatibility with both languages

## 📦 Installation

### Through Maven (JitPack)

#### Add JitPack URL to `settings.gradle`

#### Gradle Groovy

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

#### Gradle.kts

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

#### Add the dependency to your project

#### Gradle Groovy

```groovy
dependencies {
    implementation 'com.github.nettanvirdev:nextwebview:latest-version'
}
```

#### Gradle.kts

```kotlin
dependencies {
    implementation("com.github.nettanvirdev:nextwebview:latest-version")
}
```

### Using as a Library Module

#### Add to your project's build.gradle

```groovy
// filepath: app/build.gradle
dependencies {
    implementation project(':duneweb')
}
```

### Directly including the module

1. Copy the `duneweb` directory into your project
2. Add the module in your app's settings.gradle
3. Add the dependency in your app's build.gradle

## 🎯 Basic Usage

### XML Layout

```xml
<!-- filepath: app/src/main/res/layout/activity_main.xml -->
<com.levelpixel.NextWebView
    android:id="@+id/webView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### Kotlin Implementation

```kotlin
// Basic implementation
class MainActivity : AppCompatActivity() {
    private lateinit var webView: NextWebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        // Enable protection features
        webView.apply {
            setAdBlockEnabled(true)
            setPopupBlockEnabled(true)
            setRedirectBlockEnabled(true)
            loadUrl("https://example.com")
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}
```

### Java Implementation

```java
public class MainActivity extends AppCompatActivity {
    private NextWebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);

        // Enable protection features
        webView.setAdBlockEnabled(true);
        webView.setPopupBlockEnabled(true);
        webView.setRedirectBlockEnabled(true);
        webView.loadUrl("https://example.com");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
```

## 🛠️ Advanced Configuration

### Ad Blocking

```kotlin
// Load blocklist from app resources
webView.loadAdBlockListFromResource(R.raw.adblockserverlist)

// Add custom domains to block
webView.addCustomBlockedDomain("ads.example.com")
webView.addCustomBlockedDomain("analytics.trackingsite.com")

// Remove a domain from blocklist
webView.removeBlockedDomain("allowedsite.com")

// Check if a domain is being blocked
val isBlocked = webView.isBlockedDomain("ads.example.com")

// Clear the entire blocklist
webView.clearBlocklist()
```

### Download Handling

```java
// Use Android's system download manager
webView.setUseSystemDownloader(true);

// Custom download handling
webView.setCustomDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
    // Create custom download handling logic
    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
    String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
    request.setTitle(filename);
    request.setDescription("Downloading file...");
    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
    dm.enqueue(request);

    return true; // Download handled
});
```

### Progress Tracking

```kotlin
// Monitor page loading progress
webView.setProgressListener { progress ->
    progressBar.apply {
        setProgress(progress)
        visibility = if (progress == 100) View.GONE else View.VISIBLE
    }
}
```

### Desktop Mode

```kotlin
// Enable/disable desktop site mode
webView.setDesktopMode(true) // Request desktop version
webView.setDesktopMode(false) // Return to mobile version
```

### Find in Page

```kotlin
// Search for text in the current page
webView.findInPage("search term")

// Clear search highlights
webView.stopFindInPage()
```

### Complete Example

```kotlin
class BrowserActivity : AppCompatActivity() {
    private lateinit var webView: NextWebView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)

        webView.apply {
            // Security features
            setAdBlockEnabled(true)
            setPopupBlockEnabled(true)
            setRedirectBlockEnabled(true)

            // Load blocklist
            loadAdBlockListFromResource(R.raw.adblockserverlist)

            // Progress tracking
            setProgressListener { progress ->
                progressBar.apply {
                    setProgress(progress)
                    visibility = if (progress == 100) View.GONE else View.VISIBLE
                }
            }

            // Download handling
            setUseSystemDownloader(true)

            // Load initial URL
            loadUrl("https://example.com")
        }
    }
}
```

## 📱 Required Permissions

Add these to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28"/>
<uses-permission android:name="android.permission.DOWNLOAD_WITHOUT_NOTIFICATION" />
```

## 🔧 Customization Options

| Feature             | Method                                | Description                        |
| ------------------- | ------------------------------------- | ---------------------------------- |
| Ad Blocking         | `setAdBlockEnabled(Boolean)`          | Enable/disable ad blocking         |
| Popup Blocking      | `setPopupBlockEnabled(Boolean)`       | Enable/disable popup blocking      |
| Redirect Protection | `setRedirectBlockEnabled(Boolean)`    | Enable/disable redirect protection |
| Download Handler    | `setUseSystemDownloader(Boolean)`     | Toggle system download manager     |
| Progress Tracking   | `setProgressListener(listener)`       | Set progress callback              |
| Custom Downloads    | `setCustomDownloadListener(listener)` | Custom download handling           |
| Desktop Mode        | `setDesktopMode(Boolean)`             | Toggle desktop site mode           |
| Find in Page        | `findInPage(String)`                  | Search for text in page            |
| Cookie Blocking     | `setCookieBlockingEnabled(Boolean)`   | Block cookie consent dialogs       |

## 🗺️ Future Plans

### Coming in v1.1

- **Dark Mode Support**: Automatic dark theme for web pages
- **Privacy Dashboard**: Statistics on blocked content
- **Enhanced Ad Blocking**: Category-based content filtering
- **Performance Improvements**: Faster page loading times

### Planned for v1.2

- **Tab Management API**: Support for multiple tabs in a single view
- **Reading Mode**: Clean, distraction-free reading experience
- **Full-page Screenshots**: Capture entire pages programmatically
- **Custom Filter Lists**: Import/export blocking rules

### Long-term Goals

- **Script Injection API**: Easily inject custom scripts into pages
- **Form Autofill**: Securely store and fill form data
- **Advanced User Agent Controls**: Detailed browser fingerprinting protection
- **Extension System**: Plugin architecture for added functionality

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the Level Pixel License - see the [LICENSE](LICENSE) file for details.

**Important License Terms:**

1. You are free to use and modify this software in your projects
2. Redistribution requires attribution to Level Pixel as the original creator
3. If you make improvements, we appreciate (but don't require) sharing them back

## 📞 Support

For support, please open an issue in the repository or contact us at support@levelpixel.com

---

⭐ Don't forget to star the repo if you find it useful!

## 📸 Screenshots

<p align="center">
  <img src="images/image1.jpg" alt="Main Activity" width="280">
  <img src="images/image2.jpg" alt="Browser Activity" width="280">
</p>

<p align="center">
  <img src="images/image3.jpg" alt="Settings Screen" width="280">
  <img src="images/image4.jpg" alt="Ad Blocking Demo" width="280">
</p>
