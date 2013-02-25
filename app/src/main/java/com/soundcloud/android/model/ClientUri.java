package com.soundcloud.android.model;

import com.soundcloud.android.provider.Content;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.Intent;
import android.net.Uri;

/**
 * Models a SoundCloud client uri
 * @see <a href="https://soundcloudnet-main.pbworks.com/w/page/53336406/Client%20URL%20Scheme">Client URL Scheme</a>
 */
public class ClientUri {
    public @NotNull final Uri uri;
    public @NotNull final String type;
    public @NotNull final String id;
    public final long numericId;

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
            long n = -1;
            try {
                n = Long.parseLong(id);
            } catch (NumberFormatException ignored) {
            }
            numericId = n;
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

    public Uri contentProviderUri() {
        return isUser() ? Content.USER.forId(numericId) : Content.TRACK.forId(numericId);
    }

    public Uri imageUri() {
        return Uri.parse("https://api.soundcloud.com/resolve/image")
                .buildUpon()
                .appendQueryParameter("url", toString())
                .appendQueryParameter("client_id", "40ccfee680a844780a41fbe23ea89934")
                .build();
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
        return Uri.parse("soundcloud:sounds:"+id);
    }

    public static Uri forUser(long id) {
        return Uri.parse("soundcloud:users:"+id);
    }

    @Override
    public String toString() {
        return uri.toString();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientUri clientUri = (ClientUri) o;
        return uri.equals(clientUri.uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }
}
