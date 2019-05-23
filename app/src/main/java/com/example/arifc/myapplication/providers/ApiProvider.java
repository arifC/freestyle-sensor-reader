package com.example.arifc.myapplication.providers;

import android.util.JsonReader;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

public class ApiProvider {
    public static final String API_ROOT = "http://smartbuddy.arif-cerit.de:5000";

    // Endpoint: POST /sensor/measurement
    // Sends the last measurement to the backend
    public static void sendMeasurement(final byte sensorId, final float glucoseLevel, final long timestamp, final long sensorAge) {
        Log.d("sendMeasurement", "Calling sendMeasurement(" + sensorId + ", " + glucoseLevel + ", " + timestamp + ", " + sensorAge + ")");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(API_ROOT + "/sensor/measurement");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("sensor_id", sensorId);
                    jsonParam.put("glucose_level", glucoseLevel);
                    jsonParam.put("timestamp", timestamp);
                    jsonParam.put("sensor_age", sensorAge);

                    Log.i("JSON", jsonParam.toString());
                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                    os.writeBytes(jsonParam.toString());

                    os.flush();
                    os.close();

                    Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                    Log.i("MSG", conn.getResponseMessage());

                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }

    // Endpoint: POST /sensor/history
    // Sends historical data from 8 hours to the backend
    public static void sendMeasurementHistory(final int sensorId, final float glucoseLevel, final float timestamp, final float sensorAge) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(API_ROOT + "/sensor/history");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("sensor_id", sensorId);
                    jsonParam.put("glucose_level", glucoseLevel);
                    jsonParam.put("timestamp", timestamp);
                    jsonParam.put("sensor_age", sensorAge);

                    Log.i("JSON", jsonParam.toString());
                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                    os.writeBytes(jsonParam.toString());

                    os.flush();
                    os.close();

                    Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                    Log.i("MSG", conn.getResponseMessage());

                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    // Endpoint: GET /speech
    // Get the speech tags available
    public static JSONArray getSpeechTags() {
        Log.d("getSpeechTags", "Calling getSpeechTags()");
        final String[] result = new String[1];
        final CountDownLatch latch = new CountDownLatch(1);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    URL url = new URL(API_ROOT + "/speech");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoInput(true);

                    if (conn.getResponseCode() == 200) {
                        InputStream responseBody = conn.getInputStream();
                        StringBuffer sb = new StringBuffer();
                        InputStream is = null;
                        try {
                            is = new BufferedInputStream(conn.getInputStream());
                            BufferedReader br = new BufferedReader(new InputStreamReader(is));
                            String inputLine = "";
                            while ((inputLine = br.readLine()) != null) {
                                sb.append(inputLine);
                            }
                            result[0] = sb.toString();
                        } catch (Exception e) {
                            result[0] = "";
                        }
                        responseBody.close();
                    }

                    conn.disconnect();
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        JSONArray json = null;
        try {
            thread.start();
            latch.await();
            json = new JSONArray(result[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    // Endpoint: GET /speech/<tag_id>
    // Gets the message for a speech tag
    public static void getMessageForSpeechTag(final String tag) {
        Log.d("getMessageForSpeechTags", "Calling getMessageForSpeechTags()");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(API_ROOT + "/speech/" + tag);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    if (conn.getResponseCode() == 200) {
                        InputStream responseBody = conn.getInputStream();
                        InputStreamReader responseBodyReader = new InputStreamReader(responseBody, "UTF-8");
                        JsonReader jsonReader = new JsonReader(responseBodyReader);
                        //do stuff with json

                        responseBody.close();
                    }

                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    // Endpoint: GET /getHugMe
    // Checks the hugme flag
    public static boolean getHugMeFlag() {
        Log.d("getSpeechTags", "Calling getHugMeFlag()");
        final boolean[] result = new boolean[1];
        final CountDownLatch latch = new CountDownLatch(1);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    URL url = new URL(API_ROOT + "/hugme/1");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoInput(true);

                    if (conn.getResponseCode() == 200) {
                        InputStream responseBody = conn.getInputStream();
                        StringBuffer sb = new StringBuffer();
                        InputStream is = null;
                        try {
                            is = new BufferedInputStream(conn.getInputStream());
                            BufferedReader br = new BufferedReader(new InputStreamReader(is));
                            String inputLine = "";
                            while ((inputLine = br.readLine()) != null) {
                                sb.append(inputLine);
                            }
                            JSONObject json = new JSONObject(sb.toString());
                            result[0] = json.getBoolean("hugme");
                        } catch (Exception e) {
                            result[0] = false;
                        }
                        responseBody.close();
                    }

                    conn.disconnect();
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        try {
            thread.start();
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return result[0];
    }

    // Endpoint: POST /hugme/1
    // Sets the hugme flag to false after it is spoken
    public static void setHugMeFlag(final boolean flag) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(API_ROOT + "/hugme/1");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("hugme", flag);

                    Log.i("JSON", jsonParam.toString());
                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                    os.writeBytes(jsonParam.toString());

                    os.flush();
                    os.close();

                    Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                    Log.i("MSG", conn.getResponseMessage());

                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
}
