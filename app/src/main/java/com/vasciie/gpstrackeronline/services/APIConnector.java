package com.vasciie.gpstrackeronline.services;

import android.telephony.TelephonyManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

    public static boolean CallerLogin(String username, String password) {
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

                return Boolean.parseBoolean(responseStr);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean TargetLogin(int code, long imei) {
        try {
            URL url = new URL(primaryLink + "/api/caller/login?targetCode=" + code + "&IMEI=" + imei);
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
}
