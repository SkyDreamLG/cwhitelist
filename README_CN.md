# CWhitelist - Minecraft 高级白名单管理系统

<div align="center">
  <br>
  <em>🔒 支持 API 集成的智能白名单系统，专为现代 Minecraft 服务器设计</em>
</div>

<p align="center">
  <a href="https://github.com/SkyDreamLG/CWhitelist/releases"><img alt="发行版" src="https://img.shields.io/github/v/release/SkyDreamLG/CWhitelist?style=for-the-badge&color=4A90E2"></a>
  <a href="https://github.com/SkyDreamLG/CWhitelist/issues"><img alt="问题" src="https://img.shields.io/github/issues/SkyDreamLG/CWhitelist?style=for-the-badge&color=FF6B6B"></a>
  <a href="./LICENSE"><img alt="许可证" src="https://img.shields.io/badge/许可证-GPL--3.0-blue?style=for-the-badge"></a>
  <a href="https://neo-forge.net/"><img alt="NeoForge" src="https://img.shields.io/badge/NeoForge-1.21.x-7B68EE?style=for-the-badge&logo=curseforge"></a>
</p>

---

**中文** | [English](./README.md)

## ✨ 功能特性

### 🔐 多维度身份验证
- **玩家名称**：传统的用户名白名单
- **UUID**：安全的玩家身份标识
- **IP 地址**：支持通配符的 IP 验证（例如：`192.168.*.*`）
- **可配置检查类型**：独立启用/禁用每种验证方式

### 🌐 API 集成
- **双模式操作**：API 优先，本地回退
- **集中化管理**：跨多个服务器的单一数据源
- **实时同步**：自动更新白名单
- **令牌认证**：带权限级别的安全 API 通信
- **健康监控**：内置 API 健康检查

### 📊 智能日志系统
- **完整审计跟踪**：带时间戳的玩家登录尝试记录
- **日志轮转**：带大小限制的自动文件管理
- **保留策略**：可配置的日志保留期限
- **远程日志**：可选的基于 API 的事件记录

### 🛠️ 高级管理功能
- **异步操作**：非阻塞 API 调用和文件操作
- **智能缓存**：可配置缓存时长以减少 API 负载
- **容错能力**：API 不可用时优雅降级
- **热重载**：无需重启服务器的配置更改

### 🎮 用户体验
- **基于权限的命令**：细粒度的命令访问控制
- **实时反馈**：即时操作确认
- **完整状态信息**：详细的 API 和令牌信息
- **回退保护**：API 中断时的无缝本地操作

## 🚀 快速开始

