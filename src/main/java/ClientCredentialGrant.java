// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.MessageCollectionPage;
import com.microsoft.graph.requests.MessageCollectionRequestBuilder;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

class ClientCredentialGrant {

    private static String authority;
    private static String clientId;
    private static String secret;
    private static String scope;
    private static ConfidentialClientApplication app;

    public static void main(String args[]) throws Exception {

        setUpSampleData();

        try {
            BuildConfidentialClientObject();
            // IAuthenticationResult result = getAccessTokenByClientCredentialGrant();
            // String usersListFromGraph = getUsersListFromGraph(result.accessToken());

            // System.out.println("Users in the Tenant = " + usersListFromGraph);
            getEmailListFromGraph3();
            System.out.println("DONE!!");
            System.out.println("Press any key to exit ...");
            System.in.read();

        } catch (Exception ex) {
            System.out.println("Oops! We have an exception of type - " + ex.getClass());
            System.out.println("Exception message - " + ex.getMessage());
            throw ex;
        }
    }

    private static void BuildConfidentialClientObject() throws Exception {

        // Load properties file and set properties used throughout the sample
        app = ConfidentialClientApplication.builder(
                clientId,
                ClientCredentialFactory.createFromSecret(secret))
                .authority(authority)
                .build();
    }

    private static IAuthenticationResult getAccessTokenByClientCredentialGrant() throws Exception {

        // With client credentials flows the scope is ALWAYS of the shape
        // "resource/.default", as the
        // application permissions need to be set statically (in the portal), and then
        // granted by a tenant administrator
        ClientCredentialParameters clientCredentialParam = ClientCredentialParameters.builder(
                Collections.singleton(scope))
                .build();

        CompletableFuture<IAuthenticationResult> future = app.acquireToken(clientCredentialParam);
        return future.get();
    }

    public static void getEmailListFromGraph3() throws Exception {

        IAuthenticationResult auth = getAccessTokenByClientCredentialGrant();

        String accessToken = auth.accessToken();

        SimpleAuthProvider simpleAuthProvider = new SimpleAuthProvider(accessToken);

        GraphServiceClient graphClient =

                GraphServiceClient

                        .builder()

                        .authenticationProvider(simpleAuthProvider)

                        .buildClient();

        MessageCollectionPage messagesPage = graphClient.users("addi@addidev.onmicrosoft.com").messages()

                .buildRequest()

                .filter("isRead eq true")

                .top(10)

                .orderBy("receivedDateTime desc")

                .select("Subject,Sender")

                .get(); // ERROR appears! (Caused by:
                        // javax.net.ssl.SSLHandshakeException:sun.security.validator.ValidatorException:
                        // PKIX path building failed:
                        // sun.security.provider.certpath.SunCertPathBuilderException: unable to find
                        // valid certification path to requested target)

        while (messagesPage != null) {

            final List<Message> messages = messagesPage.getCurrentPage();

            messages.forEach(i -> {

                System.out.println("subject=" + i.subject);

            });

            final MessageCollectionRequestBuilder nextPage = messagesPage.getNextPage();

            if (nextPage == null) {

                break;

            } else {

                messagesPage = nextPage.buildRequest(

                ).get();

            }

        }

    }

    private static String getUsersListFromGraph(String accessToken) throws IOException {
        URL url = new URL("https://graph.microsoft.com/v1.0/users");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Accept", "application/json");

        int httpResponseCode = conn.getResponseCode();
        if (httpResponseCode == HTTPResponse.SC_OK) {

            StringBuilder response;
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {

                String inputLine;
                response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }
            return response.toString();
        } else {
            return String.format("Connection returned HTTP code: %s with message: %s",
                    httpResponseCode, conn.getResponseMessage());
        }
    }

    /**
     * Helper function unique to this sample setting. In a real application these
     * wouldn't be so hardcoded, for example
     * different users may need different authority endpoints or scopes
     */
    private static void setUpSampleData() throws IOException {
        // Load properties file and set properties used throughout the sample
        Properties properties = new Properties();
        properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties"));
        authority = properties.getProperty("AUTHORITY");
        clientId = properties.getProperty("CLIENT_ID");
        secret = properties.getProperty("SECRET");
        scope = properties.getProperty("SCOPE");
    }
}