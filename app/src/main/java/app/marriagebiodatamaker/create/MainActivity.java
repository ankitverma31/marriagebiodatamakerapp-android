package app.marriagebiodatamaker.create;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {  

    private WebView webView;  
    private ProgressBar progressBar;

    @SuppressLint("SetJavaScriptEnabled")  
    @Override  
    protected void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);  
        EdgeToEdge.enable(this);  
        setContentView(R.layout.activity_main);  

        // Set the status bar color  
        Window window = getWindow();  
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);  
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));  

        // Initialize ProgressBar and WebView  
        progressBar = findViewById(R.id.progressBar);  
        webView = findViewById(R.id.web);  

        // Configure WebView  
        webView.loadUrl("https://create.marriagebiodatamaker.app");  
        webView.getSettings().setJavaScriptEnabled(true);  
        webView.getSettings().setDomStorageEnabled(true);  

        // Custom WebViewClient to handle loading states  
        webView.setWebViewClient(new WebViewClient() {  
            @Override  
            public void onPageStarted(WebView view, String url, Bitmap favicon) {  
                super.onPageStarted(view, url, favicon);  
                progressBar.setVisibility(View.VISIBLE);  
            }  

            @Override  
            public void onPageFinished(WebView view, String url) {  
                super.onPageFinished(view, url);  
                progressBar.setVisibility(View.GONE);  
            }  

            @Override  
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {  
                super.onReceivedError(view, request, error);  
                progressBar.setVisibility(View.GONE);  
                Toast.makeText(MainActivity.this, "Error loading page", Toast.LENGTH_SHORT).show();  
            }  
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());  
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);  
            return insets;  
        });

        // Handle back button behavior
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            private long backPressedTime = 0;  // Variable to store time of back press
            private Toast backToast;  // Variable for showing toast message

            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    // Check if back button is pressed within 2 seconds
                    if (backPressedTime + 2000 > System.currentTimeMillis()) {
                        // If pressed within 2 seconds, finish the activity
                        finish();
                    } else {
                        // If pressed for the first time, show a toast message
                        backToast = Toast.makeText(getApplicationContext(), "Press back again to exit", Toast.LENGTH_SHORT);
                        backToast.show();
                        backPressedTime = System.currentTimeMillis();
                    }
                }
            }
        });
    }
}  