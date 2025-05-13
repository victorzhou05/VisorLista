package com.acutecoder.pdf

import android.webkit.WebView
import com.acutecoder.pdf.js.callDirectly
import com.acutecoder.pdf.js.invoke
import com.acutecoder.pdf.js.toJsString

class FindController internal constructor(private val webView: WebView) {

    var highlightAll: Boolean = true
        set(value) {
            field = value
            webView callDirectly "setFindHighlightAll"(value)
        }

    var matchCase: Boolean = false
        set(value) {
            field = value
            webView callDirectly "setFindMatchCase"(value)
        }

    var entireWord: Boolean = false
        set(value) {
            field = value
            webView callDirectly "setFindEntireWord"(value)
        }

    var matchDiacritics: Boolean = false
        set(value) {
            field = value
            webView callDirectly "setFindMatchDiacritics"(value)
        }

    fun startFind(searchTerm: String) {
        webView callDirectly "startFind"(searchTerm.toJsString())
    }

    fun stopFind() {
        webView callDirectly "stopFind"()
    }

    fun findNext() {
        webView callDirectly "findNext"()
    }

    fun findPrevious() {
        webView callDirectly "findPrevious"()
    }

}
