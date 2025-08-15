package fun.xingwangzhe.tryfishport.client;

import com.google.gson.Gson;
import net.minidev.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class UnbanService {
    public static class CheckResult {
        private final String ip;
        private final boolean blacklisted;
        
        public CheckResult(String ip, boolean blacklisted) {
            this.ip = ip;
            this.blacklisted = blacklisted;
        }
        
        public String getIp() {
            return ip;
        }
        
        public boolean isBlacklisted() {
            return blacklisted;
        }
    }
    
    public CheckResult checkBanStatus(String ipv4Token) throws Exception {
        if (ipv4Token != null && !ipv4Token.isEmpty()) {
            // POST /check 提交 IPv4 token
            URL checkUrl = URI.create("https://ip-unban.fishport.net/check").toURL();
            HttpURLConnection checkConn = (HttpURLConnection) checkUrl.openConnection();
            checkConn.setRequestMethod("POST");
            checkConn.setRequestProperty("Content-Type", "application/json");
            checkConn.setDoOutput(true);
            String body = "{\"token\":\"" + ipv4Token + "\"}";
            try (OutputStream os = checkConn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int checkCode = checkConn.getResponseCode();
            if (checkCode == 200) {
                BufferedReader checkReader = new BufferedReader(new InputStreamReader(checkConn.getInputStream()));
                StringBuilder checkResp = new StringBuilder();
                String l;
                while ((l = checkReader.readLine()) != null) {
                    checkResp.append(l);
                }
                checkReader.close();
                String checkText = checkResp.toString();
                // 解析JSON格式
                Gson gson = new Gson();
                JSONObject checkJson = gson.fromJson(checkText, JSONObject.class);

                String ip = (String) checkJson.get("ip");
                Boolean blacklisted = (Boolean) checkJson.get("blacklisted");
                
                return new CheckResult(ip, blacklisted != null ? blacklisted : false);
            } else {
                throw new Exception("Check failed with code: " + checkCode);
            }
        } else {
            // 默认IPV4 直接 GET /check
            // 检查IP是否被封 使用URI方式创建URL对象，避免使用已过时的URL构造函数
            URL url = new URI("https://ip-unban.fishport.net/check").toURL();
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

                String responseText = response.toString();
                // JSON解析
                Gson gson = new Gson();
                JSONObject jsonObject = gson.fromJson(responseText, JSONObject.class);
                Boolean blacklisted = (Boolean) jsonObject.get("blacklisted");
                String ip = (String) jsonObject.get("ip");
                
                return new CheckResult(ip, blacklisted != null ? blacklisted : false);
            } else {
                throw new Exception("Check failed with code: " + responseCode);
            }
        }
    }
    
    public static class UnbanResult {
        private final boolean success;
        private final String message;
        
        public UnbanResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    public UnbanResult submitUnbanRequest(String captcha, String captchaToken, String ipv4Token) throws Exception {
        URL url = URI.create("https://ip-unban.fishport.net/unblock").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        // 构造请求体
        StringBuilder body = new StringBuilder();
        body.append("{\"suffix\":\"").append(captcha).append("\"");
        body.append(",\"captcha_token\":\"").append(captchaToken).append("\"");
        // 新增：如果有ipv4Token，带上
        if (ipv4Token != null && !ipv4Token.isEmpty()) {
            body.append(",\"ipv4_token\":\"").append(ipv4Token).append("\"");
        }
        body.append("}");
        try (OutputStream os = connection.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            String responseText = response.toString();
            // 解析JSON格式
            Gson gson = new Gson();
            JSONObject jsonObject = gson.fromJson(responseText, JSONObject.class);
            if (jsonObject.containsKey("msg")) {
                // success
                return new UnbanResult(true, (String) jsonObject.get("msg"));
            } else {
                // fail
                return new UnbanResult(false, (String) jsonObject.get("error"));
            }
        } else {
            throw new Exception("Unban request failed with code: " + responseCode);
        }
    }
}