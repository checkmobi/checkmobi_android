package com.checkmobi;

import android.app.Application;
import android.content.Context;

public class CkApplication extends Application
{
    private static Context context;

    public void onCreate()
    {
        super.onCreate();
        CkApplication.context = getApplicationContext();
    }

    public static Context getAppContext()
    {
        return CkApplication.context;
    }
}