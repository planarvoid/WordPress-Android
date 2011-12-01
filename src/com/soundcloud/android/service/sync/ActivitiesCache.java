package com.soundcloud.android.service.sync;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.utils.CloudUtils.md5;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.Event;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.accounts.Account;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ActivitiesCache {
    private static final String PREFIX = "activities-";

    public static Activities get(SoundCloudApplication context,
                          Account account,
                          final Request request) throws IOException {

        final File cachedFile = new File(context.getCacheDir(),
                PREFIX+md5(account == null ? "" : account.name+request.toUrl())+".json");

        Activities activities;
        try {
            if (cachedFile.exists()) {
                Activities cached = Activities.fromJSON(cachedFile);
                String future_href = cached.future_href;

                Log.d(TAG, "read from activities cache "+cachedFile+
                        ", requesting updates from " +(future_href == null ? request.toUrl() : future_href));

                if (future_href != null) {
                    Activities updates = getEvents(context, Request.to(future_href));
                    activities = updates.merge(cached);
                } else {
                    activities = cached;
                }
            } else {
              activities = getEvents(context, request);
            }
        } catch (IOException e) {
            Log.w(TAG, "error", e);
            // fallback, load events from normal resource
            activities = getEvents(context, request);
        }

        activities.toJSON(cachedFile, Views.Mini.class);
        Log.d(TAG, "cached activities to "+cachedFile);
        return activities;
    }

    private static Activities getEvents(SoundCloudApplication app, final Request resource)
            throws IOException {
        String future_href = null;
        String next_href = null;
        List<Event> events = new ArrayList<Event>();

        Request request = resource.add("limit", 20);
        do {
            HttpResponse response = app.get(request);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                Activities activities = Activities.fromJSON(response.getEntity().getContent());
                request = activities.hasMore() ? activities.getNextRequest() : null;

                if (next_href == null){
                    Log.i("asdf","Storing next href " + activities.next_href);
                    next_href = activities.next_href;
                } else {
                    Log.i("asdf","Storing next href " + activities.next_href + " in " + activities.get(activities.size()-1));
                    activities.get(activities.size()-1).next_href = activities.next_href;
                }
                future_href = activities.future_href;
                events.addAll(activities.collection);

            } else {
                Log.w(TAG, "unexpected status code: " + response.getStatusLine());

                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                     throw new CloudAPI.InvalidTokenException(HttpStatus.SC_UNAUTHORIZED,
                             response.getStatusLine().getReasonPhrase());
                } else {
                    throw new IOException(response.getStatusLine().toString());
                }
            }
        } while (request != null);

        return new Activities(events, future_href, next_href);
    }

    public static void clear(Context c) {
        File[] cached = c.getCacheDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.startsWith(ActivitiesCache.PREFIX);
            }
        });

        if (cached != null) {
            for (File f : cached) {
                if (!f.delete()) {
                    Log.w(TAG, "could not delete " + f);
                }
            }
        }
    }
}
