package com.soundcloud.android.utils;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Copyright (C) 2011, Karsten Priegnitz
 * <p/>
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * @author: Karsten Priegnitz
 * see: http://code.google.com/p/android-change-log/
 */
public class ChangeLog {
    private final Context mContext;
    private int mOldVersion;
    private int mThisVersion;

    // this is the key for storing the version name in SharedPreferences
    private static final String VERSION_KEY = "changeLogVersionCode";
    private static final String CONTENT = "__CHANGELOG_CONTENT__";

    private Listmode mListMode = Listmode.NONE;
    private StringBuilder mSb;

    private enum Listmode {
        NONE,
        ORDERED,
        UNORDERED,
    }

    /**
     * Retrieves the version names and stores the new version name in
     * SharedPreferences
     * @param context the context
     */
    public ChangeLog(Context context) {
        mContext = context;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        mOldVersion = sp.getInt(VERSION_KEY, 0);
        mThisVersion = CloudUtils.getAppVersionCode(context, 0);
        if (mThisVersion != 0) {
            sp.edit().putInt(VERSION_KEY, mThisVersion).commit();
        }
    }

    /**
     * @return true if this version of your app is started the first time
     */
    public boolean isFirstRun() {
        return mOldVersion < mThisVersion;
    }


    public AlertDialog getDialog(boolean full) {
        WebView wv = new WebView(mContext);
        wv.setBackgroundColor(0xFFFFFFFF); // transparent
        wv.loadData(getLog(full), "text/html", "UTF-8");

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getResources().getString(
                full
                        ? R.string.changelog_full_title
                        : R.string.changelog_title))
                .setView(wv)
                .setCancelable(false)
                .setPositiveButton(
                        mContext.getResources().getString(R.string.btn_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        return builder.create();
    }

    private String getLog(boolean full) {
        return wrapLog(getLogContent(full));
    }

    private String wrapLog(String s) {
        StringBuilder wrapped = new StringBuilder();
        InputStream ins = mContext.getResources().openRawResource(R.raw.changelog_template);
        BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.contains(CONTENT)) {
                    wrapped.append(line);
                } else {
                    wrapped.append(s);
                }
            }
            return wrapped.toString();
        } catch (IOException e) {
            Log.w(TAG, "error", e);
            return s;
        }
    }

    private String getLogContent(boolean full) {
        mSb = new StringBuilder();
        try
        {
            InputStream ins = mContext.getResources().openRawResource(R.raw.changelog);
            BufferedReader br = new BufferedReader(new InputStreamReader(ins));

            String line;
            boolean advanceToEOVS = false; // true = ignore further version sections
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("$")) {
                    // begin of a version section
                    closeList();
                    String version = line.substring(1).trim();
                    // stop output?
                    if (!full) {
                        advanceToEOVS = Integer.toString(mOldVersion).equals(version);
                    }
                } else if (!advanceToEOVS) {
                    // line contains version title
                    if (line.startsWith("%")) {
                        closeList();
                        String version = line.substring(1).trim();
                        if ("v current".equals(version)) {
                            version = "v " + CloudUtils.getAppVersion(mContext, "unknown");
                        }
                        mSb.append("<div class='title'>").append(version).append("</div>\n");
                    // line contains version title
                    } else if (line.startsWith("_")) {
                        closeList();
                        mSb.append("<div class='subtitle'>").append(line.substring(1).trim()).append("</div>\n");
                    // line contains free text
                    } else if (line.startsWith("!")) {
                        closeList();
                        mSb.append("<div class='freetext'>").append(line.substring(1).trim()).append("</div>\n");
                    // line contains numbered list item
                    } else if (line.startsWith("#")) {
                        openList(Listmode.ORDERED);
                        mSb.append("<li>").append(line.substring(1).trim()).append("</li>\n");
                    // line contains bullet list item
                    } else if (line.startsWith("*")) {
                        openList(Listmode.UNORDERED);
                        mSb.append("<li>").append(line.substring(1).trim()).append("</li>\n");
                    } else if (line.startsWith("-")) {
                        // private changelog entry, skip
                    } else {
                        // no special character: just use line as is
                        closeList();
                        mSb.append(line).append("\n");
                    }
                }
            }
            closeList();
            br.close();
        } catch (IOException e) {
            Log.w(TAG, "error", e);
        }
        return mSb.toString();
    }

    private void openList(Listmode listMode) {
        if (mListMode != listMode) {
            closeList();
            switch (listMode) {
                case ORDERED:
                    mSb.append("<div class='list'><ol>\n");
                    break;
                case NONE:
                    break;
                case UNORDERED:
                    mSb.append("<div class='list'><ul>\n");
                    break;
            }
            mListMode = listMode;
        }
    }

    private void closeList() {
        switch (mListMode) {
            case NONE:
                break;
            case ORDERED:
                mSb.append("</ol></div>\n");
                break;
            case UNORDERED:
                mSb.append("</ul></div>\n");
                break;
        }
        mListMode = Listmode.NONE;
    }
}