package wseemann.media.romote.tasks

import com.wseemann.ecp.api.ResponseCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class ResponseCallbackWrapper<T>(private val callback: ResponseCallback<T>) : ResponseCallback<T> {
    override fun onSuccess(data: T?) {
        runBlocking {
            withContext(Dispatchers.Main) {
                callback.onSuccess(data)
            }
        }
    }

    override fun onError(ex: Exception) {
        runBlocking {
            withContext(Dispatchers.Main) {
                callback.onError(ex)
            }
        }
    }
}