package com.soundcloud.android.playlists;

import static com.soundcloud.android.utils.DateUtils.yearFromDateString;

import com.soundcloud.java.strings.Strings;

import android.content.res.Resources;

import java.text.ParseException;

class PlaylistUtils {
    static String formatPlaylistTitle(Resources resources, String playableType, boolean album, String releaseDate) {
        final String title = resources.getString(PlaylistItem.getSetTypeTitle(playableType));

        if (!album) {
            return title;
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append(title);
            String releaseYear = releaseYear(releaseDate);
            if (!releaseYear.isEmpty()) {
                builder.append(String.format(" · %s", releaseYear));
            }
            return builder.toString();
        }
    }

    static String releaseYear(String releaseDate) {
        if (releaseDate.isEmpty()) return Strings.EMPTY;

        try {
            return Integer.toString(yearFromDateString(releaseDate, "yyyy-MM-dd"));
        } catch (ParseException e) {
            return Strings.EMPTY;
        }
    }
}
