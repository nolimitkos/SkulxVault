package com.skulx.vault

import android.webkit.JavascriptInterface

class VideoDetector(private val onVideoFound: (String, String) -> Unit) {
    private val reported = mutableSetOf<String>()

    @JavascriptInterface
    fun detectVideo(url: String, title: String) {
        if (url.isBlank()) return
        synchronized(reported) {
            if (url in reported) return
            reported.add(url)
        }
        onVideoFound(url, title.ifBlank { "Video" })
    }

    companion object {
        fun getMasterScript(): String {
            return """
(function(){
var SV=window.SkulxInterface;
if(!SV)return;
var done=new Set();
function send(u,t){
if(!u||done.has(u))return;
done.add(u);
SV.detectVideo(u,t||document.title);
}
function isVid(u){
return u&&(u.includes('.mp4')||u.includes('.m3u8')||u.includes('.webm')||u.includes('.ts')||u.includes('.m4s')||u.includes('.mpd')||u.includes('.mov')||u.includes('.mkv')||u.includes('.flv')||u.includes('.avi')||u.startsWith('blob:'));
}
function scan(){
var all=document.querySelectorAll('video');
for(var i=0;i<all.length;i++){
var v=all[i];
if(v.src&&isVid(v.src))send(v.src,document.title);
var src=v.querySelectorAll('source');
for(var j=0;j<src.length;j++)if(src[j].src&&isVid(src[j].src))send(src[j].src,document.title);
if(v.currentSrc&&isVid(v.currentSrc))send(v.currentSrc,document.title);
}
var ifr=document.querySelectorAll('iframe');
for(var k=0;k<ifr.length;k++){
try{
var d=ifr[k].contentDocument;
if(!d)continue;
var fv=d.querySelectorAll('video');
for(var x=0;x<fv.length;x++){
if(fv[x].src&&isVid(fv[x].src))send(fv[x].src,document.title);
if(fv[x].currentSrc&&isVid(fv[x].currentSrc))send(fv[x].currentSrc,document.title);
}
}catch(e){}
}
}
var origX=XMLHttpRequest.prototype.open;
XMLHttpRequest.prototype.open=function(m,u){
if(isVid(u))send(u,document.title);
return origX.apply(this,arguments);
};
var origF=window.fetch;
window.fetch=function(i,init){
var u=(typeof i==='string')?i:(i&&i.url?i.url:'');
if(isVid(u))send(u,document.title);
return origF.apply(this,arguments);
};
var origU=URL.createObjectURL;
if(origU){
URL.createObjectURL=function(obj){
var r=origU.call(this,obj);
if(r&&r.startsWith('blob:'))send(r,document.title);
return r;
};
}
var origRev=URL.revokeObjectURL;
if(origRev){
URL.revokeObjectURL=function(u){
if(u&&u.startsWith('blob:'))send(u,document.title);
return origRev.call(this,u);
};
}
var origSrc=Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype,'src');
if(origSrc){
Object.defineProperty(HTMLMediaElement.prototype,'src',{
set:function(v){
if(isVid(v))send(v,document.title);
if(origSrc.set)origSrc.set.call(this,v);else this.setAttribute('src',v);
},
get:function(){return origSrc.get?origSrc.get.call(this):this.getAttribute('src');}
});
}
var origPlay=HTMLMediaElement.prototype.play;
HTMLMediaElement.prototype.play=function(){
var s=this.src||this.currentSrc;
if(isVid(s))send(s,document.title);
return origPlay.apply(this,arguments);
};
if(window.MediaSource){
var origAdd=window.MediaSource.prototype.addSourceBuffer;
window.MediaSource.prototype.addSourceBuffer=function(mt){
var r=origAdd.call(this,mt);
send('blob:'+this.url,document.title);
return r;
};
}
setInterval(scan,1000);
scan();
})();
            """.trimIndent()
        }
    }
}
