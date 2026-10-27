package com.gathin.tvhome;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The panel the remote's Settings button opens (settings_key_panel, HomeKeyService) and Home's bell opens
 * (notifications_button), shaped like Google TV's own, which left with its stock launcher: a tall drawer on the right
 * with the date and time, a grid of quick tiles, the notifications as cards and Clear all. Translucent and in a task
 * of its own, so the app behind keeps running and Back returns to it. Pressing Settings again closes it.
 */
public final class QuickSettingsActivity extends Activity {
    /** Opened from Home's bell: focus starts on the first notification instead of the first tile. */
    static final String EXTRA_NOTIFICATIONS = "com.gathin.tvhome.extra.NOTIFICATIONS";
    private static final int MAX_CARDS = 8;

    private static final int CARD = 0xFF2E333D, CARD_FOCUSED = 0xFFE9EEF6;
    private static final int TEXT = 0xFFFFFFFF, TEXT_FOCUSED = 0xFF141922;
    private static final int SUB = 0xB3FFFFFF, SUB_FOCUSED = 0xFF4B525D;
    private static final int BADGE = 0xFF3B414C, BADGE_FOCUSED = 0xFFD3DAE6;

    private float dp;
    private LinearLayout cards;
    private View firstTile;

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // locale
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_panel);
        dp = getResources().getDisplayMetrics().density;
        findViewById(R.id.scrim).setOnClickListener(v -> finish());
        findViewById(R.id.drawer).setOnClickListener(v -> { });   // a click on the drawer itself does not close it

        TextClock date = findViewById(R.id.panelDate);
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEMMMd");
        date.setFormat12Hour(pattern);
        date.setFormat24Hour(pattern);
        ImageView gear = findViewById(R.id.panelGear);
        gear.setBackground(states(0x00000000, CARD_FOCUSED, 20 * dp));
        gear.setImageTintList(colors(TEXT, TEXT_FOCUSED));
        gear.setOnClickListener(v -> open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));

        List<View> tiles = new ArrayList<>();
        tiles.add(tile(R.drawable.ic_screensaver, R.string.quick_screensaver, () -> open(new Intent(Intent.ACTION_MAIN)
                .setClassName("com.android.systemui", "com.android.systemui.Somnambulator"))));
        tiles.add(tile(R.drawable.ic_wifi, R.string.quick_network,
                () -> open(SystemSettings.intent(this, Settings.ACTION_WIFI_SETTINGS))));
        tiles.add(tile(R.drawable.ic_bluetooth, R.string.quick_bluetooth,
                () -> open(SystemSettings.intent(this, Settings.ACTION_BLUETOOTH_SETTINGS))));
        tiles.add(tile(R.drawable.ic_picture, R.string.quick_picture,
                () -> open(SystemSettings.intent(this, Settings.ACTION_DISPLAY_SETTINGS))));
        tiles.add(tile(R.drawable.ic_system_update, R.string.system_update,
                () -> open(SystemSettings.intent(this, "android.settings.SYSTEM_UPDATE_SETTINGS"))));
        // Through Home, so it rebuilds with the new choices when Launcher settings closes.
        tiles.add(tile(R.drawable.ic_home_settings, R.string.launcher_settings,
                () -> open(new Intent(this, MainActivity.class).setAction(MainActivity.ACTION_LAUNCHER_SETTINGS))));
        LinearLayout grid = findViewById(R.id.tiles);
        for (int i = 0; i < tiles.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, Math.round(58 * dp), 1f);
            row.addView(tiles.get(i), half);
            LinearLayout.LayoutParams second = new LinearLayout.LayoutParams(0, Math.round(58 * dp), 1f);
            second.setMarginStart(Math.round(10 * dp));
            row.addView(i + 1 < tiles.size() ? tiles.get(i + 1) : new View(this), second);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = Math.round(10 * dp);
            grid.addView(row, rowLp);
        }
        firstTile = tiles.get(0);

        cards = findViewById(R.id.cards);
        boolean notifications;
        try {
            notifications = Config.load(this).notificationsButton;
        } catch (RuntimeException e) {
            notifications = false;
        }
        if (notifications) fillCards();
        else cards.setVisibility(View.GONE);

        View first = firstTile;
        if (getIntent().getBooleanExtra(EXTRA_NOTIFICATIONS, false) && cards.getChildCount() > 0
                && cards.getChildAt(0).isFocusable()) first = cards.getChildAt(0);
        first.requestFocus();
    }

    /** The notification cards, newest first, then Clear all; a quiet line when there are none. */
    private void fillCards() {
        cards.removeAllViews();
        List<StatusBarNotification> list = GuardRevival.active(this);
        PackageManager pm = getPackageManager();
        for (int i = 0; i < list.size() && i < MAX_CARDS; i++) {
            StatusBarNotification sbn = list.get(i);
            Notification n = sbn.getNotification();
            Drawable icon;
            try {
                icon = pm.getApplicationIcon(sbn.getPackageName());
            } catch (PackageManager.NameNotFoundException e) {
                icon = getDrawable(R.drawable.ic_bell);
            }
            View card = card(icon, false, n.extras.getCharSequence(Notification.EXTRA_TITLE),
                    n.extras.getCharSequence(Notification.EXTRA_TEXT));
            String pkg = sbn.getPackageName(), key = sbn.getKey();
            PendingIntent target = n.contentIntent;
            boolean autoCancel = (n.flags & Notification.FLAG_AUTO_CANCEL) != 0;
            card.setOnClickListener(v -> {
                try {
                    if (target != null) target.send();
                    else open(pm.getLeanbackLaunchIntentForPackage(pkg));
                } catch (PendingIntent.CanceledException e) {
                    open(pm.getLeanbackLaunchIntentForPackage(pkg));
                }
                if (autoCancel) GuardRevival.cancel(key);
                finish();
            });
            cards.addView(card);
        }
        if (list.isEmpty()) {
            TextView none = new TextView(this);
            none.setText(R.string.no_notifications);
            none.setTextColor(SUB);
            none.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            none.setTypeface(UiFont.get(this, R.string.font_regular));
            none.setPadding(Math.round(6 * dp), Math.round(6 * dp), 0, 0);
            cards.addView(none);
            return;
        }
        View clear = card(getDrawable(R.drawable.ic_clear_all), true, getString(R.string.clear_all), null);
        clear.setOnClickListener(v -> {
            GuardRevival.clearAll();
            firstTile.requestFocus();
            cards.postDelayed(this::fillCards, 400);
        });
        cards.addView(clear);
    }

    /** A quick tile: icon and label on a rounded card that turns light when focused. */
    private View tile(int icon, int label, Runnable action) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        int pad = Math.round(14 * dp);
        tile.setPadding(pad, 0, pad, 0);
        tile.setBackground(states(CARD, CARD_FOCUSED, 16 * dp));
        tile.setFocusable(true);
        tile.setClickable(true);
        ImageView image = new ImageView(this);
        image.setImageResource(icon);
        image.setImageTintList(colors(TEXT, TEXT_FOCUSED));
        image.setDuplicateParentStateEnabled(true);
        int size = Math.round(20 * dp);
        tile.addView(image, new LinearLayout.LayoutParams(size, size));
        TextView text = label(getString(label), 14, R.string.font_medium, TEXT, TEXT_FOCUSED);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMarginStart(Math.round(10 * dp));
        tile.addView(text, lp);
        tile.setOnClickListener(v -> action.run());
        return tile;
    }

    /** A notification card: the app's icon on a round badge, the title and up to two lines of text. */
    private View card(Drawable icon, boolean tinted, CharSequence title, CharSequence text) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        int pad = Math.round(12 * dp);
        card.setPadding(pad, pad, pad, pad);
        card.setBackground(states(CARD, CARD_FOCUSED, 18 * dp));
        card.setFocusable(true);
        card.setClickable(true);
        FrameLayout badge = new FrameLayout(this);
        badge.setBackground(states(BADGE, BADGE_FOCUSED, 18 * dp));
        badge.setDuplicateParentStateEnabled(true);
        ImageView image = new ImageView(this);
        image.setImageDrawable(icon);
        if (tinted) image.setImageTintList(colors(TEXT, TEXT_FOCUSED));
        image.setDuplicateParentStateEnabled(true);
        int inner = Math.round(20 * dp);
        badge.addView(image, new FrameLayout.LayoutParams(inner, inner, Gravity.CENTER));
        int outer = Math.round(36 * dp);
        card.addView(badge, new LinearLayout.LayoutParams(outer, outer));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setDuplicateParentStateEnabled(true);
        texts.addView(label(title, 14, R.string.font_medium, TEXT, TEXT_FOCUSED));
        if (!TextUtils.isEmpty(text)) {
            TextView body = label(text, 12, R.string.font_regular, SUB, SUB_FOCUSED);
            body.setSingleLine(false);
            body.setMaxLines(2);
            texts.addView(body);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMarginStart(Math.round(12 * dp));
        card.addView(texts, lp);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = Math.round(10 * dp);
        card.setLayoutParams(cardLp);
        return card;
    }

    private TextView label(CharSequence value, int sp, int font, int color, int focused) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        text.setTypeface(UiFont.get(this, font));
        text.setTextColor(colors(color, focused));
        text.setSingleLine(true);
        text.setEllipsize(TextUtils.TruncateAt.END);
        text.setDuplicateParentStateEnabled(true);
        return text;
    }

    private static ColorStateList colors(int normal, int focused) {
        return new ColorStateList(new int[][]{{android.R.attr.state_focused}, {android.R.attr.state_pressed}, {}},
                new int[]{focused, focused, normal});
    }

    private static StateListDrawable states(int normal, int focused, float radius) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, round(focused, radius));
        states.addState(new int[]{android.R.attr.state_pressed}, round(focused, radius));
        states.addState(new int[]{}, round(normal, radius));
        return states;
    }

    private static GradientDrawable round(int color, float radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radius);
        return shape;
    }

    private void open(Intent target) {
        try {
            if (target != null) startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            else Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    /** Settings (or the bell) pressed again while the panel is up. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        finish();
    }

    /** Anything else taking the screen (Home, an app, a screensaver) closes the panel. */
    @Override
    protected void onPause() {
        super.onPause();
        finish();
        overridePendingTransition(0, 0);
    }
}
