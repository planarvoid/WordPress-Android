package com.soundcloud.android.view;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.LinearLayout;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.R;
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

    private static final int MAX_AVATARS = 100;
    private static final int CHANGE_AVATARS_POLL_DELAY = 100;

    private static final double CHANCE_OF_EMPTY = .1;

    private static final int TILE_COLS = 8;

    private int mCurrentRows;

    private static final int MAX_TILE_TRIES = 5;
    private static final int MIN_TILE_AGE = 5000;
    private static final int ALPHA_STEP = 5;

    private static final int[] EMPTY_COLORS = {0xffd4e7fc, 0xff7ab8ff, 0xff3399ff, 0xffff6600, 0xffff3300, 0xffff9a56};
    private static final Avatar[] DEFAULT_AVATARS = {
            new Avatar(4420810, R.drawable.avatars_newyorker),
            new Avatar(4225846, R.drawable.avatars_marihuertas),
            new Avatar(511721, R.drawable.avatars_max_richter),
            new Avatar(4247850, R.drawable.avatars_penguin_books),
            new Avatar(2610070, R.drawable.avatars_cyramorgan),
            new Avatar(1008779, R.drawable.avatars_fatfrumos),
            new Avatar(104386, R.drawable.avatars_nonagon),
            new Avatar(3907706, R.drawable.avatars_youthlagoon),
            new Avatar(5510726, R.drawable.avatars_herring1967),
            new Avatar(422725, R.drawable.avatars_thenextweb),
            new Avatar(34424, R.drawable.avatars_thommyc)
    };


    private DrawAvatarThread mDrawThread;
    private GradientDrawable mBgGradient;

    private class AvatarTile {
        public AvatarTile(int col, int row) {
            this.row = row;
            this.col = col;
        }

        Avatar currentAvatar; // the current bitmap displayed
        Avatar nextAvatar; // transitioning to this bitmap
        int nextAlpha; // alpha value of next bitmap
        long lastAssigned; //timestamp last transition completed
        final int row;
        final int col;
        int l;
        int t;
        int r;
        int b;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Avatar {
        public Avatar() {

        }

        public Avatar(long id, int resource_id) {
            this.id = id;
            this.resource_id = resource_id;
            isDefault = true;
        }

        public boolean isDefault;
        public long id;
        public int resource_id;
        public String avatar_url;
        public Bitmap bitmap;
        public int fillColor;
    }

    private static class AvatarHolder extends CollectionHolder<Avatar> {
    }

    private List<AvatarTile> mAvatarTiles;
    private final HashMap<String, Avatar> mAvatars = new HashMap<String, Avatar>();

    private AvatarTile mNextTile;
    private Avatar mNextAvatar;

    private final Paint mImagePaint;
    private final Paint mFillPaint;
    private final Matrix mMatrix;

    private final Queue<Avatar> mLoadedAvatars = new ArrayBlockingQueue<Avatar>(MAX_AVATARS);

    private float mAvatarScale;
    private float mDefaultAvatarScale;
    private int mColSize;
    private int mRowSize;

    private int mDisplayIndex;

    public AvatarTiler(Context context, AttributeSet attrs) {
        super(context, attrs);

        mImagePaint = new Paint();
        mImagePaint.setAntiAlias(false);
        mImagePaint.setFilterBitmap(true);

        mFillPaint = new Paint();
        mFillPaint.setStyle(Paint.Style.FILL);

        mMatrix = new Matrix();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        for (Avatar a : DEFAULT_AVATARS) {
            a.bitmap = BitmapFactory.decodeResource(getResources(), a.resource_id, options);
        }

        loadMoreAvatars(null);

        getHolder().addCallback(this);

    }

    private void loadDefaults() {
        int mLastAvatarIndex = 0;
        Collections.shuffle(Arrays.asList(DEFAULT_AVATARS));
        for (AvatarTile at : mAvatarTiles) {
            if (Math.random() < .25) {
                Avatar a = new Avatar();
                a.fillColor = EMPTY_COLORS[((int) (Math.random() * EMPTY_COLORS.length))];
                at.currentAvatar = a;
            } else {
                at.currentAvatar = DEFAULT_AVATARS[mLastAvatarIndex%DEFAULT_AVATARS.length];
                mLastAvatarIndex++;
            }
        }
    }

    private void loadAvatarImage(Avatar a) {
        a.avatar_url = ImageUtils.formatGraphicsUrlForList(getContext(), a.avatar_url);
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
        mPollHandler.sendEmptyMessageDelayed(0, CHANGE_AVATARS_POLL_DELAY);
    }

    private AvatarTile getNextTile() {
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


        mBgGradient.draw(c);

        if (mAvatarTiles == null) return;

        for (AvatarTile at : mAvatarTiles) {

            if (at.currentAvatar != null) {
                mImagePaint.setAlpha(255);
                if (at.currentAvatar.bitmap == null) {
                    mFillPaint.setColor(at.currentAvatar.fillColor);
                    mFillPaint.setAlpha(255);
                    c.drawRect(at.l, at.t, at.r, at.b, mFillPaint);
                } else {
                    c.drawBitmap(at.currentAvatar.bitmap, getAvatarMatrix(at.currentAvatar, mColSize * at.col, mRowSize * at.row), mImagePaint);
                }
            }

            if (at.nextAvatar != null) {
                if (at.nextAvatar.bitmap == null) {
                    mFillPaint.setColor(at.nextAvatar.fillColor);
                    mFillPaint.setAlpha(at.nextAlpha);
                    c.drawRect(at.l, at.t, at.r, at.b, mFillPaint);
                } else {
                    mImagePaint.setAlpha(at.nextAlpha);
                    c.drawBitmap(at.nextAvatar.bitmap, getAvatarMatrix(at.nextAvatar, mColSize * at.col, mRowSize * at.row), mImagePaint);
                }

                at.nextAlpha = Math.min(255, at.nextAlpha + ALPHA_STEP);
                if (at.nextAlpha == 255) {
                    if (at.currentAvatar != null && !at.currentAvatar.isDefault) mLoadedAvatars.offer(at.currentAvatar);
                    at.currentAvatar = at.nextAvatar;
                    at.nextAlpha = 0;
                    at.nextAvatar = null;
                }
            }

        }

    }

    public Matrix getAvatarMatrix(Avatar a, int col, int row) {
        if (a.isDefault) {
            mMatrix.setScale(mDefaultAvatarScale, mDefaultAvatarScale);
        } else {
            mMatrix.setScale(mAvatarScale, mAvatarScale);
        }
        mMatrix.postTranslate(col, row);
        return mMatrix;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mDrawThread = new DrawAvatarThread(getHolder(), this);
        mDrawThread.setRunning(true);
        mDrawThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        final int color1 = getResources().getColor(R.color.blue_gradient_1);
        final int color2 = getResources().getColor(R.color.blue_gradient_half);

        final int[] colors = {color1, color2};
        mBgGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
        mBgGradient.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        mBgGradient.setBounds(new Rect(0,0,width,height));

        mColSize = width / TILE_COLS;
        mAvatarScale = ((float) mColSize) / ImageUtils.getListItemGraphicDimension(getContext());
        mDefaultAvatarScale = ((float) mColSize) / 100;
        mRowSize = (int) (ImageUtils.getListItemGraphicDimension(getContext()) * mAvatarScale);

        if (mCurrentRows == 0) {
            mCurrentRows = (int) (height / mRowSize);
        }

        if (mAvatarTiles == null) {
            mAvatarTiles = new ArrayList<AvatarTile>();
            for (int i = 0; i < mCurrentRows; i++) {
                for (int j = 0; j < TILE_COLS; j++) {
                    mAvatarTiles.add(new AvatarTile(j, i));
                }
            }
        }

        for (AvatarTile at : mAvatarTiles) {
            at.l = mColSize * at.col;
            at.t = mRowSize * at.row;
            at.r = mColSize * (1 + at.col);
            at.b = mRowSize * (1 + at.row);
        }
        loadDefaults();
        Collections.shuffle(mAvatarTiles);
        queueNextPoll();

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
        private final SurfaceHolder mSurfaceHolder;
        private final AvatarTiler mAvatarTiler;
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
        LoadAvatarsTask mLoadAvatarsTask = new LoadAvatarsTask((SoundCloudApplication) ((Activity) getContext()).getApplication());
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
                mAvatars.put(a.avatar_url, a);
                loadAvatarImage(a);
            }


            if (mAvatars.size() < MAX_AVATARS && !TextUtils.isEmpty(nextHref)) {
                loadMoreAvatars(nextHref);
            } else {
                Log.i(getClass().getSimpleName(), "Done loading avatars, loaded a total of " + mAvatars.size());
            }
        } else {
            Log.i(getClass().getSimpleName(), "no avatars returned ");
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
