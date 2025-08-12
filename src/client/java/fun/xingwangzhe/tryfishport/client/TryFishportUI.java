package fun.xingwangzhe.tryfishport.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.Inet6Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class TryFishportUI extends Screen {
    private Screen parent;
    private String ipAddress = Text.translatable("tryfishport.ui.unban.ip.getting").getString();
    private String status = Text.translatable("tryfishport.ui.unban.status.checking").getString();
    private String captcha = "";
    // 保存翻译键而不是实际文本
    private String resultMessage = "";
    private boolean isChecking = false;
    private boolean isBanned = false;
    private boolean isIPv6 = false;

    public TryFishportUI(Screen parent) {
        super(Text.translatable("tryfishport.ui.unban.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // 添加关闭按钮
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("tryfishport.ui.unban.button.close"), button -> {
            close();
        }).dimensions(this.width / 2 - 50, this.height - 30, 100, 20).build());

        // 添加检查按钮
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("tryfishport.ui.unban.button.check_status"), button -> {
            checkBanStatus();
        }).dimensions(this.width / 2 - 105, this.height - 60, 100, 20).build());

        // 添加解封按钮（初始禁用）
        ButtonWidget unbanButton = ButtonWidget.builder(Text.translatable("tryfishport.ui.unban.button.submit_unban"), button -> {
            submitUnbanRequest();
        }).dimensions(this.width / 2 + 5, this.height - 60, 100, 20).build();
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
                // 尝试多种方法获取公网IP
                boolean ipAcquired = false;
                
                // 方法1: 从ip-unban.fishport.net获取
                if (!ipAcquired && tryGetIPFromService("https://ip-unban.fishport.net/ip")) {
                    ipAcquired = true;
                }
                
                // 方法2: 从httpbin.org获取 (仅当前一个方法失败时)
                if (!ipAcquired && tryGetIPFromService("https://httpbin.org/ip")) {
                    ipAcquired = true;
                }
                
                // 方法3: 从api.ipify.org获取 (仅当前一个方法失败时)
                if (!ipAcquired && tryGetIPFromService("https://api.ipify.org")) {
                    ipAcquired = true;
                }
                
                // 方法4: 从icanhazip.com获取 (仅当前一个方法失败时)
                if (!ipAcquired && tryGetIPFromService("https://icanhazip.com")) {
                    ipAcquired = true;
                }
                
                // 更新状态文本为可翻译格式
                status = Text.translatable(status).getString();
                
                // 如果所有外部服务都失败了，获取本地IP（作为最后的备选方案）
                if (!ipAcquired) {
                    InetAddress localhost = InetAddress.getLocalHost();
                    ipAddress = localhost.getHostAddress();
                    status = Text.translatable("tryfishport.ui.unban.ip.local").getString();
                }
                
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

    private boolean tryGetIPFromService(String urlStr) {
        try {
            // 使用URI方式创建URL对象，避免使用已过时的URL构造函数
            URL url = new URI(urlStr).toURL();
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
                    // 简单提取origin字段
                    int originIndex = ip.indexOf("\"origin\"");
                    if (originIndex != -1) {
                        int startIndex = ip.indexOf(":", originIndex) + 2;
                        int endIndex = ip.indexOf("\"", startIndex);
                        if (startIndex > 1 && endIndex > startIndex) {
                            ip = ip.substring(startIndex, endIndex);
                        }
                    }
                }
                
                // 验证IP地址格式
                if (isValidIPAddress(ip)) {
                    ipAddress = ip;
                    status = Text.translatable("tryfishport.ui.unban.status.ready").getString();
                    return true;
                }
            }
        } catch (Exception e) {
            // 忽略单个服务的错误，继续尝试其他服务
            System.out.println("Failed to get IP from " + urlStr + ": " + e.getMessage());
        }
        return false;
    }

    private boolean isValidIPAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        // 检查是否为IPv6地址
        if (ip.contains(":")) {
            // 简单的IPv6地址验证
            try {
                Inet6Address.getByName(ip);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        
        // 验证IPv4地址格式
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        
        for (String part : parts) {
            try {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        return true;
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
                // 检查IP是否被封 使用URI方式创建URL对象，避免使用已过时的URL构造函数
                URL url = new URI("https://ip-unban.fishport.net/check?ip=" + ipAddress).toURL();
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
                    
                    String responseText = response.toString().trim();
                    if (responseText.contains("banned") || responseText.contains("封禁")) {
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

    private void calculateCaptcha() {
        // 如果是IPv6地址，不计算验证码
        if (isIPv6) {
            resultMessage = "tryfishport.ui.unban.result.ipv6_not_supported";
            refreshUI();
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                // 获取验证码计算参数 使用URI方式创建URL对象，避免使用已过时的URL构造函数
                URL url = new URI("https://ip-unban.fishport.net/captcha?ip=" + ipAddress).toURL();
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
                    
                    // 解析验证码（实际实现可能需要根据具体API调整）
                    String captchaText = response.toString().trim();
                    // 假设服务器返回的是纯数字验证码
                    try {
                        Integer.parseInt(captchaText);  // 验证是否为纯数字
                        captcha = captchaText;
                        resultMessage = "tryfishport.ui.unban.captcha.ready";
                        status = Text.translatable("tryfishport.ui.unban.status.ready").getString();
                    } catch (NumberFormatException e) {
                        captcha = "";
                        resultMessage = "tryfishport.ui.unban.result.captcha_invalid";
                        status = Text.translatable("tryfishport.ui.unban.status.error").getString();
                    }
                } else {
                    captcha = "";
                    resultMessage = "tryfishport.ui.unban.result.captcha_fetch_failed";
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

        resultMessage = "tryfishport.ui.unban.result.request_submitted";
        refreshUI();
        
        CompletableFuture.runAsync(() -> {
            try {
                // 提交解封请求 使用URI方式创建URL对象，避免使用已过时的URL构造函数
                URL url = new URI("https://ip-unban.fishport.net/unban").toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                String postData = "ip=" + ipAddress + "&captcha=" + captcha;
                byte[] postDataBytes = postData.getBytes(StandardCharsets.UTF_8);
                
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(postDataBytes);
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
                    
                    String responseText = response.toString().trim();
                    resultMessage = "tryfishport.ui.unban.result.request_success";
                    // 解封成功后更新状态
                    if (responseText.contains("success") || responseText.contains("成功")) {
                        isBanned = false;
                        status = Text.translatable("tryfishport.ui.unban.status.unbanned").getString();
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
            if (element instanceof ButtonWidget) {
                ButtonWidget button = (ButtonWidget) element;
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
}