# SQLNV - SQL 版本管理与迁移工具

SQLNV 是基于 Flyway 扩展的数据库迁移工具，增加了 DQL 转 DML、数据备份和回滚功能（当前支持 Oracle 数据库）。

## 功能特点

1. **数据库迁移** - 增强的 Flyway 功能，带有额外的版本控制规则
2. **DQL 转 DML** - 根据记录的查询语句生成数据操作语句
3. **数据回滚** - 使用生成的 DML 脚本回滚数据变更

## 使用说明

### 1. 数据库迁移

SQLVN 在 Flyway 规范基础上扩展了版本规则：

- 版本号至少包含3部分（如 `V2025.02.001`）
  - **第一部分**：年份（如 `V2025`），可扩展（如 `2025.05`）
  - **第二部分**：执行优先级（`01` 或 `02`）
    - `01`：高优先级（通常用于 DDL 语句）
    - `02`：低优先级（通常用于可重复执行的 DML 语句，可使用 DQL 进行备份和重新生成）
  - **第三部分**：自动生成的序列号

### 2. 从 DQL 生成 DML

SQLVN 相关配置：

```yaml
sqlvn:
  # SQLVN 版本号 - 只执行对应版本的脚本
  version: "2025.02"
  
  # DQL 文件目录（生成的 DML 会输出到其他位置）
  sql-query-locations: file:src/main/resources/sqlvn/query
  sql-bak-locations: file:src/main/resources/sqlvn/bak
  sql-migration-locations: file:src/main/resources/sqlvn/migration
```

**操作流程**：
1. 将 DQL 文件放入 `sql-query-locations` 目录
   - 文件名格式：`dql_名称.sql`
2. SQLVN 会自动生成对应的 DML 文件：
   - 输出到 `sql-bak-locations` 和 `sql-migration-locations` 目录
   - `sql-bak-locations`文件名格式：`dml_名称.sql`
   - `sql-migration-locations`文件名格式：`V版本号__dml_名称.sql`,只要符合flyway要求的文件才会被迁移程序执行。

**执行模式控制**：

```yaml
# 仅生成 SQL 不执行迁移
genmigsql-enabled: true
migration-enabled: false

# 启用迁移功能
migration-enabled: true
```

### 3. 数据回滚

回滚相关配置：

```yaml
# 操作模式：rollback 或 migration
action: rollback

# 要回滚的版本（备份时的时间戳）
rollback-version: "20250625_104412"

# 回滚记录表前缀
rollback-table-prefix: "SQLVN_ROLLBACK_"
```

**回滚过程**：
1. 每次备份会在 `sql-bak-locations` 下创建带时间戳的子目录
2. 回滚时：
   - 执行指定版本目录中的 DML 文件
   - 在跟踪表中记录回滚操作（如 `SQLVN_ROLLBACK_20250625_104412`）

## 文件结构示例

```
src/main/resources/sqlvn/
├── query/            # DQL 查询文件
│   └── V2025.01/
│       └── dql_示例.sql
├── bak/              # 备份版本
│   └── V2025.01/
│       └── 20250625_104412/
│           └── dml_示例.sql
└── migration/        # 迁移脚本
│   └── V2025.01/
│       └── V2025.01.02.001__dml_示例.sql
```

## 环境要求

- Spring Boot 应用
- Flyway 依赖
- Oracle 数据库支持（当前版本）