package fun.xingwangzhe.tryfishport.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TryFishportUI extends Screen {
    // 创建一个共享的线程池用于所有网络操作
    private static final ExecutorService networkExecutor = Executors.newFixedThreadPool(4);
    
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
    
    // 服务类
    private final IPAddressService ipService = new IPAddressService();
    private final UnbanService unbanService = new UnbanService();
    private final CaptchaService captchaService = new CaptchaService();

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
                ipService.fetchIPAddress();
                ipAddress = ipService.getIpAddress();
                isIPv6 = ipService.isIPv6();
                ipv4Token = ipService.getIpv4Token();
                
                // 更新状态文本为可翻译格式
                status = Text.translatable(status).getString();
                // 检查是否为IPv6地址
                isIPv6 = ipAddress.contains(":") && !ipAddress.contains(".");

                // CHECK STATUS
                checkBanStatus();
            } catch (Exception e) {
                ipAddress = Text.translatable("tryfishport.ui.unban.ip.failed").getString();
                status = Text.translatable("tryfishport.ui.unban.status.error").getString();
                resultMessage = Text.translatable("tryfishport.ui.unban.result.get_ip_failed").getString();
                e.printStackTrace();
            }
            // 更新UI
            MinecraftClient.getInstance().execute(this::refreshUI);
        }, networkExecutor);
    }

    private void checkBanStatus() {
        if (isChecking) {
            return;
        }

        // 如果是IPv6地址，不允许检查状态
        if (isIPv6) {
            resultMessage = Text.translatable("tryfishport.ui.unban.result.ipv6_not_supported").getString();
            refreshUI();
            return;
        }

        isChecking = true;
        status = Text.translatable("tryfishport.ui.unban.status.checking").getString();
        resultMessage = "";
        refreshUI();

        CompletableFuture.runAsync(() -> {
            try {
                UnbanService.CheckResult result = unbanService.checkBanStatus(ipv4Token);
                
                ipAddress = result.getIp();
                isBanned = result.isBlacklisted();
                
                if (isBanned) {
                    status = Text.translatable("tryfishport.ui.unban.status.banned").getString();
                    resultMessage = Text.translatable("tryfishport.ui.unban.captcha.calculating").getString();
                    calculateCaptcha();
                } else {
                    status = Text.translatable("tryfishport.ui.unban.status.normal").getString();
                    resultMessage = Text.translatable("tryfishport.ui.unban.result.not_banned").getString();
                }
            } catch (Exception e) {
                status = Text.translatable("tryfishport.ui.unban.status.check_failed").getString();
                resultMessage = Text.translatable("tryfishport.ui.unban.result.check_failed").getString();
                e.printStackTrace();
            } finally {
                isChecking = false;
                MinecraftClient.getInstance().execute(this::refreshUI);
            }
        }, networkExecutor);
    }

    private void calculateCaptcha() {
        // 如果是IPv6地址，不计算验证码
        if (isIPv6) {
            resultMessage = Text.translatable("tryfishport.ui.unban.result.ipv6_not_supported").getString();
            refreshUI();
            return;
        }

        // 使用新的异步方法
        captchaService.calculateCaptchaAsync()
            .thenAccept(result -> {
                this.captcha = result.getCaptcha();
                this.captchaToken = result.getCaptchaToken();
                
                resultMessage = Text.translatable("tryfishport.ui.unban.captcha.ready").getString();
                status = Text.translatable("tryfishport.ui.unban.status.ready").getString();
                
                MinecraftClient.getInstance().execute(this::refreshUI);
            })
            .exceptionally(throwable -> {
                captcha = "";
                resultMessage = Text.translatable("tryfishport.ui.unban.result.captcha_calc_failed").getString();
                status = Text.translatable("tryfishport.ui.unban.status.error").getString();
                throwable.printStackTrace();
                
                MinecraftClient.getInstance().execute(this::refreshUI);
                return null;
            });
    }

    private void submitUnbanRequest() {
        // 如果是IPv6地址，不允许提交解封请求
        if (isIPv6) {
            resultMessage = Text.translatable("tryfishport.ui.unban.result.ipv6_not_supported").getString();
            refreshUI();
            return;
        }

        if (captcha.isEmpty() || !isBanned) {
            resultMessage = Text.translatable("tryfishport.ui.unban.result.captcha_failed").getString();
            refreshUI();
            return;
        }

        boolean useToken = ipv4Token != null && !ipv4Token.isEmpty();

        resultMessage = Text.translatable("tryfishport.ui.unban.result.request_submitted").getString();
        refreshUI();

        CompletableFuture.runAsync(() -> {
            try {
                UnbanService.UnbanResult result = unbanService.submitUnbanRequest(captcha, captchaToken, ipv4Token);
                
                if (result.isSuccess()) {
                    // success
                    resultMessage = Text.translatable("tryfishport.ui.unban.status.unbanned").getString();
                    isBanned = false;
                } else {
                    // fail
                    resultMessage = Text.translatable("tryfishport.ui.unban.result.unban_failed").getString() + result.getMessage();
                }
            } catch (Exception e) {
                resultMessage = Text.translatable("tryfishport.ui.unban.result.unban_failed").getString();
                e.printStackTrace();
            }
            // 更新UI
            MinecraftClient.getInstance().execute(this::refreshUI);
        }, networkExecutor);
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
    
    // 添加资源清理方法
    public static void shutdown() {
        networkExecutor.shutdown();
    }
}