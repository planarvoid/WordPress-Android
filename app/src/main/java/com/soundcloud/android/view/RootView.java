package com.soundcloud.android.view;

import android.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

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
    private static final int BEZEL_HIT_WIDTH = 30;

    private static final float DROP_SHADOW_WIDTH = 10.0f;

    private static final int EXPANDED_FULL_OPEN = -10001;
    private static final int COLLAPSED_FULL_CLOSED = -10002;

    private static final int MAXIMUM_MENU_ALPHA_OVERLAY = 180;
    private static final float PARALLAX_SPEED_RATIO = 0.5f;

    public static final String EXTRA_MENU_STATE = "fim_menuState";

    private static boolean mExpanded;

    private View mMenu;
    private View mContent;

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
    private final Paint mMenuOverlayPaint;
    private final GradientDrawable mShadowDrawable;

    private final int mMaximumMinorVelocity;
    private final int mMaximumMajorVelocity;
    private final int mMaximumAcceleration;
    private final int mVelocityUnits;
    private final boolean mAnimateOnClick;

    private final int mFlingTolerance;
    private final int mOffsetRight;
    private final int mDrowShadoWidth;
    private final int mBezelHitWidth;

    /**
     * Callback invoked when the menu is opened.
     */
    public static interface OnMenuOpenListener {

        /**
         * Invoked when the menu becomes fully open.
         */
        public void onMenuOpened();
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
     */
    public RootView(Context context) {
        super(context, null, 0);

        setId(101010101);

        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        mScroller = new Scroller(context,new DecelerateInterpolator());

        mAnimateOnClick = true;

        final float density = getResources().getDisplayMetrics().density;
        mMaximumMinorVelocity = (int) (MAXIMUM_MINOR_VELOCITY * density + 0.5f);
        mMaximumMajorVelocity = (int) (MAXIMUM_MAJOR_VELOCITY * density + 0.5f);
        mMaximumAcceleration = (int) (MAXIMUM_ACCELERATION * density + 0.5f);
        mVelocityUnits = (int) (VELOCITY_UNITS * density + 0.5f);

        mFlingTolerance = (int) (FLING_TOLERANCE * density + 0.5f);
        mOffsetRight = (int) (OFFSET_RIGHT * density + 0.5f);
        mBezelHitWidth = (int) (BEZEL_HIT_WIDTH * density + 0.5f);

        mMenuOverlayPaint = new Paint();
        mMenuOverlayPaint.setColor(Color.BLACK);

        mDrowShadoWidth = (int) (DROP_SHADOW_WIDTH * density);
        mShadowDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0x00000000,0x8F000000});

        setBackgroundColor(getContext().getResources().getColor(R.color.background_dark));
        setAlwaysDrawnWithCacheEnabled(false);
    }


    public void setContent(View content){
        if (mContent != null && mContent.getParent() == this) {
            removeView(mContent);
        }

        mContent = content;
        mContent.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        addView(mContent);
    }

    public void setMenu(int menuResourceId, final SimpleListMenu.OnMenuItemClickListener listener) {
        final SimpleListMenu menu = new SimpleListMenu(getContext(), menuResourceId);
        setMenu(menu);
        menu.setOnItemClickListener(new SimpleListMenu.OnMenuItemClickListener() {
            @Override
            public void onMenuItemClicked(int id) {
                if (listener != null) listener.onMenuItemClicked(id);
            }
        });

    }

    public Bundle getMenuBundle() {
        Bundle bundle = new Bundle();
        SparseArray<Parcelable> container = new SparseArray<Parcelable>();
        saveHierarchyState(container);
        bundle.putSparseParcelableArray(RootView.EXTRA_MENU_STATE, container);
        return bundle;
    }

    /**
     * global expansion state may have changed in another activity. make sure we are showing the correct state
     */
    public void onResume() {
        if (!mExpanded){
            closeMenu();
        } else {
            openMenu();
        }
    }

    @Override
    public void saveHierarchyState(SparseArray<Parcelable> container) {
        super.saveHierarchyState(container);

    }

    @Override
    public void restoreHierarchyState(SparseArray<Parcelable> container) {
        super.restoreHierarchyState(container);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState ss = new SavedState(super.onSaveInstanceState());
        ss.expanded = mExpanded;
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null) return;

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mExpanded = ss.expanded;
        if (mExpanded){
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    animateClose();
                }
            }, 100); // post on delay to avoid animation jank at start of activity
        }
    }

    public void setMenuView(View menu) {

    }

    private void setMenu(View menu) {
        if (mMenu != null && mMenu.getParent() == this) {
            removeView(mMenu);
        }

        mMenu = menu;
        mMenu.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        addView(mMenu);
        mMenu.requestLayout();
        mMenu.invalidate();

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
            int width = widthSpecSize - mOffsetRight;
            mMenu.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(heightSpecSize, MeasureSpec.EXACTLY));
        }

        mShadowDrawable.setBounds(0, 0, mDrowShadoWidth, getHeight());
        setMeasuredDimension(widthSpecSize, heightSpecSize);

        // since we are measured we can now find a proper expanded position if necessary
        if (mExpanded) open();
    }

    public boolean isExpanded(){
        return mExpanded;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        final long drawingTime = getDrawingTime();

        float openRatio = (float) mContent.getLeft() / (getWidth() - mOffsetRight);
        float closedRatio = 1 - openRatio;

        if (mTracking || mAnimating || mExpanded) {

            // menu
            final int offset = (int) (-closedRatio * mMenu.getWidth() * PARALLAX_SPEED_RATIO) - mMenu.getLeft();
            mMenu.offsetLeftAndRight(offset);
            drawChild(canvas, mMenu, drawingTime);

            // menuOverlay
            final Paint menuOverlayPaint = mMenuOverlayPaint;
            final int alpha = (int) (MAXIMUM_MENU_ALPHA_OVERLAY * (closedRatio));
            if (alpha > 0) {
                menuOverlayPaint.setColor(Color.argb(alpha, 0, 0, 0));
                canvas.drawRect(0, 0, mMenu.getRight(), getHeight(), mMenuOverlayPaint);
            }

            // gradient
            canvas.save();
            canvas.translate(mContent.getLeft() - mDrowShadoWidth, 0);
            mShadowDrawable.draw(canvas);
            canvas.restore();

            // content
            final Bitmap cache = mContent.getDrawingCache();

            if (cache != null) {
                canvas.drawBitmap(cache, mContent.getLeft(), 0, null);
            } else {
                canvas.save();
                drawChild(canvas, mContent, drawingTime);
                canvas.restore();
            }

            /*
            final int menuWidth = mMenu.getWidth();
            if (menuWidth != 0) {
                final float opennessRatio = (menuWidth - mHost.getLeft()) / (float) menuWidth;

                // We also draw an overlay over the menu indicating the menu is
                // in the process of being visible or invisible.
                onDrawMenuOverlay(canvas, opennessRatio);

                // Finally we draw an arrow indicating the feature we are
                // currently in
                onDrawMenuArrow(canvas, opennessRatio);
            }
            */
        } else {
            canvas.drawColor(Color.WHITE);
            drawChild(canvas, mContent, drawingTime);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mTracking) {
            return;
        }
        mMenu.layout(0, 0, getWidth() - mOffsetRight, getHeight());
        mContent.layout(mContent.getLeft(), 0, mContent.getLeft() + getWidth(), getHeight());
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mLocked) {
            return false;
        }

        final int action = event.getAction();
        float x = event.getX();
        if (action == MotionEvent.ACTION_DOWN
                && event.getX() > mContent.getLeft()
                && event.getX() < (mExpanded ? getRight() : mContent.getLeft() + mBezelHitWidth)) {
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
                    performFling(mContent.getLeft(), velocity, false);
                }
                break;
            }
        }

        return true;
    }

    private void animateClose(int position) {
        prepareTracking(position);
        performFling(position, -mMaximumAcceleration, true);
    }

    private void animateOpen(int position) {
        prepareTracking(position);
        performFling(position, mMaximumAcceleration, true);
    }

    private void performFling(int position, float velocity, boolean always) {
        final int motion;
        if (mExpanded) {
            if (!always && (velocity > mMaximumMajorVelocity ||
                    (position > mFlingTolerance &&
                            velocity > -mMaximumMajorVelocity))) {
                // We are expanded, but they didn't move sufficiently to cause
                // us to retract.  Animate back to the expanded position.
                motion = getWidth() - position - mOffsetRight;
            } else {
                // We are expanded and are now going to animate away.
                motion = -position;
            }
        } else {
            if (always || (velocity > mMaximumMajorVelocity ||
                    (position > (getWidth()) / 2 &&
                            velocity > -mMaximumMajorVelocity))) {
                // We are collapsed, and they moved enough to allow us to expand.
                motion = getWidth() - position - mOffsetRight;
            } else {
                // We are collapsed, but they didn't move sufficiently to cause
                // us to retract.  Animate back to the collapsed position.
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
        moveContent(mExpanded ? position : 0);
    }

    private void moveContent(int position) {
        if (position == EXPANDED_FULL_OPEN) {
            final int togo = (getMeasuredWidth() - mOffsetRight) - mContent.getLeft();
            mContent.offsetLeftAndRight(togo); //todo proper value
        } else if (position == COLLAPSED_FULL_CLOSED) {
            mContent.offsetLeftAndRight(0 - mContent.getLeft());
        } else {
            final int left = mContent.getLeft();
            int deltaX = position - left;
            if (position < 0) {
                deltaX = -left;
            } else if (deltaX > getWidth() - getLeft() - mOffsetRight) {
                deltaX = getWidth() - getLeft() - mOffsetRight;
            }
            mContent.offsetLeftAndRight(deltaX);
        }
        invalidate();
    }

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
        if (!content.isHardwareAccelerated()) content.buildDrawingCache();

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
                if (mContent.getLeft() >= getWidth() / 2) {
                    openMenu();
                } else {
                    closeMenu();
                }
                mAnimating = false;
            }
            super.computeScroll();
        }
    }

    /**
     * Toggles the menu open and close. Takes effect immediately.
     *
     * @see #open()
     * @see #close()
     * @see #animateClose()
     * @see #animateOpen()
     * @see #animateToggle()
     */
    public void toggle() {
        if (!mExpanded) {
            openMenu();
        } else {
            closeMenu();
        }
        invalidate();
        requestLayout();
    }

    /**
     * Toggles the menu open and close with an animation.
     *
     * @see #open()
     * @see #close()
     * @see #animateClose()
     * @see #animateOpen()
     * @see #toggle()
     */
    public void animateToggle() {
        if (!mExpanded) {
            animateOpen();
        } else {
            animateClose();
        }
    }

    /**
     * Opens the menu immediately.
     *
     * @see #toggle()
     * @see #close()
     * @see #animateOpen()
     */
    public void open() {
        openMenu();
        invalidate();
        requestLayout();

        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    /**
     * Closes the menu immediately.
     *
     * @see #toggle()
     * @see #open()
     * @see #animateClose()
     */
    public void close() {
        closeMenu();
        invalidate();
        requestLayout();
    }

    /**
     * Closes the menus with an animation.
     *
     * @see #close()
     * @see #open()
     * @see #animateOpen()
     * @see #animateToggle()
     * @see #toggle()
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
     *
     * @see #close()
     * @see #open()
     * @see #animateClose()
     * @see #animateToggle()
     * @see #toggle()
     */
    public void animateOpen() {
        prepareContent();
        final OnMenuScrollListener scrollListener = mOnMenuScrollListener;
        if (scrollListener != null) {
            scrollListener.onScrollStarted();
        }
        animateOpen(mContent.getLeft());

        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);

        if (scrollListener != null) {
            scrollListener.onScrollEnded();
        }
    }

    private void closeMenu() {
        moveContent(COLLAPSED_FULL_CLOSED);
        mContent.destroyDrawingCache();
        mContent.setVisibility(View.VISIBLE);
        if (!mExpanded) {
            return;
        }

        mExpanded = false;
        if (mOnMenuCloseListener != null) {
            mOnMenuCloseListener.onMenuClosed();
        }
    }

    private void openMenu() {
        moveContent(EXPANDED_FULL_OPEN);
        if (mExpanded) {
            return;
        }

        mExpanded = true;

        if (mOnMenuOpenListener != null) {
            mOnMenuOpenListener.onMenuOpened();
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
        return mExpanded;
    }

    /**
     * Indicates whether the menu is scrolling or flinging.
     *
     * @return True if the menu is scroller or flinging, false otherwise.
     */
    public boolean isMoving() {
        return mTracking || mAnimating;
    }

    private class MenuToggler implements OnClickListener {
        public void onClick(View v) {
            if (mLocked) {
                return;
            }
            // mAllowSingleTap isn't relevant here; you're *always*
            // allowed to open/close the menu by clicking with the
            // trackball.


            if (mAnimateOnClick) {
                animateToggle();
            } else {
                toggle();
            }

        }
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
       boolean expanded;

        SavedState(Parcelable superState) {
          super(superState);
        }

        private SavedState(Parcel in) {
          super(in);
          this.expanded = in.readInt() == 1;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
          super.writeToParcel(out, flags);
          out.writeInt(expanded ? 1 : 0);
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


