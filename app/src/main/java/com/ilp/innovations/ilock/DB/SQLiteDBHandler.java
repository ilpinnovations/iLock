package com.ilp.innovations.ilock.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class SQLiteDBHandler extends SQLiteOpenHelper{

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "test";
    private static final String NOTIFICATION_TABLE = "notifications";

    private static final String KEY_NOTI_ID = "id";
    private static final String KEY_NOTI_CONTENT = "content";
    private static final String KEY_TIME = "time";

    public SQLiteDBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE_QUERY = "CREATE TABLE "+NOTIFICATION_TABLE+"("+KEY_NOTI_ID+" INTEGER " +
                "PRIMARY KEY AUTOINCREMENT,"+KEY_NOTI_CONTENT+" TEXT,"+KEY_TIME+" DATETIME DEFAULT" +
                " CURRENT_TIMESTAMP)";
        db.execSQL(CREATE_TABLE_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+NOTIFICATION_TABLE);
        onCreate(db);
    }

    public void insertNotification(String message) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NOTI_CONTENT,message);
        db.insert(NOTIFICATION_TABLE, null, values);
        db.close();
    }

    public ArrayList<String> getAllNotifications() {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<String> notifications = new ArrayList<>();
        Cursor cursor = db.query(NOTIFICATION_TABLE,new String[] {KEY_NOTI_CONTENT,KEY_TIME},
                null,null,null,null,KEY_NOTI_ID+" DESC",null);
        if(cursor!=null) {
            cursor.moveToFirst();
            do {
                notifications.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        return notifications;
    }
}
