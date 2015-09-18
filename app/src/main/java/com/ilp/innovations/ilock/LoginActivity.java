package com.ilp.innovations.ilock;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.ilp.innovations.ilock.GCM.GCMRegisterService;

public class LoginActivity extends Activity {
    // LogCat tag
    private static final String TAG = RegisterActivity.class.getSimpleName();
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private Button btnLogin;
    private Button btnLinkToRegister;
    private EditText inputServerAddr;
    private EditText inputEmail;
    private EditText inputPassword;
    private ProgressDialog pDialog;
    private SessionManager session;
    private SQLiteHandler db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        inputServerAddr = (EditText) findViewById(R.id.serverAddr);
        inputEmail = (EditText) findViewById(R.id.email);
        inputPassword = (EditText) findViewById(R.id.password);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnLinkToRegister = (Button) findViewById(R.id.btnLinkToRegisterScreen);

        db = new SQLiteHandler(getApplicationContext());

        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        // Session manager
        session = new SessionManager(getApplicationContext());

        // Check if user is already logged in or not
        if (session.isLoggedIn()) {
            // User is already logged in. Take him to main activity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        HashMap<String,String> user  = db.getUserDetails();

        if(user.get("email")!=null)
            inputEmail.setText(user.get("email"));
        if(user.get("server")!=null)
            inputServerAddr.setText(user.get("server"));

        // Login button Click Event
        btnLogin.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                String serverAddr = inputServerAddr.getText().toString();
                String email = inputEmail.getText().toString();
                String password = inputPassword.getText().toString();

                // Check for empty data in the form
                if (email.trim().length() > 0 && password.trim().length() > 0) {
                    String ipExprn = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.)" +
                            "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?$)";
                    String emailExprn = "^([a-z0-9_\\.-]+)@([\\da-z\\.-]+)\\.([a-z\\.]{2,6})$";

                    if(!serverAddr.matches(ipExprn))
                        inputServerAddr.setError("Enter correct address format eg 192.168.43.1");
                    else if(!email.matches(emailExprn))
                        inputEmail.setError("Enter correct email format eg example@domain.com");
                    else if(password.length()<8)
                        inputPassword.setError("Password must have minimum 8 charactors");
                    else
                        checkLogin(serverAddr, email, password);
                } else {
                    // Prompt user to enter credentials
                    Toast.makeText(getApplicationContext(),
                            "Please enter the credentials!", Toast.LENGTH_LONG)
                            .show();
                }
            }

        });

        // Link to Register Screen
        btnLinkToRegister.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(),
                        RegisterActivity.class);
                startActivity(i);
                finish();
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        checkPlayServices();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * function to verify login details in mysql db
     * */
    private void checkLogin(final String serverAddr, final String email, final String password) {
        // Tag used to cancel the request
        String tag_string_req = "req_login";

        pDialog.setMessage("Logging in to server "+serverAddr);
        showDialog();

        Map<String, String> params = new HashMap<String, String>();
        params.put("tag", "login");
        params.put("server",serverAddr);
        params.put("email", email);
        params.put("password", password);

        db.updateServer(serverAddr);

        new HttpRequestTask(params).execute(serverAddr);

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

        private Map<String,String> userData;

        public HttpRequestTask(Map<String,String> data) {
            this.userData = data;
        }

        protected String doInBackground(String... urls) {

            String response=null;
            try {
                String url = "http://" + urls[0] + "/index.php";
                Log.d("myTag", url);
                response = HttpRequest.post(url).accept("application/json")
                        .form(userData)
                        .body();
                Log.d("myTag","Response-->"+response);

            } catch (HttpRequest.HttpRequestException exception) {
                exception.printStackTrace();
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

                // Check for error node in json
                if (!error) {
                    // user successfully logged in
                    // Create login session
                    session.setLogin(true);
                    hideDialog();
                    // Launch main activity
                    JSONObject user = jObj.getJSONObject("user");
                    db.addUser(userData.get("server"), user.getString("lockId"),
                            user.getString("name"),user.getString("email"),jObj.getString("uid"),
                            user.getString("created_at"));
                    Intent intent = new Intent(LoginActivity.this,
                            MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // Error in login. Get the error message
                    String errorMsg = jObj.getString("error_msg");
                    hideDialog();
                    Toast.makeText(getApplicationContext(),
                            errorMsg, Toast.LENGTH_LONG).show();
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