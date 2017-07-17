package com.checkmobi;

import android.content.SharedPreferences;

public class Settings
{
    private static final String SETTINGS_PREF_NAME = "CheckmobiSettings";

    private static final Settings ourInstance = new Settings();
    private boolean useClientHangup;
    private boolean closeCallForFailedAni;
    private String secretKey;
    private String baseUrl;

    public static Settings getInstance()
    {
        return ourInstance;
    }

    private Settings()
    {
        SharedPreferences settings = CkApplication.getAppContext().getSharedPreferences(SETTINGS_PREF_NAME, 0);
        this.secretKey = settings.getString("secretKey", "");
        this.baseUrl = settings.getString("baseUrl", "https://api.checkmobi.com");
        this.closeCallForFailedAni = settings.getBoolean("closeCallForFailedAni", true);
        this.useClientHangup = settings.getBoolean("useClientHangup", false);
    }

    public void commit()
    {
        SharedPreferences settings = CkApplication.getAppContext().getSharedPreferences(SETTINGS_PREF_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("secretKey", this.secretKey);
        editor.putString("baseUrl", this.baseUrl);
        editor.putBoolean("closeCallForFailedAni", this.closeCallForFailedAni);
        editor.putBoolean("useClientHangup", this.useClientHangup);
        editor.commit();
    }

    public boolean isUseClientHangup()
    {
        return useClientHangup;
    }

    public void setUseClientHangup(boolean useClientHangup)
    {
        this.useClientHangup = useClientHangup;
    }

    public boolean isCloseCallForFailedAni()
    {
        return closeCallForFailedAni;
    }

    public void setCloseCallForFailedAni(boolean closeCallForFailedAni)
    {
        this.closeCallForFailedAni = closeCallForFailedAni;
    }

    public String getSecretKey()
    {
        return secretKey;
    }

    public void setSecretKey(String secretKey)
    {
        this.secretKey = secretKey;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }
}
