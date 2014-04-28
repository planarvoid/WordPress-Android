package com.soundcloud.android.utils;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
 * @author Karsten Priegnitz
 * @see <a href="http://code.google.com/p/android-change-log/">http://code.google.com/p/android-change-log</a>
 */
public class ChangeLog {

    private final Context context;

    private static final String CONTENT = "__CHANGELOG_CONTENT__";

    private Listmode listMode = Listmode.NONE;
    private StringBuilder builder;

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
        this.context = context;
    }

    public AlertDialog getDialog() {
        WebView wv = new WebView(context);
        wv.setBackgroundColor(0xFFFFFFFF); // transparent
        wv.loadData(getLog(), "text/html", "UTF-8");

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.changelog_full_title))
                .setView(wv)
                .setCancelable(false)
                .setPositiveButton(
                        android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        return builder.create();
    }

    private String getLog() {
        return wrapLog(getLogContent());
    }

    private String wrapLog(String s) {
        StringBuilder wrapped = new StringBuilder();
        InputStream ins = context.getResources().openRawResource(R.raw.changelog_template);
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

    private String getLogContent() {
        builder = new StringBuilder();
        try
        {
            InputStream ins = context.getResources().openRawResource(R.raw.changelog);
            BufferedReader br = new BufferedReader(new InputStreamReader(ins));

            String line;
            boolean advanceToEOVS = false; // true = ignore further version sections
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("$")) {
                    // begin of a version section
                    closeList();
                } else if (!advanceToEOVS) {
                    // line contains version title
                    if (line.startsWith("%")) {
                        closeList();
                        String version = line.substring(1).trim();
                        if ("v current".equals(version)) {
                            version = "v " + new DeviceHelper(context).getAppVersion();
                        }
                        builder.append("<div class='title'>").append(version).append("</div>\n");
                    // line contains date
                    } else if (line.startsWith("_")) {
                        closeList();
                        builder.append("<div class='subtitle'>").append(line.substring(1).trim()).append("</div>\n");
                    // line contains free text
                    } else if (line.startsWith("!")) {
                        closeList();
                        builder.append("<div class='freetext'>").append(line.substring(1).trim()).append("</div>\n");
                    // line contains numbered list item
                    } else if (line.startsWith("#")) {
                        openList(Listmode.ORDERED);
                        builder.append("<li>").append(line.substring(1).trim()).append("</li>\n");
                    // line contains bullet list item
                    } else if (line.startsWith("*")) {
                        openList(Listmode.UNORDERED);
                        builder.append("<li>").append(line.substring(1).trim()).append("</li>\n");
                    } else if (line.startsWith("-")) {
                        // private changelog entry, skip
                    } else {
                        // no special character: just use line as is
                        closeList();
                        builder.append(line).append("\n");
                    }
                }
            }
            closeList();
            br.close();
        } catch (IOException e) {
            Log.w(TAG, "error", e);
        }
        return builder.toString();
    }

    private void openList(Listmode listMode) {
        if (this.listMode != listMode) {
            closeList();
            switch (listMode) {
                case ORDERED:
                    builder.append("<div class='list'><ol>\n");
                    break;
                case NONE:
                    break;
                case UNORDERED:
                    builder.append("<div class='list'><ul>\n");
                    break;
            }
            this.listMode = listMode;
        }
    }

    private void closeList() {
        switch (listMode) {
            case NONE:
                break;
            case ORDERED:
                builder.append("</ol></div>\n");
                break;
            case UNORDERED:
                builder.append("</ul></div>\n");
                break;
        }
        listMode = Listmode.NONE;
    }
}