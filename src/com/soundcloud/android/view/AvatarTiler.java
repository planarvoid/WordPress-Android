package com.soundcloud.android.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import static com.soundcloud.android.SoundCloudApplication.TAG;

class AvatarTiler extends View {

    private static final int MAX_AVATARS = 60;
    private static final int POLL_DELAY = 300;

    private class AvatarTile {
        Avatar currentAvatar; // the current bitmap displayed
        Avatar nextAvatar; // transitioning to this bitmap
        int nextColor; //transitioning to this color
        int nextAlpha; // alpha value of next bitmap
        long lastTransition; //timestamp last transition completed
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Avatar {
        String avatar_url;
        Bitmap bitmap;
    }

    private static class AvatarHolder extends CollectionHolder<Avatar> {
    }

    private AvatarTile[][] mAvatarTiles = new AvatarTile[5][8];
    private final HashMap<String, Avatar> mAvatars = new HashMap<String, Avatar>();
    private LoadAvatarsTask mLoadAvatarsTask;
    private final Queue<Avatar> mLoadedAvatars = new ArrayBlockingQueue<Avatar>(MAX_AVATARS);

    public AvatarTiler(Context context, AttributeSet attrs) {
        super(context, attrs);
        loadMoreAvatars(null);
    }

    private final Handler mPollHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Avatar a = mLoadedAvatars.poll();
                    Log.i("asdf","polled and got " + a + " , next " + mLoadedAvatars.peek());
                    queueNextPoll();
                    break;
            }
        }
    };

    private void queueNextPoll() {
        mPollHandler.sendEmptyMessageDelayed(0, POLL_DELAY);
    }


    /* Async Loading */

    private void loadMoreAvatars(String nextHref) {
        mLoadAvatarsTask = new LoadAvatarsTask((SoundCloudApplication) ((Activity) getContext()).getApplication());
        Request request;
        if (TextUtils.isEmpty(nextHref)) {
            request = Request.to(Endpoints.SUGGESTED_USERS);
            request.add("linked_partitioning", "1");
            request.add("limit", 20);
        } else {
            request = new Request(nextHref);
        }

        mLoadAvatarsTask.execute(request);
    }

    private void onAvatarTaskComplete(List<Avatar> avatars, String nextHref) {
        if (avatars.size() > 0) {
            Log.i("asdf", "Got more avatars, " + avatars.size());
            for (Avatar a : avatars) {
                a.avatar_url = ImageUtils.formatGraphicsUrlForList(getContext(),a.avatar_url);
                mAvatars.put(a.avatar_url, a);
                ImageLoader.get(getContext()).getBitmap(a.avatar_url, new ImageLoader.BitmapCallback() {
                    @Override
                    public void onImageLoaded(Bitmap mBitmap, String uri) {
                        mAvatars.get(uri).bitmap = mBitmap;
                        mLoadedAvatars.offer(mAvatars.get(uri));
                    }

                    @Override
                    public void onImageError(String uri, Throwable error) {
                    }
                }, null);
            }


            if (mAvatars.size() < MAX_AVATARS && !TextUtils.isEmpty(nextHref)) {
                loadMoreAvatars(nextHref);
            } else {
                Log.i("asdf", "Done loading avatars, loaded a total of " + mAvatars.size());
            }
        } else {
            Log.i("asdf", "no avatars returned ");
        }
    }


    private class LoadAvatarsTask extends AsyncTask<Request, Parcelable, Boolean> {
        private final SoundCloudApplication mApp;
        ArrayList<Avatar> newAvatars = new ArrayList<Avatar>();

        String mNextHref;
        int mResponseCode;

        public LoadAvatarsTask(SoundCloudApplication app) {
            mApp = app;
        }

        protected void onPostExecute(Boolean keepGoing) {
            onAvatarTaskComplete(newAvatars, keepGoing ? mNextHref : null);
        }

        @Override
        protected Boolean doInBackground(Request... request) {
            final Request req = request[0];
            if (req == null) return false;
            try {
                HttpResponse resp = mApp.get(req);

                mResponseCode = resp.getStatusLine().getStatusCode();
                if (mResponseCode != HttpStatus.SC_OK) {
                    throw new IOException("Invalid response: " + resp.getStatusLine());
                }

                InputStream is = resp.getEntity().getContent();

                AvatarHolder holder = mApp.getMapper().readValue(is, AvatarHolder.class);
                if (holder.size() > 0) {
                    newAvatars = new ArrayList<Avatar>();
                    for (Avatar avatar : holder) {
                        newAvatars.add(avatar);
                    }
                    mNextHref = holder.next_href;
                    return true;
                }

            } catch (IOException e) {
                Log.e(TAG, "error", e);
            }
            return false;

        }
    }
}
