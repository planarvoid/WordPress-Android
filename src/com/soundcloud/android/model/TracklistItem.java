package com.soundcloud.android.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.Consts;
import com.soundcloud.android.provider.DatabaseHelper.Content;
import com.soundcloud.android.provider.DatabaseHelper.Tables;
import com.soundcloud.android.provider.DatabaseHelper.TrackPlays;
import com.soundcloud.android.provider.DatabaseHelper.Tracks;
import com.soundcloud.android.task.LoadCommentsTask;
import com.soundcloud.android.task.LoadTrackInfoTask;
import com.soundcloud.android.utils.CloudUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings({"UnusedDeclaration"})
@JsonIgnoreProperties(ignoreUnknown=true)
public class TracklistItem {
    private static final String TAG = "TracklistItem";

    public long id;
    public String title;
    public long user_id;
    public User user;
    public Date created_at;
    public int duration;
    public String permalink;
    public String sharing;
    public boolean commentable;
    public boolean streamable;
    public String artwork_url;
    public String waveform_url;
    public String stream_url;

}
