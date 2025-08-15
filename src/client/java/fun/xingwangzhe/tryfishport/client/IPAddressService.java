package fun.xingwangzhe.tryfishport.client;

import com.google.gson.Gson;
import net.minidev.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class IPAddressService {
    private String ipAddress = "";
    private boolean isIPv6 = false;
    private String ipv4Token = "";

    public void fetchIPAddress() throws Exception {
        tryGetIPFromService();
    }

    private void tryGetIPFromService() throws Exception {
        try {
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
                            throw new Exception("Failed to get IPv4 token");
                        }

                        Gson jwtGson = new Gson();
                        JSONObject jwtObject = jwtGson.fromJson(jws, JSONObject.class);
                        // 获取IP地址
                        if (jwtObject == null || !jwtObject.containsKey("ip")) {
                            throw new Exception("Invalid JWT token");
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
                    isIPv6 = ip.contains(":") && !ip.contains(".");
                }
            }
        } catch (Exception e) {
            // 忽略单个服务的错误，继续尝试其他服务
            System.out.println("Failed to get IP from https://ip-unban.fishport.net/check: " + e.getMessage());
            throw e;
        }
    }

    private String fetchIPv4Token() {
        try {
            URL tokenUrl = URI.create("https://ip.fishport.pp.ua/").toURL();
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

    // Getters
    public String getIpAddress() {
        return ipAddress;
    }

    public boolean isIPv6() {
        return isIPv6;
    }

    public String getIpv4Token() {
        return ipv4Token;
    }
}