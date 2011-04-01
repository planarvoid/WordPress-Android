package com.soundcloud.android.view;

import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.EmailPicker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.ContactsContract;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

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
                intent.putExtra(EmailPicker.SELECTED, ((TextView)((RelativeLayout)v).findViewById(R.id.email)).getText());
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
            
           Cursor c = parent.getContext().getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    new String[] {
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                            ContactsContract.Contacts.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Email._ID,
                            ContactsContract.CommonDataKinds.Email.DATA
                    },
                    ContactsContract.CommonDataKinds.Email.DATA + "=  ?",
                    new String[] {getItem(position).toString()}, null);
            
           if (c != null && c.moveToFirst()){
               
               View view;
               if (convertView == null) {
                   view = ((LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.contacts_list_item, parent, false);
               } else {
                   view = convertView;
               }
               

               ((TextView) view.findViewById(R.id.name)).setText(c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));
               ((TextView) view.findViewById(R.id.email)).setText(c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)));

               Bitmap bitmap = CloudUtils.loadContactPhoto(parent.getContext().getContentResolver(), c.getLong(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)));
               ((ImageView) view.findViewById(R.id.icon))
                       .setImageBitmap(bitmap == null ? BitmapFactory.decodeResource(
                               parent.getContext().getResources(), R.drawable.ic_contact_list_picture) : bitmap);
               return view;
               
           } else {
               TextView text = new TextView(parent.getContext());
               text.setPadding(10, 10, 10, 10);
               text.setTextSize(20f);
               text.setTextColor(parent.getResources().getColor(R.color.white)); // XXX hardcoded color
               text.setText(getItem(position).toString());
               return text;   
           }
           
            
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