### 安装步骤
1. 从 [发行版](https://github.com/SkyDreamLG/CWhitelist/releases) 下载最新的 `cwhitelist-x.x-NeoForge-1.21.x.jar`
2. 将其放入服务器的 `mods` 文件夹
3. 启动服务器生成默认配置
4. 配置完成后重启服务器

### 基础配置

**仅本地模式：**
```toml
# config/cwhitelist-common.toml
[basic]
enableLogging = true
logRetentionDays = 7
logCutSizeMB = 10

[checks]
enableNameCheck = true
enableUuidCheck = true
enableIpCheck = true

[api]
enableApi = false  # 禁用 API 集成
```

**启用 API 模式：**
```toml
[api]
enableApi = true
baseUrl = "http://your-api-server.com/api"
token = "your-secure-api-token-here"
useHeaderAuth = true
timeoutSeconds = 10
syncOnStartup = true
logLoginEvents = true
```

后端程序仓库地址：[cwhitelist-backend](https://github.com/SkyDreamLG/cwhitelist-backend)

## ⚙️ 配置指南

### 基础设置 (`[basic]`)
| 参数 | 默认值 | 描述 | 范围 |
|------|--------|------|------|
| `enableLogging` | `true` | 启用本地文件日志 | 布尔值 |
| `logRetentionDays` | `7` | 日志文件保留天数 | 1-365 |
| `logCutSizeMB` | `10` | 最大日志文件大小 (MB) | 1-100 |

### 检查设置 (`[checks]`)
| 参数 | 默认值 | 描述 |
|------|--------|------|
| `enableNameCheck` | `true` | 按玩家名称验证 |
| `enableUuidCheck` | `true` | 按玩家 UUID 验证 |
| `enableIpCheck` | `true` | 按 IP 地址验证 |

### API 设置 (`[api]`)
| 参数 | 默认值 | 描述 |
|------|--------|------|
| `enableApi` | `false` | 启用 API 集成 |
| `baseUrl` | `http://127.0.0.1:5000/api` | API 服务器基础 URL |
| `token` | `""` | API 认证令牌 |
| `useHeaderAuth` | `true` | 使用 Authorization 头部 (true) 或查询参数 (false) |
| `timeoutSeconds` | `10` | API 请求超时时间 |
| `cacheDurationSeconds` | `30` | 本地缓存时长 (0 表示禁用) |
| `syncOnStartup` | `true` | 服务器启动时同步 |
| `logLoginEvents` | `true` | 发送登录事件到 API |
| `serverId` | `""` | 可选的服务器标识符 |
| `sendServerId` | `false` | 在 API 请求中包含服务器 ID |
| `includeExpired` | `false` | 同步时包含过期条目 |

## 📋 命令参考

### 🎮 玩家命令
| 命令 | 描述 | 权限 |
|------|------|------|
| 无直接玩家命令 | 所有白名单管理都需要管理员权限 | - |

### 👑 管理员命令

**基础白名单管理：**
```bash
# 添加条目
/cwhitelist add name <用户名>
/cwhitelist add uuid <uuid>
/cwhitelist add ip <ip地址>

# 移除条目
/cwhitelist remove name <用户名>
/cwhitelist remove uuid <uuid>
/cwhitelist remove ip <ip地址>

# 查看条目
/cwhitelist list

# 重载配置
/cwhitelist reload
```

**API 管理命令：**
```bash
# 检查 API 状态
/cwhitelist api status

# 验证 API 令牌
/cwhitelist api verify

# 执行健康检查
/cwhitelist api health

# 手动从 API 同步
/cwhitelist api sync

# 清除 API 缓存
/cwhitelist api clearcache
```

## 🔌 API 集成

### API 要求
CWhitelist 支持与实现以下端点的兼容 API 服务器集成：

- `GET /health` - 健康检查（无需认证）
- `GET /whitelist/sync` - 获取白名单条目（需要读取权限）
- `POST /whitelist/entries` - 添加新条目（需要写入权限）
- `DELETE /whitelist/entries/{type}/{value}` - 移除条目（需要删除权限）
- `POST /login/log` - 记录登录事件（需要写入权限）
- `GET /tokens/verify` - 验证令牌有效性（需要认证）

### 令牌权限
API 令牌必须具有适当的权限：
- **读取**：同步白名单所需
- **写入**：添加条目和记录事件所需
- **删除**：移除条目所需
- **管理**：系统管理（通常不需要）

### 认证方法
**头部认证（推荐）：**
```http
Authorization: Bearer your-token-here
```

**查询参数认证：**
```http
GET /api/whitelist/sync?token=your-token-here
```

## 🗂️ 文件结构

```
config/
├── cwhitelist-common.toml          # 主配置文件
└── cwhitelist_entries.json         # 本地白名单备份

logs/
└── cwhitelist/
    ├── 2024-01-01.log              # 每日日志文件
    └── 2024-01-01.log.1704067200000 # 轮转日志
```

### 数据文件格式

**cwhitelist_entries.json：**
```json
[
  {"type": "name", "value": "PlayerOne"},
  {"type": "uuid", "value": "123e4567-e89b-12d3-a456-426614174000"},
  {"type": "ip", "value": "192.168.1.*"}
]
```

## 🔄 操作模式

### 模式 1：仅本地（默认）
- 所有数据存储在本地
- 无外部依赖
- 部署简单
- 适合单服务器

### 模式 2：API 优先，带回退
- 主要：与中央 API 同步
- 回退：API 不可用时使用本地缓存
- API 恢复时自动重新同步
- 适合多服务器设置

### 模式 3：仅 API
- 所有操作通过 API
- 无本地白名单存储
- 集中管理
- 需要可靠的 API 连接

## 🛡️ 安全特性

### 认证安全
- **基于令牌的认证**：安全的 API 通信
- **权限验证**：细粒度的访问控制
- **令牌过期**：自动令牌有效性检查
- **无硬编码密钥**：基于配置文件的令牌管理

### 数据保护
- **本地加密**：配置中的敏感数据
- **访问控制**：仅管理员命令权限（等级 4）
- **审计日志**：完整的访问日志记录
- **输入验证**：清理的 API 请求参数

### 网络安全
- **HTTPS 支持**：安全的 API 通信（配置时）
- **超时保护**：可配置的请求超时
- **重试逻辑**：优雅的错误处理
- **速率限制**：内置请求队列

## 📈 性能优化

### 缓存策略
```java
// 可配置的缓存时长
cacheDurationSeconds = 30  // 在新鲜度和 API 负载之间平衡

// 智能缓存失效
- 添加/删除操作清除缓存
- 手动同步刷新缓存
- 自动定期验证
```

### 异步操作
- **非阻塞 API 调用**：HTTP 请求在独立线程上
- **并行处理**：并发请求处理
- **队列管理**：有序请求处理
- **资源优化**：高效内存使用

## 🐛 故障排除

### 常见问题

**API 连接失败：**
```
[Server] WARN API health check failed, falling back to local file
```
**解决方案：** 验证 API 服务器是否运行且可访问。检查网络连接和防火墙设置。

**认证失败：**
```
[Server] ERROR Token verification failed: Authentication required
```
**解决方案：** 验证 API 令牌是否正确且具有所需权限。使用 `/cwhitelist api verify` 测试。

**权限拒绝：**
```
[Server] ERROR Token does not have write permission
```
**解决方案：** 生成具有适当权限的新令牌，或使用具有正确权限的现有令牌。

**缓存问题：**
```
[Server] DEBUG API cache cleared
```
**解决方案：** 修改时缓存自动清除。使用 `/cwhitelist api clearcache` 强制刷新。

### 日志文件
检查日志文件获取详细错误信息：
- 位置：`logs/cwhitelist/YYYY-MM-DD.log`
- 包含：API 调用、认证尝试、错误
- 格式：`[HH:mm:ss] [RESULT] PlayerName UUID IP`

### 调试命令
```bash
# 检查当前模式
/cwhitelist list

# 验证 API 连接性
/cwhitelist api health

# 测试令牌权限
/cwhitelist api verify

# 查看详细状态
/cwhitelist api status
```

## 🧩 API 兼容性

### 支持的 API 版本
- **最低**：v1.0.0
- **推荐**：v1.1.0+
- **测试版本**：CWhitelist API v1.2.0

### 响应格式期望
```json
{
  "success": true,
  "message": "操作成功",
  "data": { /* 操作特定数据 */ }
}
```

### 错误处理
模组处理以下 HTTP 状态码：
- `200-299`：成功 - 处理响应
- `401`：未授权 - 令牌无效/过期
- `403`：禁止 - 权限不足
- `429`：速率限制 - 自动退避重试
- `500+`：服务器错误 - 回退到本地模式

## 🔧 开发

### 从源码构建
```bash
# 克隆仓库
git clone https://github.com/SkyDreamLG/CWhitelist.git
cd CWhitelist

# 使用 Gradle 构建
./gradlew build

# 输出：build/libs/cwhitelist-x.x.x.jar
```

### 先决条件
- **Java**：17 或更高版本
- **Minecraft**：1.21.x
- **NeoForge**：最新推荐构建
- **构建工具**：Gradle 8.0+

### 项目结构
```
src/main/java/org/skydream/cwhitelist/
├── Cwhitelist.java              # 主模组类
├── Config.java                  # 配置管理
├── ApiClient.java               # API 通信
├── WhitelistManager.java        # 核心白名单逻辑
├── WhitelistCommand.java        # 命令实现
├── LogHandler.java              # 日志系统
└── WhitelistEntry.java          # 数据模型
```

### 扩展模组

**添加新的认证方法：**
1. 在 `Config.java` 中更新新的检查设置
2. 修改 `WhitelistManager.isAllowed()` 方法
3. 添加相应的命令处理器
4. 更新 API 客户端以支持新端点

**自定义 API 集成：**
```java
// 实现自定义 ApiClient 接口
public interface CustomApiClient {
    CompletableFuture<List<WhitelistEntry>> fetchEntries();
    CompletableFuture<Boolean> validateEntry(WhitelistEntry entry);
}
```

## 🤝 贡献指南

我们欢迎贡献！请参阅我们的 [贡献指南](CONTRIBUTING.md) 了解详情。

### 开发工作流程
1. Fork 仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m '添加神奇的特性'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 打开 Pull Request

### 编码标准
- 遵循现有的代码风格和模式
- 添加全面的 JavaDoc 注释
- 为新功能包含单元测试
- 为 API 更改更新文档
- 确保向后兼容性
- 
### 第三方许可证
- **Gson**：Apache License 2.0
- **NeoForge**：LGPL 2.1
- **SLF4J**：MIT License

## 🌟 致谢

- **NeoForge 团队** 提供优秀的模组框架
- **Mojang Studios** 开发 Minecraft
- **贡献者** 帮助改进本项目
- **社区** 提供反馈和支持

## 📞 支持

- **文档**：[GitHub Wiki](https://github.com/SkyDreamLG/CWhitelist/wiki)
- **问题跟踪**：[GitHub Issues](https://github.com/SkyDreamLG/CWhitelist/issues)
- **讨论**：[GitHub Discussions](https://github.com/SkyDreamLG/CWhitelist/discussions)
- **邮箱**：1607002411@qq.com

---

<div align="center">
  <sub>由 <a href="https://github.com/SkyDreamLG">SkyDream 团队</a> ❤️ 构建</sub>
  <br>
  <sub>如果您觉得这个项目有用，请在 GitHub 上给它一个 ⭐！</sub>
</div>

## 🎯 快速参考

### 部署清单
- [ ] 验证 Java 17+ 安装
- [ ] 配置 API 令牌（如果使用 API 模式）
- [ ] 设置适当的权限等级
- [ ] 测试 API 连接性
- [ ] 配置日志首选项
- [ ] 设置日志轮转计划
- [ ] 测试本地回退功能

### 性能提示
1. **缓存时长**：根据更新频率设置 `cacheDurationSeconds`
2. **日志轮转**：配置 `logCutSizeMB` 防止磁盘空间问题
3. **超时设置**：根据网络延迟调整 `timeoutSeconds`
4. **API 调用**：高峰时段最小化 API 调用

### 安全最佳实践
1. **令牌安全**：将令牌存储在配置中，而不是代码中
2. **权限最小化**：授予所需的最小权限
3. **定期审计**：审查日志文件查找可疑活动
4. **API 安全**：使用 HTTPS 进行 API 通信
5. **备份策略**：定期备份本地白名单文件

---

**准备好保护您的 Minecraft 服务器了吗？** 立即安装 CWhitelist，体验专业级的白名单管理！