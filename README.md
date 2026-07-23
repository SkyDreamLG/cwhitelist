# CWhitelist - Advanced Whitelist Management for Minecraft

<div align="center">
  <br>
  <em>🔒 Intelligent Whitelist System with API Integration for Modern Minecraft Servers</em>
</div>

<p align="center">
  <a href="https://github.com/SkyDreamLG/CWhitelist/releases"><img alt="release" src="https://img.shields.io/github/v/release/SkyDreamLG/CWhitelist?style=for-the-badge&color=4A90E2"></a>
  <a href="https://github.com/SkyDreamLG/CWhitelist/issues"><img alt="issues" src="https://img.shields.io/github/issues/SkyDreamLG/CWhitelist?style=for-the-badge&color=FF6B6B"></a>
  <a href="./LICENSE"><img alt="license" src="https://img.shields.io/badge/license-LGPL--3.0-blue?style=for-the-badge"></a>
  <a href="https://neo-forge.net/"><img alt="NeoForge" src="https://img.shields.io/badge/NeoForge-1.21.x-7B68EE?style=for-the-badge&logo=curseforge"></a>
</p>

---

**English** | [中文](./README_CN.md)

## ✨ Features

### 🔐 Multi-Dimensional Authentication
- **Player Name**: Traditional username-based whitelisting
- **UUID**: Secure player identification
- **IP Address**: IP-based authentication with wildcard support (e.g., `192.168.*.*`)
- **Configurable Check Types**: Enable/disable each authentication method independently

### 🌐 API Integration
- **Dual-Mode Operation**: API-first with local fallback
- **Centralized Management**: Single source of truth across multiple servers
- **Real-time Synchronization**: Automatic whitelist updates
- **Token-based Authentication**: Secure API communication with permission levels
- **Health Monitoring**: Built-in API health checks with automatic heartbeat
- **Player Tracking**: Login/logout event reporting for online status monitoring
- **Adaptive Heartbeat**: 30s normal interval, 5s fast retry on failure, auto-recovery

### 📊 Intelligent Logging System
- **Comprehensive Audit Trail**: Player login attempts with timestamps
- **Log Rotation**: Automatic file management with size limits
- **Retention Policies**: Configurable log retention periods
- **Remote Logging**: Optional API-based event logging

### 🛠️ Advanced Management
- **Async Operations**: Non-blocking API calls and file operations
- **Smart Caching**: Configurable cache durations to reduce API load
- **Error Resilience**: Graceful degradation when API is unavailable
- **Hot Reloading**: Configuration changes without server restart

### 🎮 User Experience
- **Permission-Based Commands**: Granular command access control
- **Real-time Feedback**: Immediate operation confirmation
- **Comprehensive Status**: Detailed API and token information
- **Fallback Protection**: Seamless local operation during API outages

## 🚀 Quick Start

