package com.skulx.vault

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var urlBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var goButton: ImageButton
    private lateinit var menuButton: ImageButton
    private lateinit var fullscreenFab: FloatingActionButton
    private val detectedVideos = LinkedHashMap<String, String>()
    private var isFullscreen = false

    private val adList = listOf(
        "googleads","googlesyndication","doubleclick","adsystem","amazon-adsystem",
        "facebook.com/tr","googletagmanager","google-analytics","scorecardresearch",
        "outbrain","taboola","adsrvr","bounceexchange","quantserve","moatads",
        "adsafeprotected","btloader","adnxs","rubiconproject","openx.net",
        "pubmatic","casalemedia","advertising","analytics","tracking",
        "telemetry","metrics","log.","logger.","pixel.","beacon.",
        "cdn.ads","pagead","adserver","adservice","adform","adroll",
        "criteo","mediavine","adsymptotic","adspeed","adzerk","buysellads",
        "revcontent","popads","onclickads","adsterra","propellerads",
        "exoclick","juicyads","eroadvertising","trafficfactory","yllix",
        "ad.plus","ezoic","infolinks","bidvertiser","adblade",
        "sponsored","affiliate","promo.","campaign.","ads.","ad.",
        "banner","popunder","interstitial","prebid","header-bidding",
        "googletagservices","facebook.net","connect.facebook.net",
        "twitter.com/i/ads","linkedin.com/ads","pinterest.com/ads",
        "redditstatic.com/ads","tiktok.com/ads","snapchat.com/ads",
        "youtube.com/pagead","youtube.com/api/stats/ads"
    )

    private val vidExts = listOf(".mp4",".m3u8",".webm",".ts",".m4s",".mpd",".mov",".mkv",".flv",".avi",".wmv")

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        urlBar = findViewById(R.id.urlBar)
        progressBar = findViewById(R.id.progressBar)
        goButton = findViewById(R.id.goButton)
        menuButton = findViewById(R.id.menuButton)
        fullscreenFab = findViewById(R.id.fullscreenFab)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
            cacheMode = WebSettings.LOAD_DEFAULT
            loadsImagesAutomatically = true
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress == 100) View.GONE else View.VISIBLE
            }
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                isFullscreen = true
                fullscreenFab.hide()
            }
            override fun onHideCustomView() {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                isFullscreen = false
                fullscreenFab.show()
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                urlBar.setText(url)
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                injectDetector()
            }
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                request?.url?.toString()?.let { webView.loadUrl(it) }
                return true
            }
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
                val low = url.lowercase()

                if (adList.any { low.contains(it) }) {
                    return WebResourceResponse("text/plain", "UTF-8", null)
                }

                if (vidExts.any { low.contains(it) } || request.url.toString().startsWith("blob:")) {
                    val mime = request.requestHeaders?.get("Accept") ?: ""
                    if (mime.contains("video") || mime.contains("application") || vidExts.any { low.contains(it) } || url.startsWith("blob:")) {
                        val title = webView.title ?: "Video"
                        runOnUiThread {
                            if (!detectedVideos.containsKey(url)) {
                                detectedVideos[url] = title
                                Toast.makeText(this@MainActivity, "Video detected", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        val detector = VideoDetector { url, title ->
            runOnUiThread {
                if (!detectedVideos.containsKey(url)) {
                    detectedVideos[url] = title
                    Toast.makeText(this, "Detected: ${title.take(30)}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        webView.addJavascriptInterface(detector, "SkulxInterface")

        goButton.setOnClickListener {
            var url = urlBar.text.toString().trim()
            if (url.isEmpty()) return@setOnClickListener
            if (!url.startsWith("http")) url = "https://$url"
            webView.loadUrl(url)
            urlBar.clearFocus()
        }

        urlBar.setOnEditorActionListener { _, _, _ ->
            goButton.performClick()
            true
        }

        menuButton.setOnClickListener { showMenu() }
        fullscreenFab.setOnClickListener { toggleFullscreen() }

        webView.loadUrl("https://www.google.com")
    }

    private fun injectDetector() {
        webView.evaluateJavascript(VideoDetector.getMasterScript(), null)
    }

    private fun toggleFullscreen() {
        if (isFullscreen) {
            webView.evaluateJavascript("document.exitFullscreen&&document.exitFullscreen();", null)
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            webView.evaluateJavascript("document.documentElement.requestFullscreen&&document.documentElement.requestFullscreen();", null)
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        isFullscreen = !isFullscreen
    }

    private fun showMenu() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.item_download, null)
        dialog.setContentView(view)

        val listView = view.findViewById<ListView>(R.id.videoList)
        val refreshBtn = view.findViewById<Button>(R.id.refreshBtn)
        val backBtn = view.findViewById<Button>(R.id.backBtn)
        val forwardBtn = view.findViewById<Button>(R.id.forwardBtn)
        val reloadBtn = view.findViewById<Button>(R.id.reloadBtn)

        val items = detectedVideos.entries.map { "${it.value.take(40)} | ${it.key.take(50)}..." }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val url = detectedVideos.keys.toList()[position]
            val title = detectedVideos.values.toList()[position]
            AlertDialog.Builder(this)
                .setTitle("Download?")
                .setMessage(title.take(60))
                .setPositiveButton("Download") { _, _ ->
                    startDownload(url, title)
                    dialog.dismiss()
                }
                .setNegativeButton("Copy") { _, _ ->
                    val cb = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cb.setPrimaryClip(android.content.ClipData.newPlainText("Skulx", url))
                    Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
                }
                .setNeutralButton("Cancel", null)
                .show()
        }

        refreshBtn.setOnClickListener {
            injectDetector()
            Toast.makeText(this, "Scanning page...", Toast.LENGTH_SHORT).show()
        }

        backBtn.setOnClickListener { if (webView.canGoBack()) webView.goBack() }
        forwardBtn.setOnClickListener { if (webView.canGoForward()) webView.goForward() }
        reloadBtn.setOnClickListener { webView.reload() }

        dialog.show()
    }

    private fun startDownload(url: String, title: String) {
        val intent = Intent(this, DownloadService::class.java).apply {
            putExtra("url", url)
            putExtra("title", title)
        }
        ContextCompat.startForegroundService(this, intent)
        Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}
