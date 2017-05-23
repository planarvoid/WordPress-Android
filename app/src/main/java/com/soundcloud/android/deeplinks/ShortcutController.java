package com.soundcloud.android.deeplinks;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper.ExperimentString;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

public class ShortcutController {

    private final Context context;
    private final ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;

    @Inject
    public ShortcutController(Context context, ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper) {
        this.context = context;
        this.changeLikeToSaveExperimentStringHelper = changeLikeToSaveExperimentStringHelper;
    }

    @SuppressLint("NewApi")
    public void createShortcuts() {
        if (missingSdk()) {
            return;
        }

        List<ShortcutInfo> shortcuts = Arrays.asList(
                createSearchShortcut(),
                createShuffleLikesShortcut()
        );

        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        shortcutManager.setDynamicShortcuts(shortcuts);
    }

    @SuppressLint("NewApi")
    public void removeShortcuts() {
        if (missingSdk()) {
            return;
        }

        if (hasShortcuts()) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            shortcutManager.removeAllDynamicShortcuts();
        }
    }

    @SuppressLint("NewApi")
    public void reportUsage(Shortcut shortcut) {
        if (missingSdk()) {
            return;
        }

        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        shortcutManager.reportShortcutUsed(shortcut.id);
    }


    @TargetApi(Build.VERSION_CODES.N_MR1)
    private ShortcutInfo createSearchShortcut() {
        Icon icon = Icon.createWithResource(context, R.drawable.ic_shortcut_search);

        return new ShortcutInfo.Builder(context, Shortcut.SEARCH.id)
                .setShortLabel(context.getString(R.string.shortcut_search))
                .setLongLabel(context.getString(R.string.shortcut_search))
                .setIcon(icon)
                .setIntent(new Intent(Actions.SHORTCUT_SEARCH))
                .build();
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private ShortcutInfo createShuffleLikesShortcut() {
        Icon icon = Icon.createWithResource(context, R.drawable.ic_shortcut_collection);

        return new ShortcutInfo.Builder(context, Shortcut.PLAY_LIKES.id)
                .setShortLabel(context.getString(changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.SHORTCUT_PLAY_LIKES)))
                .setLongLabel(context.getString(changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.SHORTCUT_PLAY_LIKES)))
                .setIcon(icon)
                .setIntent(new Intent(Actions.SHORTCUT_PLAY_LIKES))
                .build();
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private boolean hasShortcuts() {
        if (missingSdk()) {
            return false;
        }
        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        return !shortcutManager.getDynamicShortcuts().isEmpty();
    }

    private boolean missingSdk() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1;
    }

    public enum Shortcut {
        SEARCH("search"),
        PLAY_LIKES("play_likes");

        public final String id;

        Shortcut(String id) {
            this.id = id;
        }
    }
}
