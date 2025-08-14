package fun.xingwangzhe.tryfishport.client;

import com.google.gson.Gson;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minidev.json.JSONObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class TryFishportUI extends Screen {
    private final Screen parent;
    private String ipAddress = Text.translatable("tryfishport.ui.unban.ip.getting").getString();
    private String status = Text.translatable("tryfishport.ui.unban.status.checking").getString();
    private String captcha = "";
    // 保存翻译键而不是实际文本
    private String resultMessage = "";
    private boolean isChecking = false;
    private boolean isBanned = false;
    private boolean isIPv6 = false;
    private String ipv4Token = "";
    private String captchaToken = "";

    public TryFishportUI(Screen parent) {
        super(Text.translatable("tryfishport.ui.unban.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // 添加关闭按钮
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("tryfishport.ui.unban.button.close"), button -> close()).dimensions(this.width / 2 - 50, this.height - 30, 100, 20).build());

        // 添加检查按钮
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("tryfishport.ui.unban.button.check_status"), button -> checkBanStatus()).dimensions(this.width / 2 - 105, this.height - 60, 100, 20).build());

        // 添加解封按钮（初始禁用）
        ButtonWidget unbanButton = ButtonWidget.builder(Text.translatable("tryfishport.ui.unban.button.submit_unban"), button -> submitUnbanRequest()).dimensions(this.width / 2 + 5, this.height - 60, 100, 20).build();
        unbanButton.active = false;
        this.addDrawableChild(unbanButton);

        // 获取IP地址
        getIPAddress();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        // 绘制标题
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
        
        // 绘制IP地址信息
        context.drawTextWithShadow(this.textRenderer, Text.translatable("tryfishport.ui.general.ip").getString() + ipAddress, 10, 50, 0xFFFFFF);
        
        // 绘制IPv6提示信息
        if (isIPv6) {
            context.drawTextWithShadow(this.textRenderer, Text.translatable("tryfishport.ui.unban.ipv6.warning").getString(), 10, 65, 0xFFFF00);
        }
        
        // 绘制状态信息
        context.drawTextWithShadow(this.textRenderer, Text.translatable("tryfishport.ui.general.status").getString() + status, 10, 70, 0xFFFFFF);
        
        // 绘制封禁状态
        if (isBanned) {
            context.drawTextWithShadow(this.textRenderer, Text.translatable("tryfishport.ui.general.captcha").getString() + captcha, 10, 90, 0xFFFFFF);
        }
        
        // 绘制结果信息
        if (!Text.translatable(resultMessage).getString().isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, Text.translatable(resultMessage), 10, 110, 0xFFFFFF);
        }
    }

    private void getIPAddress() {
        // 避免重复执行IP获取
        if (!ipAddress.equals(Text.translatable("tryfishport.ui.unban.ip.getting").getString())) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                tryGetIPFromService();
                // 更新状态文本为可翻译格式
                status = Text.translatable(status).getString();
                // 检查是否为IPv6地址
                isIPv6 = ipAddress.contains(":") && !ipAddress.contains(".");

            } catch (Exception e) {
                ipAddress = Text.translatable("tryfishport.ui.unban.ip.failed").getString();
                status = Text.translatable("tryfishport.ui.unban.status.error").getString();
                resultMessage = "tryfishport.ui.unban.result.get_ip_failed";
                e.printStackTrace();
            }
            // 更新UI
            MinecraftClient.getInstance().execute(this::refreshUI);
        });
    }

    private void tryGetIPFromService() {
        try {
            // 使用URI方式创建URL对象，避免使用已过时的URL构造函数
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
                
                String ip = response.toString().trim();
                
                // 对于返回JSON格式的API (如httpbin.org)
                if (ip.startsWith("{")) {
                    // 解析JSON格式到HashMap
                    Gson gson = new Gson();
                    JSONObject jsonObject = gson.fromJson(ip, JSONObject.class);

                    if ((boolean) jsonObject.get("ipv6only")) {
                        // check pp.ua
                        String jws = fetchIPv4Token();
                        if (jws != null && !jws.isEmpty()) {
                            // 报错
                            status = Text.translatable("tryfishport.ui.unban.ip.failed").getString();
                            return;
                        }

                        Gson jwtGson = new Gson();
                        JSONObject jwtObject = jwtGson.fromJson(jws, JSONObject.class);
                        // 获取IP地址
                        if (jwtObject == null || !jwtObject.containsKey("ip")) {
                            status = Text.translatable("tryfishport.ui.unban.ip.failed").getString();
                            return;
                        }
                        ip = (String) jwtObject.get("ip");
                        isIPv6 = false;
                        ipv4Token = (String) jwtObject.get("jws");
                    } else {
                        // 获取IP地址
                        ip = (String) jsonObject.get("ip");
                    }
                }
                
                // 验证IP地址格式
                if (isValidIPAddress(ip)) {
                    ipAddress = ip;
                    status = Text.translatable("tryfishport.ui.unban.status.ready").getString();
                    // CHECK STATUS
                    checkBanStatus();
                }
            }
        } catch (Exception e) {
            // 忽略单个服务的错误，继续尝试其他服务
            System.out.println("Failed to get IP from " + "https://ip-unban.fishport.net/check" + ": " + e.getMessage());
        }
    }

    private boolean isValidIPAddress(String ip) {
        if (ip == null || ip.isEmpty()) return false;

        // 更严格的IPv6校验：8组16进制数，每组1-4位，用:分隔，允许::简写
        if (ip.contains(":")) {
            String ipv6Pattern = "^(?:[0-9a-fA-F]{1,4}:){2,7}[0-9a-fA-F]{1,4}$|^(?:[0-9a-fA-F]{1,4}:){1,7}:$|^:(:[0-9a-fA-F]{1,4}){1,7}$|^(?:[0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}$|^(?:[0-9a-fA-F]{1,4}:){1,5}(?::[0-9a-fA-F]{1,4}){1,2}$|^(?:[0-9a-fA-F]{1,4}:){1,4}(?::[0-9a-fA-F]{1,4}){1,3}$|^(?:[0-9a-fA-F]{1,4}:){1,3}(?::[0-9a-fA-F]{1,4}){1,4}$|^(?:[0-9a-fA-F]{1,4}:){1,2}(?::[0-9a-fA-F]{1,4}){1,5}$|^[0-9a-fA-F]{1,4}:(?::[0-9a-fA-F]{1,4}){1,6}$|^::$";
            return ip.matches(ipv6Pattern);
        }

        // IPv4校验
        String ipv4Pattern = "^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)$";
        return ip.matches(ipv4Pattern);
    }

    private void checkBanStatus() {
        if (isChecking) {
            return;
        }

        // 如果是IPv6地址，不允许检查状态
        if (isIPv6) {
            resultMessage = "tryfishport.ui.unban.result.ipv6_not_supported";
            refreshUI();
            return;
        }

        isChecking = true;
        status = Text.translatable("tryfishport.ui.unban.status.checking").getString();
        resultMessage = "";
        refreshUI();

        CompletableFuture.runAsync(() -> {
            try {

                if (ipv4Token != null && !ipv4Token.isEmpty()) {
                    // POST /check 提交 IPv4 token
                    URL checkUrl = new URL("https://ip-unban.fishport.net/check");
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

                        if (ip != null) {
                            ipAddress = ip;
                            isIPv6 = false;
                        }
                        if (blacklisted != null) {
                            isBanned = blacklisted;
                        }
                    } else {
                        status = Text.translatable("tryfishport.ui.unban.status.check_failed").getString();
                        resultMessage = "tryfishport.ui.unban.result.check_failed_code" + checkCode;
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
                        if (blacklisted) {
                            isBanned = true;
                            status = Text.translatable("tryfishport.ui.unban.status.banned").getString();
                            resultMessage = "tryfishport.ui.unban.captcha.calculating";
                            calculateCaptcha();
                        } else {
                            isBanned = false;
                            status = Text.translatable("tryfishport.ui.unban.status.normal").getString();
                            resultMessage = "tryfishport.ui.unban.result.not_banned";
                        }
                    } else {
                        status = Text.translatable("tryfishport.ui.unban.status.check_failed").getString();
                        resultMessage = "tryfishport.ui.unban.result.check_failed_code" + responseCode;
                    }
                }


            } catch (Exception e) {
                status = Text.translatable("tryfishport.ui.unban.status.check_failed").getString();
                resultMessage = "tryfishport.ui.unban.result.check_failed";
                e.printStackTrace();
            } finally {
                isChecking = false;
                MinecraftClient.getInstance().execute(this::refreshUI);
            }
        });
    }

    // 获取IPv4 token（JWS）
    private @Nullable String fetchIPv4Token() {
        try {
            URL tokenUrl = new URL("https://ip.fishport.pp.ua/");
            HttpURLConnection tokenConn = (HttpURLConnection) tokenUrl.openConnection();
            tokenConn.setRequestMethod("GET");
            tokenConn.setConnectTimeout(5000);
            tokenConn.setReadTimeout(5000);
            int tokenCode = tokenConn.getResponseCode();
            if (tokenCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(tokenConn.getInputStream()));
                StringBuilder resp = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    resp.append(line);
                }
                reader.close();
                return resp.toString();
            }
        } catch (Exception e) {
            System.out.println("IPv6 token/jwt处理失败: " + e.getMessage());
        }
        return null;
    }

    private void calculateCaptcha() {
        // 如果是IPv6地址，不计算验证码
        if (isIPv6) {
            resultMessage = "tryfishport.ui.unban.result.ipv6_not_supported";
            refreshUI();
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String captchaText = null;
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
                    captchaText = (String) jsonObject.get("captcha_token");
                    this.captchaToken = captchaText; // 保存token以便后续使用
                }

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
                    Gson gson = new Gson();
                    JSONObject payloadJson = gson.fromJson(decodedPayload, JSONObject.class);
                    // 获取captcha字段
//                    {
//                        "prefix": "WLfFNPaI",
//                            "target": "a070",
//                            "iat": 1755179051,
//                            "exp": 1755179111
//                    }


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
                    long startTime = System.currentTimeMillis();
                    boolean[] found = {false};
                    java.util.concurrent.atomic.AtomicReference<String> result = new java.util.concurrent.atomic.AtomicReference<>("");

                    final int batchSize = 1000;
                    for (long batchStart = 0; batchStart < total && !found[0]; batchStart += batchSize) {
                        java.util.List<Thread> tasks = new java.util.ArrayList<>();
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
                        long now = System.currentTimeMillis();
                        long elapsed = now - startTime;
                        if (elapsed > 0) {
                            long hashPerSec = count * 1000 / elapsed;
                            status = "已计算：" + count + " 次，耗时：" + elapsed + "ms，速度：" + hashPerSec + " hash/s";
                            MinecraftClient.getInstance().execute(this::refreshUI);
                        }
                    }
                    if (!found[0]) {
                        throw new NumberFormatException("Captcha calculation failed, no match found");
                    }
                    if (result.get() != null && !result.get().isEmpty()) {
                        this.captcha = result.get();
                    } else {
                        throw new NumberFormatException("Captcha result is empty");
                    }

                    resultMessage = "tryfishport.ui.unban.captcha.ready";
                    status = Text.translatable("tryfishport.ui.unban.status.ready").getString();
                } catch (NumberFormatException e) {
                    captcha = "";
                    resultMessage = "tryfishport.ui.unban.result.captcha_invalid";
                    status = Text.translatable("tryfishport.ui.unban.status.error").getString() + e.getMessage();
                }
            } catch (Exception e) {
                captcha = "";
                resultMessage = "tryfishport.ui.unban.result.captcha_calc_failed";
                status = Text.translatable("tryfishport.ui.unban.status.error").getString();
                e.printStackTrace();
            } finally {
                MinecraftClient.getInstance().execute(this::refreshUI);
            }
        });
    }

    private void submitUnbanRequest() {
        // 如果是IPv6地址，不允许提交解封请求
        if (isIPv6) {
            resultMessage = "tryfishport.ui.unban.result.ipv6_not_supported";
            refreshUI();
            return;
        }

        if (captcha.isEmpty() || !isBanned) {
            resultMessage = "tryfishport.ui.unban.result.captcha_failed";
            refreshUI();
            return;
        }

        boolean useToken = ipv4Token != null && !ipv4Token.isEmpty();

        resultMessage = "tryfishport.ui.unban.result.request_submitted";
        refreshUI();

        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL("https://ip-unban.fishport.net/unblock");
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
                        resultMessage = "tryfishport.ui.unban.status.unbanned";
                        isBanned = false;
                    } else {
                        // fail
                        resultMessage = "tryfishport.ui.unban.result.unban_failed" + jsonObject.get("error");

                    }
                } else {
                    resultMessage = "tryfishport.ui.unban.result.unban_failed_code" + responseCode;
                }
            } catch (Exception e) {
                resultMessage = "tryfishport.ui.unban.result.unban_failed";
                e.printStackTrace();
            }
            // 更新UI
            MinecraftClient.getInstance().execute(this::refreshUI);
        });
    }

    private void refreshUI() {
        // 触发UI重绘
        this.init();
        
        // 更新解封按钮状态
        this.children().forEach(element -> {
            if (element instanceof ButtonWidget button) {
                if (button.getMessage().getString().equals(Text.translatable("tryfishport.ui.unban.button.submit_unban").getString())) {
                    button.active = isBanned && !captcha.isEmpty() && !Text.translatable("tryfishport.ui.unban.result.captcha_invalid").getString().equals(Text.translatable(resultMessage).getString()) && !isIPv6;
                }
            }
        });
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private @NotNull String utilsSha256(@NotNull String input) {
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
