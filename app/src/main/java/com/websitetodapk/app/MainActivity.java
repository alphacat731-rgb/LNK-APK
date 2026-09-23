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
    private android.content.SharedPreferences prefs;
    private String language = "en";
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
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        language = prefs.getString("language", Locale.getDefault().getLanguage());
        if (!language.equals("es") && !language.equals("zh") && !language.equals("ja") && !language.equals("de") && !language.equals("fr") && !language.equals("pt")) language = "en";
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

        ArrayList<File> dashboardFiles = new ArrayList<>();
        File dashboardBase = getExternalMediaDirs().length > 0 ? getExternalMediaDirs()[0] : getExternalFilesDir(null);
        if (dashboardBase != null) collectMedia(new File(dashboardBase, "GrokBot Avatars"), dashboardFiles);

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(26));

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout headerText = new LinearLayout(this);
        headerText.setOrientation(LinearLayout.VERTICAL);
        headerText.addView(makeText("GROKBOT AVATAR HUB", 9, withAlpha(textColor, 110), true),
                new LinearLayout.LayoutParams(-1, dp(17)));
        headerText.addView(makeText(t("welcome"), 25, textColor, true),
                new LinearLayout.LayoutParams(-1, dp(32)));
        header.addView(headerText, new LinearLayout.LayoutParams(0, dp(49), 1f));

        TextView settingsIcon = makeIconButton("⚙");
        settingsIcon.setOnClickListener(v -> pressAnimation(settingsIcon, v2 -> showSettings()));
        header.addView(settingsIcon, new LinearLayout.LayoutParams(dp(48), dp(48)));
        content.addView(header);

        content.addView(makeText(t("homeLead"), 12, withAlpha(textColor, 150), false),
                new LinearLayout.LayoutParams(-1, dp(26)));

        // Hero / primary action
        LinearLayout hero = roundedPanel(dp(18), Color.rgb(22, 22, 22));
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout heroTop = new LinearLayout(this);
        heroTop.setGravity(Gravity.CENTER_VERTICAL);

        TextView heroTag = makeText(t("heroTag"), 8, withAlpha(Color.WHITE, 125), true);
        heroTop.addView(heroTag, new LinearLayout.LayoutParams(0, dp(18), 1f));

        TextView editorCount = makeText(editors.size() + " " + t("editorsCount"), 9, withAlpha(Color.WHITE, 135), true);
        editorCount.setGravity(Gravity.CENTER);
        GradientDrawable editorCountBg = new GradientDrawable();
        editorCountBg.setColor(Color.rgb(34, 34, 34));
        editorCountBg.setCornerRadius(dp(12));
        editorCountBg.setStroke(dp(1), withAlpha(Color.WHITE, 18));
        editorCount.setBackground(editorCountBg);
        heroTop.addView(editorCount, new LinearLayout.LayoutParams(dp(86), dp(28)));
        hero.addView(heroTop);

        hero.addView(makeText(t("makeAvatar"), 22, Color.WHITE, true),
                new LinearLayout.LayoutParams(-1, dp(30)));
        hero.addView(makeText(t("heroHint"), 11, withAlpha(Color.WHITE, 142), false),
                new LinearLayout.LayoutParams(-1, dp(22)));

        LinearLayout heroActions = new LinearLayout(this);
        heroActions.setGravity(Gravity.CENTER_VERTICAL);

        TextView create = makeActionPill("＋  " + t("create"), Color.WHITE, Color.BLACK);
        create.setOnClickListener(v -> pressAnimation(create, v2 -> {
            if (!editors.isEmpty()) openEditor(0);
        }));
        heroActions.addView(create, new LinearLayout.LayoutParams(dp(146), dp(38)));

        TextView library = makeActionPill(t("library"), Color.rgb(40, 40, 40), Color.WHITE);
        library.setOnClickListener(v -> pressAnimation(library, v2 -> showGallery()));
        LinearLayout.LayoutParams libraryLp = new LinearLayout.LayoutParams(dp(110), dp(38));
        libraryLp.leftMargin = dp(8);
        heroActions.addView(library, libraryLp);

        hero.addView(heroActions, new LinearLayout.LayoutParams(-1, dp(38)));
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(-1, dp(154));
        heroLp.topMargin = dp(10);
        content.addView(hero, heroLp);

        // Stats strip
        LinearLayout stats = new LinearLayout(this);
        stats.setPadding(0, dp(8), 0, 0);
        stats.setGravity(Gravity.CENTER_VERTICAL);
        stats.addView(makeStatTile(String.valueOf(dashboardFiles.size()), t("avatars"), "✦"),
                new LinearLayout.LayoutParams(0, dp(70), 1f));
        stats.addView(makeStatDivider());
        stats.addView(makeStatTile(String.valueOf(editors.size()), t("editors"), "◈"),
                new LinearLayout.LayoutParams(0, dp(70), 1f));
        stats.addView(makeStatDivider());
        stats.addView(makeStatTile(String.valueOf(countFavorites(dashboardFiles)), t("favorites"), "★"),
                new LinearLayout.LayoutParams(0, dp(70), 1f));
        content.addView(stats);

        // Quick actions
        addSectionTitle(content, t("quickActions"), t("quickActionsHint"));
        LinearLayout quickRow = new LinearLayout(this);
        quickRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout importTile = makeQuickTile("＋", t("import"), t("importHint"));
        importTile.setOnClickListener(v -> pressAnimation(importTile, v2 -> importAvatars()));
        quickRow.addView(importTile, new LinearLayout.LayoutParams(0, dp(88), 1f));

        LinearLayout favoritesTile = makeQuickTile("★", t("favorites"), t("favoritesHint"));
        favoritesTile.setOnClickListener(v -> pressAnimation(favoritesTile, v2 -> {
            galleryFilter = "favorite";
            showGallery();
        }));
        LinearLayout.LayoutParams favLp = new LinearLayout.LayoutParams(0, dp(88), 1f);
        favLp.leftMargin = dp(8);
        quickRow.addView(favoritesTile, favLp);
        content.addView(quickRow);

        // Editors
        addSectionTitle(content, t("editors"), t("editorsHint"));
        HorizontalScrollView editorScroll = new HorizontalScrollView(this);
        editorScroll.setHorizontalScrollBarEnabled(false);
        editorScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout editorRow = new LinearLayout(this);
        for (int i = 0; i < editors.size(); i++) {
            final int index = i;
            LinearLayout card = makeEditorTile(editors.get(i).name, i);
            card.setOnClickListener(v -> pressAnimation(card, v2 -> openEditor(index)));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(164), dp(126));
            if (i > 0) p.leftMargin = dp(8);
            editorRow.addView(card, p);
        }
        editorScroll.addView(editorRow);
        content.addView(editorScroll, new LinearLayout.LayoutParams(-1, dp(132)));

        // Library overview
        addSectionTitle(content, t("library"), t("libraryHint"));
        LinearLayout libraryCard = roundedPanel(dp(16), cardColor);
        libraryCard.setPadding(dp(12), dp(10), dp(12), dp(10));

        LinearLayout libraryMain = new LinearLayout(this);
        libraryMain.setGravity(Gravity.CENTER_VERTICAL);

        TextView libraryBadge = makeIconBadge("★", 44, Color.WHITE);
        libraryMain.addView(libraryBadge, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout libraryText = new LinearLayout(this);
        libraryText.setOrientation(LinearLayout.VERTICAL);
        libraryText.setPadding(dp(10), 0, dp(8), 0);
        libraryText.addView(makeText(t("avatarHub"), 16, textColor, true),
                new LinearLayout.LayoutParams(-1, dp(22)));
        libraryText.addView(makeText(dashboardFiles.size() + " " + t("savedItems"), 10, withAlpha(textColor, 120), false),
                new LinearLayout.LayoutParams(-1, dp(18)));
        libraryMain.addView(libraryText, new LinearLayout.LayoutParams(0, dp(44), 1f));

        TextView open = makeText("→", 20, withAlpha(textColor, 175), false);
        open.setGravity(Gravity.CENTER);
        libraryMain.addView(open, new LinearLayout.LayoutParams(dp(28), dp(44)));
        libraryCard.addView(libraryMain, new LinearLayout.LayoutParams(-1, dp(44)));

        LinearLayout libraryActions = new LinearLayout(this);
        libraryActions.setGravity(Gravity.CENTER_VERTICAL);
        TextView manage = makeActionPill(t("openLibrary"), Color.rgb(40, 40, 40), Color.WHITE);
        manage.setOnClickListener(v -> pressAnimation(manage, v2 -> showGallery()));
        libraryActions.addView(manage, new LinearLayout.LayoutParams(0, dp(36), 1f));

        TextView importSmall = makeActionPill(t("import"), Color.WHITE, Color.BLACK);
        importSmall.setOnClickListener(v -> pressAnimation(importSmall, v2 -> importAvatars()));
        LinearLayout.LayoutParams importLp = new LinearLayout.LayoutParams(dp(96), dp(36));
        importLp.leftMargin = dp(8);
        libraryActions.addView(importSmall, importLp);

        LinearLayout.LayoutParams libCardLp = new LinearLayout.LayoutParams(-1, dp(104));
        libCardLp.topMargin = dp(2);
        libraryCard.addView(libraryActions, new LinearLayout.LayoutParams(-1, dp(36)));
        content.addView(libraryCard, libCardLp);

        // Recent creations
        addRecentSection(content);

        // Creator tip
        addSectionTitle(content, t("creatorTip"), "");
        LinearLayout tip = roundedPanel(dp(14), Color.rgb(18, 18, 18));
        tip.setPadding(dp(12), dp(10), dp(12), dp(10));
        TextView tipIcon = makeIconBadge("✦", 38, Color.WHITE);
        tip.addView(tipIcon, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout tipText = new LinearLayout(this);
        tipText.setOrientation(LinearLayout.VERTICAL);
        tipText.setPadding(dp(10), 0, 0, 0);
        tipText.addView(makeText(t("creatorTipTitle"), 13, textColor, true),
                new LinearLayout.LayoutParams(-1, dp(20)));
        tipText.addView(makeText(t("creatorTipText"), 10, withAlpha(textColor, 115), false),
                new LinearLayout.LayoutParams(-1, dp(30)));
        tip.addView(tipText, new LinearLayout.LayoutParams(0, dp(38), 1f));
        content.addView(tip, new LinearLayout.LayoutParams(-1, dp(60)));

        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        launcher.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        // Bottom navigation
        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(dp(10), dp(4), dp(10), dp(4));
        GradientDrawable navBg = new GradientDrawable();
        navBg.setColor(bgColor);
        navBg.setStroke(dp(1), withAlpha(textColor, 16));
        nav.setBackground(navBg);
        nav.setElevation(dp(8));

        TextView home = makeNavItem("⌂", t("home"), true);
        TextView hub = makeNavItem("★", t("library"), false);
        TextView settings = makeNavItem("⚙", t("settings"), false);
        home.setOnClickListener(v -> scroll.smoothScrollTo(0, 0));
        hub.setOnClickListener(v -> pressAnimation(hub, v2 -> showGallery()));
        settings.setOnClickListener(v -> pressAnimation(settings, v2 -> showSettings()));

        nav.addView(home, new LinearLayout.LayoutParams(0, dp(54), 1f));
        nav.addView(hub, new LinearLayout.LayoutParams(0, dp(54), 1f));
        nav.addView(settings, new LinearLayout.LayoutParams(0, dp(54), 1f));
        launcher.addView(nav, new LinearLayout.LayoutParams(-1, dp(62)));

        animateLauncherEntrance();
    }

    private View makeStatDivider() {
        View line = new View(this);
        line.setBackgroundColor(withAlpha(textColor, 20));
        return line;
    }

    private int countFavorites(ArrayList<File> files) {
        int count = 0;
        for (File f : files) if (isFavorite(f)) count++;
        return count;
    }

    private LinearLayout makeStatTile(String value, String label, String glyph) {
        LinearLayout tile = roundedPanel(dp(12), Color.rgb(18, 18, 18));
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(5), dp(6), dp(5), dp(6));

        TextView icon = makeText(glyph, 12, withAlpha(textColor, 135), true);
        icon.setGravity(Gravity.CENTER);
        tile.addView(icon, new LinearLayout.LayoutParams(-1, dp(15)));

        TextView number = makeText(value, 19, textColor, true);
        number.setGravity(Gravity.CENTER);
        number.setIncludeFontPadding(false);
        tile.addView(number, new LinearLayout.LayoutParams(-1, dp(23)));

        TextView caption = makeText(label, 8, withAlpha(textColor, 105), true);
        caption.setGravity(Gravity.CENTER);
        caption.setIncludeFontPadding(false);
        tile.addView(caption, new LinearLayout.LayoutParams(-1, dp(15)));
        return tile;
    }

    private LinearLayout makeQuickTile(String glyph, String title, String sub) {
        LinearLayout tile = roundedPanel(dp(14), cardColor);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(dp(10), dp(8), dp(10), dp(8));

        TextView icon = makeIconBadge(glyph, 38, Color.WHITE);
        icon.setTextSize(17);
        tile.addView(icon, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(dp(9), 0, 0, 0);
        text.addView(makeText(title, 12, textColor, true),
                new LinearLayout.LayoutParams(-1, dp(20)));
        text.addView(makeText(sub, 9, withAlpha(textColor, 105), false),
                new LinearLayout.LayoutParams(-1, dp(23)));
        tile.addView(text, new LinearLayout.LayoutParams(0, dp(43), 1f));
        return tile;
    }

    private LinearLayout makeEditorTile(String label, int index) {
        LinearLayout card = roundedPanel(dp(16), cardColor);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView badge = makeIconBadge(String.valueOf(index + 1), 34, Color.WHITE);
        top.addView(badge, new LinearLayout.LayoutParams(dp(34), dp(34)));

        TextView arrow = makeText("↗", 16, withAlpha(textColor, 155), false);
        arrow.setGravity(Gravity.CENTER);
        top.addView(arrow, new LinearLayout.LayoutParams(0, dp(34), 1f));
        card.addView(top);

        card.addView(makeText(label, 14, textColor, true),
                new LinearLayout.LayoutParams(-1, dp(21)));
        card.addView(makeText(t("openEditor"), 9, withAlpha(textColor, 105), false),
                new LinearLayout.LayoutParams(-1, dp(17)));
        return card;
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
        text.addView(makeText(t("openEditor"), 10, withAlpha(textColor, 110), false),
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

        // Keep the metric value centered inside its full-width slot.
        // Without this, the TextView itself was centered but its text stayed left-aligned.
        TextView v = makeText(value, 20, textColor, true);
        v.setGravity(Gravity.CENTER);
        v.setIncludeFontPadding(false);
        box.addView(v, new LinearLayout.LayoutParams(-1, dp(24)));

        TextView l = makeText(label, 8, withAlpha(textColor, 110), true);
        l.setGravity(Gravity.CENTER);
        l.setIncludeFontPadding(false);
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

        addSectionTitle(parent, t("recent"), recent.isEmpty() ? t("recentHint") : t("recentTapHint"));
        if (recent.isEmpty()) {
            LinearLayout empty = roundedPanel(dp(14), cardColor);
            empty.setGravity(Gravity.CENTER_VERTICAL);
            empty.setPadding(dp(10), dp(8), dp(12), dp(8));

            TextView icon = makeIconBadge("✦", 38, Color.WHITE);
            empty.addView(icon, new LinearLayout.LayoutParams(dp(38), dp(38)));

            LinearLayout tx = new LinearLayout(this);
            tx.setOrientation(LinearLayout.VERTICAL);
            tx.setPadding(dp(10), 0, 0, 0);
            tx.addView(makeText(t("recentEmpty"), 13, textColor, true),
                    new LinearLayout.LayoutParams(-1, dp(19)));
            tx.addView(makeText(t("recentEmptyHint"), 9, withAlpha(textColor, 110), false),
                    new LinearLayout.LayoutParams(-1, dp(18)));
            empty.addView(tx, new LinearLayout.LayoutParams(0, dp(37), 1f));
            parent.addView(empty, new LinearLayout.LayoutParams(-1, dp(56)));
            return;
        }

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout row = new LinearLayout(this);
        for (int i = 0; i < Math.min(6, recent.size()); i++) {
            final File file = recent.get(i);
            LinearLayout thumb = roundedPanel(dp(11), cardColor);
            thumb.setPadding(dp(3), dp(3), dp(3), dp(3));
            ImageView p = new ImageView(this);
            p.setScaleType(ImageView.ScaleType.CENTER_CROP);
            String n = file.getName().toLowerCase(Locale.US);
            if (n.endsWith(".mp4") || n.endsWith(".webm")) {
                p.setImageResource(android.R.drawable.ic_media_play);
            } else {
                p.setImageDrawable(android.graphics.drawable.Drawable.createFromPath(file.getAbsolutePath()));
            }
            thumb.addView(p, new LinearLayout.LayoutParams(dp(78), dp(78)));
            thumb.setOnClickListener(v -> pressAnimation(thumb, v2 -> openMedia(file)));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(dp(84), dp(84));
            if (i > 0) tp.leftMargin = dp(8);
            row.addView(thumb, tp);
        }
        hs.addView(row);
        parent.addView(hs, new LinearLayout.LayoutParams(-1, dp(88)));
    }

    private String t(String key) {
        if (language.equals("es")) {
            if (key.equals("welcome")) return "Tu espacio creativo";
            if (key.equals("homeLead")) return "Crea, guarda y abre tus avatares desde un solo lugar.";
            if (key.equals("heroTag")) return "CENTRO DE CREACIÓN";
            if (key.equals("editorsCount")) return "editores";
            if (key.equals("makeAvatar")) return "Crea tu próximo avatar";
            if (key.equals("heroHint")) return "Elige un editor, empieza de cero o abre tu biblioteca.";
            if (key.equals("create")) return "Empezar a crear";
            if (key.equals("library")) return "Biblioteca";
            if (key.equals("quickActions")) return "Acciones rápidas";
            if (key.equals("quickActionsHint")) return "Las cosas que haces más a menudo";
            if (key.equals("import")) return "Importar";
            if (key.equals("importHint")) return "Añade archivos";
            if (key.equals("favorites")) return "Favoritos";
            if (key.equals("favoritesHint")) return "Tus avatares guardados";
            if (key.equals("avatars")) return "Avatares";
            if (key.equals("editors")) return "Editores";
            if (key.equals("openEditor")) return "Abrir editor";
            if (key.equals("editorsHint")) return "Elige dónde quieres crear";
            if (key.equals("avatarHub")) return "Avatar Hub";
            if (key.equals("libraryHint")) return "Todo tu contenido, ordenado";
            if (key.equals("savedItems")) return "elementos guardados";
            if (key.equals("openLibrary")) return "Abrir biblioteca";
            if (key.equals("recent")) return "Creaciones recientes";
            if (key.equals("recentHint")) return "Tus avatares más nuevos aparecerán aquí";
            if (key.equals("recentTapHint")) return "Toca una creación para verla";
            if (key.equals("recentEmpty")) return "Todavía no hay creaciones";
            if (key.equals("recentEmptyHint")) return "Descarga un avatar desde cualquier editor.";
            if (key.equals("creatorTip")) return "Consejo del creador";
            if (key.equals("creatorTipTitle")) return "Todo queda organizado automáticamente";
            if (key.equals("creatorTipText")) return "Las imágenes, GIFs y vídeos se guardan en sus carpetas.";
            if (key.equals("home")) return "Inicio";
            if (key.equals("settings")) return "Ajustes";
            if (key.equals("language")) return "Idioma";
            if (key.equals("openHub")) return "Abrir Avatar Hub";
            if (key.equals("clearCache")) return "Borrar caché";
            if (key.equals("cacheCleared")) return "Caché borrada";
            if (key.equals("backHome")) return "Volver al inicio";
        } else if (language.equals("zh")) {
            if (key.equals("welcome")) return "你的创作空间";
            if (key.equals("homeLead")) return "在一个地方创建、保存和打开你的头像。";
            if (key.equals("heroTag")) return "创作中心";
            if (key.equals("editorsCount")) return "个编辑器";
            if (key.equals("makeAvatar")) return "创建你的下一个头像";
            if (key.equals("heroHint")) return "选择编辑器、从零开始或打开头像库。";
            if (key.equals("create")) return "开始创作";
            if (key.equals("library")) return "头像库";
            if (key.equals("quickActions")) return "快速操作";
            if (key.equals("quickActionsHint")) return "最常用的功能";
            if (key.equals("import")) return "导入";
            if (key.equals("importHint")) return "添加文件";
            if (key.equals("favorites")) return "收藏";
            if (key.equals("favoritesHint")) return "你保存的头像";
            if (key.equals("avatars")) return "头像";
            if (key.equals("editors")) return "编辑器";
            if (key.equals("openEditor")) return "打开编辑器";
            if (key.equals("editorsHint")) return "选择你的创作工具";
            if (key.equals("avatarHub")) return "Avatar Hub";
            if (key.equals("libraryHint")) return "你的内容都在这里";
            if (key.equals("savedItems")) return "个已保存项目";
            if (key.equals("openLibrary")) return "打开头像库";
            if (key.equals("recent")) return "最近创建";
            if (key.equals("recentHint")) return "最新头像会显示在这里";
            if (key.equals("recentTapHint")) return "点击查看头像";
            if (key.equals("recentEmpty")) return "还没有创建内容";
            if (key.equals("recentEmptyHint")) return "从任意编辑器下载一个头像即可。";
            if (key.equals("creatorTip")) return "创作提示";
            if (key.equals("creatorTipTitle")) return "内容会自动整理";
            if (key.equals("creatorTipText")) return "图片、GIF 和视频会自动分类保存。";
            if (key.equals("home")) return "主页";
            if (key.equals("settings")) return "设置";
            if (key.equals("language")) return "语言";
            if (key.equals("openHub")) return "打开 Avatar Hub";
            if (key.equals("clearCache")) return "清除缓存";
            if (key.equals("cacheCleared")) return "缓存已清除";
            if (key.equals("backHome")) return "返回主页";
        } else if (language.equals("ja")) {
            if (key.equals("welcome")) return "クリエイタースペース";
            if (key.equals("homeLead")) return "アバターの作成・保存・起動をひとつに。";
            if (key.equals("heroTag")) return "クリエイターデスク";
            if (key.equals("editorsCount")) return "エディター";
            if (key.equals("makeAvatar")) return "次のアバターを作ろう";
            if (key.equals("heroHint")) return "エディターを選ぶか、ライブラリを開きます。";
            if (key.equals("create")) return "作成を始める";
            if (key.equals("library")) return "ライブラリ";
            if (key.equals("quickActions")) return "クイック操作";
            if (key.equals("quickActionsHint")) return "よく使う機能";
            if (key.equals("import")) return "インポート";
            if (key.equals("importHint")) return "ファイルを追加";
            if (key.equals("favorites")) return "お気に入り";
            if (key.equals("favoritesHint")) return "保存したアバター";
            if (key.equals("avatars")) return "アバター";
            if (key.equals("editors")) return "エディター";
            if (key.equals("openEditor")) return "エディターを開く";
            if (key.equals("editorsHint")) return "使いたい作成ツールを選択";
            if (key.equals("avatarHub")) return "Avatar Hub";
            if (key.equals("libraryHint")) return "コンテンツをまとめて管理";
            if (key.equals("savedItems")) return "件の保存アイテム";
            if (key.equals("openLibrary")) return "ライブラリを開く";
            if (key.equals("recent")) return "最近の作品";
            if (key.equals("recentHint")) return "新しいアバターがここに表示されます";
            if (key.equals("recentTapHint")) return "タップしてプレビュー";
            if (key.equals("recentEmpty")) return "まだ作品がありません";
            if (key.equals("recentEmptyHint")) return "エディターからアバターを保存してみよう。";
            if (key.equals("creatorTip")) return "クリエイターヒント";
            if (key.equals("creatorTipTitle")) return "ファイルは自動で整理されます";
            if (key.equals("creatorTipText")) return "画像、GIF、動画を種類ごとに保存します。";
            if (key.equals("home")) return "ホーム";
            if (key.equals("settings")) return "設定";
            if (key.equals("language")) return "言語";
            if (key.equals("openHub")) return "Avatar Hubを開く";
            if (key.equals("clearCache")) return "キャッシュを削除";
            if (key.equals("cacheCleared")) return "キャッシュを削除しました";
            if (key.equals("backHome")) return "ホームに戻る";
        } else if (language.equals("de")) {
            if (key.equals("welcome")) return "Dein Creator-Bereich";
            if (key.equals("homeLead")) return "Avatare erstellen, speichern und starten – an einem Ort.";
            if (key.equals("heroTag")) return "CREATOR-DESK";
            if (key.equals("editorsCount")) return "Editoren";
            if (key.equals("makeAvatar")) return "Erstelle deinen nächsten Avatar";
            if (key.equals("heroHint")) return "Editor wählen, neu starten oder Bibliothek öffnen.";
            if (key.equals("create")) return "Erstellung starten";
            if (key.equals("library")) return "Bibliothek";
            if (key.equals("quickActions")) return "Schnellaktionen";
            if (key.equals("quickActionsHint")) return "Die wichtigsten Funktionen";
            if (key.equals("import")) return "Importieren";
            if (key.equals("importHint")) return "Dateien hinzufügen";
            if (key.equals("favorites")) return "Favoriten";
            if (key.equals("favoritesHint")) return "Gespeicherte Avatare";
            if (key.equals("avatars")) return "Avatare";
            if (key.equals("editors")) return "Editoren";
            if (key.equals("openEditor")) return "Editor öffnen";
            if (key.equals("editorsHint")) return "Wähle dein Creator-Tool";
            if (key.equals("avatarHub")) return "Avatar Hub";
            if (key.equals("libraryHint")) return "Alle Inhalte an einem Ort";
            if (key.equals("savedItems")) return "gespeicherte Elemente";
            if (key.equals("openLibrary")) return "Bibliothek öffnen";
            if (key.equals("recent")) return "Letzte Kreationen";
            if (key.equals("recentHint")) return "Neue Avatare erscheinen hier";
            if (key.equals("recentTapHint")) return "Tippe auf eine Kreation zum Anzeigen";
            if (key.equals("recentEmpty")) return "Noch keine Kreationen";
            if (key.equals("recentEmptyHint")) return "Lade einen Avatar aus einem Editor herunter.";
            if (key.equals("creatorTip")) return "Creator-Tipp";
            if (key.equals("creatorTipTitle")) return "Alles wird automatisch sortiert";
            if (key.equals("creatorTipText")) return "Bilder, GIFs und Videos werden automatisch abgelegt.";
            if (key.equals("home")) return "Start";
            if (key.equals("settings")) return "Einstellungen";
            if (key.equals("language")) return "Sprache";
            if (key.equals("openHub")) return "Avatar Hub öffnen";
            if (key.equals("clearCache")) return "Cache leeren";
            if (key.equals("cacheCleared")) return "Cache geleert";
            if (key.equals("backHome")) return "Zur Startseite";
        } else if (language.equals("fr")) {
            if (key.equals("welcome")) return "Votre espace créatif";
            if (key.equals("homeLead")) return "Créez, enregistrez et lancez vos avatars au même endroit.";
            if (key.equals("heroTag")) return "ESPACE CRÉATION";
            if (key.equals("editorsCount")) return "éditeurs";
            if (key.equals("makeAvatar")) return "Créez votre prochain avatar";
            if (key.equals("heroHint")) return "Choisissez un éditeur ou ouvrez votre bibliothèque.";
            if (key.equals("create")) return "Commencer";
            if (key.equals("library")) return "Bibliothèque";
            if (key.equals("quickActions")) return "Actions rapides";
            if (key.equals("quickActionsHint")) return "Les fonctions que vous utilisez le plus";
            if (key.equals("import")) return "Importer";
            if (key.equals("importHint")) return "Ajouter des fichiers";
            if (key.equals("favorites")) return "Favoris";
            if (key.equals("favoritesHint")) return "Vos avatars enregistrés";
            if (key.equals("avatars")) return "Avatars";
            if (key.equals("editors")) return "Éditeurs";
            if (key.equals("openEditor")) return "Ouvrir l’éditeur";
            if (key.equals("editorsHint")) return "Choisissez votre outil de création";
            if (key.equals("avatarHub")) return "Avatar Hub";
            if (key.equals("libraryHint")) return "Tout votre contenu au même endroit";
            if (key.equals("savedItems")) return "éléments enregistrés";
            if (key.equals("openLibrary")) return "Ouvrir la bibliothèque";
            if (key.equals("recent")) return "Créations récentes";
            if (key.equals("recentHint")) return "Vos avatars les plus récents apparaîtront ici";
            if (key.equals("recentTapHint")) return "Touchez une création pour la voir";
            if (key.equals("recentEmpty")) return "Aucune création pour le moment";
            if (key.equals("recentEmptyHint")) return "Téléchargez un avatar depuis un éditeur.";
            if (key.equals("creatorTip")) return "Astuce créateur";
            if (key.equals("creatorTipTitle")) return "Tout est organisé automatiquement";
            if (key.equals("creatorTipText")) return "Images, GIF et vidéos sont classés dans leurs dossiers.";
            if (key.equals("home")) return "Accueil";
            if (key.equals("settings")) return "Réglages";
            if (key.equals("language")) return "Langue";
            if (key.equals("openHub")) return "Ouvrir Avatar Hub";
            if (key.equals("clearCache")) return "Vider le cache";
            if (key.equals("cacheCleared")) return "Cache vidé";
            if (key.equals("backHome")) return "Retour à l’accueil";
        } else if (language.equals("pt")) {
            if (key.equals("welcome")) return "Seu espaço criativo";
            if (key.equals("homeLead")) return "Crie, salve e abra seus avatares em um só lugar.";
            if (key.equals("heroTag")) return "CENTRO DE CRIAÇÃO";
            if (key.equals("editorsCount")) return "editores";
            if (key.equals("makeAvatar")) return "Crie seu próximo avatar";
            if (key.equals("heroHint")) return "Escolha um editor ou abra sua biblioteca.";
            if (key.equals("create")) return "Começar a criar";
            if (key.equals("library")) return "Biblioteca";
            if (key.equals("quickActions")) return "Ações rápidas";
            if (key.equals("quickActionsHint")) return "As funções que você mais usa";
            if (key.equals("import")) return "Importar";
            if (key.equals("importHint")) return "Adicionar arquivos";
            if (key.equals("favorites")) return "Favoritos";
            if (key.equals("favoritesHint")) return "Seus avatares salvos";
            if (key.equals("avatars")) return "Avatares";
            if (key.equals("editors")) return "Editores";
            if (key.equals("openEditor")) return "Abrir editor";
            if (key.equals("editorsHint")) return "Escolha sua ferramenta de criação";
            if (key.equals("avatarHub")) return "Avatar Hub";
            if (key.equals("libraryHint")) return "Todo o seu conteúdo em um só lugar";
            if (key.equals("savedItems")) return "itens salvos";
            if (key.equals("openLibrary")) return "Abrir biblioteca";
            if (key.equals("recent")) return "Criações recentes";
            if (key.equals("recentHint")) return "Seus avatares mais novos aparecerão aqui";
            if (key.equals("recentTapHint")) return "Toque para visualizar";
            if (key.equals("recentEmpty")) return "Ainda não há criações";
            if (key.equals("recentEmptyHint")) return "Baixe um avatar de qualquer editor.";
            if (key.equals("creatorTip")) return "Dica do criador";
            if (key.equals("creatorTipTitle")) return "Tudo é organizado automaticamente";
            if (key.equals("creatorTipText")) return "Imagens, GIFs e vídeos são guardados nas pastas certas.";
            if (key.equals("home")) return "Início";
            if (key.equals("settings")) return "Configurações";
            if (key.equals("language")) return "Idioma";
            if (key.equals("openHub")) return "Abrir Avatar Hub";
            if (key.equals("clearCache")) return "Limpar cache";
            if (key.equals("cacheCleared")) return "Cache limpo";
            if (key.equals("backHome")) return "Voltar ao início";
        }

        // English is the source language; keep it explicit so missing keys never leak into the UI.
        if (key.equals("welcome")) return "Creator space";
        if (key.equals("homeLead")) return "Create, save and launch your avatars from one place.";
        if (key.equals("heroTag")) return "CREATOR DESK";
        if (key.equals("editorsCount")) return "editors";
        if (key.equals("makeAvatar")) return "Build your next avatar";
        if (key.equals("heroHint")) return "Pick an editor, start fresh, or open your library.";
        if (key.equals("create")) return "Start creating";
        if (key.equals("library")) return "Library";
        if (key.equals("quickActions")) return "Quick actions";
        if (key.equals("quickActionsHint")) return "Things you use most";
        if (key.equals("import")) return "Import";
        if (key.equals("importHint")) return "Add files";
        if (key.equals("favorites")) return "Favorites";
        if (key.equals("favoritesHint")) return "Your saved avatars";
        if (key.equals("avatars")) return "Avatars";
        if (key.equals("editors")) return "Editors";
        if (key.equals("openEditor")) return "Open editor";
        if (key.equals("editorsHint")) return "Choose where you want to create";
        if (key.equals("avatarHub")) return "Avatar Hub";
        if (key.equals("libraryHint")) return "All your content, organized";
        if (key.equals("savedItems")) return "saved items";
        if (key.equals("openLibrary")) return "Open library";
        if (key.equals("recent")) return "Recent creations";
        if (key.equals("recentHint")) return "Your newest avatars will appear here";
        if (key.equals("recentTapHint")) return "Tap a creation to preview it";
        if (key.equals("recentEmpty")) return "Nothing here yet";
        if (key.equals("recentEmptyHint")) return "Download an avatar from any editor.";
        if (key.equals("creatorTip")) return "Creator tip";
        if (key.equals("creatorTipTitle")) return "Everything stays organized";
        if (key.equals("creatorTipText")) return "Images, GIFs and videos are automatically sorted into folders.";
        if (key.equals("home")) return "Home";
        if (key.equals("settings")) return "Settings";
        if (key.equals("language")) return "Language";
        if (key.equals("openHub")) return "Open Avatar Hub";
        if (key.equals("clearCache")) return "Clear cache";
        if (key.equals("cacheCleared")) return "Cache cleared";
        if (key.equals("backHome")) return "Back to home";
        return key;
    }

    private String languageName() { if (language.equals("es")) return "Español"; if (language.equals("zh")) return "中文"; if (language.equals("ja")) return "日本語"; if (language.equals("de")) return "Deutsch"; if (language.equals("fr")) return "Français"; if (language.equals("pt")) return "Português"; return "English"; }
    private void cycleLanguage() {
        String[] langs={"en","es","zh","ja","de","fr","pt"};
        int i=0; for(int n=0;n<langs.length;n++) if(langs[n].equals(language)) i=n;
        language=langs[(i+1)%langs.length]; prefs.edit().putString("language",language).apply();
        View old=(View)root.getTag(); if(old!=null) root.removeView(old); root.setTag(null); showSettings();
    }

    private void showSettings() {
        launcher.setVisibility(View.GONE);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(24), dp(20), dp(20));
        page.setBackgroundColor(bgColor);

        TextView title = new TextView(this);
        title.setText(t("settings"));
        title.setTextColor(textColor);
        title.setTextSize(28);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        page.addView(title, new LinearLayout.LayoutParams(-1, dp(60)));

        TextView about = new TextView(this);
        about.setText("GrokBot Avatar Hub\n\nCreate, download and organize your avatars from multiple editors in one place.\n\nVersion " + getString(com.websitetodapk.app.R.string.app_name));
        about.setTextColor(withAlpha(textColor, 210));
        about.setTextSize(15);
        page.addView(about, new LinearLayout.LayoutParams(-1, dp(150)));

        TextView languageButton = new TextView(this);
        styleActionButton(languageButton, t("language") + ": " + languageName());
        languageButton.setOnClickListener(v -> cycleLanguage());
        page.addView(languageButton, new LinearLayout.LayoutParams(-1, dp(52)));
        
        TextView hub = new TextView(this);
        styleActionButton(hub, t("openHub"));
        hub.setOnClickListener(v -> pressAnimation(hub, v2 -> { root.removeView(page); showGallery(); }));
        page.addView(hub, new LinearLayout.LayoutParams(-1, dp(52)));

        TextView clear = new TextView(this);
        styleActionButton(clear, t("clearCache"));
        clear.setOnClickListener(v -> pressAnimation(clear, v2 -> {
            if (webView != null) webView.clearCache(true);
            Toast.makeText(this, t("cacheCleared"), Toast.LENGTH_SHORT).show();
        }));
        page.addView(clear, new LinearLayout.LayoutParams(-1, dp(52)));

        TextView back = new TextView(this);
        styleActionButton(back, t("backHome"));
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
