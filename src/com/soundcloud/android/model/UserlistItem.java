package com.soundcloud.android.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@SuppressWarnings({"UnusedDeclaration"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserlistItem extends ScModel{
    public long id;
    public String username;
    public int track_count;
    public String city;
    public String avatar_url;
    public String permalink;
    public String full_name;
    public int followers_count;
    public int followings_count;
    public int public_favorites_count;
    public int private_tracks_count;
    public String country;
}