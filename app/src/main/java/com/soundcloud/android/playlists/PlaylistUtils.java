
package com.soundcloud.android.playlists;

import static com.soundcloud.android.utils.DateUtils.yearFromDateString;

import com.soundcloud.android.R;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.strings.Strings;

import android.content.res.Resources;

import java.text.ParseException;
import java.util.List;
import java.util.concurrent.TimeUnit;

class PlaylistUtils {

    static String getPlaylistInfoLabel(Resources resources,
                                       int trackCount,
                                       String duration) {

        final String trackCountFormatted = resources.getQuantityString(
                R.plurals.number_of_sounds, trackCount, trackCount);
        return resources.getString(R.string.playlist_new_info_header_text_trackcount_duration,
                                 trackCountFormatted, duration);
    }

    static String getDuration(Playlist playlist, List<TrackItem> tracks) {
        final long duration = tracks.isEmpty() ?
                              playlist.duration() :
                              getCombinedTrackDurations(tracks);
        return ScTextUtils.formatTimestamp(duration, TimeUnit.MILLISECONDS);
    }

    static long getCombinedTrackDurations(List<TrackItem> tracks) {
        long duration = 0;
        for (TrackItem track : tracks) {
            duration += track.getDuration();
        }
        return duration;
    }


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
