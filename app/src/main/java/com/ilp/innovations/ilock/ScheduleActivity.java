package com.ilp.innovations.ilock;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TimePicker;
import android.widget.Toast;

import com.github.kevinsawicki.http.HttpRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ScheduleActivity extends Activity implements View.OnClickListener{

    private SQLiteHandler db;
    private SessionManager session;

    private Button btnUnlockTime;
    private Button btnLockTime;
    private Button timeSetButton;
    private Button btnSaveSchedule;
    private ProgressDialog pDialog;
    private CheckBox dummyCheck;

    private int hour;
    private int minute;

    private HashMap<String,String> user;

    private static String unlockTime;
    private static String lockTime;
    private static String days="";

    static final int TIME_DIALOG_ID = 999;
    private int ACTION;
    private final int LOAD_SCHEDULE=0;
    private final int UPDATE_SCHEDULE=1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        btnUnlockTime = (Button) findViewById(R.id.btnUnlockTime);
        btnLockTime = (Button) findViewById(R.id.btnLockTime);
        btnSaveSchedule = (Button) findViewById(R.id.btnSchedule);
        btnUnlockTime.setOnClickListener(this);
        btnLockTime.setOnClickListener(this);
        btnSaveSchedule.setOnClickListener(this);

        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        db = new SQLiteHandler(getApplicationContext());

        session = new SessionManager(getApplicationContext());
        user = db.getUserDetails();

        if(!session.isLoggedIn())
            logoutUser();

        updateSchedule();

    }

    @Override
    public void onClick(View v) {
        int btnId = v.getId();
        switch(btnId) {
            case R.id.btnUnlockTime:
                timeSetButton = btnUnlockTime;
                if(unlockTime!=null) {
                    try {
                        hour = Integer.parseInt(unlockTime.split(":")[0]);
                        minute = Integer.parseInt(unlockTime.split(":")[1]);
                        Log.d("myTag","Unlock Time : "+hour+":"+minute);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                showDialog(TIME_DIALOG_ID);
                break;
            case R.id.btnLockTime:
                timeSetButton = btnLockTime;
                if(lockTime!=null) {
                    try {
                        hour = Integer.parseInt(lockTime.split(":")[0]);
                        minute = Integer.parseInt(lockTime.split(":")[1]);
                        Log.d("myTag","Lock Time : "+hour+":"+minute);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                showDialog(TIME_DIALOG_ID);
                break;
            case R.id.btnSchedule:
                ACTION = UPDATE_SCHEDULE;
                unlockTime = btnUnlockTime.getText().toString();
                lockTime = btnLockTime.getText().toString();
                if(lockTime.length()==5 && unlockTime.length()==5) {
                    pDialog.setMessage("Updating Schedule");
                    showDialog();
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("tag", "set_schedule");
                    params.put("unlockTime", unlockTime);
                    params.put("lockTime", lockTime);
                    params.put("days", days);
                    params.put("lockId", user.get("lockId"));
                    new HttpRequestTask(params).execute(user.get("server"));
                }
                else {
                    Toast.makeText(getApplicationContext(),"Please select time",
                            Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public void onCheckboxClicked(View time) {
        //coverting the view to Checkbox. Here we are sure that view is Checkbox.
        dummyCheck = (CheckBox) time;
        int id = dummyCheck.getId();
        switch (id) {
            case R.id.cboxSunday:
                if(dummyCheck.isChecked())
                    days = days + "0";
                else
                    days = days.replace("0","");
                break;
            case R.id.cboxMonday:
                if(dummyCheck.isChecked())
                    days = days + "1";
                else
                    days = days.replace("1","");
                break;
            case R.id.cboxTuesday:
                if(dummyCheck.isChecked())
                    days = days + "2";
                else
                    days = days.replace("2","");
                break;
            case R.id.cboxWednesday:
                if(dummyCheck.isChecked())
                    days = days + "3";
                else
                    days = days.replace("3","");
                break;
            case R.id.cboxThursday:
                if(dummyCheck.isChecked())
                    days = days + "4";
                else
                    days = days.replace("4","");
                break;
            case R.id.cboxFriday:
                if(dummyCheck.isChecked())
                    days = days + "5";
                else
                    days = days.replace("5","");
                break;
            case R.id.cboxSaturday:
                if(dummyCheck.isChecked())
                    days = days + "6";
                else
                    days = days.replace("6","");
                break;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case TIME_DIALOG_ID:
                // set time picker as current time
                return new TimePickerDialog(this,
                        timePickerListener, hour, minute,false);

        }
        return null;
    }

    private TimePickerDialog.OnTimeSetListener timePickerListener =
            new TimePickerDialog.OnTimeSetListener() {
                public void onTimeSet(TimePicker view, int selectedHour,
                                      int selectedMinute) {
                    hour = selectedHour;
                    minute = selectedMinute;

                    // set current time into textview
                    timeSetButton.setText(new StringBuilder().append(pad(hour))
                            .append(":").append(pad(minute)));

                    timeSetButton = null;

                }
            };

    private static String pad(int c) {
        if (c >= 10)
            return String.valueOf(c);
        else
            return "0" + String.valueOf(c);
    }

    public void updateSchedule() {
        ACTION = LOAD_SCHEDULE;
        pDialog.setMessage("Loading current schedule!");
        showDialog();
        Map<String,String> params = new HashMap<String,String>();
        params.put("tag", "get_schedule");
        params.put("lockId",user.get("lockId"));
        new HttpRequestTask(params).execute(user.get("server"));
    }

    private void logoutUser() {
        session.setLogin(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(ScheduleActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private class HttpRequestTask extends AsyncTask<String, String, String> {

        private Map<String,String> params;

        public HttpRequestTask(Map<String,String> data) {
            this.params = data;
        }

        protected String doInBackground(String... urls) {

            String response=null;
            try {
                String url = "http://" + urls[0] + "/action.php";
                Log.d("myTag", url);
                response = HttpRequest.post(url)
                        .accept("application/json")
                        .form(params)
                        .body();
                Log.d("myTag","Response-->"+response);

            } catch (HttpRequest.HttpRequestException exception) {
                Log.d("myTag","HttpRequest Exception");
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            return  response;
        }


        protected void onPostExecute(String response) {
            try {
                Log.d("myTag", response);
                JSONObject jObj = new JSONObject(response);
                boolean error = jObj.getBoolean("error");
                if (!error) {
                    // Success content
                    hideDialog();
                    if(ACTION==UPDATE_SCHEDULE)
                        Toast.makeText(getApplicationContext(),
                                "Schedule updated successfully!",
                                Toast.LENGTH_LONG).show();
                    else if(ACTION==LOAD_SCHEDULE) {
                        String unlockTime = jObj.getString("unlockTime");
                        String lockTime = jObj.getString("lockTime");
                        String days = jObj.getString("days");
                        if(unlockTime!=null && !unlockTime.equals("null")) {
                            ScheduleActivity.unlockTime = unlockTime;
                            btnUnlockTime.setText(unlockTime);
                        }
                        if(lockTime!=null && !lockTime.equals("null")) {
                            ScheduleActivity.lockTime = lockTime;
                            btnLockTime.setText(lockTime);
                        }
                        if(days!=null && !days.equals("null"))
                            ScheduleActivity.days = days;

                        ((CheckBox) findViewById(R.id.cboxSunday)).setChecked(false);
                        ((CheckBox) findViewById(R.id.cboxMonday)).setChecked(false);
                        ((CheckBox) findViewById(R.id.cboxTuesday)).setChecked(false);
                        ((CheckBox) findViewById(R.id.cboxWednesday)).setChecked(false);
                        ((CheckBox) findViewById(R.id.cboxThursday)).setChecked(false);
                        ((CheckBox) findViewById(R.id.cboxFriday)).setChecked(false);
                        ((CheckBox) findViewById(R.id.cboxSaturday)).setChecked(false);
                        if(days.contains("0"))
                            ((CheckBox) findViewById(R.id.cboxSunday)).setChecked(true);
                        if(days.contains("1"))
                            ((CheckBox) findViewById(R.id.cboxMonday)).setChecked(true);
                        if(days.contains("2"))
                            ((CheckBox) findViewById(R.id.cboxTuesday)).setChecked(true);
                        if(days.contains("3"))
                            ((CheckBox) findViewById(R.id.cboxWednesday)).setChecked(true);
                        if(days.contains("4"))
                            ((CheckBox) findViewById(R.id.cboxThursday)).setChecked(true);
                        if(days.contains("5"))
                            ((CheckBox) findViewById(R.id.cboxFriday)).setChecked(true);
                        if(days.contains("6"))
                            ((CheckBox) findViewById(R.id.cboxSaturday)).setChecked(true);
                    }
                }
                else {
                    String errorMsg = jObj.getString("error_msg");
                    Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_SHORT).show();
                }
            }  catch(JSONException je) {
                je.printStackTrace();
                Toast.makeText(getApplicationContext(),
                        "Error in response!",
                        Toast.LENGTH_SHORT).show();
            } catch (NullPointerException ne) {
                Toast.makeText(getApplicationContext(),
                        "Error in connection! Please check your connection",
                        Toast.LENGTH_SHORT).show();
            } finally {
                hideDialog();
            }

        }
    }

    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }

}
