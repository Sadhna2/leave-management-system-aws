package com.nexturn.lms;
import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.*;
/** Adapts API Gateway REST events; initialization failures propagate to the Lambda runtime. */
public class StreamLambdaHandler implements RequestStreamHandler {
    private final SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
    /** Creates one Spring context for reuse during this Lambda execution environment. */
    public StreamLambdaHandler() throws ContainerInitializationException {
        handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(Application.class);
    }
    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        handler.proxyStream(input, output, context);
    }
}
