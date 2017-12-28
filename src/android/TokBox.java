package com.fitbase.TokBox;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONException;
import org.json.JSONArray;


import android.content.Context;
import android.content.Intent;
import android.widget.Toast;


/**
 * @Author Karunakar , Anshul.
 */
public class TokBox extends CordovaPlugin {
	public void StartStream(final String params) {
		cordova.getActivity().runOnUiThread(new Runnable() {			
			@Override
			public void run() {				
				Toast myMessage = Toast.makeText(cordova.getActivity().getWindow().getContext(), "Please wait..", Toast.LENGTH_SHORT);
				myMessage.show();
				Intent in = new Intent(cordova.getActivity().getWindow().getContext(), OpenTokActivity.class);
				in.putExtra("tokbox_obj",params);
				cordova.getActivity().getWindow().getContext().startActivity(in);
			}
		});		
	}
	
    @Override
    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext)throws JSONException {
		final String params = "null".equals(args.getString(0)) ? null : args.getString(0);
        if (action.equals("startStream")){
        	StartStream(params);
            callbackContext.success("okay");
            return true;
        }         
        return false;
    }
}
