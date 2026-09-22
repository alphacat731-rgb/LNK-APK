package com.websitetodapk.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.List;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private FrameLayout root;
    private LinearLayout launcher;
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private final ArrayList<Editor> editors = new ArrayList<>();
    private int currentEditor = -1;
    private int bgColor = Color.rgb(17,17,17);
    private int cardColor = Color.rgb(32,32,32);
    private int textColor = Color.WHITE;
    private final android.content.SharedPreferences prefs = null;
    private File galleryBase;
    private ArrayList<File> allGalleryFiles = new ArrayList<>();
    private String galleryFilter = "all";

    private static class Editor {
        String name;
        String url;
        Editor(String name, String url) { this.name = name; this.url = url; }
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        loadLauncherConfig();
        buildRoot();
        showLauncher();
        hideSystemBars();
    }

    private void buildRoot() {
        root = new FrameLayout(this);
        setContentView(root);
        launcher = new LinearLayout(this);
        launcher.setOrientation(LinearLayout.VERTICAL);
        launcher.setPadding(dp(20), dp(28), dp(20), dp(20));
        root.addView(launcher, new FrameLayout.LayoutParams(-1, -1));
    }

    private void loadLauncherConfig() {
        try {
            String json = readAsset("apps.json");
            JSONObject cfg = new JSONObject(json);
            bgColor = Color.parseColor(cfg.optString("background", "#111111"));
            cardColor = Color.parseColor(cfg.optString("card", "#202020"));
            textColor = Color.parseColor(cfg.optString("text", "#FFFFFF"));
            JSONArray a = cfg.getJSONArray("apps");
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                editors.add(new Editor(o.getString("name"), o.getString("url")));
            }
        } catch (Exception e) {
            editors.clear();
            editors.add(new Editor("Editor 1", "https://bloub.vercel.app/"));
            editors.add(new Editor("Editor 2", "https://avatars.bible-strong.app/"));
            editors.add(new Editor("Editor 3", "https://grokbots.ai/studio"));
        }
    }

    private void showLauncher() {
        currentEditor = -1;
        if (webView != null) {
            webView.stopLoading();
            webView.setVisibility(View.GONE);
        }
        launcher.setVisibility(View.VISIBLE);
        launcher.setBackgroundColor(bgColor);
        launcher.removeAllViews();

        TextView title = new TextView(this);
        title.setText(getString(com.websitetodapk.app.R.string.app_name));
        title.setTextColor(textColor);
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        launcher.addView(title, new LinearLayout.LayoutParams(-1, dp(64)));

        TextView subtitle = new TextView(this);
        subtitle.setText("Create, edit and keep your avatars");
        subtitle.setTextColor(withAlpha(textColor, 170));
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);
        launcher.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(40)));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        grid.setUseDefaultMargins(false);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, 0, 1f);
        gp.topMargin = dp(12);
        launcher.addView(grid, gp);

        for (int i = 0; i < editors.size(); i++) {
            final int index = i;
            addCard(grid, editors.get(i).name, i, v -> openEditor(index));
        }
        addCard(grid, "Avatar Hub", editors.size(), v -> showGallery());

        addRecentSection();

        Button settings = new Button(this);
        settings.setText("⚙ Settings & About");
        settings.setOnClickListener(v -> showSettings());
        launcher.addView(settings, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView hint = new TextView(this);
        hint.setText("Your downloaded avatars appear in Avatar Hub automatically.");
        hint.setTextColor(withAlpha(textColor, 130));
        hint.setTextSize(12);
        hint.setGravity(Gravity.CENTER);
        launcher.addView(hint, new LinearLayout.LayoutParams(-1, dp(44)));
    }

    private void addCard(GridLayout grid, String label, int index, View.OnClickListener click) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(cardColor);
        bg.setCornerRadius(dp(22));
        card.setBackground(bg);
        card.setOnClickListener(click);

        TextView badge = new TextView(this);
        badge.setText(index < editors.size() ? String.valueOf(index + 1) : "★");
        badge.setTextColor(textColor);
        badge.setTextSize(22);
        badge.setGravity(Gravity.CENTER);
        badge.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(badge, new LinearLayout.LayoutParams(-1, dp(52)));

        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(textColor);
        t.setTextSize(17);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(t, new LinearLayout.LayoutParams(-1, dp(54)));

        GridLayout.LayoutParams cp = new GridLayout.LayoutParams();
        cp.width = 0;
        cp.height = dp(150);
        cp.columnSpec = GridLayout.spec(index % 2, 1f);
        cp.rowSpec = GridLayout.spec(index / 2);
        cp.setMargins(dp(7), dp(7), dp(7), dp(7));
        grid.addView(card, cp);
    }

    private void addRecentSection() {
        File base = getExternalMediaDirs().length > 0 ? getExternalMediaDirs()[0] : getExternalFilesDir(null);
        ArrayList<File> recent = new ArrayList<>();
        if (base != null) collectMedia(new File(base, "GrokBot Avatars"), recent);
        Collections.sort(recent, (a,b) -> Long.compare(b.lastModified(), a.lastModified()));
        if (recent.isEmpty()) return;

        TextView heading = new TextView(this);
        heading.setText("Recent creations");
        heading.setTextColor(textColor);
        heading.setTextSize(18);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        launcher.addView(heading, new LinearLayout.LayoutParams(-1, dp(38)));

        HorizontalScrollView hs = new HorizontalScrollView(this);
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, dp(4), 0, dp(8));
        for (int i = 0; i < Math.min(5, recent.size()); i++) {
            final File file = recent.get(i);
            ImageView p = new ImageView(this);
            p.setScaleType(ImageView.ScaleType.CENTER_CROP);
            String n = file.getName().toLowerCase(Locale.US);
            if (n.endsWith(".mp4") || n.endsWith(".webm")) p.setImageResource(android.R.drawable.ic_media_play);
            else p.setImageDrawable(android.graphics.drawable.Drawable.createFromPath(file.getAbsolutePath()));
            p.setBackgroundColor(cardColor);
            p.setOnClickListener(v -> openMedia(file));
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(dp(86), dp(86));
            pp.setMargins(0, 0, dp(8), 0);
            row.addView(p, pp);
        }
        hs.addView(row);
        launcher.addView(hs, new LinearLayout.LayoutParams(-1, dp(100)));
    }

    private void showSettings() {
        launcher.setVisibility(View.GONE);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(24), dp(20), dp(20));
        page.setBackgroundColor(bgColor);

        TextView title = new TextView(this);
        title.setText("Settings");
        title.setTextColor(textColor);
        title.setTextSize(28);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        page.addView(title, new LinearLayout.LayoutParams(-1, dp(60)));

        TextView about = new TextView(this);
        about.setText("GrokBot Avatar Hub\n\nCreate, download and organize your avatars from multiple editors in one place.\n\nVersion " + getString(com.websitetodapk.app.R.string.app_name));
        about.setTextColor(withAlpha(textColor, 210));
        about.setTextSize(15);
        page.addView(about, new LinearLayout.LayoutParams(-1, dp(150)));

        Button hub = new Button(this);
        hub.setText("Open Avatar Hub");
        hub.setOnClickListener(v -> { root.removeView(page); showGallery(); });
        page.addView(hub, new LinearLayout.LayoutParams(-1, dp(52)));

        Button clear = new Button(this);
        clear.setText("Clear WebView cache");
        clear.setOnClickListener(v -> {
            if (webView != null) webView.clearCache(true);
            Toast.makeText(this, "WebView cache cleared", Toast.LENGTH_SHORT).show();
        });
        page.addView(clear, new LinearLayout.LayoutParams(-1, dp(52)));

        Button back = new Button(this);
        back.setText("Back to Home");
        back.setOnClickListener(v -> { root.removeView(page); showLauncher(); });
        page.addView(back, new LinearLayout.LayoutParams(-1, dp(52)));

        root.addView(page, new FrameLayout.LayoutParams(-1, -1));
        root.setTag(page);
        hideSystemBars();
    }

    private void openEditor(int index) {
        currentEditor = index;
        launcher.setVisibility(View.GONE);
        setupWebView();
        webView.setVisibility(View.VISIBLE);
        webView.loadUrl(editors.get(index).url);
        hideSystemBars();
    }

    private void setupWebView() {
        if (webView != null) {
            root.removeView(webView);
        }
        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        webView.setVisibility(View.VISIBLE);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(false);
        s.setUseWideViewPort(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setAllowFileAccess(true);

        addJavascriptBridge();

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) ->
                downloadUrl(url, userAgent, contentDisposition, mimeType));

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return false;
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                injectBlobDownloadBridge(v);
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                if (view == webView) {
                    root.removeView(view);
                    view.setWebChromeClient(null);
                    view.setWebViewClient(null);
                    view.destroy();
                    webView = null;
                    if (currentEditor >= 0) {
                        setupWebView();
                        webView.loadUrl(editors.get(currentEditor).url);
                    }
                    hideSystemBars();
                }
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,
                        params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
                try {
                    startActivityForResult(Intent.createChooser(intent, "Choose file"), FILE_CHOOSER_REQUEST);
                    return true;
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
            }
        });
    }

    private void injectBlobDownloadBridge(WebView v) {
        String js = "(function(){" +
                "if(window.__grokDownloadHook)return;" +
                "window.__grokDownloadHook=true;" +
                "function saveUrl(url,name){" +
                "if(!url||(!url.startsWith('blob:')&&!url.startsWith('data:')))return false;" +
                "if(url.startsWith('data:')){AndroidDownload.save(url,name||'avatar','');return true;}" +
                "fetch(url).then(function(r){return r.blob()}).then(function(b){" +
                "var fr=new FileReader();" +
                "fr.onloadend=function(){AndroidDownload.save(fr.result,name||'avatar',b.type||'application/octet-stream')};" +
                "fr.readAsDataURL(b);" +
                "}).catch(function(){});" +
                "return true;" +
                "}" +
                "document.addEventListener('click',function(e){" +
                "var a=e.target.closest&&e.target.closest('a');" +
                "if(!a)return;" +
                "if(saveUrl(a.href,a.download))e.preventDefault();" +
                "},true);" +
                "var oldClick=HTMLAnchorElement.prototype.click;" +
                "HTMLAnchorElement.prototype.click=function(){" +
                "if(saveUrl(this.href,this.download))return;" +
                "return oldClick.apply(this,arguments);" +
                "};" +
                "var oldOpen=window.open;" +
                "window.open=function(url,target,features){" +
                "if(typeof url==='string'&&saveUrl(url,'avatar'))return null;" +
                "return oldOpen.apply(window,arguments);" +
                "};" +
                "})();";
        v.evaluateJavascript(js, null);
    }

    @android.webkit.JavascriptInterface
    public void saveBlob(String dataUrl, String fileName, String mimeType) {
        try {
            if (dataUrl == null || !dataUrl.startsWith("data:")) return;
            final byte[] bytes = android.util.Base64.decode(dataUrl.substring(dataUrl.indexOf(',') + 1), android.util.Base64.DEFAULT);
            saveBytes(bytes, safeFileName(fileName), mimeType);
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this, "Could not save the avatar", Toast.LENGTH_SHORT).show());
        }
    }

    private void addJavascriptBridge() {
        if (webView != null) webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void save(String dataUrl, String fileName, String mimeType) {
                saveBlob(dataUrl, fileName, mimeType);
            }
        }, "AndroidDownload");
    }

    private void downloadUrl(String url, String userAgent, String contentDisposition, String mimeType) {
        if (url == null || url.startsWith("blob:")) return;
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                URL u = new URL(url);
                c = (HttpURLConnection) u.openConnection();
                c.setInstanceFollowRedirects(true);
                if (userAgent != null) c.setRequestProperty("User-Agent", userAgent);
                String cookie = CookieManager.getInstance().getCookie(url);
                if (cookie != null) c.setRequestProperty("Cookie", cookie);
                c.connect();
                String type = c.getContentType();
                String disposition = c.getHeaderField("Content-Disposition");
                String name = filenameFrom(disposition != null ? disposition : contentDisposition);
                if (name == null) name = filenameFromUrl(url);
                if (name == null) name = "avatar";
                saveStream(c.getInputStream(), safeFileName(name), type != null ? type : mimeType);
                runOnUiThread(() -> Toast.makeText(this, "Saved to GrokBot Avatars", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Download failed", Toast.LENGTH_SHORT).show());
            } finally {
                if (c != null) c.disconnect();
            }
        }).start();
    }

    private void saveStream(InputStream in, String name, String mime) throws Exception {
        File file = targetFile(name, mime);
        try (InputStream input = in; FileOutputStream out = new FileOutputStream(file)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = input.read(buf)) != -1) out.write(buf, 0, n);
        }
        android.media.MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, new String[]{mime}, null);
    }

    private void saveBytes(byte[] bytes, String name, String mime) throws Exception {
        File file = targetFile(name, mime);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
        }
        android.media.MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, new String[]{mime}, null);
        runOnUiThread(() -> Toast.makeText(this, "Saved to GrokBot Avatars", Toast.LENGTH_SHORT).show());
    }

    private File targetFile(String name, String mime) throws Exception {
        File base = getExternalMediaDirs().length > 0 ? getExternalMediaDirs()[0] : getExternalFilesDir(null);
        if (base == null) throw new IllegalStateException("Storage unavailable");
        String folder;
        String m = mime == null ? "" : mime.toLowerCase(Locale.US);
        String lower = name.toLowerCase(Locale.US);
        if (m.contains("video") || lower.endsWith(".mp4") || lower.endsWith(".webm")) folder = "GrokBot Avatars/Videos";
        else if (m.contains("gif") || lower.endsWith(".gif")) folder = "GrokBot Avatars/GIFs";
        else folder = "GrokBot Avatars/Images";
        File dir = new File(base, folder);
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Could not create folder");
        return uniqueFile(new File(dir, name));
    }

    private File uniqueFile(File f) {
        if (!f.exists()) return f;
        String n = f.getName();
        int dot = n.lastIndexOf('.');
        String stem = dot > 0 ? n.substring(0, dot) : n;
        String ext = dot > 0 ? n.substring(dot) : "";
        int i = 2;
        File candidate;
        do { candidate = new File(f.getParentFile(), stem + " (" + i++ + ")" + ext); } while (candidate.exists());
        return candidate;
    }

    private String filenameFrom(String disposition) {
        if (disposition == null) return null;
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("filename=?([^;]+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(disposition);
            if (m.find()) return URLDecoder.decode(m.group(1).trim(), "UTF-8");
        } catch (Exception ignored) {}
        return null;
    }

    private String filenameFromUrl(String url) {
        try {
            String p = new URL(url).getPath();
            String n = p.substring(p.lastIndexOf('/') + 1);
            return n.isEmpty() ? null : n;
        } catch (Exception e) { return null; }
    }

    private String safeFileName(String n) {
        if (n == null || n.trim().isEmpty()) n = "avatar";
        n = n.replaceAll("[\\/:*?\"<>|]", "_").trim();
        return n.length() > 120 ? n.substring(0, 120) : n;
    }

    private void showGallery() {
        launcher.setVisibility(View.GONE);
        if (webView != null) webView.setVisibility(View.GONE);

        LinearLayout gallery = new LinearLayout(this);
        gallery.setOrientation(LinearLayout.VERTICAL);
        gallery.setBackgroundColor(bgColor);
        gallery.setPadding(dp(16), dp(18), dp(16), dp(12));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("Avatar Hub");
        title.setTextColor(textColor);
        title.setTextSize(25);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1f));
        Button back = new Button(this);
        back.setText("Back");
        back.setOnClickListener(v -> { root.removeView(gallery); root.setTag(null); showLauncher(); });
        top.addView(back, new LinearLayout.LayoutParams(dp(88), dp(48)));
        gallery.addView(top);

        EditText search = new EditText(this);
        search.setHint("Search avatars…");
        search.setSingleLine(true);
        search.setTextColor(textColor);
        search.setHintTextColor(withAlpha(textColor, 130));
        gallery.addView(search, new LinearLayout.LayoutParams(-1, dp(52)));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        final GridLayout gridRef = grid;

        HorizontalScrollView filters = new HorizontalScrollView(this);
        LinearLayout fr = new LinearLayout(this);
        String[] labels = {"All", "Images", "GIFs", "Videos", "★ Favorites"};
        String[] keys = {"all", "image", "gif", "video", "favorite"};
        for (int i=0;i<labels.length;i++) {
            Button b = new Button(this);
            final String key=keys[i];
            b.setText(labels[i]);
            b.setOnClickListener(v -> { galleryFilter=key; refreshGalleryGrid(gridRef, search.getText().toString()); });
            fr.addView(b, new LinearLayout.LayoutParams(-2, dp(48)));
        }
        filters.addView(fr);
        gallery.addView(filters, new LinearLayout.LayoutParams(-1, dp(54)));

        Button importButton = new Button(this);
        importButton.setText("＋ Import avatars");
        importButton.setOnClickListener(v -> importAvatars());
        gallery.addView(importButton, new LinearLayout.LayoutParams(-1, dp(48)));

        File base = getExternalMediaDirs().length > 0 ? getExternalMediaDirs()[0] : getExternalFilesDir(null);
        galleryBase = base == null ? null : new File(base, "GrokBot Avatars");
        allGalleryFiles.clear();
        if (galleryBase != null) collectMedia(galleryBase, allGalleryFiles);
        Collections.sort(allGalleryFiles, (a,b) -> Long.compare(b.lastModified(), a.lastModified()));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(grid);
        gallery.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a) {}
            public void onTextChanged(CharSequence s,int st,int before,int count) { refreshGalleryGrid(gridRef, s.toString()); }
            public void afterTextChanged(android.text.Editable e) {}
        });
        root.addView(gallery, new FrameLayout.LayoutParams(-1, -1));
        root.setTag(gallery);
        refreshGalleryGrid(grid, "");
        hideSystemBars();
    }

    private void refreshGalleryGrid(GridLayout grid, String query) {
        grid.removeAllViews();
        String q = query == null ? "" : query.toLowerCase(Locale.US).trim();
        for (File f : allGalleryFiles) {
            String n=f.getName().toLowerCase(Locale.US);
            boolean type = galleryFilter.equals("all") ||
                    (galleryFilter.equals("gif") && n.endsWith(".gif")) ||
                    (galleryFilter.equals("video") && (n.endsWith(".mp4") || n.endsWith(".webm"))) ||
                    (galleryFilter.equals("image") && (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg"))) ||
                    (galleryFilter.equals("favorite") && isFavorite(f));
            if (type && (q.isEmpty() || n.contains(q))) addMediaCard(grid,f);
        }
    }

    private boolean isFavorite(File f) {
        return getPreferences(MODE_PRIVATE).getBoolean("fav:" + f.getAbsolutePath(), false);
    }

    private void toggleFavorite(File f) {
        boolean now=!isFavorite(f);
        getPreferences(MODE_PRIVATE).edit().putBoolean("fav:" + f.getAbsolutePath(), now).apply();
        Toast.makeText(this, now ? "Added to favorites" : "Removed from favorites", Toast.LENGTH_SHORT).show();
    }

    private void importAvatars() {
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        startActivityForResult(i, 2002);
    }

    private void collectMedia(File dir, ArrayList<File> out) {
        if (dir == null || !dir.exists()) return;
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.isDirectory()) collectMedia(f, out);
            else {
                String n = f.getName().toLowerCase(Locale.US);
                if (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") ||
                    n.endsWith(".gif") || n.endsWith(".mp4") || n.endsWith(".webm")) out.add(f);
            }
        }
    }

    private void addMediaCard(GridLayout grid, File file) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(cardColor);
        bg.setCornerRadius(dp(18));
        card.setBackground(bg);

        ImageView preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (file.getName().toLowerCase(Locale.US).endsWith(".gif")) {
            android.graphics.drawable.Drawable d = android.graphics.drawable.Drawable.createFromPath(file.getAbsolutePath());
            preview.setImageDrawable(d);
        } else if (!file.getName().toLowerCase(Locale.US).endsWith(".mp4") && !file.getName().toLowerCase(Locale.US).endsWith(".webm")) {
            preview.setImageURI(Uri.fromFile(file));
        } else {
            preview.setImageResource(android.R.drawable.ic_media_play);
        }
        card.addView(preview, new LinearLayout.LayoutParams(-1, dp(125)));

        TextView name = new TextView(this);
        name.setText(file.getName());
        name.setTextColor(textColor);
        name.setTextSize(12);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        card.addView(name, new LinearLayout.LayoutParams(-1, dp(44)));

        card.setOnClickListener(v -> openMedia(file));
        card.setOnLongClickListener(v -> { toggleFavorite(file); return true; });
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0; p.height = dp(185);
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setMargins(dp(5), dp(5), dp(5), dp(5));
        grid.addView(card, p);
    }

    private void openMedia(File file) {
        FrameLayout viewer = new FrameLayout(this);
        viewer.setBackgroundColor(Color.BLACK);

        Button close = new Button(this);
        close.setText("Back");
        close.setOnLongClickListener(v -> { toggleFavorite(file); return true; });
        close.setOnClickListener(v -> {
            root.removeView(viewer);
            hideSystemBars();
        });
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(dp(90), dp(52), Gravity.TOP | Gravity.END);
        cp.setMargins(0, dp(12), dp(12), 0);
        viewer.addView(close, cp);

        String n = file.getName().toLowerCase(Locale.US);
        if (n.endsWith(".mp4") || n.endsWith(".webm")) {
            android.widget.VideoView video = new android.widget.VideoView(this);
            video.setVideoPath(file.getAbsolutePath());
            video.setMediaController(new android.widget.MediaController(this));
            video.setOnPreparedListener(mp -> mp.setLooping(false));
            FrameLayout.LayoutParams vp = new FrameLayout.LayoutParams(-1, -1);
            vp.gravity = Gravity.CENTER;
            viewer.addView(video, 0, vp);
            video.start();
        } else {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            android.graphics.drawable.Drawable d = android.graphics.drawable.Drawable.createFromPath(file.getAbsolutePath());
            image.setImageDrawable(d);
            FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(-1, -1);
            ip.gravity = Gravity.CENTER;
            viewer.addView(image, 0, ip);
        }
        root.addView(viewer, new FrameLayout.LayoutParams(-1, -1));
        root.setTag(viewer);
        hideSystemBars();
    }

    private String readAsset(String name) throws Exception {
        InputStream in = getAssets().open(name);
        byte[] b = new byte[in.available()];
        int n = in.read(b);
        in.close();
        return new String(b, 0, n, java.nio.charset.StandardCharsets.UTF_8);
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private int withAlpha(int color, int alpha) { return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)); }

    private void hideSystemBars() {
        Window window = getWindow();
        if (window == null) return;
        View decor = window.getDecorView();
        if (decor == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController c = decor.getWindowInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2002 && resultCode == RESULT_OK && data != null) {
            try {
                File base = getExternalMediaDirs().length > 0 ? getExternalMediaDirs()[0] : getExternalFilesDir(null);
                if (base != null) {
                    Uri[] uris;
                    if (data.getClipData()!=null) {
                        int count=data.getClipData().getItemCount(); uris=new Uri[count];
                        for(int i=0;i<count;i++) uris[i]=data.getClipData().getItemAt(i).getUri();
                    } else if(data.getData()!=null) uris=new Uri[]{data.getData()}; else uris=new Uri[0];
                    for(Uri u:uris) {
                        String name="imported-avatar-"+System.currentTimeMillis();
                        String mime=getContentResolver().getType(u);
                        if(mime==null) mime="image/png";
                        if(mime.contains("gif")) name+=".gif"; else if(mime.contains("video")) name+=mime.contains("webm")?".webm":".mp4"; else name+=".png";
                        File dir=new File(base,"GrokBot Avatars/"+(mime.contains("video")?"Videos":(mime.contains("gif")?"GIFs":"Images")));
                        if(!dir.exists())dir.mkdirs();
                        try(InputStream in=getContentResolver().openInputStream(u); FileOutputStream out=new FileOutputStream(uniqueFile(new File(dir,name)))) {
                            byte[] buf=new byte[8192]; int n; while((n=in.read(buf))!=-1)out.write(buf,0,n);
                        }
                    }
                    Toast.makeText(this,"Imported avatars",Toast.LENGTH_SHORT).show();
                }
            } catch(Exception e) { Toast.makeText(this,"Import failed",Toast.LENGTH_SHORT).show(); }
            return;
        }
        if (requestCode != FILE_CHOOSER_REQUEST || filePathCallback == null) return;
        Uri[] results = null;
        if (resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                results = new Uri[count];
                for (int i = 0; i < count; i++) results[i] = data.getClipData().getItemAt(i).getUri();
            } else if (data.getData() != null) results = new Uri[]{data.getData()};
        }
        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
    }

    @Override
    public void onBackPressed() {
        View tag = (View) root.getTag();
        if (tag != null && tag.getParent() == root) {
            root.removeView(tag);
            root.setTag(null);
            if (tag instanceof FrameLayout) {
                showGallery();
            } else {
                showLauncher();
            }
            return;
        }
        if (currentEditor >= 0 && webView != null && webView.getVisibility() == View.VISIBLE) {
            if (webView.canGoBack()) webView.goBack();
            else showLauncher();
            return;
        }
        showLauncher();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            ViewGroup parent = (ViewGroup) webView.getParent();
            if (parent != null) parent.removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
