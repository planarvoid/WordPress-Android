package com.soundcloud.android.view.create;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.create.EmailPicker;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.provider.ContactsContract;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;

import java.util.List;

public class AccessList extends LinearLayout implements View.OnClickListener {
    private Adapter listAdapter;

    @SuppressWarnings({"UnusedDeclaration"})
    public AccessList(Context context) {
        this(context, null);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public AccessList(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(LinearLayout.VERTICAL);
        listAdapter = new Adapter();
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

    public List<String> get() {
        return listAdapter.getAccessList();
    }

    public void set(List<String> strings) {
        listAdapter.setAccessList(strings);
    }

    public boolean isEmpty() {
        return listAdapter.getCount() == 0;
    }

    public void registerDataSetObserver(DataSetObserver observer) {
        listAdapter.registerDataSetObserver(observer);
    }

    @Override
    public void onClick(View view) {
        if (getContext() instanceof Activity) {
            List<String> accessList = listAdapter.getAccessList();
            Intent intent = new Intent(getContext(), EmailPicker.class);
            if (accessList != null) {
                EmailPickerItem item = (EmailPickerItem) view;

                intent.putExtra(EmailPicker.BUNDLE_KEY,
                        accessList.toArray(new String[accessList.size()]));
                intent.putExtra(EmailPicker.SELECTED, item.getEmail());
            }
            ((Activity) getContext()).startActivityForResult(
                    intent,
                    Consts.RequestCodes.PICK_EMAILS);
        }
    }

    private void handleDataChanged() {
        removeAllViews();
        if (listAdapter.getCount() != 0) {
            for (int i = 0; i < listAdapter.getCount(); i++) {
                View item = listAdapter.getView(i, null, this);
                item.setOnClickListener(this);

                addView(item);
                addView(getSeparator());
            }
        }
    }

    private View getSeparator() {
        final View v = new View(this.getContext());
        v.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, 1));
        v.setBackgroundColor(getResources().getColor(R.color.recordUploadBorder));
        return v;
    }


    private static class Adapter extends BaseAdapter {
        private List<String> mAccessList;

        @Override
        public int getCount() {
            return mAccessList == null ? 0 : mAccessList.size();
        }

        @Override
        public String getItem(int position) {
            return mAccessList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, @Nullable View convertView, ViewGroup parent) {
            final String email = getItem(position);

            EmailPickerItem view;
            if (convertView instanceof EmailPickerItem) {
                view = (EmailPickerItem) convertView;
            } else {
                view = (EmailPickerItem)
                        ((LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                                .inflate(R.layout.email_picker_item, parent, false);
            }

            final Cursor c = parent.getContext().getContentResolver()
                    .query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                            new String[]{
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                                    ContactsContract.Contacts.DISPLAY_NAME,
                                    ContactsContract.CommonDataKinds.Email._ID,
                                    ContactsContract.CommonDataKinds.Email.DATA
                            },
                            ContactsContract.CommonDataKinds.Email.DATA + "=  ?",
                            new String[]{email}, null);

            if (c != null && c.moveToFirst()) {
                view.initializeFromCursor(c);
            } else {
                // set as email as name (for display purposes)
                view.setName(email);
            }

            if (c != null) c.close();
            return view;
        }

        public void setAccessList(@Nullable List<String> accessList) {
            mAccessList = accessList;
            notifyDataSetChanged();
        }

        public List<String> getAccessList() {
            return mAccessList;
        }
    }
}


