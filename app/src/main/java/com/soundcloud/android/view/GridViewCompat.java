/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The fragments of code has been used from AbsListView.java of Android SDK
 *
 * @author Paramvir Bali
 * @mail paramvir@rokoder.com
 */

package com.soundcloud.android.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

/**
 * https://github.com/paramvir-b/AndroidGridViewCompatLib
 * <p/>
 * This class is based on the GridView. We need to create this class as multiselection comes in API
 * 10 into GridView. So lot of code is copied from the Android Source Code. APIs which we are
 * implementing here are suffixed with 'C' to avoid recursion which when you try to call the API
 * using reflection. So we made the signature different.
 * <p/>
 * <pre>
 * Ref code:
 * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/GridView.java
 * </pre>
 */
public class GridViewCompat extends GridView {
    private static final String TAG = GridViewCompat.class.getSimpleName();
    /**
     * Variables for backward compatibility
     */
    private static boolean inCompatibleMode = false;
    private static Method gridView_getCheckedItemIds;
    private static Method gridView_isItemChecked;
    private static Method gridView_getCheckedItemPosition;
    private static Method gridView_getCheckedItemPositions;
    private static Method gridView_clearChoices;
    private static Method gridView_setItemChecked;
    private static Method gridView_setChoiceMode;
    private static Method gridView_getChoiceMode;
    private static Method gridView_getCheckedItemCount;

    static {
        try {
            inCompatibleMode = false;

            gridView_getChoiceMode = GridView.class.getMethod("getChoiceMode", (Class<?>[]) null);
            gridView_getCheckedItemIds =
                    GridView.class.getMethod("getCheckedItemIds", (Class<?>[]) null);
            gridView_isItemChecked =
                    GridView.class.getMethod("isItemChecked", new Class[]{
                            int.class
                    });
            gridView_getCheckedItemPosition =
                    GridView.class.getMethod("getCheckedItemPosition", (Class<?>[]) null);
            gridView_getCheckedItemPositions =
                    GridView.class.getMethod("getCheckedItemPositions", (Class<?>[]) null);
            gridView_clearChoices = GridView.class.getMethod("clearChoices", (Class<?>[]) null);
            gridView_setItemChecked =
                    GridView.class.getMethod("setItemChecked", new Class[]{
                            int.class,
                            boolean.class
                    });
            gridView_setChoiceMode =
                    GridView.class.getMethod("setChoiceMode", new Class[]{
                            int.class
                    });
            gridView_getCheckedItemCount =
                    GridView.class.getMethod("getCheckedItemCount", (Class<?>[]) null);

        } catch (NoSuchMethodException e) {
            Log.d(TAG, "Running in compatibility mode as '" + e.getMessage() + "' not found");
            // If any of the method is missing, we are in compatibility mode
            inCompatibleMode = true;
            gridView_getCheckedItemIds = null;
            gridView_isItemChecked = null;
            gridView_getCheckedItemPosition = null;
            gridView_getCheckedItemPositions = null;
            gridView_clearChoices = null;
            gridView_setItemChecked = null;
            gridView_setChoiceMode = null;
            gridView_getChoiceMode = null;
            gridView_getCheckedItemCount = null;
        }

    }

    /**
     * Running count of how many items are currently checked
     */
    int checkedItemCountC;
    /**
     * Running state of which positions are currently checked
     */
    SparseBooleanArray checkStatesC;
    /**
     * Running state of which IDs are currently checked. If there is a value for a given key, the
     * checked state for that ID is true and the value holds the last known position in the adapter
     * for that id.
     */
    LongSparseArray<Integer> checkedIdStatesC;
    /**
     * Controls if/how the user may choose/check items in the list
     */
    int choiceModeC = ListView.CHOICE_MODE_NONE;

