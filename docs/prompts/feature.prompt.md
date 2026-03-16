下面这套 Prompt 可以让 AI 生成完整 Spring Boot Feature Store 模块代码。
模块名称：
fraud-feature
技术约束：
Spring Boot 4.x
JDK 25
Maven
Spring JDBC
PostgreSQL
No ORM

Prompt 1 — 生成模块结构
You are a senior Java backend architect.
Generate a production-ready module called `fraud-feature`.
Environment:
Spring Boot 4.x  
JDK 25  
Maven  
Spring JDBC  
PostgreSQL
Strict constraints:
- Do NOT use ORM frameworks (JPA, Hibernate, MyBatis).
- Use Spring JDBC only.
- Write explicit SQL queries.
- Keep dependencies minimal.
- Follow layered architecture.
Module responsibilities:
- Feature definition metadata
- Feature snapshot storage
- Feature history storage
- Batch feature generation
- Feature retrieval for risk engine
- Feature quality checks
Project structure:
fraud-feature
 ├── definition
 ├── model
 ├── repository
 ├── compute
 ├── service
 └── job
Each package should contain appropriate classes.
Generate all source files.

Prompt 2 — Feature Model
Generate Java model classes for the fraud-feature module.
Classes required:
FeatureDefinition  
AccountFeatureSnapshot  
AccountFeatureHistory  
FeatureGenerationJob  
FeatureQualityCheck
Requirements:
- Plain Java objects
- Immutable where reasonable
- Use Java record if appropriate
- Include field documentation
- Include builders where necessary
- Map exactly to database schema

Prompt 3 — JDBC Repositories
Generate Spring JDBC repositories for the fraud-feature module.
Repositories required:
FeatureDefinitionRepository  
AccountFeatureSnapshotRepository  
AccountFeatureHistoryRepository  
FeatureGenerationJobRepository  
FeatureQualityCheckRepository
Requirements:
- Use JdbcTemplate or NamedParameterJdbcTemplate
- No ORM
- SQL queries must be explicit
- Provide:
save()
update()
findById()
findBatch()
findLatestByAccountId()
insertBatch()
Implement RowMapper classes.

Prompt 4 — Feature Calculators
Generate feature calculator classes.
Each class calculates a group of related features.
Required calculators:
LoginFeatureCalculator  
TransactionFeatureCalculator  
DeviceFeatureCalculator  
SecurityFeatureCalculator  
GraphFeatureCalculator
Each calculator must:
- Query raw event tables
- Aggregate metrics
- Populate AccountFeatureSnapshot fields
Use SQL aggregation rather than Java loops where possible.
Design calculators to support batch processing.

Prompt 5 — Feature Store Service
Generate FeatureStoreService.
Responsibilities:
- Fetch latest features for account
- Fetch batch features for accounts
- Persist snapshot
- Persist history
- Support batch feature generation pipeline
Methods required:
getLatestFeatures(accountId)
getLatestFeaturesBatch(accountIds)
generateFeaturesForAccount(accountId)
generateFeaturesBatch(accountIds)
persistSnapshot(snapshot)
persistHistory(snapshot)

Prompt 6 — Batch Feature Generation Job
Generate FeatureGenerationJobRunner.
Responsibilities:
- Start feature generation job
- Process accounts in batches
- Use feature calculators
- Update feature_generation_job table
- Persist snapshot + history
Batch strategy:
- process 1000 accounts per batch
- avoid loading entire dataset into memory
- log progress
Include error handling and retry.

Prompt 7 — Feature Quality Checks
Generate FeatureQualityService.
Quality checks required:
null ratio
negative values
range validation
constant feature detection
freshness check
Results should be stored in feature_quality_check table.

Prompt 8 — Feature Store API Adapter
Generate a FeatureServiceAdapter used by other modules.
Expose:
getFeaturesForRiskEngine(accountId)
getFeaturesForML(accountId)
getFeaturesForCaseReport(accountId)
The adapter may transform internal snapshot objects to DTOs suitable for external modules.