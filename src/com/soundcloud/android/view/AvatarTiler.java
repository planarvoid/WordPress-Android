package com.soundcloud.android.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
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
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

import static com.soundcloud.android.SoundCloudApplication.TAG;

class AvatarTiler extends SurfaceView implements SurfaceHolder.Callback {

    private static final int MAX_AVATARS = 60;
    private static final int LOAD_AVATARS_POLL_DELAY = 100;
    private static final int CHANGE_AVATARS_POLL_DELAY = 300;

    private static final int DRAW_PERIOD = 50;

    private static final int TILE_ROWS = 5;
    private static final int TILE_COLS = 8;

    private static final int MAX_TILE_TRIES = 5;
    private static final int MIN_TILE_AGE = 5000;

    private DrawAvatarThread mDrawThread;

    private class AvatarTile {
        public AvatarTile(int col, int row) {
            this.row = row;
            this.col = col;
        }

        Avatar currentAvatar; // the current bitmap displayed
        Avatar nextAvatar; // transitioning to this bitmap
        int nextColor; //transitioning to this color
        int nextAlpha; // alpha value of next bitmap
        long lastAssigned; //timestamp last transition completed
        int row;
        int col;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Avatar {
        public String avatar_url;
        public Bitmap bitmap;
    }
    private static class AvatarHolder extends CollectionHolder<Avatar> {}

    private List<AvatarTile> mAvatarTiles;
    private final HashMap<String, Avatar> mAvatars = new HashMap<String, Avatar>();
    private LoadAvatarsTask mLoadAvatarsTask;

    private AvatarTile mNextTile;
    private Avatar mNextAvatar;
    private int mNextAvatarPollDelay;

    private Timer mDrawTimer;
    private boolean mInitialLoad;

    private Paint mImagePaint;
    private Matrix mMatrix;

    private final Queue<Avatar> mLoadedAvatars = new ArrayBlockingQueue<Avatar>(MAX_AVATARS);
    private final Queue<AvatarTile> mTileQueue = new ArrayBlockingQueue<AvatarTile>(40);

    private float mAvatarScale;
    private int mColSize;
    private int mRowSize;

    private int mDisplayIndex;

    public AvatarTiler(Context context, AttributeSet attrs) {
        super(context, attrs);

        mAvatarTiles = new ArrayList<AvatarTile>();
        for (int i = 0; i < TILE_ROWS; i++) {
            for (int j = 0; j < TILE_COLS; j++) {
                mAvatarTiles.add(new AvatarTile(j, i));
            }
        }
        Collections.shuffle(mAvatarTiles);
        mNextAvatarPollDelay = LOAD_AVATARS_POLL_DELAY;
        mInitialLoad = true;

        getHolder().addCallback(this);
        mDrawThread = new DrawAvatarThread(getHolder(), this);

        mImagePaint = new Paint();
        mImagePaint.setAntiAlias(false);
        mImagePaint.setFilterBitmap(true);

        mMatrix = new Matrix();

        loadMoreAvatars(null);
        queueNextPoll();
    }

    private final Handler mPollHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    mNextAvatar = mNextAvatar == null ? mLoadedAvatars.poll() : mNextAvatar;
                    mNextTile = mNextTile == null ? getNextTile() : mNextTile;
                    if (mNextTile != null && mNextAvatar != null) {
                        mNextTile.nextAvatar = mNextAvatar;
                        mNextTile.lastAssigned = System.currentTimeMillis();
                        mNextAvatar = null;
                        mNextTile = null;
                    }
                    queueNextPoll();
                    break;
            }
        }
    };

    private void queueNextPoll() {
        mPollHandler.sendEmptyMessageDelayed(0, mNextAvatarPollDelay);
    }

    private AvatarTile getNextTile() {
        if (mDisplayIndex < mAvatarTiles.size()) {
            //initial display, show in order to make sure they all fill
            mDisplayIndex++;
            return mAvatarTiles.get(mDisplayIndex - 1);
        } else if (mDisplayIndex == mAvatarTiles.size()) {
            // set to normal poll delay
            mNextAvatarPollDelay = CHANGE_AVATARS_POLL_DELAY;
            mDisplayIndex++;
        }
        int tries = 0;
        while (tries < MAX_TILE_TRIES) {
            AvatarTile at = mAvatarTiles.get((int) (Math.random() * mAvatarTiles.size()));
            if (System.currentTimeMillis() - at.lastAssigned > MIN_TILE_AGE) {
                return at;
            }
        }

        return null;
    }

    @Override
    public void onDraw(Canvas c) {
        for (AvatarTile at : mAvatarTiles){
            if (at.nextAvatar != null){
                mMatrix.setScale(mAvatarScale,mAvatarScale);
                mMatrix.postTranslate(mColSize * at.col, mRowSize * at.row);
                c.drawBitmap(at.nextAvatar.bitmap,mMatrix,mImagePaint);
            }

        }

    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mDrawThread.setRunning(true);
        mDrawThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mColSize = width/TILE_COLS;
        mAvatarScale = ((float) mColSize)/ImageUtils.getListItemGraphicDimension(getContext());
        mRowSize = (int) (ImageUtils.getListItemGraphicDimension(getContext())*mAvatarScale);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        mDrawThread.setRunning(false);
        while (retry) {
            try {
                mDrawThread.join();
                retry = false;
            } catch (InterruptedException e) {
                // we will try it again and again...
            }
        }
    }

    class DrawAvatarThread extends Thread {
        private SurfaceHolder mSurfaceHolder;
        private AvatarTiler mAvatarTiler;
        private boolean _run = false;

        public DrawAvatarThread(SurfaceHolder surfaceHolder, AvatarTiler tiler) {
            mSurfaceHolder = surfaceHolder;
            mAvatarTiler = tiler;
        }

        public void setRunning(boolean run) {
            _run = run;
        }

        @Override
        public void run() {
            Canvas c;
            while (_run) {
                c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        mAvatarTiler.onDraw(c);
                    }
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }
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
            for (Avatar a : avatars) {
                a.avatar_url = ImageUtils.formatGraphicsUrlForList(getContext(), a.avatar_url);
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
