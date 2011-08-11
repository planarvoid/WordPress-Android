package com.soundcloud.android.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.Date;

@SuppressWarnings({"UnusedDeclaration"})
@JsonIgnoreProperties(ignoreUnknown=true)
public class TracklistItem {
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
    public boolean user_favorite;

    public int playback_count;
    public int comment_count;
    public int favoritings_count;
}
