import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class MicrosoftAuthentication {
    private String client_id;
    private String client_secrtet;

    private String username;
    private String uuid;
    private String access_token;

    private HttpServer server;
    private Logger logger = LogManager.getLogger();

    private final String redirect_uri = "http://localhost/api";
    private final String login_uri;

    private String auth_code;
    private String xbox_access_token;
    private String xbl_token;
    private String xbox_userhash;
    private String xsts_token;

    private String refreshToken;
    private boolean shouldRefreshLogin;

    public MicrosoftAuthentication(String client_id, String client_secrtet) {
        this.client_id = client_id;
        this.client_secrtet = client_secrtet;

        login_uri = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?client_id=" + client_id + "&response_type=code&redirect_uri=" + redirect_uri + "&scope=XboxLive.signin%20offline_access";
        loadRefreshToken();
    }

    public void login() {
        if (shouldRefreshLogin) {
            try {
                server = HttpServer.create(new InetSocketAddress(80), 0);
                server.createContext("/api", new MyHandler(server, this));
                server.setExecutor(null);
                server.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Desktop.getDesktop().browse(new URI(login_uri));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            getToken(false);
        }
    }

    public void getToken(boolean freshLogin) {
        if (freshLogin) {
            HttpPost post = new HttpPost("https://login.microsoftonline.com/consumers/oauth2/v2.0/token");
            ArrayList<NameValuePair> urlParameters = new ArrayList<>();
            urlParameters.add(new BasicNameValuePair("grant_type", "authorization_code"));
            urlParameters.add(new BasicNameValuePair("code", auth_code));
            urlParameters.add(new BasicNameValuePair("client_id", client_id));
            urlParameters.add(new BasicNameValuePair("redirect_uri", redirect_uri));
            urlParameters.add(new BasicNameValuePair("client_secret", client_secrtet));

            try {
                post.setEntity(new UrlEncodedFormEntity(urlParameters));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            post.addHeader("Content-type", "application/x-www-form-urlencoded");

            try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(post)) {
                String json = EntityUtils.toString(response.getEntity());
                JsonObject jobj = new Gson().fromJson(json, JsonObject.class);
                xbox_access_token = jobj.get("access_token").getAsString();

                refreshToken = jobj.get("refresh_token").getAsString();
                saveRefreshToken();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            HttpPost post = new HttpPost("https://login.microsoftonline.com/consumers/oauth2/v2.0/token");

            ArrayList<NameValuePair> urlParameters = new ArrayList<>();
            urlParameters.add(new BasicNameValuePair("grant_type", "refresh_token"));
            urlParameters.add(new BasicNameValuePair("refresh_token", refreshToken));
            urlParameters.add(new BasicNameValuePair("client_id", client_id));
            urlParameters.add(new BasicNameValuePair("client_secret", client_secrtet));
            urlParameters.add(new BasicNameValuePair("scope", "XboxLive.signin"));

            try {
                post.setEntity(new UrlEncodedFormEntity(urlParameters));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            post.addHeader("Content-type", "application/x-www-form-urlencoded");

            try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(post)) {
                String json = EntityUtils.toString(response.getEntity());
                JsonObject jobj = new Gson().fromJson(json, JsonObject.class);
                xbox_access_token = jobj.get("access_token").getAsString();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        authXBL();
    }

    public void authXBL() {
        HttpPost post = new HttpPost("https://user.auth.xboxlive.com/user/authenticate");

        post.setHeader("Content-type", "application/json");
        post.setHeader("Accept", "application/json");

        String payload = "{\"Properties\": {\"AuthMethod\": \"RPS\", \"SiteName\": \"user.auth.xboxlive.com\", \"RpsTicket\": \"d=" + xbox_access_token + "\"},\"RelyingParty\": \"http://auth.xboxlive.com\", \"TokenType\": \"JWT\"}";
        StringEntity requestEntity = new StringEntity(payload, ContentType.APPLICATION_JSON);
        post.setEntity(requestEntity);

        try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(post)) {
            String json = EntityUtils.toString(response.getEntity());
            JsonObject jobj = new Gson().fromJson(json, JsonObject.class);
            xbl_token = jobj.get("Token").getAsString();
            xbox_userhash = jobj.get("DisplayClaims").getAsJsonObject().get("xui").getAsJsonArray().get(0).getAsJsonObject().get("uhs").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        authXSTS();
    }

    private void authXSTS() {
        HttpPost post = new HttpPost("https://xsts.auth.xboxlive.com/xsts/authorize");

        post.setHeader("Content-type", "application/json");
        post.setHeader("Accept", "application/json");

        String payload = "{\"Properties\": {\"SandboxId\": \"RETAIL\", \"UserTokens\": [\"" + xbl_token + "\"]}, \"RelyingParty\": \"rp://api.minecraftservices.com/\", \"TokenType\": \"JWT\"}";
        StringEntity requestEntity = new StringEntity(payload, ContentType.APPLICATION_JSON);
        post.setEntity(requestEntity);

        try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(post)) {
            String json = EntityUtils.toString(response.getEntity());
            JsonObject jobj = new Gson().fromJson(json, JsonObject.class);
            xsts_token = jobj.get("Token").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        authMinecraft();
    }

    private void authMinecraft() {
        HttpPost post = new HttpPost("https://api.minecraftservices.com/authentication/login_with_xbox");

        String payload = "{\"identityToken\": \"XBL3.0 x=" + xbox_userhash + ";" + xsts_token + "\"}";
        StringEntity requestEntity = new StringEntity(payload, ContentType.APPLICATION_JSON);
        post.setEntity(requestEntity);

        try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(post)) {
            String json = EntityUtils.toString(response.getEntity());
            JsonObject jobj = new Gson().fromJson(json, JsonObject.class);
            access_token = jobj.get("access_token").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        checkOwnership();
    }

    private void checkOwnership() {
        getProfile();
    }

    private void getProfile() {
        HttpGet get = new HttpGet("https://api.minecraftservices.com/minecraft/profile");

        get.setHeader("Authorization", "Bearer " + access_token);

        try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(get)) {
            String json = EntityUtils.toString(response.getEntity());
            JsonObject jobj = new Gson().fromJson(json, JsonObject.class);
            uuid = jobj.get("id").getAsString();
            username = jobj.get("name").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setSession();
    }

    private void setSession(){
        Minecraft.getMinecraft().session = new Session(username, uuid, access_token, "mojang");
        logger.info("Setting user: " + username);
    }

    static class MyHandler implements HttpHandler {
        private HttpServer server;
        private MicrosoftAuthentication changer;

        public MyHandler(HttpServer server, MicrosoftAuthentication changer) {
            this.server = server;
            this.changer = changer;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            String responseURI = t.getRequestURI().toString();
            String code = responseURI.split("=")[1];

            String response = "Logged in! You can now close this window!";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            server.stop(1);

            changer.auth_code = code;
            changer.getToken(true);
        }

    }

    private void saveRefreshToken(){
        FileManager.writeJsonToFile(new File(FileManager.getRootDirectory(), "token.json"), refreshToken);
    }

    private void loadRefreshToken(){
        String token = FileManager.readFromJson(new File(FileManager.getRootDirectory(), "token.json"), String.class);

        if (token == null){
            shouldRefreshLogin = true;
        }
        else {
            shouldRefreshLogin = false;
            refreshToken = token;
        }
    }

}
