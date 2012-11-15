package com.soundcloud.android.model;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.Content;

import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 Suggestions from the /search/suggest endpoint.

 <pre>
   {
     "tx_id" : "92dbb484c0d144afa6c193ece99514f3",
     "query_time_in_millis" : 0,
     "query" : "f",
     "limit" : 5,
     "suggestions" : [
        {
         "query" : "Foo Fighters",
         "kind" : "user",
         "id" : 2097360,
         "score" : 889523
        }, ...
     ]
   }
 </pre>

 @see <a href="https://github.com/soundcloud/search-suggest#setting-up-the-suggest-engine">search-suggest</a>
 */
public class SearchSuggestions implements Iterable<SearchSuggestions.Query> {
    public String tx_id;
    public long query_time_in_millis;
    public String query;
    public int limit;

    public List<Query> suggestions;

    public void putMissingIds(List<Long> missingTracks, List<Long> missingUsers) {
        for (Query q : this) {
            if (q.getIconUri() == null) {
                if (q.isUser()) {
                    missingUsers.add(q.id);
                } else {
                    missingTracks.add(q.id);
                }
            }
        }
    }

    @Override
    public Iterator<Query> iterator() {
        return suggestions.iterator();
    }

    public static class Query {
        public String query;
        public String kind;
        public long id;
        public long score;

        public String getClientUri() {
            return new ClientUri("soundcloud:" + ("user".equals(kind) ? "users" : "tracks") + ":" + id).toString();
        }

        public String getIconUri() {
            if (isUser()) {
                final User user = SoundCloudApplication.MODEL_MANAGER.getUser(id);
                return user == null ? null : user.avatar_url;
            } else {
                final Track track = SoundCloudApplication.MODEL_MANAGER.getTrack(id);
                return track == null ? null : track.artwork_url;
            }
        }

        public boolean isUser(){
            return "user".equals(kind);
        }


        public String getUriPath() {
            return ("user".equals(kind) ? Content.USER.forId(id).toString() : Content.TRACK.forId(id).toString());
        }

        @Override
        public String toString() {
            return "Query{" +
                    "query='" + query + '\'' +
                    ", kind='" + kind + '\'' +
                    ", id=" + id +
                    ", score=" + score +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "SearchSuggestions{" +
                "query_time_in_millis=" + query_time_in_millis +
                ", query='" + query + '\'' +
                ", limit=" + limit +
                ", suggestions=" + suggestions +
                '}';
    }
}
