package com.eternitywall.opentimestamps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public final class GoogleUrlShortener {

    public static String shorten(String longUrl) {
        if (longUrl == null) {
            return longUrl;
        }

        StringBuilder sb = null;
        String line = null;
        String urlStr = longUrl;

        try {
            URL url = new URL("http://goo.gl/api/url");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", "toolbar");

            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write("url=" + URLEncoder.encode(urlStr, "UTF-8"));
            writer.close();

            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            sb = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                sb.append(line + '\n');
            }

            String json = sb.toString();
            //It extracts easily...
            return json.substring(json.indexOf("http"), json.indexOf("\"", json.indexOf("http")));
        } catch (MalformedURLException e) {
            return longUrl;
        } catch (IOException e) {
            return longUrl;
        }
    }

}