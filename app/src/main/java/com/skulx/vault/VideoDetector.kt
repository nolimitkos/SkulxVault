package com.skulx.vault

import android.webkit.JavascriptInterface
import org.json.JSONArray
import org.json.JSONObject

class VideoDetector(private val onVideoFound: (String, String) -> Unit) {
    @JavascriptInterface
    fun detectVideo(url: String, title: String) {
        if (url.isNotBlank() && (url.contains(".mp4") || url.contains(".m3u8") || url.contains(".webm"))) {
            onVideoFound(url, title.ifBlank { "Video" })
        }
    }

    companion object {
        fun getInjectionScript(): String {
            return """
                (function() {
                    function extractVideos() {
                        var videos = document.querySelectorAll('video');
                        var sources = [];
                        videos.forEach(function(v) {
                            if (v.src) sources.push({url: v.src, title: document.title});
                            v.querySelectorAll('source').forEach(function(s) {
                                if (s.src) sources.push({url: s.src, title: document.title});
                            });
                        });
                        var iframes = document.querySelectorAll('iframe');
                        iframes.forEach(function(f) {
                            try {
                                var fv = f.contentDocument.querySelectorAll('video');
                                fv.forEach(function(v) {
                                    if (v.src) sources.push({url: v.src, title: document.title});
                                });
                            } catch(e) {}
                        });
                        sources.forEach(function(s) {
                            if (window.SkulxInterface) {
                                window.SkulxInterface.detectVideo(s.url, s.title);
                            }
                        });
                    }
                    setInterval(extractVideos, 2000);
                    extractVideos();
                })();
            """.trimIndent()
        }
    }
}
