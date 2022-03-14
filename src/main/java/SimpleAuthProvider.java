import java.net.URL;
import java.util.concurrent.CompletableFuture;

import com.microsoft.graph.authentication.IAuthenticationProvider;

public class SimpleAuthProvider implements IAuthenticationProvider {

    private String accessToken = null;

    public SimpleAuthProvider(String accessToken) {

        this.accessToken = accessToken;

    }

    @Override
    public CompletableFuture<String> getAuthorizationTokenAsync(URL requestUrl) {

        return CompletableFuture.completedFuture(accessToken);

    }

}