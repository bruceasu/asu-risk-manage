# Maven 依赖下载指南

本增强版系统目前**不强制**依赖外部库，因为我们实现了简化的 JSON 解析器。但如果需要更强大的功能，可以添加以下依赖。

## 可选依赖

### 1. Gson（推荐用于生产环境的 JSON 解析）

**下载地址：**
https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar

**下载步骤：**
```bash
# Windows PowerShell
cd c:\Users\svictor\workspace\tools\交易分析\online\lib
Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar" -OutFile "gson-2.10.1.jar"

# 或使用 curl
curl -o gson-2.10.1.jar https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar
```

**使用方法：**
修改 `EventText.parse()` 方法使用 Gson：
```java
import com.google.gson.Gson;
import com.google.gson.JsonObject;

static EventText parse(String json) {
    if (json == null || json.trim().isEmpty()) {
        return empty();
    }
    
    try {
        Gson gson = new Gson();
        JsonObject obj = gson.fromJson(json, JsonObject.class);
        
        BigDecimal px = obj.has("px") ? new BigDecimal(obj.get("px").getAsString()) : null;
        Long tm = obj.has("tm") ? obj.get("tm").getAsLong() : null;
        // ... 其他字段
        
        return new EventText(px, tm, td, ip, clt, clv, log, cm, mg, eqt, mgl);
    } catch (Exception e) {
        return empty();
    }
}
```

---

### 2. Apache Commons Math（用于高级统计）

如果需要更精确的统计功能（如 Kolmogorov-Smirnov 检验、多元相关性分析）：

**下载地址：**
https://repo1.maven.org/maven2/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar

**下载命令：**
```bash
cd c:\Users\svictor\workspace\tools\交易分析\online\lib
curl -o commons-math3-3.6.1.jar https://repo1.maven.org/maven2/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar
```

---

### 3. 数据库连接（用于持久化特征数据）

#### MySQL Connector

**下载地址：**
https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar

**下载命令：**
```bash
cd c:\Users\svictor\workspace\tools\交易分析\online\lib
curl -o mysql-connector-j-8.3.0.jar https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar
```

#### HikariCP（连接池，推荐）

**下载地址：**
https://repo1.maven.org/maven2/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar

**下载命令：**
```bash
curl -o HikariCP-5.1.0.jar https://repo1.maven.org/maven2/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar
```

---

### 4. Binlog CDC 集成

#### Alibaba Canal Client（推荐用于 MySQL Binlog）

**下载地址：**
- https://repo1.maven.org/maven2/com/alibaba/otter/canal.client/1.1.7/canal.client-1.1.7.jar
- https://repo1.maven.org/maven2/com/alibaba/otter/canal.protocol/1.1.7/canal.protocol-1.1.7.jar
- https://repo1.maven.org/maven2/com/google/protobuf/protobuf-java/3.21.12/protobuf-java-3.21.12.jar

**下载脚本：**
```bash
cd c:\Users\svictor\workspace\tools\交易分析\online\lib

# Canal Client
curl -o canal.client-1.1.7.jar https://repo1.maven.org/maven2/com/alibaba/otter/canal.client/1.1.7/canal.client-1.1.7.jar

# Canal Protocol
curl -o canal.protocol-1.1.7.jar https://repo1.maven.org/maven2/com/alibaba/otter/canal.protocol/1.1.7/canal.protocol-1.1.7.jar

# Protobuf
curl -o protobuf-java-3.21.12.jar https://repo1.maven.org/maven2/com/google/protobuf/protobuf-java/3.21.12/protobuf-java-3.21.12.jar
```

---

## 一键下载所有依赖脚本

### Windows PowerShell

保存为 `download_dependencies.ps1`：

```powershell
# 创建 lib 目录
$libDir = "lib"
if (!(Test-Path $libDir)) {
    New-Item -ItemType Directory -Path $libDir
}

# 依赖列表
$dependencies = @(
    @{
        Name = "Gson";
        Url = "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar";
        File = "gson-2.10.1.jar"
    },
    @{
        Name = "MySQL Connector";
        Url = "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar";
        File = "mysql-connector-j-8.3.0.jar"
    },
    @{
        Name = "HikariCP";
        Url = "https://repo1.maven.org/maven2/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar";
        File = "HikariCP-5.1.0.jar"
    },
    @{
        Name = "Commons Math";
        Url = "https://repo1.maven.org/maven2/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar";
        File = "commons-math3-3.6.1.jar"
    }
)

# 下载
foreach ($dep in $dependencies) {
    $outFile = Join-Path $libDir $dep.File
    Write-Host "Downloading $($dep.Name)..."
    
    try {
        Invoke-WebRequest -Uri $dep.Url -OutFile $outFile
        Write-Host "[OK] $($dep.File)" -ForegroundColor Green
    } catch {
        Write-Host "[FAILED] $($dep.Name): $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Done. Dependencies downloaded to $libDir\" -ForegroundColor Cyan
```

