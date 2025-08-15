# TryFishport

![GitHub License](https://img.shields.io/github/license/xingwangzhe/TryFishport)
![Minecraft Version](https://img.shields.io/badge/minecraft-1.21.4-blue)
![Fabric API](https://img.shields.io/badge/fabric--api-0.119.4%2B1.21.4-orange)

TryFishport 是一个为 Minecraft 设计的 Fabric 客户端模组，主要为 Fishport 服务器提供额外功能。

## 功能

TryFishport 提供以下功能：

1. **IP 解禁工具** - 集成在游戏主菜单中，方便玩家自助解禁被封禁的 IP 地址
2. **服务器网络诊断** - 可以对 Fishport 服务器进行网络路由跟踪测试
3. **便捷访问** - 直接在游戏界面中集成相关功能，无需额外工具

## 安装

1. 确保已安装 [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.4
2. 下载最新版本的 TryFishport.jar
3. 将 jar 文件放入 Minecraft 的 `mods` 文件夹中
4. 启动游戏

## 使用说明

模组安装后会在游戏主菜单和服务器列表中添加额外的按钮：

- 在服务器列表左下角添加"TryFishport"按钮，点击可打开 IP 解禁界面
- 在服务器列表中，每个服务器条目右下角添加"Ping"按钮，可测试到服务器的网络路由

详细的使用说明和功能映射文档请参阅 [TryFishport 功能详细说明](./TryFishport.md)。

## 构建

要构建此项目，您需要：

1. JDK 21
2. Gradle

执行以下命令进行构建：

```bash
./gradlew build
```

构建后的模组文件将位于 `build/libs/` 目录中。

## 版本细节

- Minecraft 版本: 1.21.4
- Fabric API: 0.119.4+1.21.4
- Fabric Loader: 0.17.2
- Java 版本: 21


## 贡献者

<!-- readme: contributors -start -->
<table>
	<tbody>
		<tr>
            <td align="center">
                <a href="https://github.com/xingwangzhe">
                    <img src="https://avatars.githubusercontent.com/u/162127610?v=4" width="100;" alt="xingwangzhe"/>
                    <br />
                    <sub><b>王兴家</b></sub>
                </a>
            </td>
            <td align="center">
                <a href="https://github.com/Kongchenglige">
                    <img src="https://avatars.githubusercontent.com/u/35166074?v=4" width="100;" alt="Kongchenglige"/>
                    <br />
                    <sub><b>空城 離歌</b></sub>
                </a>
            </td>
		</tr>
	<tbody>
</table>
<!-- readme: contributors -end -->

## 许可证

本项目基于 MIT 许可证发布，详情请参阅 [LICENSE.txt](./LICENSE.txt) 文件。