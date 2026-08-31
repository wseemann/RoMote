package wseemann.media.romote.tasks

import com.wseemann.ecp.api.ResponseCallback
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class ResponseCallbackWrapper<T>(
    private val dispatcher: CoroutineDispatcher,
    private val callback: ResponseCallback<T>,
) : ResponseCallback<T> {
    override fun onSuccess(data: T?) {
        runBlocking {
            withContext(dispatcher) {
                callback.onSuccess(data)
            }
        }
    }

    override fun onError(ex: Exception) {
        runBlocking {
            withContext(dispatcher) {
                callback.onError(ex)
            }
        }
    }
}