**运行：**
```powershell
cd c:\Users\svictor\workspace\tools\交易分析\online
powershell -ExecutionPolicy Bypass -File download_dependencies.ps1
```

---

### Linux/Mac (Bash)

保存为 `download_dependencies.sh`：

```bash
#!/bin/bash

# 创建 lib 目录
mkdir -p lib
cd lib

echo "Downloading dependencies..."

# Gson
curl -L -o gson-2.10.1.jar \
  https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar

# MySQL Connector
curl -L -o mysql-connector-j-8.3.0.jar \
  https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar

# HikariCP
curl -L -o HikariCP-5.1.0.jar \
  https://repo1.maven.org/maven2/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar

# Commons Math
curl -L -o commons-math3-3.6.1.jar \
  https://repo1.maven.org/maven2/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar

echo ""
echo "Done. Dependencies downloaded to lib/"
ls -lh
```

**运行：**
```bash
chmod +x download_dependencies.sh
./download_dependencies.sh
```

---

## 使用依赖

一旦下载了 JAR 文件到 `lib/` 目录，构建脚本会自动将它们包含在 classpath 中：

```bash
# Online 系统
cd online
run_example.bat        # 会自动检测 lib\*.jar

# Offline 系统
cd offline
run_example.bat        # 会自动检测 lib\*.jar
```

---

## 验证依赖

创建测试类 `TestDependencies.java`：

```java
public class TestDependencies {
    public static void main(String[] args) {
        System.out.println("Testing dependencies...");
        
        // Test Gson
        try {
            Class.forName("com.google.gson.Gson");
            System.out.println("[OK] Gson");
        } catch (ClassNotFoundException e) {
            System.out.println("[MISSING] Gson");
        }
        
        // Test MySQL Connector
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("[OK] MySQL Connector");
        } catch (ClassNotFoundException e) {
            System.out.println("[MISSING] MySQL Connector");
        }
        
        // Test HikariCP
        try {
            Class.forName("com.zaxxer.hikari.HikariDataSource");
            System.out.println("[OK] HikariCP");
        } catch (ClassNotFoundException e) {
            System.out.println("[MISSING] HikariCP");
        }
        
        System.out.println("\nAll available dependencies are loaded.");
    }
}
```

**编译并运行：**
```bash
javac -cp "lib/*" TestDependencies.java
java -cp ".;lib/*" TestDependencies
```

---

## 注意事项

1. **当前系统可以无依赖运行**
   - EventText 使用了简化的正则表达式解析
   - 统计功能使用纯 Java 实现
   - 适合快速测试和学习

2. **生产环境建议添加依赖**
   - Gson：更健壮的 JSON 解析
   - 数据库连接：持久化特征数据
   - Canal Client：如果使用 Binlog CDC

3. **依赖文件大小参考**
   - gson-2.10.1.jar: ~250KB
   - mysql-connector-j-8.3.0.jar: ~2.5MB
   - HikariCP-5.1.0.jar: ~160KB
   - commons-math3-3.6.1.jar: ~2.2MB
   - 总计: ~5MB

4. **离线环境**
   - 可以在有网络的机器上下载
   - 复制整个 `lib/` 目录到目标机器

---

## 常见问题

**Q: 为什么不使用 Maven 或 Gradle？**
A: 考虑到这是一个简化的 DEMO 系统，使用 bat 脚本更直接。如果需要，可以创建 `pom.xml` 或 `build.gradle`。

**Q: 依赖下载失败怎么办？**
A: 
1. 检查网络连接
2. 尝试使用 Maven 中央仓库镜像（如阿里云）
3. 手动浏览器下载后放入 `lib/` 目录

**Q: 依赖冲突怎么办？**
A: 
1. 确保只有一个版本的 JAR
2. 删除旧版本
3. 使用 `java -verbose:class` 查看加载的类

---

## 联系方式

如需帮助，请参考：
- 📄 交易分析指标说明文档.md（指标详解）
- 📄 IntegrationGuide.java（生产集成指南）
- 📄 README_v2.md（快速开始）
