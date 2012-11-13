package com.soundcloud.android.model;

import com.soundcloud.android.view.adapter.UserlistRow;
import org.jetbrains.annotations.Nullable;

import android.content.Intent;
import android.net.Uri;

/**
 * Models a SoundCloud client uri
 * @see <a href="https://soundcloudnet-main.pbworks.com/w/page/53336406/Client%20URL%20Scheme">Client URL Scheme</a>
 */
public class ClientUri {
    public final Uri uri;
    public final String type;
    public final String id;

    public ClientUri(String uri) {
        this(Uri.parse(uri));
    }

    public ClientUri(Uri uri) {
        if (!"soundcloud".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("not a soundcloud uri");
        }
        final String specific = uri.getSchemeSpecificPart();
        final String[] components = specific.split(":", 2);
        if (components != null && components.length == 2) {
            type = components[0];
            id = components[1];
        } else {
            throw new IllegalArgumentException("invalid uri: "+uri);
        }
        this.uri = uri;
    }

    public Intent getViewIntent() {
        return new Intent(Intent.ACTION_VIEW).setData(uri);
    }

    public boolean isSound() {
        return "tracks".equalsIgnoreCase(type) || "sounds".equalsIgnoreCase(type);
    }

    public boolean isUser() {
        return "users".equalsIgnoreCase(type);
    }

    public static @Nullable ClientUri fromUri(String uri) {
        return fromUri(Uri.parse(uri));
    }

    public static @Nullable ClientUri fromUri(Uri uri) {
        try {
            return new ClientUri(uri);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Uri forTrack(long id) {
        return Uri.parse("soundcloud:tracks:"+id);
    }

    public static Uri forUser(long id) {
        return Uri.parse("soundcloud:users:"+id);
    }

    @Override
    public String toString() {
        return uri.toString();
    }
}
