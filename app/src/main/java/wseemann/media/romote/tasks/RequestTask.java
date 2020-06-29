package wseemann.media.romote.tasks;

import android.os.AsyncTask;

import com.jaku.core.JakuRequest;
import com.jaku.core.JakuResponse;
import com.jaku.model.Channel;

import java.io.ByteArrayOutputStream;
import java.util.List;

import wseemann.media.romote.model.Device;
import wseemann.media.romote.utils.RokuRequestTypes;

/**
 * Created by wseemann on 10/6/17.
 */
public class RequestTask extends AsyncTask<RokuRequestTypes, Void, RequestTask.Result> {

    private RequestCallback mCallback;

    private JakuRequest request;
    private RokuRequestTypes rokuRequestType;

    public RequestTask(JakuRequest request, RequestCallback callback) {
        this.request = request;
        setCallback(callback);
    }

    void setCallback(RequestCallback callback) {
        mCallback = callback;
    }

    /**
     * Wrapper class that serves as a union of a result value and an exception. When the download
     * task has completed, either the result value or exception can be a non-null value.
     * This allows you to pass exceptions to the UI thread that were thrown during doInBackground().
     */
    public static class Result {
        public Object mResultValue;
        public Exception mException;
        public Result(Object resultValue) {
            mResultValue = resultValue;
        }
        public Result(Exception exception) {
            mException = exception;
        }
    }

    /**
     * Cancel background network operation if we do not have network connectivity.
     */
    @Override
    protected void onPreExecute() {

    }

    @Override
    protected RequestTask.Result doInBackground(RokuRequestTypes... requestTypes) {
        Result result = null;
        if (!isCancelled() && requestTypes != null && requestTypes.length > 0) {
            RokuRequestTypes requestType = requestTypes[0];
            try {
                if (requestType.equals(RokuRequestTypes.query_active_app)) {
                    JakuResponse response = request.send();
                    List<Channel> channels = (List<Channel>) response.getResponseData();
                    result = new Result(channels);
                } else if (requestType.equals(RokuRequestTypes.query_device_info)) {
                    JakuResponse response = request.send();
                    Device device = (Device) response.getResponseData();
                    result = new Result(device);
                } else if (requestType.equals(RokuRequestTypes.query_icon)) {
                    JakuResponse response = request.send();
                    byte [] data = ((ByteArrayOutputStream) response.getResponseData()).toByteArray();
                    result = new Result(data);
                } else {
                    request.send();
                }
            } catch(Exception e) {
                e.printStackTrace();
                result = new Result(e);
            }
        }
        return result;
    }

    /**
     * Updates the DownloadCallback with the result.
     */
    @Override
    protected void onPostExecute(Result result) {
        if (result != null && mCallback != null) {
            if (result.mException != null) {
                mCallback.onErrorResponse(result);
            } else if (result.mResultValue != null) {
                mCallback.requestResult(rokuRequestType, result);
            }
        }
    }

    /**
     * Override to add special behavior for cancelled AsyncTask.
     */
    @Override
    protected void onCancelled(Result result) {
    }
}
