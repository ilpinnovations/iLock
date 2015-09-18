package com.ilp.innovations.ilock;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.github.kevinsawicki.http.HttpRequest;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class ChartActivity extends Activity{

    private BarChart myChart;
    private ProgressDialog pDialog;
    private SQLiteHandler db;
    private SessionManager session;
    private HashMap<String,String> user;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        // SQLite database handler
        db = new SQLiteHandler(getApplicationContext());

        session = new SessionManager(getApplicationContext());
        user = db.getUserDetails();

        if(!session.isLoggedIn())
            logoutUser();


        Map<String,String> params = new HashMap<String,String>();
        params.put("tag","get_stats");
        params.put("lockId",user.get("lockId"));

        myChart = (BarChart) findViewById(R.id.chart);
        myChart.setClickable(false);
        myChart.setHighlightIndicatorEnabled(false);
        pDialog.setMessage("Getting data...");
        showDialog();
        new HttpRequestTask(params).execute(user.get("server"));



    }

    private void logoutUser() {
        session.setLogin(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(ChartActivity.this, LoginActivity.class);
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
                exception.printStackTrace();
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }   finally {
                hideDialog();
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
                    ArrayList<BarEntry> yVals = new ArrayList<BarEntry>();
                    ArrayList<String> xVals = new ArrayList<String>();
                    JSONObject data = jObj.getJSONObject("data");
                    Iterator<?> keys = data.keys();
                    int xIndex = 0;
                    while(keys.hasNext()) {
                        String key = (String) keys.next();
                        BarEntry entry = new BarEntry(Float.parseFloat(data.getString(key)),xIndex);
                        xIndex++;
                        yVals.add(entry);
                        xVals.add(key);
                    }
                    BarDataSet myStatsDataSet = new BarDataSet(yVals,"Date");
                    BarData myStatsData = new BarData(xVals,myStatsDataSet);
                    myChart.setData(myStatsData);
                    myChart.invalidate();
                } else {
                    // Error in login. Get the error message
                    String errorMsg = jObj.getString("error_msg");
                    Toast.makeText(getApplicationContext(),
                            errorMsg, Toast.LENGTH_LONG).show();
                }
            }  catch(JSONException je) {
                hideDialog();
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
    //Works with dd/MM/yyyy not time
    public Date stringToDate(String dateString, String format) throws DateFormatException{
        Date date = null;
        try {
            date = new Date();
            int day = format.indexOf("dd");
            date.setDate(Integer.parseInt(dateString.substring(day, 2)));
            int month = format.indexOf("MM");
            date.setMonth(Integer.parseInt(dateString.substring(month, 2)));
            int year = format.indexOf("yyyy");
            date.setYear(Integer.parseInt(dateString.substring(year, 4)));
            /*int hour = format.indexOf("HH");
            date.setHours(Integer.parseInt(dateString.substring(hour, 2)));
            int minutes = format.indexOf("mm");
            date.setMinutes(Integer.parseInt(dateString.substring(minutes,2)));
            int seconds = format.indexOf("ss");
            date.setSeconds(Integer.parseInt(dateString.substring(seconds,2)));*/
        } catch (Exception e) {
            throw new DateFormatException();
        }
        return date;
    }

}