### Installation
1. Download the latest `cwhitelist-x.x-NeoForge-1.21.x.jar` from [Releases](https://github.com/SkyDreamLG/CWhitelist/releases)
2. Place it in your server's `mods` folder
3. Start the server to generate default configuration
4. Restart the server after configuration

### Basic Configuration

**For Local-Only Mode:**
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
enableApi = false  # Disable API integration
```

**For API-Enabled Mode:**
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

Backend program repository address：[cwhitelist-backend](https://github.com/SkyDreamLG/cwhitelist-backend)

## ⚙️ Configuration Guide

### Basic Settings (`[basic]`)
| Parameter | Default | Description | Range |
|-----------|---------|-------------|-------|
| `enableLogging` | `true` | Enable local file logging | boolean |
| `logRetentionDays` | `7` | Days to keep log files | 1-365 |
| `logCutSizeMB` | `10` | Maximum log file size (MB) | 1-100 |

### Check Settings (`[checks]`)
| Parameter | Default | Description |
|-----------|---------|-------------|
| `enableNameCheck` | `true` | Validate by player name |
| `enableUuidCheck` | `true` | Validate by player UUID |
| `enableIpCheck` | `true` | Validate by IP address |

### API Settings (`[api]`)
| Parameter | Default | Description |
|-----------|---------|-------------|
| `enableApi` | `false` | Enable API integration |
| `baseUrl` | `http://127.0.0.1:5000/api` | API server base URL |
| `token` | `""` | API authentication token |
| `useHeaderAuth` | `true` | Use Authorization header (true) or query param (false) |
| `timeoutSeconds` | `10` | API request timeout |
| `cacheDurationSeconds` | `30` | Local cache duration (0 to disable) |
| `syncOnStartup` | `true` | Sync with API on server start |
| `logLoginEvents` | `true` | Send login/logout events to API |
| `serverId` | `""` | Server identifier (recommended to set) |
| `includeExpired` | `false` | Include expired entries when syncing |

## 📋 Command Reference

### 🎮 Player Commands
| Command | Description | Permission |
|---------|-------------|------------|
| No direct player commands | All whitelist management requires admin permissions | - |

### 👑 Admin Commands

**Basic Whitelist Management:**
```bash
# Add entries
/cwhitelist add name <username>
/cwhitelist add uuid <uuid>
/cwhitelist add ip <ip-address>

# Remove entries
/cwhitelist remove name <username>
/cwhitelist remove uuid <uuid>
/cwhitelist remove ip <ip-address>

# View entries
/cwhitelist list

# Reload configuration
/cwhitelist reload
```

**API Management Commands:**
```bash
# Check API status
/cwhitelist api status

# Verify API token
/cwhitelist api verify

# Perform health check
/cwhitelist api health

# Manual sync from API
/cwhitelist api sync

# Clear API cache
/cwhitelist api clearcache
```

## 🔌 API Integration

### API Requirements
CWhitelist supports integration with compatible API servers that implement the following endpoints:

- `GET /health` - Health check / heartbeat (no authentication, `?server_id=` required)
- `GET /whitelist/sync` - Retrieve whitelist entries (requires read permission, `&server_id=` required)
- `POST /whitelist/entries` - Add new entries (requires write permission, `server_id` in body)
- `DELETE /whitelist/entries/{type}/{value}` - Remove entries (requires delete permission, `?server_id=` required)
- `POST /login/log` - Log login events (requires write permission, `server_id` in body)
- `POST /login/logout` - Log logout events (requires write permission, `server_id` in body)
- `GET /tokens/verify` - Verify token validity (requires authentication)

> **Note:** `server_id` is always sent with every API request. If not configured, `"undefined"` is used as fallback.

### Token Permissions
API tokens must be created with appropriate permissions:
- **Read**: Required for syncing whitelist
- **Write**: Required for adding entries and logging events
- **Delete**: Required for removing entries
- **Manage**: System administration (not typically needed)

### Authentication Methods
**Header Authentication (Recommended):**
```http
Authorization: Bearer your-token-here
```

**Query Parameter Authentication:**
```http
GET /api/whitelist/sync?token=your-token-here
```

## 🗂️ File Structure

```
config/
├── cwhitelist-common.toml          # Main configuration
└── cwhitelist_entries.json         # Local whitelist backup

logs/
└── cwhitelist/
    ├── 2024-01-01.log              # Daily log files
    └── 2024-01-01.log.1704067200000 # Rotated logs
```

### Data Files Format

**cwhitelist_entries.json:**
```json
[
  {"type": "name", "value": "PlayerOne"},
  {"type": "uuid", "value": "123e4567-e89b-12d3-a456-426614174000"},
  {"type": "ip", "value": "192.168.1.*"}
]
```

## 🔄 Operation Modes

### Mode 1: Local-Only (Default)
- All data stored locally
- No external dependencies
- Simple deployment
- Suitable for single servers

### Mode 2: API-Primary with Fallback
- Primary: Sync with central API
- Fallback: Use local cache if API unavailable
- Automatic re-sync when API restored
- Ideal for multi-server setups

### Mode 3: API-Only
- All operations through API
- No local whitelist storage
- Centralized management
- Requires reliable API connection

## 🛡️ Security Features

### Authentication Security
- **Token-based Authentication**: Secure API communication
- **Permission Validation**: Granular access control
- **Token Expiry**: Automatic token validity checks
- **No Hardcoded Secrets**: Config file-based token management

### Data Protection
- **Local Encryption**: Sensitive data in configuration
- **Access Control**: Admin-only command permissions (level 4)
- **Audit Logging**: Comprehensive access logging
- **Input Validation**: Sanitized API request parameters

### Network Security
- **HTTPS Support**: Secure API communication (when configured)
- **Timeout Protection**: Configurable request timeouts
- **Retry Logic**: Graceful error handling
- **Rate Limiting**: Built-in request queuing

## 📈 Performance Optimization

### Caching Strategy
```java
// Configurable cache duration
cacheDurationSeconds = 30  // Balance between freshness and API load

// Smart cache invalidation
- Add/Remove operations clear cache
- Manual sync refreshes cache
- Automatic periodic validation
```

### Async Operations
- **Non-blocking API Calls**: HTTP requests on separate threads
- **Parallel Processing**: Concurrent request handling
- **Queue Management**: Ordered request processing
- **Resource Optimization**: Efficient memory usage

## 🐛 Troubleshooting

### Common Issues

**Server ID Not Set:**
```
[Server] WARN ⚠ [CWhitelist] server_id 未设置！
```
**Solution:** Set `serverId` in `cwhitelist-common.toml`. Without it, `"undefined"` is used as fallback.

**API Connection Failed:**
```
[Server] WARN API health check failed, falling back to local file
```
**Solution:** Verify API server is running and accessible. Check network connectivity and firewall settings.

**Heartbeat Failure:**
```
[Server] WARN ⚠ Heartbeat failed, switching to fast interval (5s)
```
**Solution:** Check API server stability. Heartbeat auto-recovers with `✅ Heartbeat recovered` when connection restores.

**Authentication Failed:**
```
[Server] ERROR Token verification failed: Authentication required
```
**Solution:** Verify API token is correct and has required permissions. Use `/cwhitelist api verify` to test.

**Permission Denied:**
```
[Server] ERROR Token does not have write permission
```
**Solution:** Generate new token with appropriate permissions or use existing token with correct permissions.

**Cache Issues:**
```
[Server] DEBUG API cache cleared
```
**Solution:** Cache automatically clears on modification. Use `/cwhitelist api clearcache` to force refresh.

### Log Files
Check log files for detailed error information:
- Location: `logs/cwhitelist/YYYY-MM-DD.log`
- Contains: API calls, authentication attempts, errors
- Format: `[HH:mm:ss] [RESULT] PlayerName UUID IP`

### Debug Commands
```bash
# Check current mode
/cwhitelist list

# Verify API connectivity
/cwhitelist api health

# Test token permissions
/cwhitelist api verify

# View detailed status
/cwhitelist api status
```

## 🧩 API Compatibility

### Supported API Versions
- **Minimum**: v2.1.0
- **Recommended**: v2.1.0
- **Tested With**: CWhitelist API v2.1.0

### Response Format Expectations
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { /* operation-specific data */ }
}
```

### Error Handling
The mod handles the following HTTP status codes:
- `200-299`: Success - Process response
- `401`: Unauthorized - Token invalid/expired
- `403`: Forbidden - Insufficient permissions
- `429`: Rate Limited - Automatic retry with backoff
- `500+`: Server Error - Fallback to local mode

## 🔧 Development

### Building from Source
```bash
# Clone repository
git clone https://github.com/SkyDreamLG/CWhitelist.git
cd CWhitelist

