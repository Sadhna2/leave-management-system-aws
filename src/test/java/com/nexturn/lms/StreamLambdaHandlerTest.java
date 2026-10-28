package com.nexturn.lms;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
/** Verifies the actual API Gateway stream adapter initializes and returns JSON. */
class StreamLambdaHandlerTest {
    @Test void lambdaAdapterHandlesHealthAndProtectedApi() throws Exception {
        System.setProperty("spring.profiles.active","test");
        StreamLambdaHandler handler=new StreamLambdaHandler();
        System.clearProperty("spring.profiles.active");
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        handler.handleRequest(new AwsProxyRequestBuilder("/ping","GET").buildStream(),output,new MockLambdaContext());
        AwsProxyResponse response=new ObjectMapper().readValue(output.toByteArray(),AwsProxyResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).contains("UP");
        output.reset();
        handler.handleRequest(new AwsProxyRequestBuilder("/api/manager/leaves/pending","GET").buildStream(),output,new MockLambdaContext());
        response=new ObjectMapper().readValue(output.toByteArray(),AwsProxyResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(401);
    }
}
