package com.soundcloud.android.users;

import com.soundcloud.android.R;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.DrawableRes;

import java.util.Arrays;

enum Network {
    APPLE_MUSIC("itunes_podcast", Optional.of("iTunes Podcast"), R.drawable.favicon_applemusic),
    BANDCAMP("bandcamp", Optional.of("Bandcamp"), R.drawable.favicon_bandcamp),
    BANDS_IN_TOWN("bandsintown", Optional.of("Bandsintown"), R.drawable.favicon_bandsintown),
    DISCOGS("discogs", Optional.of("Discogs"), R.drawable.favicon_discogs),
    EMAIL("email", Optional.absent(), R.drawable.favicon_email),
    FACEBOOK("facebook", Optional.of("Facebook"), R.drawable.favicon_fb),
    FLICKR("flickr", Optional.of("Flickr"), R.drawable.favicon_flickr),
    GOOGLE_PLUS("google_plus", Optional.of("Google+"), R.drawable.favicon_gplus),
    INSTAGRAM("instagram", Optional.of("Instagram"), R.drawable.favicon_instagram),
    LASTFM("lastfm", Optional.of("Last.fm"), R.drawable.favicon_lastfm),
    MIXCLOUD("mixcloud", Optional.of("Mixcloud"), R.drawable.favicon_mixcloud),
    PINTEREST("pinterest", Optional.of("Pinterest"), R.drawable.favicon_pinterest),
    RESIDENTADVISOR("residentadvisor", Optional.of("Resident Advisor"), R.drawable.favicon_residentadvisor),
    REVERBNATION("reverbnation", Optional.of("ReverbNation"), R.drawable.favicon_reverbnation),
    SONGKICK("songkick", Optional.of("Songkick"), R.drawable.favicon_songkick),
    SOUNDCLOUD("soundcloud", Optional.of("SoundCloud"), R.drawable.favicon_sc),
    SNAPCHAT("snapchat", Optional.of("Snapchat"), R.drawable.favicon_snap),
    SPOTIFY("spotify", Optional.of("Spotify"), R.drawable.favicon_spotify),
    TUMBLR("tumblr", Optional.of("Tumblr"), R.drawable.favicon_tumblr),
    TWITTER("twitter", Optional.of("Twitter"), R.drawable.favicon_twitter),
    VIMEO("vimeo", Optional.of("Vimeo"), R.drawable.favicon_vimeo),
    YOUTUBE("youtube", Optional.of("YouTube"), R.drawable.favicon_youtube),
    PERSONAL("personal", Optional.absent(), R.drawable.favicon_generic);

    private final String network;
    private final Optional<String> displayName;
    private final int drawableId;

    Network(String network, Optional<String> displayName, @DrawableRes int drawableId) {
        this.network = network;
        this.displayName = displayName;
        this.drawableId = drawableId;
    }

    static Network from(String network) {
        return Iterables.find(Arrays.asList(Network.values()), item -> item.network.equals(network), PERSONAL);
    }

    public Optional<String> displayName() {
        return displayName;
    }

    @DrawableRes
    public int drawableId() {
        return drawableId;
    }

    public boolean isEmail() {
        return network.equals(Network.EMAIL.network);
    }
}
