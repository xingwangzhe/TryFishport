package fun.xingwangzhe.tryfishport.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.ServerAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class PingUI extends Screen {
    private final ServerInfo serverInfo;
    private String pingResult = "正在获取路由信息...";
    private boolean isPinging = false;

    public PingUI(ServerInfo serverInfo) {
        super(Text.of("服务器路由追踪"));
        this.serverInfo = serverInfo;
        System.out.println("PingUI initialized with serverInfo: " + serverInfo.name);
        System.out.println("ServerInfo details - Name: " + serverInfo.name + ", Address: " + serverInfo.address + ", Version: " + (serverInfo.version != null ? serverInfo.version.getString() : "null"));
    }

    @Override
    protected void init() {
        System.out.println("Initializing PingUI with serverInfo: " + serverInfo.name);
        System.out.println("ServerInfo details - Name: " + serverInfo.name + ", Address: " + serverInfo.address + ", Version: " + (serverInfo.version != null ? serverInfo.version.getString() : "null"));
        
        // 添加重试按钮
        this.addDrawableChild(ButtonWidget.builder(Text.of("重试"), button -> {
            System.out.println("Retry button clicked for server: " + serverInfo.name);
            System.out.println("ServerInfo details on retry - Name: " + serverInfo.name + ", Address: " + serverInfo.address + ", Version: " + (serverInfo.version != null ? serverInfo.version.getString() : "null"));
            traceRoute();
        })
        .dimensions(this.width / 2 - 105, this.height - 30, 100, 20)
        .build());

        this.addDrawableChild(ButtonWidget.builder(Text.of("复制结果"), button -> {
            System.out.println("Copy result button clicked for server: " + serverInfo.name);
            System.out.println("Copying result: " + pingResult);
            MinecraftClient.getInstance().keyboard.setClipboard(pingResult);
        })
        .dimensions(this.width / 2 + 5, this.height - 30, 100, 20)
        .build());

        // 初始traceroute
        traceRoute();
    }

    private void traceRoute() {
        if (isPinging) {
            System.out.println("Already tracing route, ignoring request.");
            return;
        }
        
        isPinging = true;
        pingResult = "正在获取路由信息...";
        System.out.println("Starting to trace route: " + serverInfo.address);
        System.out.println("ServerInfo details - Name: " + serverInfo.name + ", Address: " + serverInfo.address + ", Version: " + (serverInfo.version != null ? serverInfo.version.getString() : "null"));
    
        new Thread(() -> {
            try {
                // 使用系统traceroute命令
                pingResult = "正在执行路由追踪命令...";
                System.out.println(pingResult);
                System.out.println("Parsing server address: " + serverInfo.address);
                
                ServerAddress address = ServerAddress.parse(serverInfo.address);
                String host = address.getAddress();
                
                System.out.println("Parsed address - Host: " + host);
                
                // 执行traceroute命令
                System.out.println("Starting traceroute command execution");
                String traceOutput = executeTraceRouteCommand(host);
                System.out.println("Traceroute command completed");
                
                if (traceOutput == null || traceOutput.isEmpty()) {
                    System.out.println("Traceroute command returned empty result");
                    pingResult = "路由追踪命令执行失败或无响应";
                } else {
                    System.out.println("Traceroute command result: " + traceOutput);
                    pingResult = "路由追踪结果:\n" + traceOutput;
                }
                
                // 更新UI
                MinecraftClient.getInstance().execute(() -> {
                    System.out.println("Updating UI with result: " + pingResult);
                });
            } catch (Exception e) {
                System.out.println("Exception in traceRoute: " + e.getMessage());
                e.printStackTrace();
                pingResult = "路由追踪错误: " + e.getMessage();
                e.printStackTrace();
                
                // 更新UI显示错误
                MinecraftClient.getInstance().execute(() -> {
                    System.out.println("Updating UI with error: " + pingResult);
                });
            } finally {
                isPinging = false;
                System.out.println("Finished tracing route: " + serverInfo.address);
                System.out.println("ServerInfo details - Name: " + serverInfo.name + ", Address: " + serverInfo.address + ", Version: " + (serverInfo.version != null ? serverInfo.version.getString() : "null"));
                
                // 确保UI更新
                MinecraftClient.getInstance().execute(() -> {
                    System.out.println("Ensuring UI update after completion");
                });
            }
        }).start();
    }

    /**
     * 根据操作系统执行相应的路由追踪命令
     * 
     * @param host 要traceroute的主机名或IP地址
     * @return 路由追踪命令的输出结果
     */
    private String executeTraceRouteCommand(String host) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String[] command;
            String friendlyName;
            
            System.out.println("Operating system: " + os);
            
            // 根据操作系统选择合适的路由追踪命令
            if (os.contains("win")) {
                // Windows系统: tracert host (系统自带)
                command = new String[]{"cmd.exe", "/c", "tracert", host};
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
                StringBuilder output = new StringBuilder();
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
                
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    System.out.println("Route trace output line: " + line);
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
                if (errorOutput.length() > 0) {
                    // 检查是否是因为命令不存在导致的错误
                    String errorStr = errorOutput.toString().toLowerCase();
                    if (errorStr.contains("not found") || errorStr.contains("not recognized") || 
                        errorStr.contains("No such file or directory".toLowerCase()) || 
                        errorStr.contains("command not found")) {
                        return getCommandNotFoundMessage(friendlyName, os);
                    }
                    return output.toString() + "\n错误信息:\n" + errorOutput.toString();
                }
                
                // 如果输出为空但退出码为0，可能是命令执行了但没有输出
                if (output.length() == 0 && exitCode == 0) {
                    return "路由追踪命令执行完成，但未返回任何输出";
                }
                
                return output.toString();
            } catch (IOException e) {
                System.out.println("IOException when executing command: " + e.getMessage());
                // 如果命令不存在或无法执行
                if (e.getMessage().contains("Cannot run program") || 
                    e.getMessage().contains("error=2") || 
                    e.getMessage().contains("No such file or directory")) {
                    return getCommandNotFoundMessage(friendlyName, os);
                }
                throw e; // 重新抛出其他IO异常
            }
        } catch (Exception e) {
            System.out.println("Exception in executeTraceRouteCommand: " + e.getMessage());
            e.printStackTrace();
            return "执行路由追踪命令时出错: " + e.getMessage();
        }
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
        message.append("未找到路由追踪命令: ").append(command).append("\n\n");
        
        if (os.contains("win")) {
            message.append("Windows系统说明:\n");
            message.append("- tracert命令应该是Windows系统自带的\n");
            message.append("- 请确认您在命令提示符中可以执行tracert命令\n");
            message.append("- 如果仍然无法使用，请检查系统环境变量设置\n");
        } else if (os.contains("mac") || os.contains("darwin")) {
            message.append("Mac系统说明:\n");
            message.append("- traceroute命令应该是macOS系统自带的\n");
            message.append("- 请确认您在终端中可以执行traceroute命令\n");
            message.append("- 如果仍然无法使用，请检查系统完整性或重新安装系统\n");
        } else {
            message.append("Linux系统说明:\n");
            message.append("- 某些Linux发行版可能未预装traceroute工具\n");
            message.append("- Ubuntu/Debian系统可执行以下命令安装:\n");
            message.append("  sudo apt-get update && sudo apt-get install traceroute\n");
            message.append("- CentOS/RHEL/Fedora系统可执行以下命令安装:\n");
            message.append("  sudo yum install traceroute\n");
            message.append("  或\n");
            message.append("  sudo dnf install traceroute\n");
        }
        
        message.append("\n提示: 您也可以手动在系统终端中执行以下命令来查看路由信息:\n");
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
        context.drawTextWithShadow(this.textRenderer, "服务器名称: " + serverInfo.name, 10, 35, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "服务器地址: " + serverInfo.address, 10, 50, 0xFFFFFF);
        
        // 绘制结果信息 - 支持多行显示
        String[] lines = pingResult.split("\n");
        for (int i = 0; i < lines.length && i < 20; i++) { // 限制显示20行
            context.drawTextWithShadow(this.textRenderer, lines[i], 10, 70 + i * 10, 0xFFFFFF);
        }
        
        // 添加调试信息
        System.out.println("Render method called");
    }
    
    @Override
    public boolean shouldPause() {
        return false; // 不暂停游戏
    }
    
    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
    }
}