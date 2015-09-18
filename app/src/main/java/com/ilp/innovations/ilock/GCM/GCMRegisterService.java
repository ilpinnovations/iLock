package com.ilp.innovations.ilock.GCM;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.ilp.innovations.ilock.LoginActivity;
import com.ilp.innovations.ilock.SQLiteHandler;
import com.ilp.innovations.ilock.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GCMRegisterService extends IntentService {

    private static final String TAG = "GCMRegistratrionService";
    public static String SENT_TOKEN_TO_SERVER = "sentTokenToServer";
    private static String REGISTRATION_COMPLETE = "registrationComplete";
    private static String REG_ID="gcm_reg_id";
    private static final String[] TOPICS = {"iLockAlert"};
    private SessionManager session;
    private SQLiteHandler db;
    private HashMap<String,String> user;

    public GCMRegisterService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            synchronized (TAG) {

                // GCM Registration starts here
                // Initially this call goes out to the network to retrieve the token, subsequent
                // calls are local.
                InstanceID instanceID = InstanceID.getInstance(this);
                String token = instanceID.getToken(AppConstants.SENDER_ID,
                        GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

                registerOnThirdPartyServer(token);

                //subscription to topics on which the app should accept push messages
                subscribeTopics(token);

                //This boolean value indicates that the token is already sent to 3rd party server
                //
                sharedPreferences.edit().putBoolean(SENT_TOKEN_TO_SERVER, true).apply();
                sharedPreferences.edit().putString(REG_ID,token).apply();

                //GCM Registration ends here
            }
        }catch(IOException e) {
            e.printStackTrace();
            sharedPreferences.edit().putBoolean(SENT_TOKEN_TO_SERVER, false).apply();
        }
        Intent registrationComplete = new Intent(REGISTRATION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    public void registerOnThirdPartyServer(String token) {
        db = new SQLiteHandler(this);
        session = new SessionManager(getApplicationContext());
        user = db.getUserDetails();
        if(user.get("uid")!=null) {
            //add the token to database
            Map<String, String> params = new HashMap<>();
            params.put("tag","reg_gcm_id");
            params.put("uid",user.get("uid"));
            params.put("reg_id",token);
            new HttpRequestTask(params).execute(user.get("server"));
        }
    }

    private void subscribeTopics(String token) throws IOException {
        for (String topic : TOPICS) {
            GcmPubSub pubSub = GcmPubSub.getInstance(this);
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }

    private class HttpRequestTask extends AsyncTask<String, String, String> {

        private Map<String,String> userData;

        public HttpRequestTask(Map<String,String> data) {
            this.userData = data;
        }

        protected String doInBackground(String... urls) {

            String response=null;
            try {
                String url = "http://" + urls[0] + "/action.php";
                Log.d("myTag", url);
                response = HttpRequest.post(url)
                        .accept("application/json")
                        .form(userData)
                        .body();
                Log.d("myTag","Response-->"+response);

            } catch (HttpRequest.HttpRequestException exception) {
                Log.d("myTag","HttpRequest Exception");
                Toast.makeText(getApplicationContext(),"GCM server registration failed",
                        Toast.LENGTH_SHORT).show();
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(),"GCM server registration failed",
                        Toast.LENGTH_SHORT).show();
                return null;
            }
            return  response;
        }


        protected void onPostExecute(String response) {
            Toast.makeText(getApplicationContext(),"GCM registration successfull",Toast.LENGTH_SHORT).show();
        }
    }
}
