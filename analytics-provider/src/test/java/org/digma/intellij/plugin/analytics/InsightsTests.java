package org.digma.intellij.plugin.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import okhttp3.mockwebserver.MockResponse;
import org.digma.intellij.plugin.model.rest.insights.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InsightsTests extends AbstractAnalyticsProviderTest {


    //run against running env just for local test
//    @Test
    public void getInsightsTemp() {
        {
            List<String> ids = new ArrayList<>();
            ids.add("method:Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService$_$TransferFunds");
            ids.add("span:MoneyTransferDomainService$_$Peristing balance transfer");
            ids.add("span:MoneyTransferDomainService$_$Creating record of transaction");
            InsightsRequest insightsRequest = new InsightsRequest("UNSET_ENV", ids);
            AnalyticsProvider analyticsProvider = new RestAnalyticsProvider("http://localhost:5051");
            List<CodeObjectInsight> codeObjectInsights = analyticsProvider.getInsights(insightsRequest);

            System.out.println(codeObjectInsights);
        }

        {
            List<String> ids = new ArrayList<>();
            ids.add("method:Sample.MoneyTransfer.API.Controllers.TransferController$_$TransferFunds");
            ids.add("span:TransferController$_$Process transfer");
            InsightsRequest insightsRequest = new InsightsRequest("UNSET_ENV", ids);
            AnalyticsProvider analyticsProvider = new RestAnalyticsProvider("http://localhost:5051");
            List<CodeObjectInsight> codeObjectInsights = analyticsProvider.getInsights(insightsRequest);

            System.out.println(codeObjectInsights);
        }
    }


    @Test
    public void getInsights() throws JsonProcessingException {

        String codeObjectId = "Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService$_$TransferFunds";
        List<CodeObjectInsight> expectedCodeObjectInsights = new ArrayList<>();

        HotspotInsight expectedHotspotInsight = new HotspotInsight(codeObjectId, 75);

        ErrorInsightNamedError namedError1 = new ErrorInsightNamedError("e0a4d03c-c609-11ec-a9d6-0242ac130006", "System.NullReferenceException", codeObjectId, "Sample.MoneyTransfer.API.Controllers.TransferController$_$TransferFunds");
        ErrorInsightNamedError namedError2 = new ErrorInsightNamedError("de63a938-c609-11ec-b388-0242ac130006", "System.Exception", codeObjectId, "Sample.MoneyTransfer.API.Controllers.TransferController$_$TransferFunds");
        List<ErrorInsightNamedError> namedErrors = new ArrayList<>();
        namedErrors.add(namedError1);
        namedErrors.add(namedError2);
        ErrorInsight expectedErrorInsight = new ErrorInsight(codeObjectId, 1, 0, 0, namedErrors);

        expectedCodeObjectInsights.add(expectedHotspotInsight);
        expectedCodeObjectInsights.add(expectedErrorInsight);


        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(expectedCodeObjectInsights))
                .addHeader("Content-Type", "application/json"));

        AnalyticsProvider analyticsProvider = new RestAnalyticsProvider(baseUrl);
        List<CodeObjectInsight> codeObjectInsights = analyticsProvider.getInsights(new InsightsRequest("myenv", Collections.singletonList("method:" + codeObjectId)));

        assertEquals(2, codeObjectInsights.size());
        assertEquals(HotspotInsight.class, codeObjectInsights.get(0).getClass());
        HotspotInsight hotspotInsight = (HotspotInsight) codeObjectInsights.get(0);
        assertEquals(expectedHotspotInsight, hotspotInsight);

        assertEquals(ErrorInsight.class, codeObjectInsights.get(1).getClass());
        ErrorInsight errorInsight = (ErrorInsight) codeObjectInsights.get(1);
        assertEquals(expectedErrorInsight, errorInsight);
    }


    @Test
    public void getInsightsEmptyResultTest() throws JsonProcessingException {

        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(Collections.emptyList()))
                .addHeader("Content-Type", "application/json"));

        AnalyticsProvider analyticsProvider = new RestAnalyticsProvider(baseUrl);
        List<CodeObjectInsight> codeObjectInsights = analyticsProvider.getInsights(new InsightsRequest("myenv", Collections.singletonList("method:aaaa")));

        assertEquals(0, codeObjectInsights.size(), "unexpected summaries size");
    }

    @Test
    public void getSummariesNullResultTest() {

        mockBackEnd.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json"));

        AnalyticsProviderException exception = assertThrows(AnalyticsProviderException.class, () -> {
            AnalyticsProvider analyticsProvider = new RestAnalyticsProvider(baseUrl);
            List<CodeObjectInsight> codeObjectInsights = analyticsProvider.getInsights(new InsightsRequest("myenv", Collections.singletonList("method:aaaa")));
        });

        assertEquals(MismatchedInputException.class, exception.getCause().getClass());
    }

    @Test
    public void getSummariesErrorResultTest() {

        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(500)
                .addHeader("Content-Type", "application/json"));

        AnalyticsProviderException exception = assertThrows(AnalyticsProviderException.class, () -> {
            AnalyticsProvider analyticsProvider = new RestAnalyticsProvider(baseUrl);
            List<CodeObjectInsight> codeObjectInsights = analyticsProvider.getInsights(new InsightsRequest("myenv", Collections.singletonList("method:aaaa")));
        });

        assertEquals(500, exception.getResponseCode());
    }


    @Test
    public void getSummariesWrongResultTest() throws JsonProcessingException {

        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString("mystring"))
                .addHeader("Content-Type", "application/json"));

        AnalyticsProviderException exception = assertThrows(AnalyticsProviderException.class, () -> {
            AnalyticsProvider analyticsProvider = new RestAnalyticsProvider(baseUrl);
            List<CodeObjectInsight> codeObjectInsights = analyticsProvider.getInsights(new InsightsRequest("myenv", Collections.singletonList("method:aaaa")));
        });

        assertEquals(MismatchedInputException.class, exception.getCause().getClass());
    }


}
