package com.soundcloud.android.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.EmailPicker;

import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;

@SuppressWarnings({"UnusedDeclaration"})
public class AccessList extends LinearLayout implements View.OnClickListener {
    private Adapter listAdapter;

    public AccessList(Context context) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
    }

    public AccessList(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(LinearLayout.VERTICAL);
    }

    protected void handleDataChanged() {
        Log.d(TAG, "handleDataChanged()");

        removeAllViews();

        if (listAdapter.getCount() == 0) {
            TextView view = new TextView(getContext(), null, R.style.txt_record_rdo);
            view.setText("With Access - Only you");
            view.setTextSize(20f);
            view.setTextColor(getResources().getColor(R.color.white)); // XXX hardcoded styles

            addView(view);
            view.setOnClickListener(this);
        } else {
            for (int i = 0; i < listAdapter.getCount(); i++) {
                View item = listAdapter.getView(i, null, this);
                item.setOnClickListener(this);

                addView(item);
                addView(getSeparator());
            }
        }
    }

    public Adapter getAdapter() {
        return listAdapter;
    }

    public void setAdapter(Adapter listAdapter) {
        this.listAdapter = listAdapter;
        listAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                handleDataChanged();
            }

            @Override
            public void onInvalidated() {
                invalidate();
            }
        });
    }

    private View getSeparator() {
        final View v = new View(this.getContext());
        v.setLayoutParams(new LayoutParams(
                LayoutParams.FILL_PARENT,
                1));

        v.setBackgroundColor(getResources().getColor(R.color.recordUploadBorder));
        return v;
    }

    @Override
    public void onClick(View v) {
        if (getContext() instanceof  Activity) {
            List<String> accessList = getAdapter().getAccessList();
            Intent intent = new Intent(getContext(), EmailPicker.class);
            if (accessList != null) {
                intent.putExtra(EmailPicker.BUNDLE_KEY, accessList.toArray(new String[accessList.size()]));
                intent.putExtra(EmailPicker.SELECTED, ((TextView)v).getText());
            }
            ((Activity)getContext()).startActivityForResult(
                    intent,
                    EmailPicker.PICK_EMAILS);
        }
    }

    public static class Adapter extends BaseAdapter {
        private List<String> mAccessList;

        @Override
        public int getCount() {
            return mAccessList == null ? 0 : mAccessList.size();
        }

        @Override
        public Object getItem(int position) {
            return mAccessList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView text = new TextView(parent.getContext());
            text.setTextSize(20f);
            text.setTextColor(parent.getResources().getColor(R.color.white)); // XXX hardcoded color
            text.setText(getItem(position).toString());
            return text;
        }

        public void setAccessList(List<String> accessList) {
            this.mAccessList = accessList;
            notifyDataSetChanged();
        }

        public List<String> getAccessList() {
            return mAccessList;
        }
    }
}


