package com.checkmobi;

import android.content.Context;
import android.content.Intent;

public class CallReceiver extends PhoneCallReceiver
{
    public static final String MSG_CALL_START = "CallReceiver.MSG_CALL_START";
    public static final String MSG_CALL_END = "CallReceiver.MSG_CALL_END";

    @Override
    void OnCallStarted(Context ctx, String number, boolean incoming)
    {
        SendNotification(ctx, MSG_CALL_START, number, incoming);
    }

    @Override
    void OnCallEnded(Context ctx, String number, boolean incoming)
    {
        SendNotification(ctx, MSG_CALL_END, number, incoming);
    }

    private void SendNotification(Context ctx, String msg, String number, boolean incoming)
    {
        Intent intent = new Intent(msg);
        intent.putExtra("incoming", incoming);
        intent.putExtra("number", number);
        ctx.sendBroadcast(intent);
    }

}