package com.soundcloud.android.model;

import com.soundcloud.android.provider.Content;

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

        public String getUri() {
            return ("user".equals(kind) ? Content.USERS.forId(id) : Content.TRACKS.forId(id)).toString();
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
