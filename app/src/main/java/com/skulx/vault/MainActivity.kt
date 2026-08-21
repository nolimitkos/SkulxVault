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
import java.net.URL

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var urlBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var goButton: ImageButton
    private lateinit var menuButton: ImageButton
    private lateinit var fullscreenFab: FloatingActionButton
    private val detectedVideos = mutableMapOf<String, String>()
    private var isFullscreen = false

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
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.0 Chrome/120.0.0.0 Mobile Safari/537.0"
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
                view?.evaluateJavascript(VideoDetector.getInjectionScript(), null)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                request?.url?.toString()?.let { webView.loadUrl(it) }
                return true
            }
        }

        val detector = VideoDetector { url, title ->
            runOnUiThread {
                if (!detectedVideos.containsKey(url)) {
                    detectedVideos[url] = title
                    Toast.makeText(this, "Video detected: $title", Toast.LENGTH_SHORT).show()
                }
            }
        }
        webView.addJavascriptInterface(detector, "SkulxInterface")

        goButton.setOnClickListener {
            var url = urlBar.text.toString()
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

    private fun toggleFullscreen() {
        if (isFullscreen) {
            webView.evaluateJavascript(
                "document.exitFullscreen && document.exitFullscreen();",
                null
            )
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            webView.evaluateJavascript(
                "document.documentElement.requestFullscreen && document.documentElement.requestFullscreen();",
                null
            )
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

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
            detectedVideos.values.toList())
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val url = detectedVideos.keys.toList()[position]
            val title = detectedVideos.values.toList()[position]
            AlertDialog.Builder(this)
                .setTitle("Download Video?")
                .setMessage(title)
                .setPositiveButton("Download") { _, _ ->
                    startDownload(url, title)
                    dialog.dismiss()
                }
                .setNegativeButton("Copy Link") { _, _ ->
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Skulx", url))
                    Toast.makeText(this, "Link copied", Toast.LENGTH_SHORT).show()
                }
                .setNeutralButton("Cancel", null)
                .show()
        }

        refreshBtn.setOnClickListener {
            webView.evaluateJavascript(VideoDetector.getInjectionScript(), null)
            Toast.makeText(this, "Scanning for videos...", Toast.LENGTH_SHORT).show()
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
