package com.ilp.innovations.ilock;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.github.kevinsawicki.http.HttpRequest;
import com.ilp.innovations.ilock.GCM.GCMRegisterService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends Activity implements View.OnClickListener{

    private TextView txtName;
    private TextView txtInfo;
    private Button btnToggle;
    private Button btnSchedule;
    private Button btnLogout;
    private Button btnGraph;
    private ProgressDialog pDialog;
    private SQLiteHandler db;
    private SessionManager session;
    private HashMap<String,String> user;

    private final String LOCK_ON="0";
    private final String LOCK_OFF="1";

    private boolean isLockOn=false;
    private boolean lockShouldToggle = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        boolean sentToken = sharedPreferences
                .getBoolean(GCMRegisterService.SENT_TOKEN_TO_SERVER, false);
        if(!sentToken) {
            Intent gcmServiceIntent = new Intent(this, GCMRegisterService.class);
            startService(gcmServiceIntent);
        }

        txtName = (TextView) findViewById(R.id.name);
        txtInfo = (TextView) findViewById(R.id.info);
        btnToggle = (Button) findViewById(R.id.btnToggle);
        btnSchedule = (Button) findViewById(R.id.btnSchedule);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnGraph = (Button) findViewById(R.id.btnGraph);
        btnToggle.setOnClickListener(this);
        btnSchedule.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
        btnGraph.setOnClickListener(this);

        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        // SQLite database handler
        db = new SQLiteHandler(getApplicationContext());

        session = new SessionManager(getApplicationContext());
        user = db.getUserDetails();

        if(!session.isLoggedIn())
            logoutUser();

        updateLockStatus();

        txtName.setText(user.get("name"));
        txtInfo.setText("Server Address : '" +
                "http://"+user.get("server")+"'\nLock Id : " +
                user.get("lockId"));

    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLockStatus();
    }

    private void updateLockStatus() {
        if(user!=null) {
            pDialog.setMessage("Updating lock status");
            showDialog();
            String lockId = user.get("lockId");
            Map<String, String> params = new HashMap<String, String>();
            params.put("tag", "get_lock_status");
            params.put("lockId", lockId);
            new HttpRequestTask(params).execute(user.get("server"));
        }
    }

    private void toggleLock() {
        if(user!=null) {
            pDialog.setMessage("Setting lock status");
            showDialog();
            String lockId = user.get("lockId");
            Map<String, String> params = new HashMap<String, String>();
            params.put("tag", "set_lock_status");
            params.put("lockId", lockId);
            if(isLockOn)
                params.put("status",LOCK_OFF);
            else
                params.put("status",LOCK_ON);
            new HttpRequestTask(params).execute(user.get("server"));
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btnToggle:
                lockShouldToggle = true;
                updateLockStatus();
                break;
            case R.id.btnSchedule:
                Intent scheduleActivity = new Intent(getApplicationContext(),ScheduleActivity.class);
                startActivity(scheduleActivity);
                break;
            case R.id.btnLogout:
                logoutUser();
                break;
            case R.id.btnGraph:
                Intent chartActivity = new Intent(getApplicationContext(),ChartActivity.class);
                startActivity(chartActivity);
                break;
        }
    }

    private void logoutUser() {
        session.setLogin(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
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
                    // We are handling the actions of btnToggle here
                    // Scheduling is handled in another activity
                    String status = jObj.getString("status");
                    if(status.equals(LOCK_ON)) {
                        isLockOn = true;
                        btnToggle.setText(getString(R.string.open));
                        btnToggle.setBackgroundColor(Color.parseColor("#5bec00"));
                        Log.d("myTag","Locked");
                    }
                    else if (status.equals(LOCK_OFF)) {
                        isLockOn = false;
                        btnToggle.setText(getString(R.string.close));
                        btnToggle.setBackgroundColor(Color.parseColor("#ec0000"));
                        Log.d("myTag","Unlocked");
                    }
                    if(lockShouldToggle)
                    {
                        // This is to enable updateLock function every time before toggleLock
                        lockShouldToggle = false;
                        toggleLock();
                    }
                    else {
                        hideDialog();
                    }
                }
                else {
                    String errorMsg = jObj.getString("error_msg");
                    Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_SHORT).show();
                }
            }  catch(JSONException je) {
                je.printStackTrace();
                hideDialog();
                Toast.makeText(getApplicationContext(),
                        "Error in response!",
                        Toast.LENGTH_SHORT).show();
            } catch (NullPointerException ne) {
                hideDialog();
                Toast.makeText(getApplicationContext(),
                        "Error in connection! Please check your connection",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

}
