package com.example.digitalplatform;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;

public class ActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        //Toast.makeText(context,"recieved",Toast.LENGTH_SHORT).show();

        String action = intent.getStringExtra("action");
        if (action.equals("next")) {
            try {
                JSONObject message = new JSONObject();
                message.put("action","next");

            FullscreenActivity.WEPORT.postMessage(message);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (action.equals("back")) {
            try {
                JSONObject message = new JSONObject();
                message.put("action","back");
                FullscreenActivity.WEPORT.postMessage(message);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        //This is used to close the notification tray
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(it);
    }

}