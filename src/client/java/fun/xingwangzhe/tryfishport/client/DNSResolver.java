package fun.xingwangzhe.tryfishport.client;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

public class DNSResolver {

    /**
     * 解析主机名到IP地址
     *
     * @param host 主机名
     * @return 解析后的IP地址或原始主机名
     */
    public static String resolveToIp(String host) {
        try {
            String targetHost = host;

            // 尝试 SRV 查询
            try {
                Hashtable<String, String> env = new Hashtable<>();
                env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
                env.put("java.naming.provider.url", "dns:");

                Attributes attrs = new InitialDirContext(env)
                        .getAttributes("_minecraft._tcp." + host, new String[]{"SRV"});
                Attribute attr = attrs.get("SRV");

                if (attr != null && attr.size() > 0) {
                    String[] parts = attr.get(0).toString().split(" ", 4);
                    targetHost = parts[3].endsWith(".") ? parts[3].substring(0, parts[3].length() - 1) : parts[3];
                }
            } catch (Exception ignored) {
            }

            // 查 A/AAAA 记录
            return targetHost;

        } catch (Exception e) {
            return host; // 出错返回原 host
        }
    }
}