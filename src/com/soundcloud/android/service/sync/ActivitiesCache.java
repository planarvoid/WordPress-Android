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
                          long lastSeen,
                          final Request request) throws IOException {

        final File cachedFile = new File(context.getCacheDir(),
                PREFIX+md5(account == null ? "" : account.name+request.toUrl())+".json");

        Activities activities;
        try {
            if (cachedFile.exists()) {
                Activities cached = Activities.fromJSON(cachedFile).filter(lastSeen);
                String future_href = cached.future_href;

                Log.d(TAG, "read from activities cache "+cachedFile+
                        ", requesting updates from " +(future_href == null ? request.toUrl() : future_href));

                if (future_href != null) {
                    Activities updates = getEvents(context, lastSeen, Request.to(future_href));
                    activities = updates.merge(cached);
                } else {
                    activities = cached;
                }
            } else {
              activities = getEvents(context, lastSeen, request);
            }
        } catch (IOException e) {
            Log.w(TAG, "error", e);
            // fallback, load events from normal resource
            activities = getEvents(context, lastSeen, request);
        }

        activities.toJSON(cachedFile, Views.Mini.class);
        Log.d(TAG, "cached activities to "+cachedFile);
        return activities;
    }

    private static Activities getEvents(SoundCloudApplication app, final long since, final Request resource)
            throws IOException {
        boolean caughtUp = false;
        String future_href = null;
        List<Event> events = new ArrayList<Event>();

        Request request = resource.add("limit", 20);
        do {
            HttpResponse response = app.get(request);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                Activities activities = Activities.fromJSON(response.getEntity().getContent());
                request = activities.hasMore() ? activities.getNextRequest() : null;
                if (future_href == null) {
                    future_href = activities.future_href;
                }

                if (activities.olderThan(since)) {
                    caughtUp = true; // nothing new
                } else {
                    for (Event evt : activities) {
                        if (evt.created_at.getTime() > since) {
                            events.add(evt);
                        } else {
                            caughtUp = true;
                            break;
                        }
                    }
                }
            } else {
                Log.w(TAG, "unexpected status code: " + response.getStatusLine());

                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                     throw new CloudAPI.InvalidTokenException(HttpStatus.SC_UNAUTHORIZED,
                             response.getStatusLine().getReasonPhrase());
                } else {
                    throw new IOException(response.getStatusLine().toString());
                }
            }
        } while (!caughtUp
                && events.size() < SyncAdapterService.NOTIFICATION_MAX
                && request != null);

        return new Activities(events, future_href);
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
