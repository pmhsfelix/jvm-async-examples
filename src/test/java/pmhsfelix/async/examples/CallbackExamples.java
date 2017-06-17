package pmhsfelix.async.examples;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

public class CallbackExamples {

    @Test
    public void testSimpleGet() throws IOException, InterruptedException {
        final CountDownLatch c = new CountDownLatch(1);
        AsynchronousSocketChannel ch = AsynchronousSocketChannel.open();
        ch.connect(new InetSocketAddress("httpbin.org", 80), ch,
                new CompletionHandler<Void, Object>() {
                    @Override
                    public void completed(Void result, Object attachment) {
                        ByteBuffer src = ByteBuffer.wrap("GET /get HTTP/1.1\r\nHost: httpbin.org\r\n\r\n".getBytes());
                        ch.write(src, ch, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
                            @Override
                            public void completed(Integer result, AsynchronousSocketChannel attachment) {
                                ByteBuffer dst = ByteBuffer.allocate(2048);
                                ch.read(dst, ch, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
                                    @Override
                                    public void completed(Integer result, AsynchronousSocketChannel attachment) {
                                        String txt = new String(dst.array(), 0, result, Charset.forName("UTF-8"));
                                        System.out.println(txt);
                                        c.countDown();
                                    }

                                    @Override
                                    public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
                                        // everything is going to be fine, trust me
                                    }
                                });
                            }
                            @Override
                            public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
                                // everything is going to be fine, trust me
                            }
                        });
                    }

                    @Override
                    public void failed(Throwable exc, Object attachment) {
                        // everything is going to be fine, trust me
                    }
                });
        c.await();
    }
}
