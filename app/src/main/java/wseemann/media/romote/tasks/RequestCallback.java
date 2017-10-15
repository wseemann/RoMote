package wseemann.media.romote.tasks;

import wseemann.media.romote.utils.RokuRequestTypes;

/**
 * Created by wseemann on 10/6/17.
 */

public abstract class RequestCallback {
    public abstract void requestResult(RokuRequestTypes rokuRequestType, RequestTask.Result result);
    public abstract void onErrorResponse(RequestTask.Result result);
}
