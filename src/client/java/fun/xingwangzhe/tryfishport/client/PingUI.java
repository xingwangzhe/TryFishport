package fun.xingwangzhe.tryfishport.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.ServerAddress;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class PingUI extends Screen {
    private final ServerInfo serverInfo;
    private final MultiplayerScreen parent; // 添加父级屏幕引用
    private volatile String pingResult = Text.translatable("tryfishport.ui.ping.status.getting_route_info").getString();
    private final AtomicBoolean isPinging = new AtomicBoolean(false);
    private final StringBuilder traceOutputBuffer = new StringBuilder();

    public PingUI(MultiplayerScreen parent, ServerInfo serverInfo) {
        super(Text.translatable("tryfishport.ui.ping.title"));
        this.parent = parent;
        this.serverInfo = serverInfo;
        System.out.println(Text.translatable("tryfishport.log.pingui.init").getString() + serverInfo.name);
        System.out.println(Text.translatable("tryfishport.log.serverinfo.details").getString() + serverInfo.name + Text.translatable("tryfishport.log.serverinfo.address").getString() + serverInfo.address + Text.translatable("tryfishport.log.serverinfo.version").getString() + (serverInfo.version != null ? serverInfo.version.getString() : "null"));
    }

    @Override
    protected void init() {
        System.out.println(Text.translatable("tryfishport.log.pingui.init").getString() + serverInfo.name);
        System.out.println(Text.translatable("tryfishport.log.serverinfo.details").getString() + serverInfo.name + Text.translatable("tryfishport.log.serverinfo.address").getString() + serverInfo.address + Text.translatable("tryfishport.log.serverinfo.version").getString() + (serverInfo.version != null ? serverInfo.version.getString() : "null"));
        
        // 添加重试按钮
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("tryfishport.ui.ping.button.retry"), button -> {
            System.out.println(Text.translatable("tryfishport.log.button.retry").getString() + serverInfo.name);
            System.out.println(Text.translatable("tryfishport.log.serverinfo.details").getString() + serverInfo.name + Text.translatable("tryfishport.log.serverinfo.address").getString() + serverInfo.address + Text.translatable("tryfishport.log.serverinfo.version").getString() + (serverInfo.version != null ? serverInfo.version.getString() : "null"));
            traceRoute();
        })
        .dimensions(this.width / 2 - 105, this.height - 30, 100, 20)
        .build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("tryfishport.ui.ping.button.copy_result"), button -> {
            System.out.println(Text.translatable("tryfishport.log.button.copy").getString() + serverInfo.name);
            System.out.println(Text.translatable("tryfishport.log.copying.result").getString() + pingResult);
            MinecraftClient.getInstance().keyboard.setClipboard(pingResult);
        })
        .dimensions(this.width / 2 + 5, this.height - 30, 100, 20)
        .build());

        // 添加关闭按钮
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("tryfishport.ui.ping.button.close"), button -> close())
        .dimensions(this.width / 2 - 50, this.height - 60, 100, 20)
        .build());

        // 初始traceroute
        traceRoute();
    }

    @Override
    public boolean shouldPause() {
        return false; // 不暂停游戏
    }
    
    @Override
    public void close() {
        // 关闭时直接返回到服务器列表页面
        MinecraftClient.getInstance().setScreen(parent);
    }
    
    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
    }
    
    private void traceRoute() {
        if (isPinging.get()) {
            System.out.println(Text.translatable("tryfishport.log.tracing.ignored").getString());
            return;
        }
        
        isPinging.set(true);
        pingResult = Text.translatable("tryfishport.ui.ping.status.getting_route_info").getString();

        var serverHost = DNSResolver.resolveToIp(serverInfo.address);

        if (serverHost.contains(":")) {
            // 如果地址包含端口号，分离出主机名和端口
            String[] parts = serverHost.split(":");
            serverHost = parts[0];
        }

        System.out.println(Text.translatable("tryfishport.log.tracing.start").getString() + serverInfo.address);
        System.out.println(Text.translatable("tryfishport.log.serverinfo.details").getString() + serverInfo.name + Text.translatable("tryfishport.log.serverinfo.address").getString() + serverInfo.address + Text.translatable("tryfishport.log.serverinfo.version").getString() + (serverInfo.version != null ? serverInfo.version.getString() : "null"));
    
        traceOutputBuffer.setLength(0); // 清空缓冲区
        traceOutputBuffer.append(Text.translatable("tryfishport.ui.ping.status.getting_route_info").getString()).append("\n");
        updatePingResult();
        System.out.println(Text.translatable("tryfishport.log.parsing.address").getString() + serverInfo.address);
        
        ServerAddress address = ServerAddress.parse(serverInfo.address);
        String host = address.getAddress();
        
        System.out.println(Text.translatable("tryfishport.log.parsed.host").getString() + host);
        
        // 执行traceroute命令
        System.out.println(Text.translatable("tryfishport.log.traceroute.start").getString());
        String finalServerHost = serverHost;
        new Thread(() -> {
            try {
                executeTraceRouteCommand(String.valueOf(finalServerHost));
            } catch (Exception e) {
                System.out.println(Text.translatable("tryfishport.log.exception.traceroute").getString() + e.getMessage());
                e.printStackTrace();
                traceOutputBuffer.append(Text.translatable("tryfishport.ui.ping.error.route_trace").getString()).append(e.getMessage()).append("\n");
                updatePingResult();
            } finally {
                isPinging.set(false);
                System.out.println(Text.translatable("tryfishport.log.traceroute.finish").getString() + serverInfo.address);
                System.out.println(Text.translatable("tryfishport.log.serverinfo.details").getString() + serverInfo.name + Text.translatable("tryfishport.log.serverinfo.address").getString() + serverInfo.address + Text.translatable("tryfishport.log.serverinfo.version").getString() + (serverInfo.version != null ? serverInfo.version.getString() : "null"));
            }
        }).start();
    }

    /**
     * 根据操作系统执行相应的路由追踪命令
     * 
     * @param host 要traceroute的主机名或IP地址
     */
    private void executeTraceRouteCommand(String host) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String[] command;
            String friendlyName;
            
            System.out.println("Operating system: " + os);
            
            // 根据操作系统选择合适的路由追踪命令
            if (os.contains("win")) {
                // Windows系统: tracert host (系统自带)
                command = new String[]{"cmd.exe", "/c", "tracert", "-d", host};
                friendlyName = "tracert";
                System.out.println("Using Windows tracert command (built-in)");
            } else if (os.contains("mac") || os.contains("darwin")) {
                // Mac系统: traceroute host (系统自带)
                command = new String[]{"traceroute", host};
                friendlyName = "traceroute";
                System.out.println("Using Mac traceroute command (built-in)");
            } else {
                // Linux系统: 尝试多种方式
                command = new String[]{"traceroute", host};
                friendlyName = "traceroute";
                System.out.println("Using Linux traceroute command");
            }
            
            System.out.println("Executing route trace command: " + String.join(" ", command));
            
            // 尝试执行命令
            try {
                Process process = Runtime.getRuntime().exec(command);
                
                // 读取命令输出
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
                
                String line;
                while ((line = reader.readLine()) != null) {
                    traceOutputBuffer.append(line).append("\n");
                    System.out.println("Route trace output line: " + line);
                    updatePingResult(); // 实时更新结果
                }
                
                // 读取错误输出
                java.io.BufferedReader errorReader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getErrorStream()));
                StringBuilder errorOutput = new StringBuilder();
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                    System.out.println("Route trace error line: " + line);
                }
                
                // 等待命令执行完成
                int exitCode = process.waitFor();
                System.out.println("Route trace command exited with code: " + exitCode);
                
                // 如果有错误输出，也包含在结果中
                if (!errorOutput.isEmpty()) {
                    // 检查是否是因为命令不存在导致的错误
                    String errorStr = errorOutput.toString().toLowerCase();
                    if (errorStr.contains("not found") || errorStr.contains("not recognized") || 
                        errorStr.contains("No such file or directory".toLowerCase()) || 
                        errorStr.contains("command not found")) {
                        traceOutputBuffer.append(getCommandNotFoundMessage(friendlyName, os));
                    } else {
                        traceOutputBuffer.append("\n").append(Text.translatable("tryfishport.ui.ping.system.error_info").getString()).append(errorOutput);
                    }
                }
                
                // 如果输出为空但退出码为0，可能是命令执行了但没有输出
                if (traceOutputBuffer.isEmpty() && exitCode == 0) {
                    traceOutputBuffer.append(Text.translatable("tryfishport.ui.ping.system.command.no_output").getString());
                }
                
                updatePingResult(); // 最终更新结果
                
            } catch (IOException e) {
                System.out.println("IOException when executing command: " + e.getMessage());
                // 如果命令不存在或无法执行
                if (e.getMessage().contains("Cannot run program") || 
                    e.getMessage().contains("error=2") || 
                    e.getMessage().contains("No such file or directory")) {
                    traceOutputBuffer.append(getCommandNotFoundMessage(friendlyName, os));
                } else {
                    traceOutputBuffer.append(Text.translatable("tryfishport.ui.ping.system.command.error").getString()).append(e.getMessage());
                }
                updatePingResult();
                throw e; // 重新抛出其他IO异常
            }
        } catch (Exception e) {
            System.out.println(Text.translatable("tryfishport.log.exception.traceroute").getString() + e.getMessage());
            e.printStackTrace();
            traceOutputBuffer.append(Text.translatable("tryfishport.ui.ping.system.command.error").getString()).append(e.getMessage()).append("\n");
            updatePingResult();
        }
    }

    /**
     * 更新界面显示结果
     */
    private void updatePingResult() {
        // 在主线程中更新UI
        MinecraftClient.getInstance().execute(() -> {
            pingResult = traceOutputBuffer.toString();
            System.out.println("Updating UI with current result");
        });
    }
    
    /**
     * 获取命令未找到时的友好提示信息
     * 
     * @param command 命令名称
     * @param os 操作系统名称
     * @return 友好的错误提示信息
     */
    private String getCommandNotFoundMessage(String command, String os) {
        StringBuilder message = new StringBuilder();
        message.append(Text.translatable("tryfishport.ui.ping.error.command_not_found").getString()).append(command).append("\n\n");
        
        if (os.contains("win")) {
            message.append(Text.translatable("tryfishport.ui.ping.system.windows.info").getString()).append("\n");
            message.append(Text.translatable("tryfishport.ui.ping.system.windows.line1").getString()).append("\n");
            message.append(Text.translatable("tryfishport.ui.ping.system.windows.line2").getString()).append("\n");
            message.append(Text.translatable("tryfishport.ui.ping.system.windows.line3").getString()).append("\n");
        } else if (os.contains("mac") || os.contains("darwin")) {
            message.append(Text.translatable("tryfishport.ui.ping.system.mac.info").getString()).append("\n");
            message.append(Text.translatable("tryfishport.ui.ping.system.mac.line1").getString()).append("\n");
            message.append(Text.translatable("tryfishport.ui.ping.system.mac.line2").getString()).append("\n");
            message.append(Text.translatable("tryfishport.ui.ping.system.mac.line3").getString()).append("\n");
        } else {
            message.append(Text.translatable("tryfishport.ui.ping.system.linux.info").getString()).append("\n");
            message.append(Text.translatable("tryfishport.ui.ping.system.linux.line1").getString()).append("\n");
            message.append(Text.translatable("tryfishport.ui.ping.system.linux.line2").getString()).append("\n");
            message.append(Text.translatable("tryfishport.ui.ping.system.linux.line3").getString()).append("\n");
            message.append(Text.translatable("tryfishport.ui.ping.system.linux.line4").getString()).append("\n");
            message.append(Text.translatable("tryfishport.ui.ping.system.linux.line5").getString()).append("\n");
            message.append(Text.translatable("tryfishport.ui.ping.system.linux.line6").getString()).append("\n");
            message.append(Text.translatable("tryfishport.ui.ping.system.linux.line7").getString()).append("\n");
        }
        
        message.append(Text.translatable("tryfishport.ui.ping.system.tip").getString());
        if (os.contains("win")) {
            message.append("  tracert ").append(ServerAddress.parse(serverInfo.address).getAddress());
        } else {
            message.append("  traceroute ").append(ServerAddress.parse(serverInfo.address).getAddress());
        }
        
        return message.toString();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 首先调用父类的render方法来渲染背景和控件
        super.render(context, mouseX, mouseY, delta);
    
        // 绘制标题文本在屏幕顶部中央
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
        
        // 绘制服务器信息在左上角区域
        context.drawTextWithShadow(this.textRenderer, Text.translatable("tryfishport.ui.general.ip").getString() + serverInfo.name, 10, 35, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("tryfishport.ui.general.status").getString() + serverInfo.address, 10, 50, 0xFFFFFF);
        
        // 绘制结果信息 - 支持多行显示
        String[] lines = pingResult.split("\n");
        for (int i = 0; i < lines.length && i < 20; i++) { // 限制显示20行
            context.drawTextWithShadow(this.textRenderer, lines[i], 10, 70 + i * 10, 0xFFFFFF);
        }
        
        // 添加调试信息
//        System.out.println("Render method called");
    }
}