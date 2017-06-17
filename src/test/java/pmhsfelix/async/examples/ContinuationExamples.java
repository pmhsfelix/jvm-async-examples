package pmhsfelix.async.examples;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.concurrent.*;

/**
 * Created by PFE06 on 17/06/2017.
 */
public class ContinuationExamples {

    private static Logger logger = LogManager.getLogger(ContinuationExamples.class);
    private static ScheduledExecutorService pool = new ScheduledThreadPoolExecutor(1);

    @Test
    public void synchronousContinuationTest() throws ExecutionException, InterruptedException {
        final CompletableFuture<String> future = new CompletableFuture<String>();
        final CompletionStage<String> state = future
                .thenApply(s -> {
                    logger.info("running first continuation");
                    return s.toUpperCase();
                })
                .thenApply(s -> {
                    logger.info("running second continuation");
                    return s.toUpperCase();
                });
        pool.schedule(() -> {
            logger.info("before completing...");
            future.complete("done");
            logger.info("after completing...");
        }, 2, TimeUnit.SECONDS);
        logger.info("Done: "+future.get());
    }

    @Test
    public void asynchronousContinuationTest() throws ExecutionException, InterruptedException {
        final CompletableFuture<String> future = new CompletableFuture<String>();
        final CompletionStage<String> state = future
                .thenApplyAsync(s -> {
                    logger.info("running first continuation");
                    return s.toUpperCase();
                })
                .thenApplyAsync(s -> {
                    logger.info("running second continuation");
                    return s.toUpperCase();
                });
        pool.schedule(() -> {
            logger.info("before completing...");
            future.complete("done");
            logger.info("after completing...");
        }, 2, TimeUnit.SECONDS);
        logger.info("Done: "+future.get());
    }
}
