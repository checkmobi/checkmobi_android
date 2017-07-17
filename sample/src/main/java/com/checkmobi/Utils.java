package com.checkmobi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

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

    public static boolean isCompatible(int apiLevel)
    {
        return android.os.Build.VERSION.SDK_INT >= apiLevel;
    }

}