    public GridViewCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
    }

    public GridViewCompat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttrs(attrs);
    }

    public GridViewCompat(Context context) {
        super(context);
    }

    // XXX these should be changed to reflect the actual memory allocator we
    // use.
    // it looks like right now objects want to be powers of 2 minus 8
    // and the array size eats another 4 bytes

    public int getNumColumnsCompat() {
        if (Build.VERSION.SDK_INT >= 11) {
            return getNumColumnsCompat11();

        } else {
            int columns = 0;
            int children = getChildCount();
            if (children > 0) {
                int width = getChildAt(0).getMeasuredWidth();
                if (width > 0) {
                    columns = getWidth() / width;
                }
            }
            return columns > 0 ? columns : AUTO_FIT;
        }
    }

    /**
     * WARN Do not call the default api
     *
     * @return The current choice mode
     * @see #setChoiceMode(int)
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public int getChoiceMode() {
        if (!inCompatibleMode && gridView_getChoiceMode != null) {
            return super.getChoiceMode();
        }
        return choiceModeC;
    }

    /**
     * WARN Do not call the default api
     *
     * @param choiceMode
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void setChoiceMode(int choiceMode) {
        if (!inCompatibleMode) {
            super.setChoiceMode(choiceMode);
            return;
        }

        // Code copied from Android source
        choiceModeC = choiceMode;
        if (choiceModeC != ListView.CHOICE_MODE_NONE) {
            if (checkStatesC == null) {
                checkStatesC = new SparseBooleanArray();
            }
            if (checkedIdStatesC == null && getAdapter() != null && getAdapter().hasStableIds()) {
                checkedIdStatesC = new LongSparseArray<Integer>();
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see android.widget.GridView#setAdapter(android.widget.ListAdapter)
     */
    @Override
    public void setAdapter(ListAdapter adapter) {
        if (!inCompatibleMode) {
            super.setAdapter(adapter);
            return;
        }

        // Code copied from Android source
        super.setAdapter(adapter);
        if (adapter != null) {
            if (choiceModeC != ListView.CHOICE_MODE_NONE && getAdapter().hasStableIds()
                    && checkedIdStatesC == null) {
                checkedIdStatesC = new LongSparseArray<Integer>();
            }
        }

        if (checkStatesC != null) {
            checkStatesC.clear();
        }

        if (checkedIdStatesC != null) {
            checkedIdStatesC.clear();
        }

    }

    /**
     * WARN Do not call the default api
     *
     * @return
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public long[] getCheckedItemIds() {
        if (!inCompatibleMode && gridView_getCheckedItemIds != null) {
            return super.getCheckedItemIds();
        }

        // Code copied from Android source
        if (choiceModeC == ListView.CHOICE_MODE_NONE || checkedIdStatesC == null
                || getAdapter() == null) {
            return new long[0];
        }

        final LongSparseArray<Integer> idStates = checkedIdStatesC;
        final int count = idStates.size();
        final long[] ids = new long[count];

        for (int i = 0; i < count; i++) {
            ids[i] = idStates.keyAt(i);
        }

        return ids;
    }

    /**
     * WARN Do not call the default api
     *
     * @param position
     * @return
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public boolean isItemChecked(int position) {
        if (!inCompatibleMode && gridView_isItemChecked != null) {
            return super.isItemChecked(position);
        }

        // Code copied from Android source
        if (choiceModeC != ListView.CHOICE_MODE_NONE && checkStatesC != null) {
            return checkStatesC.get(position);
        }

        return false;
    }

    /**
     * WARN Do not call the default api
     *
     * @return
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public int getCheckedItemPosition() {
        if (!inCompatibleMode && gridView_getCheckedItemPosition != null) {
            return super.getCheckedItemPosition();
        }

        // Code copied from Android source
        if (choiceModeC == ListView.CHOICE_MODE_SINGLE && checkStatesC != null
                && checkStatesC.size() == 1) {
            return checkStatesC.keyAt(0);
        }

        return INVALID_POSITION;
    }

    /**
     * WARN Do not call the default api
     *
     * @return
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public SparseBooleanArray getCheckedItemPositions() {
        if (!inCompatibleMode) {
            return super.getCheckedItemPositions();
        }

        // Code copied from Android source
        if (choiceModeC != ListView.CHOICE_MODE_NONE) {
            return checkStatesC;
        }
        return null;
    }

    /**
     * WARN Do not call the default api
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void clearChoices() {
        if (!inCompatibleMode) {
            super.clearChoices();
            return;
        }

        // Code copied from Android source
        if (checkStatesC != null) {
            checkStatesC.clear();
        }
        if (checkedIdStatesC != null) {
            checkedIdStatesC.clear();
        }
        checkedItemCountC = 0;
    }

    /**
     * WARN Do not call the default api
     * <p/>
     * <pre>
     *
     * public void setItemChecked(int position, boolean value) {
     *     if (mChoiceMode == CHOICE_MODE_NONE) {
     *         return;
     *     }
     *
     *     // Start selection mode if needed. We don't need to if we're unchecking
     *     // something.
     *     if (value &amp;&amp; mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL &amp;&amp; mChoiceActionMode == null) {
     *         mChoiceActionMode = startActionMode(mMultiChoiceModeCallback);
     *     }
     *
     *     if (mChoiceMode == CHOICE_MODE_MULTIPLE || mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL) {
     *         boolean oldValue = mCheckStates.get(position);
     *         mCheckStates.put(position, value);
     *         if (mCheckedIdStates != null &amp;&amp; mAdapter.hasStableIds()) {
     *             if (value) {
     *                 mCheckedIdStates.put(mAdapter.getItemId(position), position);
     *             } else {
     *                 mCheckedIdStates.delete(mAdapter.getItemId(position));
     *             }
     *         }
     *         if (oldValue != value) {
     *             if (value) {
     *                 mCheckedItemCount++;
     *             } else {
     *                 mCheckedItemCount--;
     *             }
     *         }
     *         if (mChoiceActionMode != null) {
     *             final long id = mAdapter.getItemId(position);
     *             mMultiChoiceModeCallback.onItemCheckedStateChanged(mChoiceActionMode, position, id,
     *                 value);
     *         }
     *     } else {
     *         boolean updateIds = mCheckedIdStates != null &amp;&amp; mAdapter.hasStableIds();
     *         // Clear all values if we're checking something, or unchecking the
     *         // currently
     *         // selected item
     *         if (value || isItemChecked(position)) {
     *             mCheckStates.clear();
     *             if (updateIds) {
     *                 mCheckedIdStates.clear();
     *             }
     *         }
     *         // this may end up selecting the value we just cleared but this way
     *         // we ensure length of mCheckStates is 1, a fact getCheckedItemPosition
     *         // relies on
     *         if (value) {
     *             mCheckStates.put(position, true);
     *             if (updateIds) {
     *                 mCheckedIdStates.put(mAdapter.getItemId(position), position);
     *             }
     *             mCheckedItemCount = 1;
     *         } else if (mCheckStates.size() == 0 || !mCheckStates.valueAt(0)) {
     *             mCheckedItemCount = 0;
     *         }
     *     }
     *
     *     // Do not generate a data change while we are in the layout phase
     *     if (!mInLayout &amp;&amp; !mBlockLayoutRequests) {
     *         mDataChanged = true;
     *         rememberSyncState();
     *         requestLayout();
     *     }
     * }
     *
     * We are using it where we dont have access to private members and we need to update views
     * public void invalidateViews() {
     *     mDataChanged = true;
     *     rememberSyncState();
     *     requestLayout();
     *     invalidate();
     * }
     * </pre>
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB) @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    public void setItemChecked(int position, boolean value) {
        if (!inCompatibleMode) {
            super.setItemChecked(position, value);
            return;
        }

        // Code copied from Android source. The code below is slightly
        // different.
        if (choiceModeC == ListView.CHOICE_MODE_NONE) {
            return;
        }

        if (choiceModeC == ListView.CHOICE_MODE_MULTIPLE) {
            boolean oldValue = checkStatesC.get(position);
            checkStatesC.put(position, value);
            if (checkedIdStatesC != null && getAdapter().hasStableIds()) {
                if (value) {
                    checkedIdStatesC.put(getAdapter().getItemId(position), position);
                } else {
                    checkedIdStatesC.delete(getAdapter().getItemId(position));
                }
            }
            if (oldValue != value) {
                if (value) {
                    checkedItemCountC++;
                } else {
                    checkedItemCountC--;
                }
            }
        } else {
            boolean updateIds = checkedIdStatesC != null && getAdapter().hasStableIds();
            // Clear all values if we're checking something, or unchecking the
            // currently
            // selected item
            if (value || isItemChecked(position)) {
                checkStatesC.clear();
                if (updateIds) {
                    checkedIdStatesC.clear();
                }
            }
            // this may end up selecting the value we just cleared but this way
            // we ensure length of mCheckStates is 1, a fact
            // getCheckedItemPosition relies on
            if (value) {
                checkStatesC.put(position, true);
                if (updateIds) {
                    checkedIdStatesC.put(getAdapter().getItemId(position), position);
                }
                checkedItemCountC = 1;
            } else if (checkStatesC.size() == 0 || !checkStatesC.valueAt(0)) {
                checkedItemCountC = 0;
            }
        }

        // Since we dont have access to private members this is the closest we
        // can get.
        invalidateViews();
    }

    /**
     * <pre>
     *  public boolean performItemClick(View view, int position, long id) {
     *      boolean handled = false;
     *      boolean dispatchItemClick = true;
     *
     *      if (mChoiceMode != CHOICE_MODE_NONE) {
     *          handled = true;
     *
     *          if (mChoiceMode == CHOICE_MODE_MULTIPLE
     *              || (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL &amp;&amp; mChoiceActionMode != null)) {
     *              boolean newValue = !mCheckStates.get(position, false);
     *              mCheckStates.put(position, newValue);
     *              if (mCheckedIdStates != null &amp;&amp; mAdapter.hasStableIds()) {
     *                  if (newValue) {
     *                      mCheckedIdStates.put(mAdapter.getItemId(position), position);
     *                  } else {
     *                      mCheckedIdStates.delete(mAdapter.getItemId(position));
     *                  }
     *              }
     *              if (newValue) {
     *                  mCheckedItemCount++;
     *              } else {
     *                  mCheckedItemCount--;
     *              }
     *              if (mChoiceActionMode != null) {
     *                  mMultiChoiceModeCallback.onItemCheckedStateChanged(mChoiceActionMode, position, id,
     *                      newValue);
     *                  dispatchItemClick = false;
     *              }
     *          } else if (mChoiceMode == CHOICE_MODE_SINGLE) {
     *              boolean newValue = !mCheckStates.get(position, false);
     *              if (newValue) {
     *                  mCheckStates.clear();
     *                  mCheckStates.put(position, true);
     *                  if (mCheckedIdStates != null &amp;&amp; mAdapter.hasStableIds()) {
     *                      mCheckedIdStates.clear();
     *                      mCheckedIdStates.put(mAdapter.getItemId(position), position);
     *                  }
     *                  mCheckedItemCount = 1;
     *              } else if (mCheckStates.size() == 0 || !mCheckStates.valueAt(0)) {
     *                  mCheckedItemCount = 0;
     *              }
     *          }
     *
     *          mDataChanged = true;
     *          rememberSyncState();
     *          requestLayout();
     *      }
     *
     *      if (dispatchItemClick) {
     *          handled |= super.performItemClick(view, position, id);
     *      }
     *
     *      return handled;
     *  }
     *
     * We are using it where we dont have access to private members and we need to update views
     *  public void invalidateViews() {
     *      mDataChanged = true;
     *      rememberSyncState();
     *      requestLayout();
     *      invalidate();
     *  }
     *
     * </pre>
     */
    @Override @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    public boolean performItemClick(View view, int position, long id) {
        if (!inCompatibleMode) {
            return super.performItemClick(view, position, id);
        }

        boolean handled = false;
        boolean dispatchItemClick = true;

        if (choiceModeC != ListView.CHOICE_MODE_NONE) {
            handled = true;

            if (choiceModeC == ListView.CHOICE_MODE_MULTIPLE) {
                boolean newValue = !checkStatesC.get(position, false);
                checkStatesC.put(position, newValue);
                if (checkedIdStatesC != null && getAdapter().hasStableIds()) {
                    if (newValue) {
                        checkedIdStatesC.put(getAdapter().getItemId(position), position);
                    } else {
                        checkedIdStatesC.delete(getAdapter().getItemId(position));
                    }
                }
                if (newValue) {
                    checkedItemCountC++;
                } else {
                    checkedItemCountC--;
                }
            } else if (choiceModeC == ListView.CHOICE_MODE_SINGLE) {
                boolean newValue = !checkStatesC.get(position, false);
                if (newValue) {
                    checkStatesC.clear();
                    checkStatesC.put(position, true);
                    if (checkedIdStatesC != null && getAdapter().hasStableIds()) {
                        checkedIdStatesC.clear();
                        checkedIdStatesC.put(getAdapter().getItemId(position), position);
                    }
                    checkedItemCountC = 1;
                } else if (checkStatesC.size() == 0 || !checkStatesC.valueAt(0)) {
                    checkedItemCountC = 0;
                }
            }

            // Since we dont have access to private members this is the closest
            // we can get.
            invalidateViews();
        }

        if (dispatchItemClick) {
            handled |= super.performItemClick(view, position, id);
        }

        return handled;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        if (!inCompatibleMode) {
            return super.onSaveInstanceState();
        }

        // Restoring the state if we are in compatible mode
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);

        if (checkStatesC != null) {
            ss.checkState = makeClone(checkStatesC);
        }

        if (checkedIdStatesC != null) {
            final LongSparseArray<Integer> idState = new LongSparseArray<Integer>();
            final int count = checkedIdStatesC.size();
            for (int i = 0; i < count; i++) {
                idState.put(checkedIdStatesC.keyAt(i), checkedIdStatesC.valueAt(i));
            }
            ss.checkIdState = idState;
        }
        ss.checkedItemCount = checkedItemCountC;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!inCompatibleMode) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());

        if (ss.checkState != null) {
            checkStatesC = ss.checkState;
        }

        if (ss.checkIdState != null) {
            checkedIdStatesC = ss.checkIdState;
        }

        checkedItemCountC = ss.checkedItemCount;

        // Since we dont have access to private members this is the closest we
        // can get.
        invalidateViews();
    }

    /**
     * WARN Do not call the default api
     *
     * @return
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public int getCheckedItemCount() {
        if (!inCompatibleMode) {
            return super.getCheckedItemCount();
        }

        return checkedItemCountC;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private int getNumColumnsCompat11() {
        return getNumColumns();
    }

    /**
     * This api is not implemented yet but can be implemented if you want to set the multi-selection
     * from the xml file not from the code
     */
    private void initAttrs(AttributeSet attrs) {

    }

    private SparseBooleanArray makeClone(SparseBooleanArray sba) {
        // Code copied from Android source
        SparseBooleanArray sbaClone = new SparseBooleanArray();
        int sbaLen = sba.size();
        for (int i = 0; i < sbaLen; i++) {
            int key = sba.keyAt(i);
            sbaClone.put(key, sba.get(key));
        }
        return sbaClone;
    }

    /**
     * SparseArrays map longs to Objects. Unlike a normal array of Objects, there can be gaps in the
     * indices. It is intended to be more efficient than using a HashMap to map Longs to Objects.
     * <p/>
     * <pre>
     * Source : https://github.com/android/platform_frameworks_base/blob/master/core/java/android/util/LongSparseArray.java
     * </pre>
     *
     * @hide
     */
    private static class LongSparseArray<E> {
        private static final Object DELETED = new Object();
        private boolean garbage = false;
        private long[] keys;
        private Object[] values;
        private int size;

        /**
         * Creates a new SparseArray containing no mappings.
         */
        public LongSparseArray() {
            this(10);
        }

        /**
         * Creates a new SparseArray containing no mappings that will not require any additional
         * memory allocation to store the specified number of mappings.
         */
        public LongSparseArray(int initialCapacity) {
            final int idealArraySize = ArrayUtils.idealIntArraySize(initialCapacity);

            keys = new long[idealArraySize];
            values = new Object[idealArraySize];
            size = 0;
        }

        /**
         * @return A copy of all keys contained in the sparse array.
         */
        public long[] getKeys() {
            int length = keys.length;
            long[] result = new long[length];
            System.arraycopy(keys, 0, result, 0, length);
            return result;
        }

        /**
         * Sets all supplied keys to the given unique value.
         *
         * @param keyArray    Keys to set
         * @param uniqueValue Value to set all supplied keys to
         */
        public void setValues(long[] keyArray, E uniqueValue) {
            int length = keyArray.length;
            for (int i = 0; i < length; i++) {
                put(keyArray[i], uniqueValue);
            }
        }

        /**
         * Gets the Object mapped from the specified key, or <code>null</code> if no such mapping
         * has been made.
         */
        public E get(long key) {
            return get(key, null);
        }

        /**
         * Gets the Object mapped from the specified key, or the specified Object if no such mapping
         * has been made.
         */
        public E get(long key, E valueIfKeyNotFound) {
            int i = binarySearch(keys, 0, size, key);

            if (i < 0 || values[i] == DELETED) {
                return valueIfKeyNotFound;
            } else {
                return (E) values[i];
            }
        }

        /**
         * Removes the mapping from the specified key, if there was any.
         */
        public void delete(long key) {
            int i = binarySearch(keys, 0, size, key);

            if (i >= 0) {
                if (values[i] != DELETED) {
                    values[i] = DELETED;
                    garbage = true;
                }
            }
        }

        /**
         * Alias for {@link #delete(long)}.
         */
        public void remove(long key) {
            delete(key);
        }

        /**
         * Adds a mapping from the specified key to the specified value, replacing the previous
         * mapping from the specified key if there was one.
         */
        public void put(long key, E value) {
            int i = binarySearch(keys, 0, size, key);

            if (i >= 0) {
                values[i] = value;
            } else {
                i = ~i;

                if (i < size && values[i] == DELETED) {
                    keys[i] = key;
                    values[i] = value;
                    return;
                }

                if (garbage && size >= keys.length) {
                    gc();

                    // Search again because indices may have changed.
                    i = ~binarySearch(keys, 0, size, key);
                }

                if (size >= keys.length) {
                    int n = ArrayUtils.idealIntArraySize(size + 1);

                    long[] nkeys = new long[n];
                    Object[] nvalues = new Object[n];

                    // Log.e("SparseArray", "grow " + keys.length + " to " +
                    // n);
                    System.arraycopy(keys, 0, nkeys, 0, keys.length);
                    System.arraycopy(values, 0, nvalues, 0, values.length);

                    keys = nkeys;
                    values = nvalues;
                }

                if (size - i != 0) {
                    // Log.e("SparseArray", "move " + (size - i));
                    System.arraycopy(keys, i, keys, i + 1, size - i);
                    System.arraycopy(values, i, values, i + 1, size - i);
                }

                keys[i] = key;
                values[i] = value;
                size++;
            }
        }

        /**
         * Returns the number of key-value mappings that this SparseArray currently stores.
         */
        public int size() {
            if (garbage) {
                gc();
            }

            return size;
        }

        /**
         * Given an index in the range <code>0...size()-1</code>, returns the key from the
         * <code>index</code>th key-value mapping that this SparseArray stores.
         */
        public long keyAt(int index) {
            if (garbage) {
                gc();
            }

            return keys[index];
        }

        /**
         * Given an index in the range <code>0...size()-1</code>, returns the value from the
         * <code>index</code>th key-value mapping that this SparseArray stores.
         */
        public E valueAt(int index) {
            if (garbage) {
                gc();
            }

            return (E) values[index];
        }

        /**
         * Given an index in the range <code>0...size()-1</code>, sets a new value for the
         * <code>index</code>th key-value mapping that this SparseArray stores.
         */
        public void setValueAt(int index, E value) {
            if (garbage) {
                gc();
            }

            values[index] = value;
        }

        /**
         * Returns the index for which {@link #keyAt} would return the specified key, or a negative
         * number if the specified key is not mapped.
         */
        public int indexOfKey(long key) {
            if (garbage) {
                gc();
            }

            return binarySearch(keys, 0, size, key);
        }

        /**
         * Returns an index for which {@link #valueAt} would return the specified key, or a negative
         * number if no keys map to the specified value. Beware that this is a linear search, unlike
         * lookups by key, and that multiple keys can map to the same value and this will find only
         * one of them.
         */
        public int indexOfValue(E value) {
            if (garbage) {
                gc();
            }

            for (int i = 0; i < size; i++) {
                if (values[i] == value) {
                    return i;
                }
            }

            return -1;
        }

        /**
         * Removes all key-value mappings from this SparseArray.
         */
        public void clear() {
            int n = size;
            Object[] values = this.values;

            for (int i = 0; i < n; i++) {
                values[i] = null;
            }

            size = 0;
            garbage = false;
        }

        /**
         * Puts a key/value pair into the array, optimizing for the case where the key is greater
         * than all existing keys in the array.
         */
        public void append(long key, E value) {
            if (size != 0 && key <= keys[size - 1]) {
                put(key, value);
                return;
            }

            if (garbage && size >= keys.length) {
                gc();
            }

            int pos = size;
            if (pos >= keys.length) {
                int n = ArrayUtils.idealIntArraySize(pos + 1);

                long[] nkeys = new long[n];
                Object[] nvalues = new Object[n];

                // Log.e("SparseArray", "grow " + keys.length + " to " + n);
                System.arraycopy(keys, 0, nkeys, 0, keys.length);
                System.arraycopy(values, 0, nvalues, 0, values.length);

                keys = nkeys;
                values = nvalues;
            }

            keys[pos] = key;
            values[pos] = value;
            size = pos + 1;
        }

        private void gc() {
            // Log.e("SparseArray", "gc start with " + size);

            int n = size;
            int o = 0;
            long[] keys = this.keys;
            Object[] values = this.values;

            for (int i = 0; i < n; i++) {
                Object val = values[i];

                if (val != DELETED) {
                    if (i != o) {
                        keys[o] = keys[i];
                        values[o] = val;
                    }

                    o++;
                }
            }

            garbage = false;
            size = o;

            // Log.e("SparseArray", "gc end with " + size);
        }

        private static int binarySearch(long[] a, int start, int len, long key) {
            int high = start + len, low = start - 1, guess;

            while (high - low > 1) {
                guess = (high + low) / 2;

                if (a[guess] < key) {
                    low = guess;
                } else {
                    high = guess;
                }
            }

            if (high == start + len) {
                return ~(start + len);
            } else if (a[high] == key) {
                return high;
            } else {
                return ~high;
            }
        }

        private void checkIntegrity() {
            for (int i = 1; i < size; i++) {
                if (keys[i] <= keys[i - 1]) {
                    for (int j = 0; j < size; j++) {
                        Log.e("FAIL", j + ": " + keys[j] + " -> " + values[j]);
                    }

                    throw new RuntimeException();
                }
            }
        }
    }

    /**
     * ArrayUtils contains some methods that you can call to find out the most efficient increments
     * by which to grow arrays. *
     * <p/>
     * <pre>
     * Source : https://github.com/android/platform_frameworks_base/blob/master/core/java/com/android/internal/util/ArrayUtils.java
     * </pre>
     */
    private static class ArrayUtils {
        private static final int CACHE_SIZE = 73;
        private static Object[] EMPTY = new Object[0];
        private static Object[] sCache = new Object[CACHE_SIZE];

        private ArrayUtils() { /* cannot be instantiated */
        }

        public static int idealByteArraySize(int need) {
            for (int i = 4; i < 32; i++) {
                if (need <= (1 << i) - 12) {
                    return (1 << i) - 12;
                }
            }

            return need;
        }

        public static int idealBooleanArraySize(int need) {
            return idealByteArraySize(need);
        }

        public static int idealShortArraySize(int need) {
            return idealByteArraySize(need * 2) / 2;
        }

        public static int idealCharArraySize(int need) {
            return idealByteArraySize(need * 2) / 2;
        }

        public static int idealIntArraySize(int need) {
            return idealByteArraySize(need * 4) / 4;
        }

        public static int idealFloatArraySize(int need) {
            return idealByteArraySize(need * 4) / 4;
        }

        public static int idealObjectArraySize(int need) {
            return idealByteArraySize(need * 4) / 4;
        }

        public static int idealLongArraySize(int need) {
            return idealByteArraySize(need * 8) / 8;
        }

        /**
         * Checks if the beginnings of two byte arrays are equal.
         *
         * @param array1 the first byte array
         * @param array2 the second byte array
         * @param length the number of bytes to check
         * @return true if they're equal, false otherwise
         */
        public static boolean equals(byte[] array1, byte[] array2, int length) {
            if (array1 == array2) {
                return true;
            }
            if (array1 == null || array2 == null || array1.length < length
                    || array2.length < length) {
                return false;
            }
            for (int i = 0; i < length; i++) {
                if (array1[i] != array2[i]) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Returns an empty array of the specified type. The intent is that it will return the same
         * empty array every time to avoid reallocation, although this is not guaranteed.
         */
        public static <T> T[] emptyArray(Class<T> kind) {
            if (kind == Object.class) {
                return (T[]) EMPTY;
            }

            int bucket = ((System.identityHashCode(kind) / 8) & 0x7FFFFFFF) % CACHE_SIZE;
            Object cache = sCache[bucket];

            if (cache == null || cache.getClass().getComponentType() != kind) {
                cache = Array.newInstance(kind, 0);
                sCache[bucket] = cache;

                // Log.e("cache", "new empty " + kind.getName() + " at " +
                // bucket);
            }

            return (T[]) cache;
        }

        /**
         * Checks that value is present as at least one of the elements of the array.
         *
         * @param array the array to check in
         * @param value the value to check for
         * @return true if the value is present in the array
         */
        public static <T> boolean contains(T[] array, T value) {
            for (T element : array) {
                if (element == null) {
                    if (value == null) {
                        return true;
                    }
                } else {
                    if (value != null && element.equals(value)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public static boolean contains(int[] array, int value) {
            for (int element : array) {
                if (element == value) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Class to save the state for the compatibility version of GridView
     */
    static class SavedState extends BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
        int checkedItemCount;
        SparseBooleanArray checkState;
        LongSparseArray<Integer> checkIdState;

        /**
         * Constructor called from {@link AbsListView#onSaveInstanceState()}
         */
        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            checkedItemCount = in.readInt();
            checkState = in.readSparseBooleanArray();
            final int N = in.readInt();
            if (N > 0) {
                checkIdState = new LongSparseArray<Integer>();
                for (int i = 0; i < N; i++) {
                    final long key = in.readLong();
                    final int value = in.readInt();
                    checkIdState.put(key, value);
                }
            }
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(checkedItemCount);
            out.writeSparseBooleanArray(checkState);
            final int N = checkIdState != null ? checkIdState.size() : 0;
            out.writeInt(N);
            for (int i = 0; i < N; i++) {
                out.writeLong(checkIdState.keyAt(i));
                out.writeInt(checkIdState.valueAt(i));
            }
        }

        @Override
        public String toString() {
            return "AbsListView.SavedState{" + Integer.toHexString(System.identityHashCode(this))
                    + " checkState=" + checkState + "}";
        }
    }

}
