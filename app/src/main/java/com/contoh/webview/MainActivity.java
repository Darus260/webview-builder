package com.contoh.webview;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    
    private String targetUrl = "https://google.com";
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);

        // --- JURUS PAMUNGKAS: Bersihkan sisa cache lama setiap aplikasi baru dibuka ---
        webView.clearCache(true);
        
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        
        // --- PAKSA AMBIL DATA BARU, JANGAN PAKAI CACHE LAMA ---
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        
        // --- KONFIGURASI PENYIMPANAN UNIVERSAL MAKSIMAL ---
        webSettings.setDomStorageEnabled(true); // Wajib untuk localStorage Frontend (Sesi Login Aman)
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setGeolocationEnabled(true);
        
        // Memaksa WebView mengizinkan pertukaran data lintas domain (CORS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webSettings.setAllowUniversalAccessFromFileURLs(true);
            webSettings.setAllowFileAccessFromFileURLs(true);
        }
        
        // Mengizinkan konten campuran (HTTP dan HTTPS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        
        // Memaksa sistem menerima semua jenis Cookie
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        // --- IZIN DINAMIS KETERANGAN KAMERA & LOKASI ---
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                    }
                }
                callback.invoke(origin, true, false);
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 2);
                    }
                }
                runOnUiThread(() -> request.grant(request.getResources()));
            }
        });

        // --- PENYIMPANAN SESI INSTAN & INJEKSI SCRIPT UNTUK HIDE BANNER GAS ---
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                CookieManager.getInstance().flush(); 

                // Menyuntikkan JavaScript untuk menghilangkan banner peringatan Google Apps Script
                view.evaluateJavascript(
                    "(function() { " +
                    "   var style = document.createElement('style'); " +
                    "   style.innerHTML = '#warning-bar, .warning-bar { display: none !important; }'; " +
                    "   document.head.appendChild(style); " +
                    "   var divs = document.getElementsByTagName('div');" +
                    "   for (var i = 0; i < divs.length; i++) { " +
                    "       var text = divs[i].innerText || divs[i].textContent;" +
                    "       if (text.includes('Aplikasi ini dibuat oleh pengguna Google Apps Script') || " +
                    "           text.includes('This application was created by another user')) { " +
                    "           divs[i].style.display = 'none'; " +
                    "       } " +
                    "   }" +
                    "})()", null);
            }
        });

        webView.loadUrl(targetUrl);
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CookieManager.getInstance().flush();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
