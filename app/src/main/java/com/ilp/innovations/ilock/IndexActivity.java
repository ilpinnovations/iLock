package com.ilp.innovations.ilock;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


public class IndexActivity extends Activity {

    private static int SPLASH_TIME_OUT = 0; //in ms
    private String serverAddr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PackageManager m = getPackageManager();
        String s = getPackageName();
        PackageInfo p = null;
        try {
            p = m.getPackageInfo(s, 0);
            s = p.applicationInfo.dataDir;
            File file = new File(s + "/serverAddr");
            if (file.exists()) {
                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);
                serverAddr = br.readLine();
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Handler().postDelayed(new Runnable() {

            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */

            @Override
            public void run() {
                // This method will be executed once the timer is over
                // Start your app main activity
                Intent intent = new Intent(IndexActivity.this, LoginActivity.class);
                intent.putExtra("addr", serverAddr);
                startActivity(intent);
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}
