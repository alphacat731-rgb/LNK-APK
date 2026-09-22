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
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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
        launcher.setOrientation(LinearLayout.VERTICAL);
        launcher.setPadding(0, 0, 0, 0);

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(18), dp(16), dp(82));

        // Header: quiet, typography-first, no decorative clutter.
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout headerText = new LinearLayout(this);
        headerText.setOrientation(LinearLayout.VERTICAL);
        headerText.addView(makeText("GROKBOT AVATAR HUB", 9, withAlpha(textColor, 115), true),
                new LinearLayout.LayoutParams(-1, dp(18)));
        headerText.addView(makeText("Your creator space", 25, textColor, true),
                new LinearLayout.LayoutParams(-1, dp(34)));
        header.addView(headerText, new LinearLayout.LayoutParams(0, dp(52), 1f));

        TextView settingsIcon = makeIconButton("⚙");
        settingsIcon.setOnClickListener(v -> pressAnimation(settingsIcon, v2 -> showSettings()));
        header.addView(settingsIcon, new LinearLayout.LayoutParams(dp(44), dp(44)));
        content.addView(header);

        content.addView(makeText("Create, remix, download and keep your favorites.",
                12, withAlpha(textColor, 145), false),
                new LinearLayout.LayoutParams(-1, dp(24)));

        // Primary action. One restrained surface instead of a giant pill.
        LinearLayout hero = roundedPanel(dp(14), Color.rgb(24, 24, 24));
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(16), dp(12), dp(16), dp(12));

        hero.addView(makeText("READY TO CREATE?", 8, withAlpha(Color.WHITE, 145), true),
                new LinearLayout.LayoutParams(-1, dp(15)));
        hero.addView(makeText("Make your next avatar", 20, Color.WHITE, true),
                new LinearLayout.LayoutParams(-1, dp(28)));
        hero.addView(makeText("Start with an editor or open your collection.", 10,
                withAlpha(Color.WHITE, 145), false),
                new LinearLayout.LayoutParams(-1, dp(21)));

        LinearLayout heroActions = new LinearLayout(this);
        heroActions.setGravity(Gravity.CENTER_VERTICAL);

        TextView create = makeActionPill("＋  Create avatar", Color.WHITE, Color.BLACK);
        create.setOnClickListener(v -> pressAnimation(create, v2 -> {
            if (!editors.isEmpty()) openEditor(0);
        }));
        heroActions.addView(create, new LinearLayout.LayoutParams(dp(132), dp(34)));

        TextView library = makeActionPill("Open library", Color.rgb(42, 42, 42), Color.WHITE);
        library.setOnClickListener(v -> pressAnimation(library, v2 -> showGallery()));
        LinearLayout.LayoutParams libraryLp = new LinearLayout.LayoutParams(dp(112), dp(34));
        libraryLp.leftMargin = dp(8);
        heroActions.addView(library, libraryLp);

        hero.addView(heroActions, new LinearLayout.LayoutParams(-1, dp(34)));
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(-1, dp(126));
        heroLp.topMargin = dp(10);
        content.addView(hero, heroLp);

        // Stats: simple numbers, separated by hairlines instead of three cards.
        File base = getExternalMediaDirs().length > 0 ? getExternalMediaDirs()[0] : getExternalFilesDir(null);
        ArrayList<File> dashboardFiles = new ArrayList<>();
        if (base != null) collectMedia(new File(base, "GrokBot Avatars"), dashboardFiles);
        int favorites = 0;
        for (File f : dashboardFiles) if (isFavorite(f)) favorites++;

        LinearLayout stats = new LinearLayout(this);
        stats.setGravity(Gravity.CENTER_VERTICAL);
        stats.setPadding(0, dp(8), 0, dp(4));
        addStatWithDivider(stats, String.valueOf(dashboardFiles.size()), "AVATARS", true);
        addStatWithDivider(stats, String.valueOf(editors.size()), "EDITORS", true);
        addStatWithDivider(stats, String.valueOf(favorites), "FAVORITES", false);
        content.addView(stats, new LinearLayout.LayoutParams(-1, dp(58)));

        // Quick launch: compact list rows avoid clipped carousel cards.
        addSectionTitle(content, "Quick launch", "Your creative tools");
        for (int i = 0; i < editors.size(); i++) {
            final int index = i;
            LinearLayout row = makeEditorRow(editors.get(i).name, i);
            row.setOnClickListener(v -> pressAnimation(row, v2 -> openEditor(index)));
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(58));
            if (i > 0) rowLp.topMargin = dp(6);
            content.addView(row, rowLp);
        }

        // Collection: one clean row with a secondary import action.
        addSectionTitle(content, "Your collection", "Everything you download, in one place");
        LinearLayout collection = roundedPanel(dp(12), cardColor);
        collection.setGravity(Gravity.CENTER_VERTICAL);
        collection.setPadding(dp(10), dp(8), dp(10), dp(8));

        TextView hubIcon = makeIconBadge("★", 38, Color.WHITE);
        collection.addView(hubIcon, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout collectionText = new LinearLayout(this);
        collectionText.setOrientation(LinearLayout.VERTICAL);
        collectionText.setPadding(dp(10), 0, dp(6), 0);
        collectionText.addView(makeText("Avatar Hub", 16, textColor, true),
                new LinearLayout.LayoutParams(-1, dp(23)));
        collectionText.addView(makeText(dashboardFiles.size() + " saved  •  Images  •  GIFs  •  Videos",
                10, withAlpha(textColor, 125), false),
                new LinearLayout.LayoutParams(-1, dp(19)));
        collection.addView(collectionText, new LinearLayout.LayoutParams(0, dp(42), 1f));

        TextView importText = makeText("Import", 10, textColor, true);
        importText.setGravity(Gravity.CENTER);
        GradientDrawable importBg = new GradientDrawable();
        importBg.setColor(Color.TRANSPARENT);
        importBg.setCornerRadius(dp(9));
        importBg.setStroke(dp(1), withAlpha(textColor, 55));
        importText.setBackground(importBg);
        importText.setPadding(dp(9), 0, dp(9), 0);
        importText.setOnClickListener(v -> pressAnimation(importText, v2 -> importAvatars()));
        collection.addView(importText, new LinearLayout.LayoutParams(dp(58), dp(34)));

        TextView open = makeText("›", 25, textColor, false);
        open.setGravity(Gravity.CENTER);
        collection.addView(open, new LinearLayout.LayoutParams(dp(22), dp(42)));
        collection.setOnClickListener(v -> pressAnimation(collection, v2 -> showGallery()));
        content.addView(collection, new LinearLayout.LayoutParams(-1, dp(58)));

        // Recent creations: compact and guaranteed not to clip text.
        addRecentSection(content);

        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        launcher.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        // Flat bottom navigation. No giant floating pill.
        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(dp(16), dp(4), dp(16), dp(4));
        GradientDrawable navBg = new GradientDrawable();
        navBg.setColor(bgColor);
        navBg.setStroke(dp(1), withAlpha(textColor, 18));
        nav.setBackground(navBg);
        nav.setElevation(dp(8));

        TextView home = makeNavItem("⌂", "Home", true);
        TextView hub = makeNavItem("★", "Library", false);
        TextView settings = makeNavItem("⚙", "Settings", false);
        home.setOnClickListener(v -> scroll.smoothScrollTo(0, 0));
        hub.setOnClickListener(v -> pressAnimation(hub, v2 -> showGallery()));
        settings.setOnClickListener(v -> pressAnimation(settings, v2 -> showSettings()));

        nav.addView(home, new LinearLayout.LayoutParams(0, dp(52), 1f));
        nav.addView(hub, new LinearLayout.LayoutParams(0, dp(52), 1f));
        nav.addView(settings, new LinearLayout.LayoutParams(0, dp(52), 1f));
        launcher.addView(nav, new LinearLayout.LayoutParams(-1, dp(60)));

        animateLauncherEntrance();
    }

    private void addStatWithDivider(LinearLayout parent, String value, String label, boolean divider) {
        LinearLayout box = dashboardStat(value, label);
        parent.addView(box, new LinearLayout.LayoutParams(0, dp(50), 1f));
        if (divider) {
            View line = new View(this);
            line.setBackgroundColor(withAlpha(textColor, 20));
            parent.addView(line, new LinearLayout.LayoutParams(dp(1), dp(28)));
        }
    }

    private LinearLayout makeEditorRow(String label, int index) {
        LinearLayout row = roundedPanel(dp(12), cardColor);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));

        TextView badge = makeIconBadge(String.valueOf(index + 1), 34, Color.WHITE);
        row.addView(badge, new LinearLayout.LayoutParams(dp(34), dp(34)));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(dp(10), 0, dp(8), 0);
        text.addView(makeText(label, 14, textColor, true),
                new LinearLayout.LayoutParams(-1, dp(20)));
        text.addView(makeText("Open editor", 10, withAlpha(textColor, 110), false),
                new LinearLayout.LayoutParams(-1, dp(16)));
        row.addView(text, new LinearLayout.LayoutParams(0, dp(38), 1f));

        TextView arrow = makeText("→", 18, withAlpha(textColor, 180), false);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(28), dp(38)));
        return row;
    }

    private TextView makeActionPill(String label, int background, int foreground) {
        TextView t = makeText(label, 11, foreground, true);
        t.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(background);
        g.setCornerRadius(dp(18));
        g.setStroke(dp(1), withAlpha(Color.WHITE, 18));
        t.setBackground(g);
        return t;
    }

    private LinearLayout dashboardStat(String value, String label) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.addView(makeText(value, 20, textColor, true), new LinearLayout.LayoutParams(-1, dp(24)));
        TextView l = makeText(label, 8, withAlpha(textColor, 110), true);
        l.setGravity(Gravity.CENTER);
        box.addView(l, new LinearLayout.LayoutParams(-1, dp(16)));
        return box;
    }

    private void animateLauncherEntrance() {
        launcher.setAlpha(0f);
        launcher.setTranslationY(dp(10));
        launcher.animate().alpha(1f).translationY(0f)
                .setDuration(420)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    private void animateCard(View v, long delay) {
        v.setAlpha(0f);
        v.setTranslationY(dp(16));
        v.setScaleX(0.97f);
        v.setScaleY(0.97f);
        v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setStartDelay(120 + delay)
                .setDuration(380)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    private void pressAnimation(View view, View.OnClickListener action) {
        view.animate().scaleX(0.96f).scaleY(0.96f)
                .setDuration(85)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f)
                        .setDuration(170)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f))
                        .withEndAction(() -> {
                            if (action != null) action.onClick(view);
                        }).start())
                .start();
    }

    private void styleActionButton(TextView t, String label) {
        t.setText(label);
        t.setTextColor(textColor);
        t.setTextSize(12);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable g = new GradientDrawable();
        g.setColor(cardColor);
        g.setCornerRadius(dp(16));
        g.setStroke(dp(1), withAlpha(textColor, 22));
        t.setBackground(g);
        t.setElevation(dp(1));
    }

    private LinearLayout roundedPanel(int radius, int color) {
        LinearLayout panel = new LinearLayout(this);
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        g.setStroke(dp(1), withAlpha(textColor, 16));
        panel.setBackground(g);
        panel.setElevation(dp(2));
        return panel;
    }

    private TextView makeText(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(size);
        t.setTypeface(null, bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        return t;
    }

    private TextView makeIconButton(String glyph) {
        TextView t = makeText(glyph, 19, textColor, true);
        t.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(withAlpha(cardColor, 210));
        g.setCornerRadius(dp(15));
        g.setStroke(dp(1), withAlpha(textColor, 18));
        t.setBackground(g);
        t.setElevation(dp(2));
        return t;
    }

    private TextView makeIconBadge(String glyph, int size, int color) {
        TextView t = makeText(glyph, 20, bgColor, true);
        t.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(15));
        t.setBackground(g);
        return t;
    }

    private LinearLayout makeEditorCard(String label, int index) {
        LinearLayout card = roundedPanel(dp(22), cardColor);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(12));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge = makeIconBadge(String.valueOf(index + 1), 36, Color.WHITE);
        top.addView(badge, new LinearLayout.LayoutParams(dp(36), dp(36)));
        TextView dots = makeText("•••", 14, withAlpha(textColor, 90), true);
        dots.setGravity(Gravity.CENTER);
        top.addView(dots, new LinearLayout.LayoutParams(0, dp(36), 1f));
        card.addView(top);

        card.addView(makeText(label, 16, textColor, true), new LinearLayout.LayoutParams(-1, dp(24)));
        card.addView(makeText("Open editor", 11, withAlpha(textColor, 125), false), new LinearLayout.LayoutParams(-1, dp(16)));
        return card;
    }

    private TextView makeUtilityTile(String glyph, String title, String sub) {
        TextView tile = makeText(glyph + "   " + title + "\n" + sub, 13, textColor, true);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(dp(15), 0, dp(10), 0);
        GradientDrawable g = new GradientDrawable();
        g.setColor(cardColor);
        g.setCornerRadius(dp(18));
        g.setStroke(dp(1), withAlpha(textColor, 15));
        tile.setBackground(g);
        return tile;
    }

    private TextView makeNavItem(String glyph, String label, boolean active) {
        TextView t = makeText(glyph + "\n" + label, 11, active ? textColor : withAlpha(textColor, 110), true);
        t.setGravity(Gravity.CENTER);
        if (active) {
            GradientDrawable g = new GradientDrawable();
            g.setColor(Color.rgb(32, 32, 32));
            g.setCornerRadius(dp(8));
            g.setStroke(dp(1), withAlpha(Color.WHITE, 18));
            t.setBackground(g);
        }
        return t;
    }

    private void addSectionTitle(LinearLayout parent, String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.BOTTOM);
        row.setPadding(0, dp(14), 0, dp(6));
        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(makeText(title, 17, textColor, true), new LinearLayout.LayoutParams(-1, dp(24)));
        text.addView(makeText(subtitle, 10, withAlpha(textColor, 105), false), new LinearLayout.LayoutParams(-1, dp(16)));
        row.addView(text, new LinearLayout.LayoutParams(0, dp(40), 1f));
        parent.addView(row);
    }

    private TextView spacer(int w, int h) {
        TextView s = new TextView(this);
        s.setVisibility(View.INVISIBLE);
        return s;
    }

    private int accentColor() { return Color.WHITE; }
    private int cyanColor() { return Color.WHITE; }
    private int pinkColor() { return Color.WHITE; }

    private void addRecentSection(LinearLayout parent) {
        File base = getExternalMediaDirs().length > 0 ? getExternalMediaDirs()[0] : getExternalFilesDir(null);
        ArrayList<File> recent = new ArrayList<>();
        if (base != null) collectMedia(new File(base, "GrokBot Avatars"), recent);
        Collections.sort(recent, (a,b) -> Long.compare(b.lastModified(), a.lastModified()));

        addSectionTitle(parent, "Recent creations", recent.isEmpty() ? "Your newest avatars will appear here" : "Tap an avatar to preview it");
        if (recent.isEmpty()) {
            LinearLayout empty = roundedPanel(dp(12), cardColor);
            empty.setGravity(Gravity.CENTER_VERTICAL);
            empty.setPadding(dp(10), dp(8), dp(12), dp(8));

            TextView icon = makeIconBadge("✦", 36, Color.WHITE);
            empty.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));

            LinearLayout tx = new LinearLayout(this);
            tx.setOrientation(LinearLayout.VERTICAL);
            tx.setPadding(dp(10), 0, 0, 0);
            tx.addView(makeText("Nothing here yet", 14, textColor, true),
                    new LinearLayout.LayoutParams(-1, dp(20)));
            tx.addView(makeText("Download an avatar from any editor.", 10,
                    withAlpha(textColor, 115), false),
                    new LinearLayout.LayoutParams(-1, dp(17)));
            empty.addView(tx, new LinearLayout.LayoutParams(0, dp(38), 1f));
            parent.addView(empty, new LinearLayout.LayoutParams(-1, dp(54)));
            return;
        }

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        for (int i = 0; i < Math.min(6, recent.size()); i++) {
            final File file = recent.get(i);
            LinearLayout thumb = roundedPanel(dp(10), cardColor);
            thumb.setPadding(dp(3), dp(3), dp(3), dp(3));
            ImageView p = new ImageView(this);
            p.setScaleType(ImageView.ScaleType.CENTER_CROP);
            String n = file.getName().toLowerCase(Locale.US);
            if (n.endsWith(".mp4") || n.endsWith(".webm")) p.setImageResource(android.R.drawable.ic_media_play);
            else p.setImageDrawable(android.graphics.drawable.Drawable.createFromPath(file.getAbsolutePath()));
            thumb.addView(p, new LinearLayout.LayoutParams(dp(74), dp(74)));
            thumb.setOnClickListener(v -> pressAnimation(thumb, v2 -> openMedia(file)));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(dp(80), dp(80));
            tp.setMargins(0, 0, dp(9), 0);
            row.addView(thumb, tp);
        }
        hs.addView(row);
        parent.addView(hs, new LinearLayout.LayoutParams(-1, dp(88)));
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

        TextView hub = new TextView(this);
        styleActionButton(hub, "Open Avatar Hub");
        hub.setOnClickListener(v -> pressAnimation(hub, v2 -> { root.removeView(page); showGallery(); }));
        page.addView(hub, new LinearLayout.LayoutParams(-1, dp(52)));

        TextView clear = new TextView(this);
        styleActionButton(clear, "Clear WebView cache");
        clear.setOnClickListener(v -> pressAnimation(clear, v2 -> {
            if (webView != null) webView.clearCache(true);
            Toast.makeText(this, "WebView cache cleared", Toast.LENGTH_SHORT).show();
        }));
        page.addView(clear, new LinearLayout.LayoutParams(-1, dp(52)));

        TextView back = new TextView(this);
        styleActionButton(back, "Back to Home");
        back.setOnClickListener(v -> pressAnimation(back, v2 -> { root.removeView(page); showLauncher(); }));
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

        TextView importButton = new TextView(this);
        styleActionButton(importButton, "＋  Import avatars");
        importButton.setOnClickListener(v -> pressAnimation(importButton, v2 -> importAvatars()));
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

    private void showMediaActions(File file) {
        String favoriteLabel = isFavorite(file) ? "Remove favorite" : "Add to favorites";
        new android.app.AlertDialog.Builder(this)
                .setTitle(file.getName())
                .setItems(new String[]{favoriteLabel, "Delete"}, (d, which) -> {
                    if (which == 0) {
                        toggleFavorite(file);
                    } else {
                        if (file.delete()) {
                            allGalleryFiles.remove(file);
                            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                            refreshGalleryGrid(findGalleryGrid(), "");
                        } else {
                            Toast.makeText(this, "Could not delete file", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).show();
    }

    private GridLayout findGalleryGrid() {
        View tag = (View) root.getTag();
        if (tag instanceof LinearLayout) {
            LinearLayout layout = (LinearLayout) tag;
            for (int i=0;i<layout.getChildCount();i++) {
                View child=layout.getChildAt(i);
                if (child instanceof ScrollView) {
                    View inner=((ScrollView)child).getChildAt(0);
                    if (inner instanceof GridLayout) return (GridLayout)inner;
                }
            }
        }
        return new GridLayout(this);
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
        card.setOnLongClickListener(v -> { showMediaActions(file); return true; });
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
}
