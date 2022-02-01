package com.vasciie.gpstrackeronline.services;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This is a helper class, providing the needed methods for accessing and using the
 * Phone Tracker-Online API. This class cannot be extended or instantiated
 */
public final class APIConnector {
    public static final String primaryLink = "http://192.168.0.107";


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
        } catch (Exception e) {
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
        } catch (Exception e) {
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
        } catch (Exception e) {
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
        } catch (Exception e) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void SendContacts(JSONObject contacts){
        try {
            URL url = new URL(primaryLink + "/api/caller/contacts");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setRequestMethod("POST");
            if(cookieManager.getCookieStore().getCookies().size() > 0){
                connection.setRequestProperty("Cookie", TextUtils.join(";",  cookieManager.getCookieStore().getCookies()));
            }
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");


            String jsonInput = contacts.toString();
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Without asking for a response from the API method the method just doesn't work...
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                String responseStr = response.toString();
                System.out.println(responseStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void SendLocationsList(int targetCode, LinkedList<Double> lats, LinkedList<Double> longs, LinkedList<Integer> images, LinkedList<String> capTimes){
        JSONArray locations = new JSONArray();
        Iterator<Double> latIter = lats.listIterator();
        Iterator<Double> longIter = longs.listIterator();
        Iterator<Integer> imageIter = images.listIterator();
        Iterator<String> capIter = capTimes.listIterator();
        while(latIter.hasNext()){
            double lat = latIter.next();
            double lng = longIter.next();
            int image = imageIter.next();
            String capTime = capIter.next();

            String data = lat + ";" + lng + ";" + image + ";" + capTime;
            locations.put(data.replace(".", ","));
        }

        try {
            URL url = new URL(primaryLink + "/api/target/locslist?targetCode=" + targetCode);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setRequestMethod("POST");
            if(cookieManager.getCookieStore().getCookies().size() > 0){
                connection.setRequestProperty("Cookie", TextUtils.join(";",  cookieManager.getCookieStore().getCookies()));
            }
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");


            String jsonInput = locations.toString();
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Without asking for a response from the API method the method just doesn't work...
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                String responseStr = response.toString();
                System.out.println(responseStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String GetTargetPhoneNumber(int targetCode){
        try {
            URL url = new URL(primaryLink + "/api/target/contact?targetCode=" + targetCode);
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
                String responseStr = response.toString().replace("\"", "");
                System.out.println(responseStr);

                return responseStr;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static int GetTargetOldCode(int targetCode){
        try {
            URL url = new URL(primaryLink + "/api/target/oldcode?targetCode=" + targetCode);
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

                return Integer.parseInt(responseStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    public static int ChangeCodeRequest(int targetCode){
        try {
            URL url = new URL(primaryLink + "/api/target/changecodereq?targetCode=" + targetCode);
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

                return Integer.parseInt(responseStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    public static int GetTargetNewCode(int targetCode){
        try {
            URL url = new URL(primaryLink + "/api/target/code?oldCode=" + targetCode);
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

                return Integer.parseInt(responseStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }
}
