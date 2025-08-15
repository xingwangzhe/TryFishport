package fun.xingwangzhe.tryfishport.client;

import net.minecraft.client.network.ServerAddress;
import net.minecraft.text.Text;

import java.io.IOException;

public class RouteTracer {
    private final StringBuilder traceOutputBuffer = new StringBuilder();
    private String serverInfoAddress;
    private String serverInfoName;

    public RouteTracer(String serverInfoAddress, String serverInfoName) {
        this.serverInfoAddress = serverInfoAddress;
        this.serverInfoName = serverInfoName;
    }

    /**
     * 根据操作系统执行相应的路由追踪命令
     *
     * @param host 要traceroute的主机名或IP地址
     */
    public void executeTraceRouteCommand(String host) {
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
                throw e; // 重新抛出其他IO异常
            }
        } catch (Exception e) {
            System.out.println(Text.translatable("tryfishport.log.exception.traceroute").getString() + e.getMessage());
            e.printStackTrace();
            traceOutputBuffer.append(Text.translatable("tryfishport.ui.ping.system.command.error").getString()).append(e.getMessage()).append("\n");
        }
    }

    /**
     * 获取命令未找到时的友好提示信息
     *
     * @param command 命令名称
     * @param os      操作系统名称
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
            message.append("  tracert ").append(ServerAddress.parse(serverInfoAddress).getAddress());
        } else {
            message.append("  traceroute ").append(ServerAddress.parse(serverInfoAddress).getAddress());
        }

        return message.toString();
    }

    public String getTraceOutput() {
        return traceOutputBuffer.toString();
    }
}