# Build with Gradle
./gradlew build

# Output: build/libs/cwhitelist-x.x.x.jar
```

### Prerequisites
- **Java**: 17 or higher
- **Minecraft**: 1.21.x
- **NeoForge**: Latest recommended build
- **Build Tools**: Gradle 8.0+

### Project Structure
```
src/main/java/org/skydream/cwhitelist/
├── Cwhitelist.java              # Main mod class
├── Config.java                  # Configuration management
├── ApiClient.java               # API communication
├── WhitelistManager.java        # Core whitelist logic
├── WhitelistCommand.java        # Command implementation
├── LogHandler.java              # Logging system
└── WhitelistEntry.java          # Data model
```

### Extending the Mod

**Adding New Authentication Methods:**
1. Update `Config.java` with new check setting
2. Modify `WhitelistManager.isAllowed()` method
3. Add corresponding command handlers
4. Update API client for new endpoint support

**Custom API Integration:**
```java
// Implement custom ApiClient interface
public interface CustomApiClient {
    CompletableFuture<List<WhitelistEntry>> fetchEntries();
    CompletableFuture<Boolean> validateEntry(WhitelistEntry entry);
}
```

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Workflow
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Coding Standards
- Follow existing code style and patterns
- Add comprehensive JavaDoc comments
- Include unit tests for new features
- Update documentation for API changes
- Ensure backward compatibility

### Third-Party Licenses
- **Gson**: Apache License 2.0
- **NeoForge**: LGPL 2.1
- **SLF4J**: MIT License

## 🌟 Acknowledgments

- **NeoForge Team** for the excellent modding framework
- **Mojang Studios** for Minecraft
- **Contributors** who help improve this project
- **Community** for feedback and support

## 📞 Support

- **Documentation**: [GitHub Wiki](https://github.com/SkyDreamLG/CWhitelist/wiki)
- **Issues**: [GitHub Issues](https://github.com/SkyDreamLG/CWhitelist/issues)
- **Discussions**: [GitHub Discussions](https://github.com/SkyDreamLG/CWhitelist/discussions)
- **Email**: 1607002411@qq.com

---

<div align="center">
  <sub>Built with ❤️ by <a href="https://github.com/SkyDreamLG">SkyDream Team</a></sub>
  <br>
  <sub>If you find this project useful, please consider giving it a ⭐ on GitHub!</sub>
</div>

## 🎯 Quick Reference

### Deployment Checklist
- [ ] Verify Java 17+ installation
- [ ] Configure API token (if using API mode)
- [ ] Set appropriate permission levels
- [ ] Test API connectivity
- [ ] Configure logging preferences
- [ ] Set up log rotation schedule
- [ ] Test local fallback functionality

### Performance Tips
1. **Cache Duration**: Set `cacheDurationSeconds` based on update frequency
2. **Log Rotation**: Configure `logCutSizeMB` to prevent disk space issues
3. **Timeout Settings**: Adjust `timeoutSeconds` based on network latency
4. **API Calls**: Minimize API calls during peak hours

### Security Best Practices
1. **Token Security**: Store tokens in configuration, not in code
2. **Permission Minimization**: Grant minimum required permissions
3. **Regular Audits**: Review log files for suspicious activity
4. **API Security**: Use HTTPS for API communication
5. **Backup Strategy**: Regular backups of local whitelist files

---

**Ready to secure your Minecraft server?** Install CWhitelist today and experience professional-grade whitelist management!