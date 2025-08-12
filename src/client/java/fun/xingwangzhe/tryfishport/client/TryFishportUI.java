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

public class TryFishportUI extends Screen {
    private Screen parent;
    private String ipAddress = "获取中...";
    private String status = "检测中...";
    private String captcha = "";
    private String resultMessage = "";
    private boolean isChecking = false;
    private boolean isBanned = false;

    public TryFishportUI(Screen parent) {
        super(Text.of("FishPort IP自助解封"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // 添加关闭按钮
        this.addDrawableChild(ButtonWidget.builder(Text.of("关闭"), button -> {
            close();
        }).dimensions(this.width / 2 - 50, this.height - 30, 100, 20).build());

        // 添加检查按钮
        this.addDrawableChild(ButtonWidget.builder(Text.of("检查状态"), button -> {
            checkBanStatus();
        }).dimensions(this.width / 2 - 105, this.height - 60, 100, 20).build());

        // 添加解封按钮（初始禁用）
        ButtonWidget unbanButton = ButtonWidget.builder(Text.of("提交解封"), button -> {
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
        context.drawTextWithShadow(this.textRenderer, "您的 IP: " + ipAddress, 10, 50, 0xFFFFFF);
        
        // 绘制状态信息
        context.drawTextWithShadow(this.textRenderer, "状态: " + status, 10, 70, 0xFFFFFF);
        
        // 绘制封禁状态
        if (isBanned) {
            context.drawTextWithShadow(this.textRenderer, "验证码: " + captcha, 10, 90, 0xFFFFFF);
        }
        
        // 绘制结果信息
        if (!resultMessage.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, resultMessage, 10, 110, 0xFFFFFF);
        }
    }

    private void getIPAddress() {
        // 避免重复执行IP获取
        if (!ipAddress.equals("获取中...")) {
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
                
                // 如果所有外部服务都失败了，获取本地IP（作为最后的备选方案）
                if (!ipAcquired) {
                    InetAddress localhost = InetAddress.getLocalHost();
                    ipAddress = localhost.getHostAddress();
                    status = "本地地址 (可能不是公网IP)";
                }
                
            } catch (Exception e) {
                ipAddress = "获取失败";
                status = "错误: " + e.getMessage();
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
                    status = "就绪";
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
        
        // 简单验证IPv4地址格式
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
        
        isChecking = true;
        status = "检查中...";
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
                        status = "已被封禁";
                        resultMessage = "检测到IP被封禁，正在计算验证码...";
                        calculateCaptcha();
                    } else {
                        isBanned = false;
                        status = "正常";
                        resultMessage = "您的IP未被封禁";
                    }
                } else {
                    status = "检查失败";
                    resultMessage = "检查请求失败: HTTP " + responseCode;
                }
            } catch (Exception e) {
                status = "检查失败";
                resultMessage = "检查失败: " + e.getMessage();
                e.printStackTrace();
            } finally {
                isChecking = false;
                MinecraftClient.getInstance().execute(this::refreshUI);
            }
        });
    }

    private void calculateCaptcha() {
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
                    captcha = captchaText;
                    resultMessage = "验证码计算完成";
                } else {
                    captcha = "计算失败";
                    resultMessage = "验证码获取失败: HTTP " + responseCode;
                }
            } catch (Exception e) {
                captcha = "计算失败: " + e.getMessage();
                resultMessage = "验证码计算失败: " + e.getMessage();
                e.printStackTrace();
            } finally {
                MinecraftClient.getInstance().execute(this::refreshUI);
            }
        });
    }

    private void submitUnbanRequest() {
        if (captcha.isEmpty() || !isBanned) {
            resultMessage = "无法提交解封请求：未被封禁或验证码未准备好";
            refreshUI();
            return;
        }

        resultMessage = "正在提交解封请求...";
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
                    
                    resultMessage = response.toString().trim();
                    // 解封成功后更新状态
                    if (resultMessage.contains("success") || resultMessage.contains("成功")) {
                        isBanned = false;
                        status = "已解封";
                    }
                } else {
                    resultMessage = "解封请求失败: HTTP " + responseCode;
                }
            } catch (Exception e) {
                resultMessage = "解封请求失败: " + e.getMessage();
                e.printStackTrace();
            }
            
            // 更新UI
            MinecraftClient.getInstance().execute(this::refreshUI);
        });
    }

    private void refreshUI() {
        // 触发UI重绘
        this.init();
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