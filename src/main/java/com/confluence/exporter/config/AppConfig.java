package com.confluence.exporter.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Base64;
import java.util.concurrent.Executor;


/**
 * This is the configuration class for the current application
 * few beans have been defined here, which will be managed by spring
 * and we can inject those in when need
 *
 * @author Nandakumar12
 */
@Configuration
public class AppConfig {

    @Value("${confluence.token}")
    String confluenceToken;

    @Value("${confluence.email}")
    String confluenceEmail;


    Base64.Encoder enc = Base64.getEncoder();

    private static final String AUTHORIZATION_HEADER_KEY = "Authorization";

    private static final String AUTHORIZATION_BASIC_HEADER_KEY = "Basic ";


    @Bean
    public ByteArrayHttpMessageConverter byteArrayHttpMessageConverter() {
        return new ByteArrayHttpMessageConverter();
    }


    /**
     * This method will return a custom restTemplate bean with headers
     * and token necessary for communicating with Confluence Rest API.
     *
     * @param restTemplateBuilder This bean will be injected by spring and will be further
     *                            used to add some custom headers
     * @return RestTemplate This returns a restTemplate which then can be used directly to
     * communicate with the Confluence API
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        String accessToken = AUTHORIZATION_BASIC_HEADER_KEY + enc.encodeToString((confluenceEmail + ":" + confluenceToken).getBytes());
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection,
                                             String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(true);
            }
        };

        requestFactory.setConnectTimeout(250000);
        requestFactory.setReadTimeout(250000);
        RestTemplate restTemplate = new RestTemplate(requestFactory);

        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add(AUTHORIZATION_HEADER_KEY, accessToken);

            return execution.execute(request, body);
        });

        return restTemplate;

    }

    /**
     * This is an method for multi-thread configuration
     *
     * @return Executor This method returns a executor which then can be used along
     * with @Async to provide the thread configuration for the
     * annotated method
     */
    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(25);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("custom-thread-");
        executor.initialize();
        return executor;
    }



}
