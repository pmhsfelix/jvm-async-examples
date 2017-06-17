package pmhsfelix.async.examples;


import io.undertow.Undertow;
import io.undertow.servlet.api.DeploymentInfo;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.undertow.UndertowBuilderCustomizer;
import org.springframework.boot.context.embedded.undertow.UndertowDeploymentInfoCustomizer;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TheApplication {

    public static void main(String[] args){
        SpringApplication.run(TheApplication.class, args);
    }

    // HACK
    public static DefaultConnectingIOReactor ioReactor;

    @Bean(name = "httpAsyncClient")
    public CloseableHttpAsyncClient closeableAsyncHttpClient() throws IOReactorException {
        final RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(1000)
                .setConnectionRequestTimeout(30000)
                .setSocketTimeout(30000)
                .build();

        final CloseableHttpAsyncClient client = HttpAsyncClientBuilder.create()
                .setConnectionManager(poolingNHttpClientConnectionManager(1000, 1000))
                .setDefaultRequestConfig(config)
                .build();
        client.start();

        return client;
    }

    private PoolingNHttpClientConnectionManager poolingNHttpClientConnectionManager(int maxConnectionsPerHost, int totalMaxConnections) throws IOReactorException {

        ioReactor = new DefaultConnectingIOReactor();
        final PoolingNHttpClientConnectionManager connectionManager = new PoolingNHttpClientConnectionManager(ioReactor);
        final ConnectionConfig connectionConfig = ConnectionConfig.copy(ConnectionConfig.DEFAULT).build();
        connectionManager.setDefaultConnectionConfig(connectionConfig);
        connectionManager.setDefaultMaxPerRoute(maxConnectionsPerHost);
        connectionManager.setMaxTotal(totalMaxConnections);
        return connectionManager;
    }

    @Bean
    public UndertowEmbeddedServletContainerFactory embeddedServletContainerFactory() {

        UndertowEmbeddedServletContainerFactory factory = new UndertowEmbeddedServletContainerFactory();
        factory.setIoThreads(8);
        factory.setWorkerThreads(8);

        return factory;
    }

    @Bean
    public FilterRegistrationBean someFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new TheFilter());
        registration.addUrlPatterns("/forward/async");
        registration.setName("someFilter");
        registration.setOrder(1);
        return registration;
    }

}

