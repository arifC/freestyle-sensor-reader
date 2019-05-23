package com.example.arifc.myapplication;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.example.arifc.myapplication.providers.ApiProvider;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter mNfcAdapter;
    private String lectura;
    private float currentGlucose = 0f;
    private TextView tvResult;
    private Vibrator vibrator;
    private TextToSpeech textToSpeech;
    private HashMap<String, String> speech = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvResult = (TextView) findViewById(R.id.result);

        final String[] welcomeMessageArray = {"I'm SmartBuddy.", "Hello there.", "Hi, I'm SmartBuddy.", "How are you, I'm SmartBuddy."};

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.UK);
                    textToSpeech.speak(getRandomWelcomeMessage(welcomeMessageArray), TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        });

        try {
            JSONArray speechTags = ApiProvider.getSpeechTags();
            for (int i = 0; i < speechTags.length(); i++) {
                JSONObject o = speechTags.getJSONObject(i);
                String tag = o.keys().next().toString().replaceAll("[\\[\\](){}]", "");
                ;
                String message = o.getString(tag);
                speech.put(tag, message);
                Log.d("PUT", tag + ", " + message);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC is disabled.", Toast.LENGTH_LONG).show();
        }

        Timer timer = new Timer();
        TimerTask hugMeTask = new TimerTask() {
            @Override
            public void run() {
                Log.d("hugme", "It's: " + ApiProvider.getHugMeFlag());
                if (ApiProvider.getHugMeFlag()) {
                    textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                        @Override
                        public void onInit(int status) {
                            if (status != TextToSpeech.ERROR) {
                                textToSpeech.setLanguage(Locale.UK);
                                textToSpeech.speak("Please hug me.", TextToSpeech.QUEUE_FLUSH, null);
                            }
                        }
                    });
                    ApiProvider.setHugMeFlag(false);
                }
            }
        };

        timer.schedule(hugMeTask, 0l, 5000);

        handleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupForegroundDispatch(this, mNfcAdapter);
    }

    @Override
    protected void onPause() {
        stopForegroundDispatch(this, mNfcAdapter);
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            Log.d("handleIntent", "NfcAdapter.ACTION_TECH_DISCOVERED");
            // In case we would still use the Tech Discovered Intent
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techList = tag.getTechList();
            String searchedTech = NfcV.class.getName();
            Log.d("handleIntent", searchedTech);
            //Tag discovered
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));

            new NfcVReaderTask().execute(tag);
        }
    }

    public static String getRandomWelcomeMessage(String[] array) {
        int rnd = new Random().nextInt(array.length);
        return array[rnd];
    }

    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private void addText(final String s) {
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                tvResult.setText(s);
            }
        });

    }

    private float glucoseReading(int val) {
        int bitmask = 0x0FFF;
        return Float.valueOf(Float.valueOf((val & bitmask) / 6) - 37);
    }

    private class NfcVReaderTask extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];
            NfcV nfcvTag = NfcV.get(tag);

            try {
                nfcvTag.connect();
            } catch (IOException e) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Error opening NFC connection!", Toast.LENGTH_SHORT).show();
                    }
                });

                return null;
            }

            lectura = "";
            byte[][] allBlocks = new byte[40][8];
            Log.d("Enter NFC Read", "---------------------------------------------------------------");

            try {
                // Get system information (0x2B)
                byte[] cmd = new byte[]{
                        (byte) 0x00, // Flags
                        (byte) 0x2B // Command: Get system information
                };
                byte[] systeminfo = nfcvTag.transceive(cmd);
                Log.d("systemInfo", systeminfo.toString() + " - " + systeminfo.length);
                Log.d("systemInfo", "HEX: " + bytesToHex(systeminfo));

                systeminfo = Arrays.copyOfRange(systeminfo, 2, systeminfo.length - 1);

                byte[] memorySize = {systeminfo[6], systeminfo[5]};
                Log.d("systemInfo", "Memory Size: " + bytesToHex(memorySize) + " / " + Integer.parseInt(bytesToHex(memorySize).trim(), 16));

                byte[] blocks = {systeminfo[8]};
                Log.d("systemInfo", "blocks: " + bytesToHex(blocks) + " / " + Integer.parseInt(bytesToHex(blocks).trim(), 16));

                for (int i = 3; i <= 40; i++) {
                    // Read single block
                    cmd = new byte[]{
                            (byte) 0x00, // Flags
                            (byte) 0x20, // Command: Read multiple blocks
                            (byte) i // block (offset)
                    };

                    byte[] oneBlock = nfcvTag.transceive(cmd);
                    oneBlock = Arrays.copyOfRange(oneBlock, 1, oneBlock.length);
                    allBlocks[i - 3] = Arrays.copyOf(oneBlock, 8);
                }

                String completeBlocks = "";
                for (int i = 0; i < 40; i++) {
                    Log.d("Blocks", "Line " + i + ": " + bytesToHex(allBlocks[i]));
                    completeBlocks = completeBlocks + bytesToHex(allBlocks[i]);
                }

                Log.d("Blocks", "completeBlocks: " + completeBlocks);
                Log.d("Blocks", "Next read: " + completeBlocks.substring(4, 6));

                int current = Integer.parseInt(completeBlocks.substring(4, 6), 16);
                Log.d("Blocks", "Next read: " + current);

                Log.d("Blocks", "Next historic read " + completeBlocks.substring(6, 8));
                int minutesSinceStart = Integer.parseInt(completeBlocks.substring(586, 588) + completeBlocks.substring(584, 586), 16);
                Log.d("Blocks", "Minutes since start: " + minutesSinceStart + " HEX: " + completeBlocks.substring(586, 588) + completeBlocks.substring(584, 586));

                String[] block1 = new String[16];
                String[] block2 = new String[32];

                int ii = 0;
                for (int i = 8; i < 8 + 15 * 12; i += 12) {
                    block1[ii] = completeBlocks.substring(i, i + 12);
                    final String g = completeBlocks.substring(i + 2, i + 4) + completeBlocks.substring(i, i + 2);

                    if (current == ii) {
                        currentGlucose = glucoseReading(Integer.parseInt(g, 16));
                    }
                    ii++;
                }

                lectura = lectura + "Current approximate glucose " + currentGlucose;
                Log.d("glucose", "Current approximate glucose " + currentGlucose);

                if (currentGlucose != 0) {
                    Log.d("glucose", "Sending glucose level to backend: " + ApiProvider.API_ROOT);
                    // make this dynamic
                    if (currentGlucose > 120) {
                        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                            @Override
                            public void onInit(int status) {
                                if (status != TextToSpeech.ERROR) {
                                    textToSpeech.setLanguage(Locale.UK);
                                    textToSpeech.speak(speech.get("GLUCOSE_HIGH"), TextToSpeech.QUEUE_FLUSH, null);
                                }
                            }
                        });
                    } else if (currentGlucose < 80) {
                        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                            @Override
                            public void onInit(int status) {
                                if (status != TextToSpeech.ERROR) {
                                    textToSpeech.setLanguage(Locale.UK);
                                    textToSpeech.speak(speech.get("GLUCOSE_LOW"), TextToSpeech.QUEUE_FLUSH, null);
                                }
                            }
                        });
                    } else {
                        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                            @Override
                            public void onInit(int status) {
                                if (status != TextToSpeech.ERROR) {
                                    textToSpeech.setLanguage(Locale.UK);
                                    textToSpeech.speak(speech.get("GLUCOSE_RIGHT"), TextToSpeech.QUEUE_FLUSH, null);
                                }
                            }
                        });
                    }
                    ApiProvider.sendMeasurement(nfcvTag.getDsfId(), currentGlucose, System.currentTimeMillis(), minutesSinceStart);
                    //Tag successfully read
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    Log.e("glucose", "Did not send glucose level since it is 0");
                }

                ii = 0;
                for (int i = 188; i < 188 + 31 * 12; i += 12) {
                    block2[ii] = completeBlocks.substring(i, i + 12);
                    ii++;
                }

            } catch (IOException e) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Error reading NFC!", Toast.LENGTH_SHORT).show();
                    }
                });

                textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status != TextToSpeech.ERROR) {
                            textToSpeech.setLanguage(Locale.UK);
                            textToSpeech.speak(speech.get("GLUCOSE_ERROR"), TextToSpeech.QUEUE_FLUSH, null);
                        }
                    }
                });

                return null;
            }

            addText(lectura);

            try {
                nfcvTag.close();
            } catch (IOException e) {
                /*
                Abbott.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Error closing NFC connection!", Toast.LENGTH_SHORT).show();
                    }
                });

                return null;
                */
            }
            return null;
        }
    }
}