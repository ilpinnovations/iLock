package com.ilp.innovations.ilock.GCM;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.ilp.innovations.ilock.DB.SQLiteDBHandler;
import com.ilp.innovations.ilock.R;
import com.ilp.innovations.ilock.SQLiteHandler;
import com.ilp.innovations.ilock.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;

public class NotificationActivity extends Activity {


    private ListView notificationView;
    private ArrayList<String> notificationList;
    private NotificationAdapter notificationAdapter;
    private SQLiteDBHandler dbNotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        notificationView = (ListView) findViewById(R.id.notificationList);
        notificationList = new ArrayList<String>();
        notificationAdapter = new NotificationAdapter();
        dbNotification = new SQLiteDBHandler(this);

        notificationView.setAdapter(notificationAdapter);
        notificationList = dbNotification.getAllNotifications();
        notificationAdapter.notifyDataSetChanged();

    }

    private class NotificationAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return notificationList.size();
        }

        @Override
        public String getItem(int position) {
            return notificationList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView==null) {
                convertView = View.inflate(NotificationActivity.this,R.layout.notification_item,null);
                new ViewHolder(convertView);
            }
            ViewHolder holder = (ViewHolder)convertView.getTag();
            holder.txtMessage.setText(getItem(position));
            return convertView;
        }

        class ViewHolder{
            TextView txtMessage;

            public ViewHolder(View view) {
                txtMessage = (TextView) view.findViewById(R.id.notificationContent);
                view.setTag(this);
            }
        }
    }
}
