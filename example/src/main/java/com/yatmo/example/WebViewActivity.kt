package com.yatmo.example

import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.yatmo.sdk.Country
import com.yatmo.sdk.Language
import com.yatmo.sdk.Yatmo
import com.yatmo.sdk.YatmoConfiguration
import com.yatmo.sdk.YatmoWebView
import com.yatmo.sdk.YatmoWebViewOptions

/** Test of the WebView wrapper: the iframe plugin in map-top mode, page load and HTTP errors logged. */
class WebViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val yatmo = Yatmo(this, YatmoConfiguration(BuildConfig.YATMO_LICENSE_KEY, Country.BE, Language.FR))
        val web = YatmoWebView(this)
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                Log.i("YatmoTest", "webview page finished: ${url.substringBefore('?')}")
            }

            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                Log.i("YatmoTest", "webview HTTP ${errorResponse.statusCode} on ${request.url.toString().substringBefore('?')}")
            }
        }
        setContentView(web)
        web.load(yatmo, YatmoWebViewOptions(latitude = 50.8520525, longitude = 4.3442926, mode = "map-top", zoom = 15, accentColor = "#428BFF"))
        Log.i("YatmoTest", "webview url: " + YatmoWebViewOptions(50.8520525, 4.3442926, mode = "map-top", zoom = 15, accentColor = "#428BFF").url(yatmo.configuration).substringBefore("?"))
    }
}
