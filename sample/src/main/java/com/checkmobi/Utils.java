package com.checkmobi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

class Utils
{
    public static final int LOLLIPOP = 21;

    public static void ShowPickerDialog(AlertDialog.Builder dialogBuilder, String title, String[] items, int selected, DialogInterface.OnClickListener listener)
    {
        dialogBuilder.setTitle(title);
        dialogBuilder.setSingleChoiceItems(items, selected, listener);
        AlertDialog alert = dialogBuilder.create();
        alert.show();
    }

    public static void ShowMessageBox(AlertDialog.Builder dialogBuilder, String title, String message)
    {
        Utils.ShowMessageBox(dialogBuilder, title, message, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });
    }

    private static void ShowMessageBox(AlertDialog.Builder dialogBuilder, String title, String message, DialogInterface.OnClickListener listener)
    {
        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Close", listener);

        if(!alertDialog.isShowing())
            alertDialog.show();
    }

    public static boolean IsNetworkConnected(Context ct)
    {
        ConnectivityManager cm = (ConnectivityManager) ct.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null;
    }

    public static void TrustInvalidSslCertificates()
    {
        try
        {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier()
            {
                public boolean verify(String hostname, SSLSession session)
                {
                    return true;
                }});

                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, new X509TrustManager[]{new X509TrustManager()
                {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                    public X509Certificate[] getAcceptedIssuers()
                    {
                        return new X509Certificate[0];
                    }}}, new SecureRandom());

                    HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static boolean isCompatible(int apiLevel)
    {
        return android.os.Build.VERSION.SDK_INT >= apiLevel;
    }

}
