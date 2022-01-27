package com.vasciie.gpstrackeronline.services;

import android.telephony.TelephonyManager;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This is a helper class, providing the needed methods for accessing and using the
 * Phone Tracker-Online API. This class cannot be extended or instantiated
 */
public final class APIConnector {
    private static final String primaryLink = "http://192.168.0.104";


    private APIConnector() {
    }

    private static final CookieManager cookieManager = new CookieManager();
    public static boolean CallerLogin(String username, String password) {
        cookieManager.getCookieStore().removeAll();
        try {
            URL url = new URL(primaryLink + "/api/caller/login");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            String jsonInput = String.format("[\"%s\", \"%s\"]", username, password);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                String responseStr = response.toString();
                System.out.println(responseStr);

                if(responseStr.equals("true")){
                    Map<String, List<String>> headerFields = connection.getHeaderFields();
                    List<String> cookiesHeader = headerFields.get("Set-Cookie");

                    if (cookiesHeader != null) {
                        for (String cookie : cookiesHeader) {
                            cookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
                        }
                    }
                }

                return Boolean.parseBoolean(responseStr);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean TargetLogin(int code) {
        try {
            URL url = new URL(primaryLink + "/api/target/login?targetCode=" + code);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                String responseStr = response.toString();
                System.out.println(responseStr);

                return Boolean.parseBoolean(responseStr);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static int[] GetTargetCodes(){
        try {
            URL url = new URL(primaryLink + "/api/caller/codes");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            if(cookieManager.getCookieStore().getCookies().size() > 0){
                connection.setRequestProperty("Cookie", TextUtils.join(";",  cookieManager.getCookieStore().getCookies()));
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                String responseStr = response.toString();
                System.out.println(responseStr);

                JSONArray jsonArray = new JSONArray(responseStr);
                int[] codes = new int[jsonArray.length()];
                for(int i = 0; i < jsonArray.length(); i++){
                    codes[i] = jsonArray.getInt(i);
                }

                return codes;
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String[] GetTargetNames(){
        try {
            URL url = new URL(primaryLink + "/api/caller/names");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            if(cookieManager.getCookieStore().getCookies().size() > 0){
                connection.setRequestProperty("Cookie", TextUtils.join(";",  cookieManager.getCookieStore().getCookies()));
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                String responseStr = response.toString();
                System.out.println(responseStr);

                JSONArray jsonArray = new JSONArray(responseStr);
                String[] names = new String[jsonArray.length()];
                for(int i = 0; i < jsonArray.length(); i++){
                    names[i] = jsonArray.getString(i);
                }

                return names;
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String[] GetPreviousLocations(int code){
        try {
            URL url = new URL(primaryLink + "/api/target/locslist?targetCode=" + code);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            if(cookieManager.getCookieStore().getCookies().size() > 0){
                connection.setRequestProperty("Cookie", TextUtils.join(";",  cookieManager.getCookieStore().getCookies()));
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                String responseStr = response.toString();
                System.out.println(responseStr);

                JSONArray jsonArray = new JSONArray(responseStr);
                String[] names = new String[jsonArray.length()];
                for(int i = 0; i < jsonArray.length(); i++){
                    names[i] = jsonArray.getString(i);
                }

                return names;
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return null;
    }
}
