package com.example.splashmaker;

import android.util.Log;
import android.widget.ImageView;

public class ActivityThread extends Thread {
    private MainActivity main;
    private boolean isRunning;

    public ActivityThread(MainActivity main){
        this.main = main;
    }

    @Override
    public void run(){
        Log.d("RUN: ", "YES");
        while(isRunning){
            //Log.d("RUNNING", ""+ this.getState());
            main.updatePhotons();
            main.updatePixelMap();
            //main.runOnUiThread(main.drawBitmap);
        }


    }



    public void runState(boolean run){
        this.isRunning = run;
    }
}
