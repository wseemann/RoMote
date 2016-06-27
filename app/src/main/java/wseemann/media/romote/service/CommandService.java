package wseemann.media.romote.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import wseemann.media.romote.utils.CommandHelper;

/**
 * Created by wseemann on 6/19/16.
 */
public class CommandService extends IntentService {

    private static final String TAG = CommandService.class.getName();

    public static final String PLAY_PAUSE_ACTION = "wseemann.media.romote.service.PLAY_PAUSE";

    public CommandService() {
        super(CommandService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent called");

        if (intent != null) {
            if (intent.getAction() != null) {
                Log.d(TAG, "onHandleIntent: " + intent.getAction());
                sendCommand(intent.getAction());
            }
        }
    }

    private void sendCommand(String command) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        String url = CommandHelper.getKeypressURL(this, command);;

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "That didn't work!");
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
}
