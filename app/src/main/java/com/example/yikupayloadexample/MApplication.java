package com.example.yikupayloadexample;

import android.app.Application;
import android.content.Context;


public class MApplication extends Application {
    public static Context applicationContext;
    public static Application application;
    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = getApplicationContext();
        application = this;
    }



    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
    }



}