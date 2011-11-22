package com.soundcloud.android.adapter;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.support.v4.widget.CursorAdapter;
import com.soundcloud.android.activity.ScActivity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class ScCursorAdapter extends CursorAdapter implements IScAdapter {

    protected int mPage = 1;
    private Class<?> mLoadModel;

    protected Map<Integer, Drawable> mIconAnimations = new HashMap<Integer, Drawable>();
    protected Set<Integer> mLoadingIcons = new HashSet<Integer>();

    public ScCursorAdapter(Context context, Cursor cursor) {
        this(context,cursor,true);
    }

    public ScCursorAdapter(Context context, Cursor cursor, boolean requery) {
        super(context, cursor, requery);
    }

	public Drawable getDrawableFromPosition(int position){
        return mIconAnimations.get(position);
    }

    @Override
    public Boolean getIconLoading(int position){
        return mLoadingIcons.contains(position);
    }

    @Override
    public void assignDrawableToPosition(Integer position, Drawable drawable) {
        mIconAnimations.put(position, drawable);
    }

    @Override
    public void setIconLoading(Integer position) {
        mLoadingIcons.add(position);
    }


}
