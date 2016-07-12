package wseemann.media.romote.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wseemann on 6/19/16.
 */
public class CommandService extends IntentService {

    private static final String TAG = CommandService.class.getName();

    public static final String PLAY_PAUSE_ACTION = "wseemann.media.romote.service.PLAY_PAUSE";

    private RequestQueue mRequestQueue;
    private List<String> mCommanQueue;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            processNextCommand();
        }
    };

    public CommandService() {
        super(CommandService.class.getName());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Instantiate the RequestQueue.
        mRequestQueue = Volley.newRequestQueue(this);
        mCommanQueue = new ArrayList<String>();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent called");

        if (intent != null) {
            if (intent.getAction() != null) {
                Log.d(TAG, "onHandleIntent: " + intent.getAction());

                if (intent.getAction().equals("command")) {
                    sendCommand(intent.getAction());
                } else if (intent.getAction().equals("commands")) {
                    String [] commands = intent.getStringArrayExtra("commands");

                    for (int i = 0; i < commands.length; i++) {
                        mCommanQueue.add(commands[i]);
                    }

                    processNextCommand();
                } else {
                    sendCommand(intent.getAction());
                }
            }
        }
    }

    private void sendCommand(String command) {
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, command,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        mHandler.sendEmptyMessage(0);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "That didn't work!");
                mHandler.sendEmptyMessage(0);
            }
        });
        // Add the request to the RequestQueue.
        mRequestQueue.add(stringRequest);
    }

    private synchronized void processNextCommand() {
        if (mCommanQueue.size() == 0) {
            return;
        }

        String command = mCommanQueue.get(0);

        mCommanQueue.remove(0);

        sendCommand(command);
    }
}
