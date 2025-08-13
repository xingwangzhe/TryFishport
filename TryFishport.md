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
| 自动检测用户IP地址 | [getIPAddress()](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)方法 | 使用多个服务依次尝试获取公网IP地址 |
| 多服务支持 | [tryGetIPFromService()](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)方法配合多个URL | 支持ip-unban.fishport.net/ip、httpbin.org/ip、api.ipify.org、icanhazip.com |
| 本地IP备选方案 | `InetAddress.getLocalHost()` | 当所有外部服务失败时获取本地IP |
| IP地址格式验证 | [isValidIPAddress()](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)方法 | 验证IPv4和IPv6地址格式的有效性 |
| JSON响应处理 | [tryGetIPFromService()](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)中的JSON解析逻辑 | 处理httpbin.org等返回JSON格式的服务 |

### 2. IPv6检测与限制功能

| 网站功能 | 代码实现 | 映射说明 |
|---------|---------|---------|
| IPv6连接检测 | [isIPv6](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)字段和检测逻辑 | 通过IP地址格式特征检测IPv6 |
| IPv6访问限制 | [checkBanStatus()](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)、[calculateCaptcha()](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)、[submitUnbanRequest()](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)中的IPv6检查 | 当检测到IPv6时阻止关键操作 |
| 用户提示信息 | `tryfishport.ui.unban.result.ipv6_not_supported`和`tryfishport.ui.unban.ipv6.warning`翻译键 | 显示与网站一致的IPv6提示信息 |

### 3. IP封禁状态检查功能

| 网站功能 | 代码实现 | 映射说明 |
|---------|---------|---------|
| 检查IP封禁状态 | [checkBanStatus()](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)方法 | 向https://ip-unban.fishport.net/check?ip=发送GET请求 |
| 状态显示 | [status](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)字段和UI渲染 | 在界面显示当前状态（正常/封禁/检查中等） |
| 封禁响应处理 | 检查响应中是否包含"banned"或"封禁"关键词 | 根据响应内容判断IP是否被封禁 |
| 自动验证码计算 | 检测到封禁时调用[calculateCaptcha()](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java) | IP被封禁时自动开始验证码计算流程 |

### 4. 验证码计算功能

| 网站功能 | 代码实现 | 映射说明 |
|---------|---------|---------|
| 获取验证码参数 | [calculateCaptcha()](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)方法 | 向https://ip-unban.fishport.net/captcha?ip=发送GET请求 |
| 验证码显示 | [captcha](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)字段和UI渲染 | 在界面显示计算出的验证码 |
| 验证码格式验证 | 使用`Integer.parseInt()`检查验证码是否为纯数字 | 确保验证码格式正确 |
| 错误处理 | 异常捕获和状态更新 | 处理验证码获取或计算失败的情况 |

### 5. 提交解禁请求功能

| 网站功能 | 代码实现 | 映射说明 |
|---------|---------|---------|
| 提交解禁请求 | [submitUnbanRequest()](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)方法 | 向https://ip-unban.fishport.net/unban发送POST请求 |
| 请求参数 | IP地址和验证码 | 将[ipAddress](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)和[captcha](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)作为表单数据发送 |
| 解禁结果处理 | 检查响应中是否包含"success"或"成功"关键词 | 根据响应内容更新界面状态 |
| 状态更新 | 更新[isBanned](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)状态和[status](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)字段 | 解禁成功后更新界面显示 |

## 界面交互映射

### 按钮功能映射

| 网站按钮 | 代码实现 | 映射说明 |
|---------|---------|---------|
| 检查状态 | "检查状态"按钮调用[checkBanStatus()](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java) | 实现与网站"检查状态"按钮相同功能 |
| 提交解封 | "提交解封"按钮调用[submitUnbanRequest()](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java) | 实现与网站"提交解封"按钮相同功能 |
| 关闭 | "关闭"按钮调用[close()](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)方法 | 关闭当前界面，返回上级界面 |

### 状态显示映射

| 网站显示 | 代码实现 | 映射说明 |
|---------|---------|---------|
| IP地址 | [ipAddress](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)字段和UI渲染 | 显示当前检测到的IP地址 |
| 状态信息 | [status](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)字段和UI渲染 | 显示当前操作状态 |
| 验证码 | [captcha](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)字段和UI渲染 | 显示计算出的验证码 |
| 结果信息 | [resultMessage](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java)字段和UI渲染 | 显示操作结果信息 |

## 异步处理映射

| 网站行为 | 代码实现                                                                                                     | 映射说明 |
|---------|----------------------------------------------------------------------------------------------------------|---------|
| 非阻塞操作 | 所有网络请求使用`CompletableFuture.runAsync()`                                                                   | 避免阻塞主线程，保持界面响应性 |
| UI更新 | 使用 [MinecraftClient.getInstance().execute()](./src/client/java/fun/xingwangzhe/tryfishport/client/TryFishportUI.java) | 确保UI更新在主线程执行 |

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

## 总结

TryFishport模组的IP解禁功能与ip-unban.fishport.net网站的功能基本保持一致，包括：

1. **完整的功能流程** - IP检测、状态检查、验证码计算、解禁请求提交
2. **多服务支持** - 通过多个IP获取服务提高成功率
3. **IPv6处理** - 正确检测并限制IPv6连接的解禁功能
4. **错误处理** - 完善的异常处理和错误提示
5. **用户界面** - 直观的状态显示和操作反馈
6. **国际化支持** - 中英文界面支持

通过这种映射关系，确保了模组用户能够获得与网站一致的使用体验。