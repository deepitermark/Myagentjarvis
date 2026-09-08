package ai.arsun.app;

import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import org.json.*;

import java.io.IOException;
import java.util.concurrent.*;

import okhttp3.*;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS = "arsun_settings";
    private final OkHttpClient http = new OkHttpClient();
    private LinearLayout messages;
    private EditText input;
    private TextView providerLabel;
    private SharedPreferences prefs;

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private TextView text(String value, float size, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); return t;
    }
    private GradientDrawable bg(int color, float radius) { GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g; }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); prefs = getSharedPreferences(PREFS, MODE_PRIVATE); buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(13,11,20));
        LinearLayout bar = new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(dp(20), dp(14), dp(14), dp(10));
        TextView title = text("Arsun AI", 24, Color.WHITE); title.setTypeface(null, 1); bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        providerLabel = text(providerName(), 12, Color.rgb(190,175,255)); providerLabel.setPadding(dp(10), dp(7), dp(10), dp(7)); providerLabel.setBackground(bg(Color.rgb(40,31,62), 20));
        bar.addView(providerLabel); Space gap = new Space(this); bar.addView(gap, new LinearLayout.LayoutParams(dp(8), 1));
        TextView settings = text("⚙", 24, Color.WHITE); settings.setGravity(Gravity.CENTER); settings.setOnClickListener(v -> showSettings()); bar.addView(settings, new LinearLayout.LayoutParams(dp(42), dp(42)));
        root.addView(bar);
        ScrollView scroll = new ScrollView(this); messages = new LinearLayout(this); messages.setOrientation(LinearLayout.VERTICAL); messages.setPadding(dp(18), dp(12), dp(18), dp(18)); scroll.addView(messages); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (messages.getChildCount() == 0) addMessage("Arsun AI", "Namaste! Main aapka AI assistant hoon. Neeche message likhiye, ya ⚙ se Gemini/OpenRouter API key set kijiye.", false);
        LinearLayout composer = new LinearLayout(this); composer.setPadding(dp(14), dp(10), dp(14), dp(14)); composer.setGravity(Gravity.CENTER_VERTICAL);
        input = new EditText(this); input.setHint("Ask Arsun AI anything..."); input.setHintTextColor(Color.rgb(150,142,165)); input.setTextColor(Color.WHITE); input.setTextSize(16); input.setSingleLine(true); input.setPadding(dp(16), 0, dp(10), 0); input.setBackground(bg(Color.rgb(32,27,43), 28)); composer.addView(input, new LinearLayout.LayoutParams(0, dp(54), 1));
        TextView send = text("➤", 25, Color.WHITE); send.setGravity(Gravity.CENTER); send.setBackground(bg(Color.rgb(124,79,210), 28)); send.setOnClickListener(v -> sendMessage()); LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(54), dp(54)); sp.setMargins(dp(8), 0, 0, 0); composer.addView(send, sp); root.addView(composer);
        setContentView(root);
    }

    private String providerName() { return prefs.getString("provider", "Gemini"); }
    private void addMessage(String who, String body, boolean user) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(15), dp(11), dp(15), dp(11)); card.setBackground(bg(user ? Color.rgb(82,52,137) : Color.rgb(31,27,41), 18));
        TextView head = text(who, 12, user ? Color.rgb(230,215,255) : Color.rgb(177,152,255)); head.setTypeface(null, 1); card.addView(head); TextView msg = text(body, 16, Color.WHITE); msg.setPadding(0, dp(5), 0, 0); card.addView(msg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, 0, 0, dp(12)); messages.addView(card, p); messages.post(() -> ((ScrollView)messages.getParent()).fullScroll(View.FOCUS_DOWN));
    }

    private void sendMessage() {
        String prompt = input.getText().toString().trim(); if (prompt.isEmpty()) return;
        String key = prefs.getString("api_key", ""); if (key.isEmpty()) { showSettings(); Toast.makeText(this, "Pehle API key save kijiye", Toast.LENGTH_LONG).show(); return; }
        input.setText(""); ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(), 0); addMessage("You", prompt, true); addMessage("Arsun AI", "Soch raha hoon...", false); final int pendingIndex = messages.getChildCount() - 1;
        Executors.newSingleThreadExecutor().execute(() -> { try { String answer = callApi(prompt, key); runOnUiThread(() -> replacePending(pendingIndex, answer)); } catch (Exception e) { runOnUiThread(() -> replacePending(pendingIndex, "Error: " + e.getMessage())); } });
    }
    private void replacePending(int index, String answer) { if (index >= 0 && index < messages.getChildCount()) { LinearLayout card = (LinearLayout)messages.getChildAt(index); ((TextView)card.getChildAt(1)).setText(answer); } }
    private String callApi(String prompt, String key) throws Exception {
        String provider = providerName(); String endpoint; String body;
        if (provider.equals("OpenRouter")) { endpoint = "https://openrouter.ai/api/v1/chat/completions"; body = "{\"model\":\"" + json(prefs.getString("model", "openai/gpt-4o-mini")) + "\",\"messages\":[{\"role\":\"user\",\"content\":\"" + json(prompt) + "\"}]}"; }
        else { String model = prefs.getString("model", "gemini-2.0-flash"); endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + key; body = "{\"contents\":[{\"parts\":[{\"text\":\"" + json(prompt) + "\"}]}]}"; }
        Request request = new Request.Builder().url(endpoint).addHeader("Authorization", provider.equals("OpenRouter") ? "Bearer " + key : "").addHeader("Content-Type", "application/json").post(RequestBody.create(body, MediaType.parse("application/json"))).build();
        try (Response response = http.newCall(request).execute()) { String raw = response.body() == null ? "" : response.body().string(); if (!response.isSuccessful()) throw new IOException("API " + response.code() + ": " + raw); JSONObject o = new JSONObject(raw); if (provider.equals("OpenRouter")) return o.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content"); return o.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text"); }
    }
    private String json(String s) { return JSONObject.quote(s).substring(1, JSONObject.quote(s).length()-1); }

    private void showSettings() {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(8), 0, dp(8), 0);
        Spinner provider = new Spinner(this); String[] providers = {"Gemini", "OpenRouter"}; ArrayAdapter<String> a = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, providers); provider.setAdapter(a); provider.setSelection(providerName().equals("OpenRouter") ? 1 : 0); box.addView(provider);
        EditText key = new EditText(this); key.setHint("API key"); key.setSingleLine(true); key.setInputType(0x00000081); key.setText(prefs.getString("api_key", "")); box.addView(key);
        EditText model = new EditText(this); model.setHint("Model (optional)"); model.setSingleLine(true); model.setText(prefs.getString("model", "")); box.addView(model);
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Arsun AI Settings").setMessage("API key sirf is device par save hoti hai.").setView(box).setNegativeButton("Cancel", null).setPositiveButton("Save", (d,w) -> { prefs.edit().putString("provider", provider.getSelectedItem().toString()).putString("api_key", key.getText().toString().trim()).putString("model", model.getText().toString().trim()).apply(); providerLabel.setText(providerName()); }).show();
    }
}
