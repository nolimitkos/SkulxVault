package com.skulx.vault

import android.webkit.JavascriptInterface

class VideoDetector(private val onVideoFound: (String, String) -> Unit) {
    @JavascriptInterface
    fun detectVideo(url: String, title: String) {
        if (url.isNotBlank()) {
            onVideoFound(url, title.ifBlank { "Video" })
        }
    }

    @JavascriptInterface
    fun detectM3U8(url: String, title: String) {
        if (url.isNotBlank() && (url.contains(".m3u8") || url.contains(".mp4") || url.contains(".webm") || url.contains(".ts"))) {
            onVideoFound(url, title.ifBlank { "HLS Stream" })
        }
    }

    companion object {
        fun getInjectionScript(): String {
            return """
(function(){
var reported=[];
function report(u,t){
if(!u||reported.includes(u))return;
reported.push(u);
if(window.SkulxInterface)window.SkulxInterface.detectVideo(u,t||document.title);
}
function scan(){
var vids=document.querySelectorAll('video');
vids.forEach(function(v){
if(v.src)report(v.src,document.title);
v.querySelectorAll('source').forEach(function(s){if(s.src)report(s.src,document.title);});
});
var ifr=document.querySelectorAll('iframe');
ifr.forEach(function(f){
try{
var fv=f.contentDocument.querySelectorAll('video');
fv.forEach(function(v){if(v.src)report(v.src,document.title);});
}catch(e){}
});
var as=document.querySelectorAll('a[href]');
as.forEach(function(a){
var h=a.href;
if(h&&(h.includes('.mp4')||h.includes('.m3u8')||h.includes('.webm')))report(h,a.innerText||document.title);
});
}
setInterval(scan,1500);
scan();
})();
            """.trimIndent()
        }

        fun getNetworkHookScript(): String {
            return """
(function(){
var origOpen=XMLHttpRequest.prototype.open;
XMLHttpRequest.prototype.open=function(method,url){
if(url&&(url.includes('.m3u8')||url.includes('.mp4')||url.includes('.ts')||url.includes('.webm'))){
if(window.SkulxInterface)window.SkulxInterface.detectM3U8(url,document.title);
}
return origOpen.apply(this,arguments);
};
var origFetch=window.fetch;
window.fetch=function(input,init){
var url=(typeof input==='string')?input:input.url;
if(url&&(url.includes('.m3u8')||url.includes('.mp4')||url.includes('.ts')||url.includes('.webm'))){
if(window.SkulxInterface)window.SkulxInterface.detectM3U8(url,document.title);
}
return origFetch.apply(this,arguments);
};
})();
            """.trimIndent()
        }

        fun getMediaHookScript(): String {
            return """
(function(){
var origSrc=Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype,'src');
Object.defineProperty(HTMLMediaElement.prototype,'src',{
set:function(value){
if(value&&(value.includes('.m3u8')||value.includes('.mp4')||value.includes('.webm')||value.includes('blob:'))){
if(window.SkulxInterface)window.SkulxInterface.detectVideo(value,document.title);
}
if(origSrc&&origSrc.set)return origSrc.set.call(this,value);
this.setAttribute('src',value);
},
get:function(){
if(origSrc&&origSrc.get)return origSrc.get.call(this);
return this.getAttribute('src');
}
});
var origPlay=HTMLMediaElement.prototype.play;
HTMLMediaElement.prototype.play=function(){
var s=this.src||this.currentSrc;
if(s&&window.SkulxInterface)window.SkulxInterface.detectVideo(s,document.title);
return origPlay.apply(this,arguments);
};
})();
            """.trimIndent()
        }
    }
}
