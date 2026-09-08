package ai.arsun.app;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.*;
import android.view.animation.*;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.*;

import okhttp3.*;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS = "arsun_settings";
    private static final int MIC_REQUEST = 41;
    private final OkHttpClient http = new OkHttpClient.Builder().callTimeout(45, TimeUnit.SECONDS).build();
    private SharedPreferences prefs;
    private SpeechRecognizer speech;
    private TextToSpeech tts;
    private LinearLayout root, history;
    private TextView status, orb, micButton;
    private EditText input;
    private boolean listening = false;

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private TextView label(String s, float size, int color) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); return t; }
    private GradientDrawable rounded(int color, float radius) { GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g; }
    private String provider() { return prefs.getString("provider", "Gemini"); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); prefs = getSharedPreferences(PREFS, MODE_PRIVATE); setupTts(); setupSpeech(); buildUi();
    }

    private void buildUi() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(250,250,252));
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(28), dp(18), dp(28), dp(10));
        TextView menu = label("☰", 30, Color.rgb(20,20,23)); menu.setGravity(Gravity.CENTER); menu.setBackground(rounded(Color.WHITE, 30)); top.addView(menu, new LinearLayout.LayoutParams(dp(66), dp(66)));
        TextView heading = label("Arsun AI", 21, Color.rgb(25,25,30)); heading.setGravity(Gravity.CENTER); heading.setTypeface(null, 1); top.addView(heading, new LinearLayout.LayoutParams(0, dp(66), 1));
        TextView settings = label("☷", 32, Color.rgb(20,20,23)); settings.setGravity(Gravity.CENTER); settings.setRotation(90); settings.setBackground(rounded(Color.WHITE, 30)); settings.setOnClickListener(v -> showSettings()); top.addView(settings, new LinearLayout.LayoutParams(dp(66), dp(66))); root.addView(top);

        history = new LinearLayout(this); history.setOrientation(LinearLayout.VERTICAL); history.setPadding(dp(22), dp(8), dp(22), dp(5)); ScrollView scroll = new ScrollView(this); scroll.addView(history); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        FrameLayout center = new FrameLayout(this); orb = label("", 1, Color.TRANSPARENT); orb.setBackground(orbBackground()); FrameLayout.LayoutParams op = new FrameLayout.LayoutParams(dp(330), dp(330), Gravity.CENTER); center.addView(orb, op); status = label("Tap the mic and start speaking", 15, Color.rgb(125,125,132)); status.setGravity(Gravity.CENTER); FrameLayout.LayoutParams stp = new FrameLayout.LayoutParams(-1, dp(50), Gravity.CENTER); stp.topMargin = dp(220); center.addView(status, stp); root.addView(center, new LinearLayout.LayoutParams(-1, 0, 1.15f));

        LinearLayout bottom = new LinearLayout(this); bottom.setPadding(dp(26), dp(10), dp(26), dp(18)); bottom.setGravity(Gravity.CENTER_VERTICAL);
        TextView plus = label("+", 34, Color.rgb(20,20,23)); plus.setGravity(Gravity.CENTER); plus.setBackground(rounded(Color.WHITE, 30)); bottom.addView(plus, new LinearLayout.LayoutParams(dp(62), dp(62)));
        input = new EditText(this); input.setSingleLine(true); input.setHint("Ask Arsun AI"); input.setHintTextColor(Color.rgb(145,145,150)); input.setTextColor(Color.DKGRAY); input.setTextSize(18); input.setPadding(dp(16), 0, dp(8), 0); input.setBackground(rounded(Color.WHITE, 40)); LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, dp(62), 1); ip.setMargins(dp(10), 0, dp(10), 0); bottom.addView(input, ip);
        micButton = label("⌕", 33, Color.rgb(20,20,23)); micButton.setGravity(Gravity.CENTER); micButton.setBackground(rounded(Color.WHITE, 32)); micButton.setOnClickListener(v -> toggleListening()); bottom.addView(micButton, new LinearLayout.LayoutParams(dp(72), dp(62)));
        TextView send = label("➤", 28, Color.WHITE); send.setGravity(Gravity.CENTER); send.setBackground(rounded(Color.rgb(15,15,18), 32)); send.setOnClickListener(v -> sendText()); LinearLayout.LayoutParams sendp = new LinearLayout.LayoutParams(dp(62), dp(62)); sendp.setMargins(dp(10), 0, 0, 0); bottom.addView(send, sendp); root.addView(bottom); setContentView(root);
    }
    private GradientDrawable orbBackground() { GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(129,147,255), Color.rgb(205,214,255), Color.rgb(239,242,255)}); g.setShape(GradientDrawable.OVAL); return g; }
    private void pulse(boolean on) { if (!on) { orb.clearAnimation(); return; } ScaleAnimation a = new ScaleAnimation(.94f, 1.06f, .94f, 1.06f, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f); a.setDuration(850); a.setRepeatMode(Animation.REVERSE); a.setRepeatCount(Animation.INFINITE); orb.startAnimation(a); }

    private void setupSpeech() { if (!SpeechRecognizer.isRecognitionAvailable(this)) return; speech = SpeechRecognizer.createSpeechRecognizer(this); speech.setRecognitionListener(new RecognitionListener() { public void onReadyForSpeech(Bundle b) { status.setText("Listening…"); } public void onBeginningOfSpeech() { status.setText("I’m listening…"); } public void onRmsChanged(float r) {} public void onBufferReceived(byte[] b) {} public void onEndOfSpeech() { listening = false; micButton.setText("⌕"); pulse(false); } public void onError(int e) { listening = false; micButton.setText("⌕"); pulse(false); status.setText(e == SpeechRecognizer.ERROR_NO_MATCH ? "Could not hear that — try again" : "Voice input stopped"); } public void onResults(Bundle b) { ArrayList<String> x = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION); if (x != null && !x.isEmpty()) { input.setText(x.get(0)); sendText(); } } public void onPartialResults(Bundle b) {} public void onEvent(int a, Bundle b) {} }); }
    private void setupTts() { tts = new TextToSpeech(this, result -> { if (result == TextToSpeech.SUCCESS) { tts.setLanguage(Locale.getDefault()); String saved = prefs.getString("voice", ""); if (!saved.isEmpty() && tts.getVoices() != null) for (Voice v : tts.getVoices()) if (saved.equals(v.getName())) { tts.setVoice(v); break; } } }); }
    private void toggleListening() { if (speech == null) { Toast.makeText(this, "Speech recognition available nahi hai", Toast.LENGTH_LONG).show(); return; } if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST); return; } if (listening) { speech.stopListening(); listening = false; micButton.setText("⌕"); pulse(false); return; } Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true); listening = true; micButton.setText("■"); pulse(true); speech.startListening(i); }

    private void sendText() { String prompt = input.getText().toString().trim(); if (prompt.isEmpty()) { toggleListening(); return; } String key = prefs.getString("api_key", ""); if (key.isEmpty()) { showSettings(); Toast.makeText(this, "Settings me API key save kijiye", Toast.LENGTH_LONG).show(); return; } input.setText(""); addHistory("You", prompt); status.setText("Thinking…"); Executors.newSingleThreadExecutor().execute(() -> { try { String answer = callApi(prompt, key); runOnUiThread(() -> { addHistory("Arsun AI", answer); status.setText("Tap the mic and start speaking"); speak(answer); }); } catch (Exception e) { runOnUiThread(() -> { addHistory("Arsun AI", friendlyError(e)); status.setText("API error — check Settings"); }); } }); }
    private void addHistory(String who, String body) { TextView item = label(who + "\n" + body, 15, who.equals("You") ? Color.rgb(60,45,105) : Color.rgb(45,45,50)); item.setPadding(dp(14), dp(10), dp(14), dp(10)); item.setBackground(rounded(Color.WHITE, 18)); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, 0, 0, dp(8)); history.addView(item, p); }
    private void speak(String answer) { if (tts != null && prefs.getBoolean("speak", true)) tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "arsun_reply"); }
    private String friendlyError(Exception e) { String m = e.getMessage() == null ? "Unknown error" : e.getMessage(); if (m.contains("401") || m.contains("403")) return "API key invalid hai. Settings me key check kijiye."; if (m.contains("404")) return "Model nahi mila. Settings me valid model select kijiye."; if (m.contains("429")) return "API limit reach ho gayi. Thodi der baad try kijiye."; if (m.contains("Unable to resolve") || m.contains("timeout")) return "Internet connection check kijiye."; return "API error: " + m; }

    private String callApi(String prompt, String key) throws Exception { String p = provider(); String model = prefs.getString("model", "").trim(); String endpoint; String body; if (p.equals("OpenRouter")) { if (model.isEmpty()) model = "openai/gpt-4o-mini"; endpoint = "https://openrouter.ai/api/v1/chat/completions"; JSONObject req = new JSONObject(); req.put("model", model); JSONArray ms = new JSONArray(); JSONObject msg = new JSONObject(); msg.put("role", "user"); msg.put("content", prompt); ms.put(msg); req.put("messages", ms); body = req.toString(); } else { if (model.isEmpty()) model = "gemini-2.0-flash"; endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + URLEncoder.encode(model, "UTF-8") + ":generateContent?key=" + URLEncoder.encode(key, "UTF-8"); JSONObject part = new JSONObject(); part.put("text", prompt); JSONObject content = new JSONObject(); content.put("parts", new JSONArray().put(part)); body = new JSONObject().put("contents", new JSONArray().put(content)).toString(); }
        Request.Builder rb = new Request.Builder().url(endpoint).addHeader("Content-Type", "application/json"); if (p.equals("OpenRouter")) rb.addHeader("Authorization", "Bearer " + key).addHeader("HTTP-Referer", "https://arsun.ai").addHeader("X-Title", "Arsun AI"); try (Response r = http.newCall(rb.post(RequestBody.create(body, MediaType.parse("application/json"))).build()).execute()) { String raw = r.body() == null ? "" : r.body().string(); if (!r.isSuccessful()) throw new IOException(r.code() + " " + raw); JSONObject o = new JSONObject(raw); if (p.equals("OpenRouter")) return o.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content", "No response"); JSONArray candidates = o.optJSONArray("candidates"); if (candidates == null || candidates.length() == 0) throw new IOException("Empty Gemini response"); return candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).optString("text", "No response"); } }

    private void showSettings() { LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(8), 0, dp(8), 0); Spinner provider = new Spinner(this); String[] list = {"Gemini", "OpenRouter"}; provider.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, list)); provider.setSelection(provider().equals("OpenRouter") ? 1 : 0); box.addView(provider); EditText key = new EditText(this); key.setHint("API key"); key.setSingleLine(true); key.setInputType(0x00000081); key.setText(prefs.getString("api_key", "")); box.addView(key); EditText model = new EditText(this); model.setHint("Model (optional)"); model.setSingleLine(true); model.setText(prefs.getString("model", "")); box.addView(model); CheckBox speak = new CheckBox(this); speak.setText("AI reply ko voice me bolna"); speak.setChecked(prefs.getBoolean("speak", true)); box.addView(speak); TextView voices = label("Voice change karein", 16, Color.rgb(45,45,45)); voices.setPadding(dp(8), dp(12), dp(8), dp(12)); voices.setOnClickListener(v -> showVoices()); box.addView(voices); new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Arsun AI Settings").setMessage("API key device par locally save hoti hai.").setView(box).setNegativeButton("Cancel", null).setPositiveButton("Save", (d,w) -> prefs.edit().putString("provider", provider.getSelectedItem().toString()).putString("api_key", key.getText().toString().trim()).putString("model", model.getText().toString().trim()).putBoolean("speak", speak.isChecked()).apply()).show(); }
    private void showVoices() { if (tts == null || tts.getVoices() == null) { Toast.makeText(this, "Voices unavailable", Toast.LENGTH_SHORT).show(); return; } ArrayList<Voice> usable = new ArrayList<>(); for (Voice v : tts.getVoices()) if (!v.isNetworkConnectionRequired()) usable.add(v); if (usable.isEmpty()) usable.addAll(tts.getVoices()); String[] names = new String[Math.min(usable.size(), 20)]; for (int i=0;i<names.length;i++) names[i] = usable.get(i).getName().replace("#", " "); new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Choose AI voice").setItems(names, (d, which) -> { tts.setVoice(usable.get(which)); prefs.edit().putString("voice", usable.get(which).getName()).apply(); Toast.makeText(this, "Voice changed", Toast.LENGTH_SHORT).show(); }).show(); }
    @Override protected void onDestroy() { if (speech != null) speech.destroy(); if (tts != null) { tts.stop(); tts.shutdown(); } super.onDestroy(); }
}
