package com.checkmobi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public abstract class PhoneCallReceiver extends BroadcastReceiver
{
    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private static boolean isIncoming;
    private static String dialedNumber;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL"))
        {
            dialedNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");
        }
        else
        {
            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);

            int state = 0;

            if(stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE))
                state = TelephonyManager.CALL_STATE_IDLE;
            else if(stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK))
                state = TelephonyManager.CALL_STATE_OFFHOOK;
            else if(stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING))
                state = TelephonyManager.CALL_STATE_RINGING;

            onCallStateChanged(context, state, number);
        }
    }

    private void onCallStateChanged(Context context, int state, String number)
    {
        if(lastState == state)
            return;

        switch (state)
        {
            case TelephonyManager.CALL_STATE_RINGING:
                isIncoming = true;
                dialedNumber = number;
                OnCallStarted(context, dialedNumber, true);
                break;

            case TelephonyManager.CALL_STATE_OFFHOOK:

                if(lastState != TelephonyManager.CALL_STATE_RINGING)
                {
                    isIncoming = false;
                    OnCallStarted(context, dialedNumber, false);
                }
                break;

            case TelephonyManager.CALL_STATE_IDLE:

                OnCallEnded(context, dialedNumber, isIncoming);
                break;
        }

        lastState = state;
    }

    void OnCallStarted(Context ctx, String number, boolean incoming){}
    void OnCallEnded(Context ctx, String number, boolean incoming){}
}