package com.soundcloud.android.view;

import com.soundcloud.android.R;
import org.jetbrains.annotations.Nullable;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class RootView extends ViewGroup {
    static String TAG = RootView.class.getSimpleName();

    private static final float MAXIMUM_MINOR_VELOCITY = 150.0f;
    private static final float MAXIMUM_MAJOR_VELOCITY = 200.0f;
    private static final float MAXIMUM_ACCELERATION = 2000.0f;
    private static final int VELOCITY_UNITS = 1000;
    private static final int MSG_ANIMATE = 1000;
    private static final int ANIMATION_DURATION = 300;
    private static final int ANIMATION_FRAME_DURATION = 1000 / 60;

    private static final int FLING_TOLERANCE = 50;
    private static final int OFFSET_RIGHT = 60;// action bar home icon mRight
    private static final int OFFSET_LEFT = 60; // whatever we want on the left
    private static final int BEZEL_HIT_WIDTH = 30;

    private static final float DROP_SHADOW_WIDTH = 10.0f;

    private static int mExpandedState;
    private static final int EXPANDED_LEFT = 100000;
    private static final int COLLAPSED_FULL_CLOSED = 100001;
    private static final int EXPANDED_RIGHT = 100002;

    private static final int MAXIMUM_OVERLAY_ALPHA = 180;

    private static final float PARALLAX_SPEED_RATIO = 0.5f;

    public static final String EXTRA_ROOT_VIEW_STATE = "fim_menu_state";
    private static final String KEY_MENU_STATE = "menuState_key";
    private static final String STATE_KEY = "state_key";
    public static final int MENU_TARGET_WIDTH = 400;

    private MainMenu mMenu;
    private @Nullable View mPlayer;
    private ViewGroup mContent;

    private boolean mTracking;
    private boolean mLocked;
    private boolean mAnimating;

    private VelocityTracker mVelocityTracker;

    private OnMenuOpenListener mOnMenuOpenListener;
    private OnMenuCloseListener mOnMenuCloseListener;
    private OnMenuScrollListener mOnMenuScrollListener;

    private final Handler mHandler = new SlidingHandler();
    private int mTouchDelta;

    private final Scroller mScroller;
    private final Paint mOverlayPaint;

    private final GradientDrawable mShadowLeftDrawable;
    private final GradientDrawable mShadowRightDrawable;

    private final int mMaximumMinorVelocity;
    private final int mMaximumMajorVelocity;
    private final int mMaximumAcceleration;
    private final int mVelocityUnits;

    private int mOffsetRight;
    private final int mOffsetLeft;
    private final int mDrowShadoWidth;
    private final int mBezelHitWidth;

    /**
     * Callback invoked when the menu is opened.
     */
    public static interface OnMenuOpenListener {

        /**
         * Invoked when the menu becomes fully open.
         */
        public void onMenuOpenLeft();
        public void onMenuOpenRight();
    }
    /**
     * Callback invoked when the menu is closed.
     */
    public static interface OnMenuCloseListener {

        /**
         * Invoked when the menu becomes fully closed.
         */
        public void onMenuClosed();
    }
    /**
     * Callback invoked when the menu is scrolled.
     */
    public static interface OnMenuScrollListener {

        /**
         * Invoked when the user starts dragging/flinging the menu's handle.
         */
        public void onScrollStarted();
        /**
         * Invoked when the user stops dragging/flinging the menu's handle.
         */
        public void onScrollEnded();

    }
    /**
     * A slide in navigation menu.
     *
     * Based on the Android SlidingDrawer component, with additional functionality
     * and ideas credited to http://android.cyrilmottier.com/?p=658
     *
     * @param context
     * @param selectedMenuId
     */
    public RootView(Context context, int selectedMenuId) {
        super(context, null, 0);

        View.inflate(context, R.layout.root_view, this);

        setId(101010101);

        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        mMenu = (MainMenu) findViewById(R.id.root_menu);
        mMenu.setSelectedMenuId(selectedMenuId);

        mContent = (ViewGroup) findViewById(R.id.content_frame);
        mContent.setBackgroundColor(Color.WHITE);
        //mPlayer = findViewById(R.id.player_frame);

        mScroller = new Scroller(context,new DecelerateInterpolator());

        final float density = getResources().getDisplayMetrics().density;

        mMaximumMinorVelocity = (int) (MAXIMUM_MINOR_VELOCITY * density + 0.5f);
        mMaximumMajorVelocity = (int) (MAXIMUM_MAJOR_VELOCITY * density + 0.5f);
        mMaximumAcceleration  = (int) (MAXIMUM_ACCELERATION * density + 0.5f);
        mVelocityUnits        = (int) (VELOCITY_UNITS * density + 0.5f);

        mOffsetRight   = (int) (OFFSET_RIGHT * density + 0.5f);
        mOffsetLeft    = (int) (OFFSET_LEFT * density + 0.5f);
        mBezelHitWidth = (int) (BEZEL_HIT_WIDTH * density + 0.5f);

        mMenu.setOffsetRight(mOffsetRight);

        mOverlayPaint = new Paint();
        mOverlayPaint.setColor(Color.BLACK);

        mDrowShadoWidth = (int) (DROP_SHADOW_WIDTH * density);
        mShadowLeftDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0x00000000,0x8F000000});
        mShadowRightDrawable = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT,new int[]{0x00000000,0x8F000000});

        mExpandedState = COLLAPSED_FULL_CLOSED;

        setBackgroundColor(getResources().getColor(R.color.main_menu_bg));
        //setAlwaysDrawnWithCacheEnabled(false);
    }

    public int getContentHolderId() {
        if (mContent != null) {
            return mContent.getId();
        }
        return -1;
    }

    public int getPlayerHolderId(){
        if (mPlayer != null) {
            return mPlayer.getId();
        }
        return -1;
    }

    public void setContent(View content){
        if (mContent.getChildCount() > 0) mContent.removeAllViews();

        content.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mContent.addView(content);
    }

    public void configureMenu(int menuResourceId, final MainMenu.OnMenuItemClickListener listener) {
        mMenu.setMenuItems(menuResourceId);
        mMenu.setOnItemClickListener(listener);
    }

    public Bundle getMenuBundle() {
        Bundle bundle = new Bundle();
        SparseArray<Parcelable> container = new SparseArray<Parcelable>();
        mMenu.saveHierarchyState(container);
        bundle.putSparseParcelableArray(RootView.KEY_MENU_STATE, container);
        bundle.putInt(RootView.STATE_KEY,mExpandedState);
        return bundle;
    }

    public void restoreStateFromExtra(Bundle state) {
        mMenu.restoreHierarchyState(state.getSparseParcelableArray(KEY_MENU_STATE));
        mExpandedState = state.getInt(STATE_KEY);

        if (mExpandedState != COLLAPSED_FULL_CLOSED) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    animateClose();
                }
            }, 100); // post on delay to avoid animation jank at start of activity
        }
    }


    /**
     * global expansion state may have changed in another activity. make sure we are showing the correct state
     */
    public void onResume() {
        setExpandedState();
    }

    private void setExpandedState() {
        switch (mExpandedState){
            case COLLAPSED_FULL_CLOSED:
                setClosed();
                break;

            case EXPANDED_LEFT:
                openLeft();
                break;

            case EXPANDED_RIGHT:
                openRight();
                break;
        }
    }

    private boolean canOpenRight(){
        return mPlayer != null;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState ss = new SavedState(super.onSaveInstanceState());
        ss.expanded = mExpandedState;
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null) return;

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mExpandedState = ss.expanded;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("FlyInMenu cannot have UNSPECIFIED dimensions");
        }

        if (mContent != null){
            measureChild(mContent, widthMeasureSpec, heightMeasureSpec);
        }

        if (mMenu != null){
            int width = widthSpecSize;
            mMenu.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(heightSpecSize, MeasureSpec.EXACTLY));
        }

        if (mPlayer != null) {
            int width = widthSpecSize - mOffsetLeft;
            mPlayer.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(heightSpecSize, MeasureSpec.EXACTLY));
        }

        mShadowLeftDrawable.setBounds(0, 0, mDrowShadoWidth, getHeight());
        mShadowRightDrawable.setBounds(0, 0, mDrowShadoWidth, getHeight());

        setMeasuredDimension(widthSpecSize, heightSpecSize);

        final float density = getResources().getDisplayMetrics().density;
        mOffsetRight = (int) max(widthSpecSize - MENU_TARGET_WIDTH * density + 0.5f,
                                 OFFSET_RIGHT * density + 0.5f);

        mMenu.setOffsetRight(mOffsetRight);

        // since we are measured we can now find a proper expanded position if necessary
        setExpandedState();
    }

    public boolean isExpanded(){
        return mExpandedState != COLLAPSED_FULL_CLOSED;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        final long drawingTime = getDrawingTime();

        float openRatio = mContent.getLeft() < 0 ?
                          (float) -1 * mContent.getLeft() / (getWidth() - mOffsetLeft) :
                          (float) mContent.getLeft() / (getWidth() - mOffsetRight) ;

        float closedRatio = 1 - openRatio;

        if (mTracking || mAnimating || isExpanded()) {


            final int alpha = (int) (MAXIMUM_OVERLAY_ALPHA * (closedRatio));
            if (alpha > 0) mOverlayPaint.setColor(Color.argb(alpha, 0, 0, 0));

            if (mContent.getLeft() > 0){

                // menu
                final int left = mMenu.getLeft();
                final int offset = Math.min(-left, (int) (-closedRatio * mMenu.getWidth() * PARALLAX_SPEED_RATIO) - left);
                mMenu.offsetLeftAndRight(offset);

                final Bitmap menuCache = mMenu.getDrawingCache();
                if (menuCache != null) {
                    canvas.drawBitmap(menuCache, mMenu.getLeft(), 0, null);
                } else {
                    canvas.save();
                    drawChild(canvas, mMenu, drawingTime);
                    canvas.restore();
                }


                // menuOverlay
                if (alpha > 0) canvas.drawRect(0, 0, mMenu.getRight(), getHeight(), mOverlayPaint);

                // gradient
                canvas.save();
                canvas.translate(mContent.getLeft() - mDrowShadoWidth, 0);
                mShadowLeftDrawable.draw(canvas);
                canvas.restore();


            } else if (mContent.getLeft() < 0 && mPlayer != null) {

                // player
                final int offset = mOffsetLeft + (int) (closedRatio * mPlayer.getWidth() * PARALLAX_SPEED_RATIO) - mPlayer.getLeft();
                mPlayer.offsetLeftAndRight(offset);

                final Bitmap playerCache = mPlayer.getDrawingCache();
                if (playerCache != null) {
                    canvas.drawBitmap(playerCache, mPlayer.getLeft(), 0, null);
                } else {
                    canvas.save();
                    drawChild(canvas, mPlayer, drawingTime);
                    canvas.restore();
                }

                // playerOverlay
                if (alpha > 0) canvas.drawRect(mPlayer.getLeft(), 0, getWidth() , getHeight(), mOverlayPaint);

                // gradient
                canvas.save();
                canvas.translate(mContent.getRight(), 0);
                mShadowRightDrawable.draw(canvas);
                canvas.restore();
            }

            // content
            final Bitmap contentCache = mContent.getDrawingCache();
            if (contentCache != null) {
                canvas.drawBitmap(contentCache, mContent.getLeft(), 0, null);
            } else {
                canvas.save();
                drawChild(canvas, mContent, drawingTime);
                canvas.restore();
            }

        } else {
            drawChild(canvas, mContent, drawingTime);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mTracking) {
            return;
        }

        final int width = getWidth();
        final int height = getHeight();
        mMenu.layout(0, 0, width, height);
        mContent.layout(mContent.getLeft(), 0, mContent.getLeft() + width, height);
        if (mPlayer != null) mPlayer.layout(0, 0, width - mOffsetLeft, height);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mLocked) {
            return false;
        }

        final int action = event.getAction();
        float x = event.getX();
        if (action == MotionEvent.ACTION_DOWN && checkShouldTrack((int) event.getX())) {
            mTracking = true;

            // Must be called before prepareTracking()
            prepareContent();

            // Must be called after prepareContent()
            if (mOnMenuScrollListener != null) {
                mOnMenuScrollListener.onScrollStarted();
            }

            final int left = mContent.getLeft();
            mTouchDelta = (int) x - left;
            prepareTracking(left);
            mVelocityTracker.addMovement(event);
            return true;
        } else {
            return false;
        }
    }

    private boolean checkShouldTrack(int x){
        if (x >= mContent.getLeft() && x <= mContent.getRight()) {
            switch (mExpandedState) {
                case COLLAPSED_FULL_CLOSED:
                    return x < mContent.getLeft() + mBezelHitWidth || (canOpenRight() && x > mContent.getRight() - mBezelHitWidth);

                case EXPANDED_LEFT:
                    return x < getRight();

                case EXPANDED_RIGHT:
                    return x > getLeft();
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mLocked) {
            return true;
        }

        if (mTracking) {
            mVelocityTracker.addMovement(event);
            final int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_MOVE:
                    moveContent((int) (event.getX()) - mTouchDelta);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(mVelocityUnits);

                    float yVelocity = velocityTracker.getYVelocity();
                    float xVelocity = velocityTracker.getXVelocity();
                    boolean negative;

                    negative = xVelocity < 0;
                    if (yVelocity < 0) {
                        yVelocity = -yVelocity;
                    }
                    if (yVelocity > mMaximumMinorVelocity) {
                        yVelocity = mMaximumMinorVelocity;
                    }

                    float velocity = (float) Math.hypot(xVelocity, yVelocity);
                    if (negative) {
                        velocity = -velocity;
                    }
                    performFling(mContent.getLeft(), velocity, -1);
                }
                break;
            }
        }

        return true;
    }

    private void animateClose(int position) {
        prepareTracking(position);
        performFling(position, -mMaximumAcceleration, 1);
    }

    private void animateMenuOpen(int position) {
        prepareTracking(position);
        performFling(position, mMaximumAcceleration, 0);
    }

    private void animatePlayerOpen(int position) {
        prepareTracking(position);
        performFling(position, mMaximumAcceleration, 2);
    }

    private void performFling(int position, float velocity, int forceState) {

        final int motion;
        if (mExpandedState == EXPANDED_LEFT) {

            if (forceState == -1 && (velocity > mMaximumMajorVelocity ||
                    (position > (getWidth() - mOffsetRight)/2 && velocity > -mMaximumMajorVelocity))) {

                // We are expanded, but they didn't move sufficiently to cause
                // us to retract.  Animate back to the expanded position.
                motion = getWidth() - position - mOffsetRight;

            } else {
                // We are expanded and are now going to animate away.
                motion = -position;
            }
        } else if (mExpandedState == EXPANDED_RIGHT) {


            if (forceState == -1 && (velocity < -mMaximumMajorVelocity ||
                    (position < -(getWidth() - mOffsetLeft)/2 && velocity < mMaximumMajorVelocity))) {
                // We are expanded, but they didn't move sufficiently to cause
                // us to retract.  Animate back to the expanded position.
                motion = -getWidth() + mOffsetLeft - position;

            } else {
                // We are expanded and are now going to animate away.
                motion = -position;
            }

        } else {

            final boolean shouldOpenLeft = velocity > mMaximumMajorVelocity ||
                    (position > (getWidth()) / 2 &&
                            velocity > -mMaximumMajorVelocity);

            final boolean shouldOpenRight = canOpenRight() && (velocity < -mMaximumMajorVelocity ||
                                (position < -(getWidth()) / 2 &&
                                        velocity < mMaximumMajorVelocity));

            if (forceState == EXPANDED_LEFT || shouldOpenLeft){
                motion = getWidth() - position - mOffsetRight;

            } else if (forceState == EXPANDED_RIGHT || shouldOpenRight) {
                motion = -getWidth() - position + mOffsetRight;

            } else  {
                // We are collapsed, but they didn't move sufficiently to cause
                // us to open either way.  Animate back to the collapsed position.
                motion = -position;
            }
        }

        mScroller.startScroll(position, 0, motion, 0, ANIMATION_DURATION);
        mAnimating = true;
        mHandler.removeMessages(MSG_ANIMATE);
        mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE), SystemClock.uptimeMillis() + ANIMATION_FRAME_DURATION);
        stopTracking();
    }

    private void prepareTracking(int position) {
        if (mAnimating) {
            mAnimating = false;
            mHandler.removeMessages(MSG_ANIMATE);
        }
        mTracking = true;
        mVelocityTracker = VelocityTracker.obtain();
        moveContent(isExpanded() ? position : 0);
    }

    private void moveContent(int position) {
        if (position == EXPANDED_LEFT) {
            final int togo = (getMeasuredWidth() - mOffsetRight) - mContent.getLeft();
            mContent.offsetLeftAndRight(togo); //todo proper value
        } else if (position == EXPANDED_RIGHT) {
            final int togo = (-getMeasuredWidth() + mOffsetLeft) - mContent.getLeft();
            mContent.offsetLeftAndRight(togo); //todo proper value
        } else if (position == COLLAPSED_FULL_CLOSED) {
            mContent.offsetLeftAndRight(0 - mContent.getLeft());

        } else {
            final int left = mContent.getLeft();
            int deltaX = position - left;
            if (position < 0 && !canOpenRight()) {
                deltaX = -left;
            }
            mContent.offsetLeftAndRight(deltaX);
        }
        invalidate();
    }

    @TargetApi(11)
    private void prepareContent() {
        if (mAnimating) {
            return;
        }

        // Something changed in the content, we need to honor the layout request
        // before creating the cached bitmap
        final View content = mContent;

        // Try only once... we should really loop but it's not a big deal
        // if the draw was cancelled, it will only be temporary anyway
        content.getViewTreeObserver().dispatchOnPreDraw();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && !content.isHardwareAccelerated()) {
            content.buildDrawingCache();
        }

        content.setVisibility(View.GONE);
    }

    private void stopTracking() {
        mTracking = false;

        if (mOnMenuScrollListener != null) {
            mOnMenuScrollListener.onScrollEnded();
        }

        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            mContent.offsetLeftAndRight(mScroller.getCurrX() - mContent.getLeft());
            mHandler.removeMessages(MSG_ANIMATE);
            mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE), SystemClock.uptimeMillis() + ANIMATION_FRAME_DURATION);
        } else {
            if (mAnimating && mScroller.isFinished()) {
                if (mContent.getLeft() >= (getWidth() - mOffsetRight) / 2) {
                    openLeft();
                } else if (mContent.getRight() < (getWidth() - mOffsetLeft) / 2 && canOpenRight()) {
                    openRight();
                } else {
                    setClosed();
                }
                mAnimating = false;
            }
            super.computeScroll();
        }
    }

    private boolean isMenuOpen() {
        return (mExpandedState == EXPANDED_LEFT);
    }

    /**
     * Toggles the menu open and close with an animation.
     */
    public void animateToggleMenu() {
        if (!isMenuOpen()) {
            animateMenuOpen();
        } else {
            animateClose();
        }
    }

    /**
     * Closes the menu immediately.
     */
    public void close() {
        setClosed();
        invalidate();
        requestLayout();
    }

    public void onBack() {
        if (!mMenu.gotoMenu()){
            animateClose();
        }
    }


    /**
     * Closes the menus with an animation.
     */
    public void animateClose() {
        prepareContent();
        final OnMenuScrollListener scrollListener = mOnMenuScrollListener;
        if (scrollListener != null) {
            scrollListener.onScrollStarted();
        }
        animateClose(mContent.getLeft());

        if (scrollListener != null) {
            scrollListener.onScrollEnded();
        }
    }

    /**
     * Opens the menu with an animation.
     */
    public void animateMenuOpen() {
        prepareContent();
        final OnMenuScrollListener scrollListener = mOnMenuScrollListener;
        if (scrollListener != null) {
            scrollListener.onScrollStarted();
        }
        animateMenuOpen(mContent.getLeft());

        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);

        if (scrollListener != null) {
            scrollListener.onScrollEnded();
        }
    }

    public void animatePlayerOpen() {
        if (!canOpenRight()) return;

        prepareContent();
        final OnMenuScrollListener scrollListener = mOnMenuScrollListener;
        if (scrollListener != null) {
            scrollListener.onScrollStarted();
        }
        animatePlayerOpen(mContent.getLeft());

        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);

        if (scrollListener != null) {
            scrollListener.onScrollEnded();
        }
    }

    private void setClosed() {
        moveContent(COLLAPSED_FULL_CLOSED);
        mContent.destroyDrawingCache();

        mContent.setVisibility(View.VISIBLE);
        mMenu.setVisibility(View.GONE);
        if (mPlayer != null) mPlayer.setVisibility(View.GONE);

        mMenu.gotoMenu();


        if (mExpandedState == COLLAPSED_FULL_CLOSED) {
            return;
        }

        mExpandedState = COLLAPSED_FULL_CLOSED;
        if (mOnMenuCloseListener != null) {
            mOnMenuCloseListener.onMenuClosed();
        }
    }

    private void openLeft() {
        moveContent(EXPANDED_LEFT);
        mMenu.setVisibility(View.VISIBLE);
        mContent.setVisibility(View.GONE);
        if (mPlayer != null) mPlayer.setVisibility(View.GONE);
        invalidate();

        if (mExpandedState == EXPANDED_LEFT) {
            return;
        }

        mExpandedState = EXPANDED_LEFT;

        if (mOnMenuOpenListener != null) {
            mOnMenuOpenListener.onMenuOpenLeft();
        }
    }

    private void openRight() {
        if (!canOpenRight()) return;
        moveContent(EXPANDED_RIGHT);
        mMenu.setVisibility(View.GONE);
        mContent.setVisibility(View.GONE);
        if (mPlayer != null) mPlayer.setVisibility(View.VISIBLE);
        invalidate();

        if (mExpandedState == EXPANDED_RIGHT) {
            return;
        }

        mExpandedState = EXPANDED_RIGHT;

        if (mOnMenuOpenListener != null) {
            mOnMenuOpenListener.onMenuOpenRight();
        }
    }

    /**
     * Sets the listener that receives a notification when the menu becomes open.
     *
     * @param onMenuOpenListener The listener to be notified when the menu is opened.
     */
    public void setOnMenuOpenListener(OnMenuOpenListener onMenuOpenListener) {
        mOnMenuOpenListener = onMenuOpenListener;
    }

    /**
     * Sets the listener that receives a notification when the menu closes and the content is full screen.
     *
     * @param onMenuCloseListener The listener to be notified when the menu is closed.
     */
    public void setOnMenuCloseListener(OnMenuCloseListener onMenuCloseListener) {
        mOnMenuCloseListener = onMenuCloseListener;
    }

    /**
     * Sets the listener that receives a notification when the menu starts or ends
     * a scroll. A fling is considered as a scroll. A fling will also trigger a
     * menu opened or menu closed event.
     *
     * @param onMenuScrollListener The listener to be notified when scrolling
     *                             starts or stops.
     */
    public void setOnMenuScrollListener(OnMenuScrollListener onMenuScrollListener) {
        mOnMenuScrollListener = onMenuScrollListener;
    }

    /**
     * Returns the handle of the menu.
     *
     * @return The View representing the handle of the menu, identified by
     *         the "handle" id in XML.
     */
    public View getMenu() {
        return mMenu;
    }

    /**
     * Returns the content of the menu.
     *
     * @return The View representing the content of the menu, identified by
     *         the "content" id in XML.
     */
    public View getContent() {
        return mContent;
    }

    /**
     * Unlocks the SlidingMenu so that touch events are processed.
     *
     * @see #lock()
     */
    public void unlock() {
        mLocked = false;
    }

    /**
     * Locks the SlidingMenu so that touch events are ignores.
     *
     * @see #unlock()
     */
    public void lock() {
        mLocked = true;
    }

    /**
     * Indicates whether the menu is currently fully opened.
     *
     * @return True if the menu is opened, false otherwise.
     */
    public boolean isOpened() {
        return mExpandedState != COLLAPSED_FULL_CLOSED;
    }

    /**
     * Indicates whether the menu is scrolling or flinging.
     *
     * @return True if the menu is scroller or flinging, false otherwise.
     */
    public boolean isMoving() {
        return mTracking || mAnimating;
    }

    private class SlidingHandler extends Handler {
        public void handleMessage(Message m) {
            switch (m.what) {
                case MSG_ANIMATE:
                    invalidate();
                    break;
            }
        }
    }

    static class SavedState extends BaseSavedState {
       int expanded;

        SavedState(Parcelable superState) {
          super(superState);
        }

        private SavedState(Parcel in) {
          super(in);
          this.expanded = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
          super.writeToParcel(out, flags);
          out.writeInt(mExpandedState);
        }

        //required field that makes Parcelables from a Parcel
        public static final Creator<SavedState> CREATOR =
            new Creator<SavedState>() {
              public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
              }
              public SavedState[] newArray(int size) {
                return new SavedState[size];
              }
        };
      }
}


