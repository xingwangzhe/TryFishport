# FishPort IP解禁逻辑与TryFishport代码实现映射文档

## 概述

本文档详细描述了ip-unban.fishport.net网站的IP解禁逻辑与TryFishport模组中对应功能的代码实现之间的映射关系。通过对比网站功能和代码实现，确保模组功能与网站保持一致。

## 网站功能分析

根据对ip-unban.fishport.net网站的观察，其主要功能包括：

1. **IP地址检测** - 自动检测用户当前的IP地址
2. **IPv6检测与限制** - 检测IPv6连接并限制解禁功能
3. **IP封禁状态检查** - 检查IP是否在黑名单中
4. **验证码计算** - 为被封禁的IP计算解禁验证码
5. **提交解禁请求** - 向服务器提交解禁请求

## 代码实现映射

### 1. IP地址检测功能

| 网站功能 | 代码实现 | 映射说明 |
|---------|---------|---------|
| 自动检测用户IP地址 | [getIPAddress()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L79-L95)方法 | 使用多个服务依次尝试获取公网IP地址 |
| 多服务支持 | [tryGetIPFromService()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/IPAddressService.java#L17-L76)方法配合多个URL | 支持ip-unban.fishport.net/check、httpbin.org/ip、api.ipify.org、icanhazip.com |
| 本地IP备选方案 | `InetAddress.getLocalHost()` | 当所有外部服务失败时获取本地IP |
| IP地址格式验证 | [isValidIPAddress()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/IPAddressService.java#L104-L119)方法 | 验证IPv4和IPv6地址格式的有效性 |
| JSON响应处理 | [tryGetIPFromService()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/IPAddressService.java#L17-L76)中的JSON解析逻辑 | 处理httpbin.org等返回JSON格式的服务 |

### 2. IPv6检测与限制功能

| 网站功能 | 代码实现 | 映射说明 |
|---------|---------|---------|
| IPv6连接检测 | [isIPv6](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L28-L28)字段和检测逻辑 | 通过IP地址格式特征检测IPv6 |
| IPv6访问限制 | [checkBanStatus()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L101-L137)、[calculateCaptcha()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L139-L165)、[submitUnbanRequest()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L167-L201)中的IPv6检查 | 当检测到IPv6时阻止关键操作 |
| 用户提示信息 | `tryfishport.ui.unban.result.ipv6_not_supported`和`tryfishport.ui.unban.ipv6.warning`翻译键 | 显示与网站一致的IPv6提示信息 |

### 3. IP封禁状态检查功能

| 网站功能 | 代码实现 | 映射说明 |
|---------|---------|---------|
| 检查IP封禁状态 | [checkBanStatus()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L101-L137)方法 | 向https://ip-unban.fishport.net/check?ip=发送GET请求 |
| 状态显示 | [status](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L23-L23)字段和UI渲染 | 在界面显示当前状态（正常/封禁/检查中等） |
| 封禁响应处理 | 检查响应中是否包含"banned"或"封禁"关键词 | 根据响应内容判断IP是否被封禁 |
| 自动验证码计算 | 检测到封禁时调用[calculateCaptcha()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L139-L165) | IP被封禁时自动开始验证码计算流程 |

### 4. 验证码计算功能

| 网站功能 | 代码实现 | 映射说明 |
|---------|---------|---------|
| 获取验证码参数 | [calculateCaptcha()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L139-L165)方法 | 向https://ip-unban.fishport.net/captcha?ip=发送GET请求 |
| 验证码显示 | [captcha](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L25-L25)字段和UI渲染 | 在界面显示计算出的验证码 |
| 验证码格式验证 | 使用`Integer.parseInt()`检查验证码是否为纯数字 | 确保验证码格式正确 |
| 错误处理 | 异常捕获和状态更新 | 处理验证码获取或计算失败的情况 |

### 5. 提交解禁请求功能

| 网站功能 | 代码实现 | 映射说明 |
|---------|---------|---------|
| 提交解禁请求 | [submitUnbanRequest()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L167-L201)方法 | 向https://ip-unban.fishport.net/unban发送POST请求 |
| 请求参数 | IP地址和验证码 | 将[ipAddress](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L22-L22)和[captcha](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L25-L25)作为表单数据发送 |
| 解禁结果处理 | 检查响应中是否包含"success"或"成功"关键词 | 根据响应内容更新界面状态 |
| 状态更新 | 更新[isBanned](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L27-L27)状态和[status](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L23-L23)字段 | 解禁成功后更新界面显示 |

## 界面交互映射

### 按钮功能映射

| 网站按钮 | 代码实现 | 映射说明 |
|---------|---------|---------|
| 检查状态 | "检查状态"按钮调用[checkBanStatus()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L101-L137) | 实现与网站"检查状态"按钮相同功能 |
| 提交解封 | "提交解封"按钮调用[submitUnbanRequest()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L167-L201) | 实现与网站"提交解封"按钮相同功能 |
| 关闭 | "关闭"按钮调用[close()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L225-L227)方法 | 关闭当前界面，返回上级界面 |

### 状态显示映射

| 网站显示 | 代码实现 | 映射说明 |
|---------|---------|---------|
| IP地址 | [ipAddress](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L22-L22)字段和UI渲染 | 显示当前检测到的IP地址 |
| 状态信息 | [status](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L23-L23)字段和UI渲染 | 显示当前操作状态 |
| 验证码 | [captcha](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L25-L25)字段和UI渲染 | 显示计算出的验证码 |
| 结果信息 | [resultMessage](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L26-L26)字段和UI渲染 | 显示操作结果信息 |

## 异步处理映射

| 网站行为 | 代码实现 | 映射说明 |
|---------|---------|---------|
| 非阻塞操作 | 所有网络请求使用`CompletableFuture.runAsync()` | 避免阻塞主线程，保持界面响应性 |
| UI更新 | 使用 [MinecraftClient.getInstance().execute()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L198-L200) | 确保UI更新在主线程执行 |

## 错误处理映射

| 网站行为 | 代码实现 | 映射说明 |
|---------|---------|---------|
| 网络错误处理 | try-catch块捕获异常 | 处理网络连接异常 |
| HTTP错误处理 | 检查HTTP响应码 | 处理非200响应 |
| 超时设置 | `setConnectTimeout(5000)`和`setReadTimeout(5000)` | 设置5秒连接和读取超时 |

## 国际化映射

| 网站文本 | 代码实现 | 映射说明 |
|---------|---------|---------|
| 中文界面 | zh_cn.json语言文件 | 提供中文界面文本 |
| 英文界面 | en_us.json语言文件 | 提供英文界面文本 |
| 文本显示 | `Text.translatable()`方法 | 实现界面文本的国际化支持 |

## Java类详细功能说明

### [CaptchaService.java](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/CaptchaService.java)

该类负责处理验证码的计算和验证功能。

**主要方法：**

1. [calculateCaptcha()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/CaptchaService.java#L35-L128) - 从服务器获取验证码挑战并计算结果
   - 向`https://ip-unban.fishport.net/captcha-challenge`发送GET请求获取验证码挑战
   - 解析JWT格式的验证码token
   - 提取prefix和target值
   - 通过暴力破解算法计算匹配的suffix
   - 返回包含验证码和token的[CaptchaResult](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/CaptchaService.java#L7-L18)对象

2. [utilsSha256()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/CaptchaService.java#L130-L147) - 计算字符串的SHA-256哈希值
   - 使用Java内置的MessageDigest类计算SHA-256哈希
   - 将字节数组转换为十六进制字符串

### [DNSResolver.java](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/DNSResolver.java)

该类负责DNS解析相关功能。

**主要方法：**

1. [resolveToIp()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/DNSResolver.java#L13-L41) - 将主机名解析为IP地址
   - 尝试通过SRV记录查询获取目标主机
   - 返回解析后的IP地址或原始主机名

### [IPAddressService.java](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/IPAddressService.java)

该类负责获取和验证IP地址。

**主要方法：**

1. [fetchIPAddress()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/IPAddressService.java#L14-L16) - 获取IP地址的主要入口方法
   - 调用[tryGetIPFromService()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/IPAddressService.java#L17-L76)方法获取IP地址

2. [tryGetIPFromService()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/IPAddressService.java#L17-L76) - 从服务获取IP地址
   - 向`https://ip-unban.fishport.net/check`发送GET请求获取IP地址
   - 处理JSON格式的响应
   - 验证IPv6-only情况并获取IPv4 token
   - 验证IP地址格式

3. [fetchIPv4Token()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/IPAddressService.java#L78-L96) - 获取IPv4 token
   - 向`https://ip.fishport.pp.ua/`发送GET请求获取IPv4 token

4. [isValidIPAddress()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/IPAddressService.java#L104-L119) - 验证IP地址格式
   - 使用正则表达式验证IPv4和IPv6地址格式

**字段：**
- [ipAddress](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/IPAddressService.java#L11-L11) - 存储获取到的IP地址
- [isIPv6](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/IPAddressService.java#L12-L12) - 标识是否为IPv6地址
- [ipv4Token](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/IPAddressService.java#L13-L13) - 存储IPv4 token

### [PingUI.java](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/PingUI.java)

该类负责实现Ping界面，提供网络路由追踪功能。

**主要方法：**

1. [init()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/PingUI.java#L35-L68) - 初始化界面组件
   - 添加重试、复制结果和关闭按钮
   - 初始化时自动开始路由追踪

2. [traceRoute()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/PingUI.java#L92-L134) - 执行路由追踪
   - 解析服务器地址
   - 在新线程中执行路由追踪命令

3. [executeTraceRouteCommand()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/PingUI.java#L141-L233) - 执行路由追踪命令
   - 根据操作系统选择合适的路由追踪命令(tracert/traceroute)
   - 执行命令并捕获输出
   - 处理命令不存在的情况

4. [updatePingResult()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/PingUI.java#L240-L246) - 更新Ping结果显示
   - 在主线程中更新UI显示

5. [getCommandNotFoundMessage()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/PingUI.java#L255-L288) - 获取命令未找到时的提示信息
   - 根据操作系统提供相应的安装指导

6. [render()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/PingUI.java#L290-L308) - 渲染界面
   - 绘制标题、服务器信息和路由追踪结果

### [RouteTracer.java](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/RouteTracer.java)

该类负责执行路由追踪功能。

**主要方法：**

1. [executeTraceRouteCommand()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/RouteTracer.java#L19-L111) - 执行路由追踪命令
   - 根据操作系统选择合适的路由追踪命令
   - 执行命令并处理输出和错误信息

2. [getCommandNotFoundMessage()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/RouteTracer.java#L121-L153) - 获取命令未找到时的提示信息

3. [getTraceOutput()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/RouteTracer.java#L155-L157) - 获取路由追踪结果

### [TryFishportUI.java](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)

该类是TryFishport模组的主界面，整合了IP解禁的所有功能。

**主要方法：**

1. [init()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L36-L56) - 初始化界面
   - 添加关闭、检查状态和提交解封按钮
   - 初始化时自动获取IP地址

2. [render()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L58-L83) - 渲染界面
   - 绘制标题、IP地址、状态、验证码和结果信息

3. [getIPAddress()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L85-L100) - 获取IP地址
   - 使用[IPAddressService](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/IPAddressService.java#L11-L122)获取IP地址
   - 自动触发封禁状态检查

4. [checkBanStatus()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L101-L137) - 检查封禁状态
   - 使用[UnbanService](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/UnbanService.java#L11-L157)检查IP是否被封禁
   - 如果被封禁则自动计算验证码

5. [calculateCaptcha()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L139-L165) - 计算验证码
   - 使用[CaptchaService](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/CaptchaService.java#L10-L147)计算验证码

6. [submitUnbanRequest()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L167-L201) - 提交解封请求
   - 使用[UnbanService](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/UnbanService.java#L11-L157)提交解封请求

7. [refreshUI()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java#L203-L223) - 刷新界面
   - 更新界面组件状态
   - 控制解封按钮的激活状态

### [TryfishportClient.java](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryfishportClient.java)

该类是模组的客户端初始化入口。

**主要方法：**

1. [onInitializeClient()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/TryfishportClient.java#L7-L9) - 客户端初始化
   - 模组加载时调用，输出初始化日志

### [UnbanService.java](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/UnbanService.java)

该类负责处理与解封相关的网络请求。

**主要方法：**

1. [checkBanStatus()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/UnbanService.java#L41-L97) - 检查IP封禁状态
   - 如果有IPv4 token则通过POST方式提交检查请求
   - 否则通过GET方式直接检查当前IP状态
   - 返回包含IP地址和封禁状态的[CheckResult](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/UnbanService.java#L12-L24)对象

2. [submitUnbanRequest()](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/UnbanService.java#L113-L156) - 提交解封请求
   - 向`https://ip-unban.fishport.net/unblock`发送POST请求提交解封请求
   - 返回包含解封结果的[UnbanResult](file:///home/xingwangzhe/%E6%A1%8C%E9%9D%A2/TryFishport/src/client/java/fun/xingwangzhe/tryfishport/client/UnbanService.java#L99-L111)对象

## 总结

TryFishport模组的IP解禁功能与ip-unban.fishport.net网站的功能基本保持一致，包括：

1. **完整的功能流程** - IP检测、状态检查、验证码计算、解禁请求提交
2. **多服务支持** - 通过多个IP获取服务提高成功率
3. **IPv6处理** - 正确检测并限制IPv6连接的解禁功能
4. **错误处理** - 完善的异常处理和错误提示
5. **用户界面** - 直观的状态显示和操作反馈
6. **国际化支持** - 中英文界面支持

通过这种映射关系，确保了模组用户能够获得与网站一致的使用体验。