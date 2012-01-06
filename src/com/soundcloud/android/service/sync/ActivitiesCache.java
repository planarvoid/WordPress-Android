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
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class ActivitiesCache {
    private static final String PREFIX = "activities-";

    public static File getCacheFile(SoundCloudApplication app, final Request request){
        return getCacheFile(app,app.getAccount(), request);
    }

    public static File getCacheFile(SoundCloudApplication app, Account account,final Request request){
        return new File(app.getCacheDir(),
                PREFIX+md5(account == null ? "" : account.name+request.toUrl())+".json");
    }

    public static Activities get(SoundCloudApplication context,
                          Account account,
                          final Request request) throws IOException {

        final File cachedFile = getCacheFile(context,account,request);

        Activities activities;
        try {
            if (cachedFile.exists()) {
                Activities cached = Activities.fromJSON(cachedFile);
                String future_href = cached.future_href;

                Log.d(TAG, "future_href href is " + future_href);
                Log.d(TAG, "read from activities cache "+cachedFile+
                        ", requesting updates from " +(future_href == null ? request.toUrl() : future_href));

                if (future_href != null) {
                    Activities updates = getEvents(context, cached.size() > 0 ? cached.get(0) : null, Request.to(future_href));
                    activities = updates == Activities.EMPTY ? cached : updates.merge(cached);
                } else {
                    activities = cached;
                }
            } else {
              activities = getEvents(context, null, request);
            }
        } catch (IOException e) {
            Log.w(TAG, "error", e);
            // fallback, load events from normal resource
            activities = getEvents(context, null, request);
        }

        Log.d(TAG, "caching activities to "+cachedFile);
        return activities.trimBelow(SyncAdapterService.NOTIFICATION_MAX)
                         .toJSON(cachedFile, Views.Mini.class);
    }

    public static Activities getEvents(SoundCloudApplication app, final Event lastCached, final Request resource)
            throws IOException {
        boolean caughtUp = false;
        String future_href = null;
        String next_href = null;
        List<Event> events = new ArrayList<Event>();

        Request request = resource.add("limit", 20);
        do {
            Log.i(TAG,"Making request " + request);
            HttpResponse response = app.get(request);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                Activities activities = Activities.fromJSON(response.getEntity().getContent());
                request = activities.hasMore() ? activities.getNextRequest() : null;
                if (future_href == null) {
                    future_href = URLDecoder.decode(activities.future_href);
                }

                if (next_href != null){
                    events.get(events.size()-1).next_href = next_href;
                }
                next_href = activities.next_href;

                Log.i(TAG,"Got events " + activities.size());

                for (Event evt : activities) {
                    if (lastCached == null || !evt.equals(lastCached)) {
                        events.add(evt);
                    } else {
                        caughtUp = true;
                        break;
                    }
                }
            } else {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                    return Activities.EMPTY;
                } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                    throw new CloudAPI.InvalidTokenException(HttpStatus.SC_UNAUTHORIZED,
                            response.getStatusLine().getReasonPhrase());
                } else {
                    throw new IOException(response.getStatusLine().toString());
                }
            }
        } while (!caughtUp
                && events.size() < SyncAdapterService.NOTIFICATION_MAX
                && request != null);

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
