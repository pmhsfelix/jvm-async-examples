package pmhsfelix.async.examples;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.reactor.ExceptionEvent;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import rx.Observable;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class TheController {

    private static ScheduledExecutorService pool = new ScheduledThreadPoolExecutor(1);
    private static Logger logger = LogManager.getLogger(TheController.class);

    private final CloseableHttpAsyncClient closeableHttpAsyncClient;

    @Autowired
    public TheController(CloseableHttpAsyncClient closeableHttpAsyncClient){

        this.closeableHttpAsyncClient = closeableHttpAsyncClient;
    }

    @RequestMapping(path="fastlane", method = RequestMethod.GET)
    public String GetFastLane(){
        return "fast lane";
    }

    @RequestMapping(path="slowlane/sync", method = RequestMethod.GET)
    public String GetSlowLaneSync() throws InterruptedException {
        Thread.sleep(2*1000);
        return "slow lane sync";
    }

    @RequestMapping(path="slowlane/async", method = RequestMethod.GET)
    public CompletableFuture<String> GetSlowLaneAsync() throws InterruptedException {
        final CompletableFuture<String> cf = new CompletableFuture<>();
        pool.schedule(() -> {
            cf.complete("slow lane async");
        }, 2, TimeUnit.SECONDS);
        return cf;
    }

    private final AtomicInteger counter = new AtomicInteger();
    @RequestMapping(path="forward/async", method = RequestMethod.GET)
    public CompletionStage<String> GetForwardAsync() throws InterruptedException {
        logger.info("start");
        int c = counter.incrementAndGet();
        CompletableFuture<String> f1 = getFuture("http://localhost:8080/slowlane/async");
        CompletableFuture<String> f2 = f1.thenComposeAsync(s -> {
            logger.info("first continuation");
            return getFuture("http://localhost:8080/slowlane/async");
        });
        return f2.thenApplyAsync(s -> {
            logger.info("second continuation");
            counter.decrementAndGet();
            return s.toUpperCase();
        });
    }

    @RequestMapping(path="forward/sync", method = RequestMethod.GET)
    public String GetForwardSync() throws InterruptedException, ExecutionException {
        int c = counter.incrementAndGet();
        CompletableFuture<String> f1 = getFuture("http://localhost:8080/slowlane/async");
        String s = f1.get();
        CompletableFuture<String> f2 = getFuture("http://localhost:8080/slowlane/async");
        s = f2.get();
        return s;
    }

    private CompletableFuture<String> getFuture(String url) {
        CompletableFuture<String> f = new CompletableFuture<>();
        HttpGet get = new HttpGet(url);
        closeableHttpAsyncClient.execute(get, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse result) {
                int status = result.getStatusLine().getStatusCode();
                if (status == HttpStatus.SC_OK) {
                    try {
                        String body = IOUtils.toString(result.getEntity().getContent(), "UTF-8");
                        f.complete(body);
                    }catch(IOException ex){
                        f.completeExceptionally(ex);
                    }
                } else {
                    f.completeExceptionally(new Exception("Not OK"));
                }
            }

            @Override
            public void failed(Exception ex) {
                f.completeExceptionally(ex);
            }

            @Override
            public void cancelled() {
                f.completeExceptionally(new Exception("request cancelled"));
            }
        });
        return f;
    }

    private Observable<String> getRequestObservable() {
        return Observable.create(subscriber -> {
            HttpGet get = new HttpGet("http://localhost:8080/slowlane/async");
            closeableHttpAsyncClient.execute(get, new FutureCallback<HttpResponse>() {
                @Override
                public void completed(HttpResponse result) {
                    int status = result.getStatusLine().getStatusCode();

                    if (status == HttpStatus.SC_OK) {
                        try {
                            String response = IOUtils.toString(result.getEntity().getContent(), "UTF-8");
                            subscriber.onNext(response);
                            subscriber.onCompleted();
                        } catch (Exception e) {
                            subscriber.onError(new Exception(e));
                        }
                    } else {
                        subscriber.onError(new Exception("no response"));
                    }
                }

                @Override
                public void failed(Exception ex) {
                    List<ExceptionEvent> aud = TheApplication.ioReactor.getAuditLog();
                    if(aud.size() > 0){
                        //System.out.println(aud.get(0));
                        aud.get(0).getCause().printStackTrace();
                    }
                    subscriber.onError(new Exception("request failed"));
                }

                @Override
                public void cancelled() {
                    subscriber.onError(new Exception("request cancelled"));
                }
            });
        });
    }
}
