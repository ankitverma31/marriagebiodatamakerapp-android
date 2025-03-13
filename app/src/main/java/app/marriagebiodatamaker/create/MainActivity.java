package app.marriagebiodatamaker.create;
import android.content.Context;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
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
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private boolean isNeedToDownload = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        progressBar = findViewById(R.id.progressBar);
        webView = findViewById(R.id.web);

        webView.loadUrl("https://create.marriagebiodatamaker.app?webview=true");
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
//        webView.getSettings().setAllowFileAccess(true);
//        webView.getSettings().setAllowContentAccess(true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                Log.d("WebViewConsole", consoleMessage.message() + " -- From line "
                        + consoleMessage.lineNumber() + " of "
                        + consoleMessage.sourceId());
                return super.onConsoleMessage(consoleMessage);
            }
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                // Implement file chooser if needed
                return super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
            }
        });

        webView.addJavascriptInterface(new BlobDownloader(), "BlobDownloader");

        webView.addJavascriptInterface(new WebAppInterface(this), "AndroidShare");

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
//                Toast.makeText(MainActivity.this, "Error loading page", Toast.LENGTH_SHORT).show();
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setTitle("MarriageBiodata.pdf");
                request.setDescription("Downloading file...");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MarriageBiodata.pdf");
                downloadManager.enqueue(request);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            private long backPressedTime = 0;
            private Toast backToast;

            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    if (backPressedTime + 2000 > System.currentTimeMillis()) {
                        finish();
                    } else {
                        backToast = Toast.makeText(getApplicationContext(), "Press back again to exit", Toast.LENGTH_SHORT);
                        backToast.show();
                        backPressedTime = System.currentTimeMillis();
                    }
                }
            }
        });
    }

    private class BlobDownloader {
        @JavascriptInterface
        public void downloadBlob(String base64Data, String fileName) {
            if (isNeedToDownload) {
                isNeedToDownload = false;
                Log.d("BlobDownloader", "downloadBlob invoked with filename: " + fileName);
                try {
                    byte[] data = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs();
                    }
                    File file = new File(downloadsDir, fileName);
                    int counter = 1;
                    String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName;
                    String extension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")) : "";
                    while (file.exists()) {
                        file = new File(downloadsDir, baseName + "_" + counter + extension);
                        counter++;
                    }
                    OutputStream out = new FileOutputStream(file);
                    out.write(data);
                    out.close();
                    Toast.makeText(MainActivity.this, "File downloaded: " + file.getName(), Toast.LENGTH_LONG).show();
                    Log.d("BlobDownloader", "File saved as: " + file.getAbsolutePath());
                } catch (Exception e) {
                    Log.e("BlobDownloader", "Error saving file", e);
                }
            }
        }
    }

    public class WebAppInterface {
        private Context mContext;

        public WebAppInterface(Context context) {
            this.mContext = context;
        }

        @JavascriptInterface
        public void shareBiodata(String downloadUrl) {
            new Thread(() -> {
                try {
                    // Step 1: Download the file
                    URL url = new URL(downloadUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        return;
                    }

                    InputStream inputStream = connection.getInputStream();
                    File file = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "MarriageBiodata.pdf");
                    FileOutputStream outputStream = new FileOutputStream(file);

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    outputStream.close();
                    inputStream.close();

                    // Step 2: Get content URI for sharing
                    Uri contentUri = FileProvider.getUriForFile(mContext, "app.marriagebiodatamaker.create.fileprovider", file);

                    // Step 3: Share the PDF file
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("application/pdf");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    Intent chooser = Intent.createChooser(shareIntent, "Share Marriage Biodata");
                    mContext.startActivity(chooser);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}