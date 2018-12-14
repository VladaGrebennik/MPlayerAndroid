package com.example.sample.mediaplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class InterceptCall extends BroadcastReceiver {

    @Override
    public  void  onReceive(Context context, Intent intent){
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if(state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING)){
            context.sendBroadcast(new Intent("INCOMMING_CALL"));
        } else  if(state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_IDLE)){
            context.sendBroadcast(new Intent("INCOMMING_CALL_DECLINE"));
            Toast.makeText(context, "Dec", Toast.LENGTH_SHORT).show();
        }
    }
}
