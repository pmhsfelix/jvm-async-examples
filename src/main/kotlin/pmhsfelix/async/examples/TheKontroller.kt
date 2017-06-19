package pmhsfelix.async.examples

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import org.apache.commons.io.IOUtils
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.concurrent.FutureCallback
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage


@RestController
class TheKontroller (val closeableHttpAsyncClient: CloseableHttpAsyncClient) {

    @RequestMapping(path = arrayOf("/kotlin/forward/async"), method = arrayOf(RequestMethod.GET))
    fun GetForwardAsync(): CompletionStage<String> {

        return future {
            val r1 = getFuture("http://localhost:8080/slowlane/async").await()
            System.out.println(r1)
            val r2= getFuture("http://localhost:8080/slowlane/async").await()
            System.out.println(r2)
            r2
        }

    }

    private fun getFuture(url: String): CompletableFuture<String> {
        val f = CompletableFuture<String>()
        val get = HttpGet(url)
        closeableHttpAsyncClient.execute(get, object : FutureCallback<HttpResponse> {
            override fun completed(result: HttpResponse) {
                val status = result.statusLine.statusCode
                if (status == HttpStatus.SC_OK) {
                    try {
                        val body = IOUtils.toString(result.entity.content, "UTF-8")
                        f.complete(body)
                    } catch (ex: IOException) {
                        f.completeExceptionally(ex)
                    }

                } else {
                    f.completeExceptionally(Exception("Not OK"))
                }
            }

            override fun failed(ex: Exception) {
                f.completeExceptionally(ex)
            }

            override fun cancelled() {
                f.completeExceptionally(Exception("request cancelled"))
            }
        })
        return f
    }


}



