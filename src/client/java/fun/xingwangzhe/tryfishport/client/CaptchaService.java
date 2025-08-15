package fun.xingwangzhe.tryfishport.client;

import com.google.gson.Gson;
import net.minidev.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CaptchaService {
    public static class CaptchaResult {
        private final String captcha;
        private final String captchaToken;
        
        public CaptchaResult(String captcha, String captchaToken) {
            this.captcha = captcha;
            this.captchaToken = captchaToken;
        }
        
        public String getCaptcha() {
            return captcha;
        }
        
        public String getCaptchaToken() {
            return captchaToken;
        }
    }
    
    public CaptchaResult calculateCaptcha() throws Exception {
        // 直接GET
        URL url = new URI("https://ip-unban.fishport.net/captcha-challenge").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            Gson gson = new Gson();
            JSONObject jsonObject = gson.fromJson(response.toString().trim(), JSONObject.class);
            String captchaText = (String) jsonObject.get("captcha_token");
            
            // 尝试解析验证码
            try {
                // hash计算
                if (captchaText == null || captchaText.isEmpty()) {
                    throw new NumberFormatException("Captcha token is empty or null");
                }

                // 解析JWT格式的token
                String[] parts = captchaText.split("\\.");
                if (parts.length != 3) {
                    throw new NumberFormatException("Invalid JWT format");
                }

                String payload = parts[1];
                // Base64解码

                byte[] decodedBytes = java.util.Base64.getUrlDecoder().decode(payload);
                String decodedPayload = new String(decodedBytes, StandardCharsets.UTF_8);
                // 解析JSON
                Gson gson2 = new Gson();
                JSONObject payloadJson = gson2.fromJson(decodedPayload, JSONObject.class);
                
                String prefix = (String) payloadJson.get("prefix");
                String target = (String) payloadJson.get("target");
                if (prefix == null || target == null) {
                    throw new NumberFormatException("Prefix or target is null");
                }
                // 计算验证码
                String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
                int matchLength = target.length();
                int suffixLength = matchLength <= 4 ? 3 : (matchLength <= 6 ? 4 : 5);
                long total = (long) Math.pow(letters.length(), suffixLength);
                long count = 0;
                boolean[] found = {false};
                java.util.concurrent.atomic.AtomicReference<String> result = new java.util.concurrent.atomic.AtomicReference<>("");

                final int batchSize = 1000;
                for (long batchStart = 0; batchStart < total && !found[0]; batchStart += batchSize) {
                    List<Thread> tasks = new ArrayList<>();
                    for (long i = batchStart; i < Math.min(batchStart + batchSize, total) && !found[0]; i++) {
                        long index = i;
                        Thread task = new Thread(() -> {
                            StringBuilder suffix = new StringBuilder();
                            long num = index;
                            for (int p = 0; p < suffixLength; p++) {
                                suffix.insert(0, letters.charAt((int) (num % letters.length())));
                                num /= letters.length();
                            }
                            String candidate = prefix + suffix;
                            String hash = utilsSha256(candidate); // 你需要实现 utilsSha256(String) 方法，返回hash字符串
                            if (hash.substring(0, matchLength).equals(target)) {
                                result.set(suffix.toString());
                                found[0] = true;
                            }
                        });
                        tasks.add(task);
                        task.start();
                        count++;
                    }
                    for (Thread t : tasks) {
                        try { t.join(); } catch (InterruptedException ignored) {}
                    }
                }
                if (!found[0]) {
                    throw new NumberFormatException("Captcha calculation failed, no match found");
                }
                if (result.get() != null && !result.get().isEmpty()) {
                    return new CaptchaResult(result.get(), captchaText);
                } else {
                    throw new NumberFormatException("Captcha result is empty");
                }
            } catch (NumberFormatException e) {
                throw new Exception("Captcha calculation failed: " + e.getMessage(), e);
            }
        } else {
            throw new Exception("Failed to fetch captcha with code: " + responseCode);
        }
    }

    private String utilsSha256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }
}