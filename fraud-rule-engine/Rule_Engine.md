涓嬮潰缁欎綘涓€浠?鐢熶骇绾?Rule Engine 璁捐锛屾寜浣犲綋鍓嶆妧鏈矾绾挎敹鏁涳細
    鈥?Java / Spring Boot 4.x / JDK 25
    鈥?Maven
    鈥?Spring JDBC
    鈥?PostgreSQL
    鈥?灏戜緷璧?
    鈥?Phase-1 绂荤嚎鎵瑰鐞?
    鈥?Phase-2 瀹炴椂鎺ュ叆
    鈥?瑙勫垯浼樺厛浜?ML
杩欎唤璁捐閲嶇偣瑙ｅ喅 8 涓棶棰橈細
    1. 瑙勫垯濡備綍琛ㄧず
    2. 瑙勫垯濡備綍鎵ц
    3. 瑙勫垯濡備綍鐗堟湰鍖?
    4. 瑙勫垯濡備綍鐑洿鏂?
    5. 瑙勫垯濡備綍瀹¤
    6. 瑙勫垯濡備綍瑙ｉ噴
    7. 瑙勫垯濡備綍涓?Feature Store / Risk Engine 瀵规帴
    8. 瑙勫垯濡備綍浠庣绾垮钩婊戞紨杩涘埌瀹炴椂

1. Rule Engine 鐨勫畾浣?
鍦ㄥ弽娆鸿瘓绯荤粺閲岋紝Rule Engine 涓嶆槸鈥滃啓鍑犱釜 if/else鈥濄€?
瀹冨簲璇ユ槸涓€涓鍒欏喅绛栧瓙绯荤粺锛岃礋璐ｏ細
    鈥?娑堣垂璐︽埛鐗瑰緛銆佷簨浠朵笂涓嬫枃銆佸浘璋变俊鍙?
    鈥?璇勪及纭畾鎬ц鍒?
    鈥?杈撳嚭鍛戒腑鐨勮鍒欍€佷弗閲嶇骇鍒€佽В閲娿€佽瘉鎹?
    鈥?褰㈡垚瑙勫垯鍒嗘暟
    鈥?缁?Risk Engine 鎻愪緵绋冲畾杈撳叆
涓€鍙ヨ瘽瀹氫箟锛?
Rule Engine 鏄弽娆鸿瘓绯荤粺涓渶鍙В閲娿€佹渶鍙帶銆佹渶瀹规槗蹇€熻凯浠ｇ殑妫€娴嬪眰銆?

2. Rule Engine 鐨勬牳蹇冨師鍒?
2.1 瑙勫垯瑕佲€滃彲瑙ｉ噴鈥?
姣忔潯瑙勫垯閮藉繀椤昏兘鍥炵瓟锛?
    鈥?涓轰粈涔堝懡涓?
    鈥?鍛戒腑鐨勯槇鍊兼槸浠€涔?
    鈥?鐢ㄤ簡鍝簺鐗瑰緛
    鈥?璇佹嵁鏄粈涔?

2.2 瑙勫垯瑕佲€滃彲瀹¤鈥?
浣犲繀椤昏兘杩芥函锛?
    鈥?鍝釜鐗堟湰鐨勮鍒欏懡涓簡璇ヨ处鎴?
    鈥?褰撴椂鐨勯槇鍊兼槸澶氬皯
    鈥?褰撴椂浣跨敤鐨勭壒寰佺増鏈槸浠€涔?

2.3 瑙勫垯瑕佲€滃彲鍒嗗眰鈥?
涓嶈鎶婃墍鏈夎鍒欐贩鍦ㄤ竴璧枫€傚缓璁嚦灏戝垎锛?
    鈥?鐧诲綍椋庨櫓瑙勫垯
    鈥?浜ゆ槗椋庨櫓瑙勫垯
    鈥?璁惧椋庨櫓瑙勫垯
    鈥?瀹夊叏浜嬩欢瑙勫垯
    鈥?鍥捐氨瑙勫垯
    鈥?缁勫悎瑙勫垯

2.4 瑙勫垯瑕佲€滈厤缃寲锛屼絾涓嶈杩囧害 DSL 鍖栤€?
绗竴鐗堜笉瑕佷笂澶嶆潅鑴氭湰寮曟搸锛屼笉瑕佷笂 Drools 杩欑被閲嶆鏋躲€?
鏈€绋崇殑鏂规鏄細
    鈥?Java 瑙勫垯绫?
    鈥?瑙勫垯鍏冩暟鎹瓨鏁版嵁搴?
    鈥?闃堝€煎弬鏁伴厤缃寲
    鈥?鏀寔鐑姞杞介厤缃?
杩欐牱鏃㈢ǔ瀹氬張鐏垫椿銆?

3. 鐢熶骇绾ф€讳綋鏋舵瀯
Feature Store / Event Context / Graph Signals
                    鈫?
             RuleEvaluationContext
                    鈫?
                Rule Engine
     鈹屸攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹尖攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹?
     鈫?             鈫?             鈫?
 Rule Library   Rule Registry   Rule Config
     鈫?             鈫?             鈫?
                Rule Results
                    鈫?
          Rule Score + Reason Codes
                    鈫?
                Risk Engine

4. 寤鸿妯″潡鍒掑垎
寤鸿鎷嗘垚涓や釜妯″潡锛?
fraud-rule-engine
fraud-rule-engine

4.1 fraud-rule-engine
鏀炬鏋惰兘鍔涳細
    鈥?FraudRule
    鈥?RuleEvaluationContext
    鈥?RuleEvaluationResult
    鈥?RuleEngine
    鈥?RuleRegistry
    鈥?RuleConfigRepository
    鈥?RuleVersionService
    鈥?RuleResultAggregator

4.2 fraud-rule-engine
鏀惧叿浣撹鍒欏疄鐜帮細
    鈥?RapidWithdrawAfterDepositRule
    鈥?HighRiskIpLoginRule
    鈥?SharedDeviceClusterRule
    鈥?RapidProfileChangeRule
    鈥?NewAccountHighActivityRule
    鈥?CredentialStuffingPatternRule
    鈥?AtoSuspicionRule
    鈥?CollectorAccountRule

5. 瑙勫垯妯″瀷璁捐

5.1 Rule 鍏冩暟鎹?
姣忔潯瑙勫垯閮藉簲鍏峰浠ヤ笅灞炴€э細
    鈥?rule_code
    鈥?rule_name
    鈥?category
    鈥?description
    鈥?severity
    鈥?enabled
    鈥?version
    鈥?score_weight
    鈥?parameter_set
    鈥?evidence_template
渚嬪锛?
rule_code: RAPID_WITHDRAW_AFTER_DEPOSIT
category: TRANSACTION
severity: HIGH
version: 3
score_weight: 25

5.2 鏍稿績鎺ュ彛
寤鸿鏍稿績鎺ュ彛闈炲父绠€鍗曪細
public interface FraudRule {
    String ruleCode();
    RuleCategory category();
    RuleSeverity severity();
    RuleEvaluationResult evaluate(RuleEvaluationContext context);
}

5.3 RuleEvaluationContext
杩欐槸瑙勫垯鐨勭粺涓€杈撳叆銆?
寤鸿鍖呮嫭锛?
    鈥?accountId
    鈥?AccountFeatureSnapshot
    鈥?Optional<RecentEventContext>
    鈥?Optional<GraphSignalContext>
    鈥?asOfTime
    鈥?featureVersion
    鈥?ruleConfigMap
渚嬪锛?
public record RuleEvaluationContext(
    String accountId,
    AccountFeatureSnapshot features,
    RecentEventContext recentEventContext,
    GraphSignalContext graphContext,
    Instant asOfTime,
    int featureVersion,
    Map<String, RuleConfig> ruleConfigs
) {}

5.4 RuleEvaluationResult
蹇呴』鑳借〃杈撅細
    鈥?鏄惁鍛戒腑
    鈥?鍛戒腑鍘熷洜
    鈥?鍒嗗€?
    鈥?璇佹嵁
    鈥?浣跨敤浜嗗摢浜涘瓧娈?
寤鸿璁捐锛?
public record RuleEvaluationResult(
    String ruleCode,
    boolean hit,
    RuleSeverity severity,
    int score,
    String reasonCode,
    String message,
    Map<String, Object> evidence,
    int ruleVersion
) {}

6. 瑙勫垯鎵ц妯″瀷
寤鸿閲囩敤锛?
6.1 Rule Registry
璐熻矗娉ㄥ唽瑙勫垯瀹炰緥锛?
Map<String, FraudRule>

6.2 Rule Engine
璐熻矗锛?
    鈥?鑾峰彇鍚敤瑙勫垯
    鈥?椤哄簭鎵ц
    鈥?鏀堕泦缁撴灉
    鈥?鑱氬悎鍒嗘暟

6.3 Rule Result Aggregator
杈撳嚭锛?
    鈥?ruleHits
    鈥?ruleScore
    鈥?reasonCodes
    鈥?evidenceSummary

7. 瑙勫垯鍒嗙被璁捐

7.1 Login Rules
渚嬪瓙锛?
    鈥?HIGH_RISK_IP_LOGIN
    鈥?VPN_LOGIN_SPIKE
    鈥?IMPOSSIBLE_TRAVEL
    鈥?LOGIN_FAILURE_BURST

7.2 Transaction Rules
渚嬪瓙锛?
    鈥?RAPID_WITHDRAW_AFTER_DEPOSIT
    鈥?HIGH_FREQUENCY_SMALL_AMOUNT
    鈥?REPEATED_AMOUNT_PATTERN
    鈥?REWARD_WITHDRAW_ABUSE

7.3 Device Rules
渚嬪瓙锛?
    鈥?SHARED_DEVICE_CLUSTER
    鈥?NEW_DEVICE_LOGIN_AFTER_DORMANCY
    鈥?DEVICE_SWITCH_SPIKE

7.4 Security Rules
渚嬪瓙锛?
    鈥?RAPID_PROFILE_CHANGE
    鈥?SECURITY_CHANGE_BEFORE_WITHDRAW
    鈥?PASSWORD_RESET_SPIKE

7.5 Graph Rules
渚嬪瓙锛?
    鈥?HIGH_RISK_NEIGHBOR_CLUSTER
    鈥?COLLECTOR_ACCOUNT_PATTERN
    鈥?SHARED_BANK_ACCOUNT_CLUSTER

7.6 Composite Rules
缁勫悎瑙勫垯鏇存湁浠峰€笺€?
渚嬪锛?
ATO 缁勫悎瑙勫垯
    鈥?楂橀闄㊣P鐧诲綍
    鈥?鏂拌澶囩櫥褰?
    鈥?鏀瑰瘑
    鈥?鎻愮幇
缇婃瘺鍏氱粍鍚堣鍒?
    鈥?鏂拌处鎴?
    鈥?鍏变韩璁惧
    鈥?濂栧姳浜ゆ槗
    鈥?蹇€熸彁鐜?

8. 閰嶇疆鍖栬璁?
杩欐槸鐢熶骇閲岄潪甯稿叧閿殑涓€閮ㄥ垎銆?

8.1 涓嶅缓璁妸瑙勫垯閫昏緫瀹屽叏瀛樻暟鎹簱
姣斿瀛樻垚琛ㄨ揪寮忚瑷€鍐嶈繍琛岋紝杩欐牱浼氾細
    鈥?璋冭瘯闅?
    鈥?绫诲瀷涓嶅畨鍏?
    鈥?鍑洪棶棰橀毦鎺掓煡

8.2 鎺ㄨ崘鍋氭硶锛氫唬鐮佷腑瀛橀€昏緫锛屾暟鎹簱涓瓨鍙傛暟
渚嬪瑙勫垯绫讳腑鍐欓€昏緫锛?
if (features.withdrawAfterDepositDelayAvg24h() <= config.maxDelayMinutes()
    && features.depositCount24h() > 0
    && features.withdrawCount24h() > 0) {
    ...
}
鏁版嵁搴撲腑瀛橈細
    鈥?maxDelayMinutes = 30
    鈥?score = 25
    鈥?enabled = true
杩欐牱灏辨棦瀹夊叏鍙堢伒娲汇€?

9. 瑙勫垯閰嶇疆琛ㄨ璁?
寤鸿鑷冲皯 3 寮犺〃銆?

9.1 rule_definition
瀹氫箟瑙勫垯涓讳俊鎭€?
CREATE TABLE rule_definition (
    rule_code            VARCHAR(128) PRIMARY KEY,
    rule_name            VARCHAR(256) NOT NULL,
    category             VARCHAR(64) NOT NULL,
    description          TEXT NOT NULL,
    severity             VARCHAR(32) NOT NULL,
    owner_module         VARCHAR(64) NOT NULL,
    current_version      INT NOT NULL,
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP NOT NULL
);

9.2 rule_version
璁板綍鐗堟湰銆?
CREATE TABLE rule_version (
    rule_code            VARCHAR(128) NOT NULL,
    version              INT NOT NULL,
    parameter_json       TEXT NOT NULL,
    score_weight         INT NOT NULL,
    enabled              BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from       TIMESTAMP NOT NULL,
    effective_to         TIMESTAMP,
    created_at           TIMESTAMP NOT NULL,
    PRIMARY KEY(rule_code, version)
);
璇存槑锛?
    鈥?parameter_json 瀛橀槇鍊?
    鈥?effective_from / to 鏀寔鐢熸晥绐楀彛
    鈥?鍥炴函妗堜欢鏃跺彲鐭ラ亾褰撴椂鐢ㄥ摢涓増鏈?

9.3 rule_hit_log
璁板綍鍛戒腑鎯呭喌銆?
CREATE TABLE rule_hit_log (
    hit_id               BIGSERIAL PRIMARY KEY,
    account_id           VARCHAR(64) NOT NULL,
    rule_code            VARCHAR(128) NOT NULL,
    rule_version         INT NOT NULL,
    hit_time             TIMESTAMP NOT NULL,
    score                INT NOT NULL,
    reason_code          VARCHAR(128) NOT NULL,
    evidence_json        TEXT,
    feature_version      INT NOT NULL
);
杩欐槸瀹¤鏍稿績琛ㄣ€?

10. 鐑姞杞借璁?
鐢熶骇閲屼綘浼氶渶瑕佷慨鏀癸細
    鈥?闃堝€?
    鈥?score weight
    鈥?enabled/disabled
浣嗕笉涓€瀹氭瘡娆￠兘鍙戠増銆?
鎵€浠ラ渶瑕佲€滅儹鍔犺浇閰嶇疆鈥濓紝浣嗕笉鏄€滅儹鍔犺浇 Java 浠ｇ爜鈥濄€?

10.1 鎺ㄨ崘鏂规
    鈥?瑙勫垯閫昏緫鍦ㄤ唬鐮侀噷
    鈥?鍙傛暟鍦?DB
    鈥?瀹氭椂鍒锋柊閰嶇疆缂撳瓨
渚嬪姣?30 绉掓垨 1 鍒嗛挓鍒锋柊涓€娆?rule_version

10.2 RuleConfigService
鎻愪緵锛?
    鈥?getConfig(ruleCode)
    鈥?reload()
    鈥?getActiveConfigs(asOfTime)

10.3 缂撳瓨寤鸿
鍙互鍏堢敤 JVM 鍐呭瓨缂撳瓨锛屼笉涓€瀹氫笂 Redis銆?

11. 瑙勫垯鐗堟湰绠＄悊
11.1 涓轰粈涔堝繀椤荤増鏈寲
濡傛灉鏌愭潯瑙勫垯浠庯細
    鈥?delay <= 60 minutes
鏀规垚锛?
    鈥?delay <= 30 minutes
閭ｅ巻鍙叉浠惰В閲婁細瀹屽叏涓嶅悓銆?

11.2 鐗堟湰鍘熷垯
浠ヤ笅鍦烘櫙蹇呴』鍗囩増鏈細
    鈥?闃堝€煎彉鍖?
    鈥?浣跨敤鐗瑰緛鍙樺寲
    鈥?reason code 鍙樺寲
    鈥?score weight 鍙樺寲
    鈥?鍛戒腑閫昏緫鍙樺寲

12. 瑙勫垯杈撳嚭璁捐
Rule Engine 鏈€缁堝簲杈撳嚭涓€涓仛鍚堢粨鏋滃璞°€?
寤鸿锛?
public record RuleEngineResult(
    String accountId,
    int totalRuleScore,
    List<RuleEvaluationResult> hits,
    List<String> reasonCodes,
    Map<String, Object> summaryEvidence
) {}
杩欎釜瀵硅薄鐩存帴缁欙細
    鈥?fraud-risk
    鈥?fraud-case
    鈥?fraud-ai

13. 瑙勫垯鍒嗘暟璁捐
瑙勫垯鍒嗘暟鏈€濂芥槸绂绘暎涓旂ǔ瀹氱殑锛屼笉瑕佹悶澶嶆潅妯″瀷鍖栥€?
寤鸿锛?
Severity	Score Range
LOW	5鈥?0
MEDIUM	10鈥?0
HIGH	20鈥?0
CRITICAL	40鈥?0
鍗曟潯瑙勫垯涓嶈鐩存帴鎷夋弧 100銆?
鏈€缁堢敱 fraud-risk 缁熶竴铻嶅悎銆?

14. 缁勫悎瑙勫垯璁捐
鐢熶骇閲屼环鍊兼渶楂樼殑寰€寰€涓嶆槸鍗曡鍒欙紝鑰屾槸缁勫悎瑙勫垯銆?

14.1 缁勫悎瑙勫垯瀹炵幇鏂瑰紡
涓嶈鐢ㄨ鍒欎簰鐩歌皟鐢ㄣ€?
寤鸿鐢ㄥ崟鐙殑 composite rule锛?
渚嬪锛?
AtoSuspicionRule
鍐呴儴鍒ゆ柇锛?
    鈥?new_device_login_count_7d > 0
    鈥?high_risk_ip_login_count_24h > 0
    鈥?security_change_before_withdraw_flag_24h = true
鍛戒腑鍚庣粰涓€涓洿楂樹环鍊?reason code銆?

14.2 濂藉
    鈥?鍙В閲?
    鈥?涓嶄細褰㈡垚澶嶆潅渚濊禆鍥?
    鈥?鏇撮€傚悎瀹¤

15. 涓?Feature Store 鐨勫叧绯?
Rule Engine 搴斿敖閲忓彧渚濊禆锛?
    鈥?AccountFeatureSnapshot
    鈥?灏戦噺浜嬩欢涓婁笅鏂?
    鈥?鍥句俊鍙?
涓嶈璁╄鍒欒嚜宸卞啀鍘诲ぇ閲忔煡鍘熷浜嬩欢琛ㄩ噸绠楅€昏緫銆?
鍚﹀垯锛?
    鈥?鎬ц兘涓嶇ǔ瀹?
    鈥?鍙ｅ緞婕傜Щ
    鈥?闅句互瑙ｉ噴
鎺ㄨ崘鍘熷垯锛?
瑙勫垯浼樺厛鐢?Feature Store锛屽皯閲忓疄鏃朵簨浠朵綔琛ュ厖銆?

16. 涓?Risk Engine 鐨勫叧绯?
Rule Engine 涓嶈礋璐ｆ渶缁堝喅绛栵紝鍙礋璐ｏ細
    鈥?瑙勫垯鍛戒腑
    鈥?瑙勫垯鍒?
    鈥?鐞嗙敱
    鈥?璇佹嵁
Risk Engine 鍐嶈瀺鍚堬細
    鈥?rule score
    鈥?graph score
    鈥?anomaly score
    鈥?behavior score

17. 涓?AI 璋冩煡鎶ュ憡鐨勫叧绯?
AI 鎶ュ憡鏈€閫傚悎寮曠敤锛?
    鈥?rule hits
    鈥?rule evidence
    鈥?reason codes
渚嬪锛?
    璐︽埛鍛戒腑浜嗏€滃叡浜澶囬泦缇も€濆拰鈥滃叆閲戝悗蹇€熸彁鐜扳€濅袱鏉￠珮椋庨櫓瑙勫垯锛屼笖鍏变韩璁惧鍏宠仈 12 涓处鎴凤紝鍏ラ噾鍚庡钩鍧?18 鍒嗛挓鍙戣捣鎻愮幇銆?
鎵€浠?Rule Engine 杈撳嚭蹇呴』瀵?AI 鍙嬪ソ銆?

18. Phase-1 涓?Phase-2 鐨勫樊寮?

18.1 Phase-1锛堢绾匡級
Rule Engine 澶勭悊瀵硅薄鏄細
    鈥?feature snapshot
    鈥?batch account list
璋冪敤鏂瑰紡锛?
accounts batch
   鈫?
feature snapshot
   鈫?
rule engine
   鈫?
rule_hit_log

18.2 Phase-2锛堝疄鏃讹級
Rule Engine 澧炲姞涓€涓叆鍙ｏ細
    鈥?event-triggered evaluation
渚嬪鏀跺埌鎻愮幇浜嬩欢鏃讹細
    鈥?璇诲彇鏈€鏂?snapshot
    鈥?鍔犲叆褰撳墠浜嬩欢 context
    鈥?璇勪及閮ㄥ垎楂樹环鍊煎疄鏃惰鍒?
杩欐椂鍙互鏀寔涓ょ璇勪及妯″紡锛?
    鈥?BATCH
    鈥?REALTIME

19. 鎺ㄨ崘 Java 鍖呯粨鏋?
fraud-rule-engine
鈹溾攢鈹€ model
鈹?  鈹溾攢鈹€ RuleCategory.java
鈹?  鈹溾攢鈹€ RuleSeverity.java
鈹?  鈹溾攢鈹€ RuleEvaluationContext.java
鈹?  鈹溾攢鈹€ RuleEvaluationResult.java
鈹?  鈹斺攢鈹€ RuleEngineResult.java
鈹溾攢鈹€ api
鈹?  鈹斺攢鈹€ FraudRule.java
鈹溾攢鈹€ engine
鈹?  鈹溾攢鈹€ RuleEngine.java
鈹?  鈹溾攢鈹€ RuleRegistry.java
鈹?  鈹斺攢鈹€ RuleResultAggregator.java
鈹溾攢鈹€ config
鈹?  鈹溾攢鈹€ RuleConfig.java
鈹?  鈹溾攢鈹€ RuleConfigService.java
鈹?  鈹斺攢鈹€ RuleVersionService.java
鈹溾攢鈹€ repository
鈹?  鈹溾攢鈹€ RuleDefinitionRepository.java
鈹?  鈹溾攢鈹€ RuleVersionRepository.java
鈹?  鈹斺攢鈹€ RuleHitLogRepository.java
鈹斺攢鈹€ service
    鈹斺攢鈹€ RuleEvaluationService.java
fraud-rule-engine
鈹溾攢鈹€ login
鈹?  鈹溾攢鈹€ HighRiskIpLoginRule.java
鈹?  鈹溾攢鈹€ LoginFailureBurstRule.java
鈹?  鈹斺攢鈹€ ImpossibleTravelRule.java
鈹溾攢鈹€ transaction
鈹?  鈹溾攢鈹€ RapidWithdrawAfterDepositRule.java
鈹?  鈹溾攢鈹€ HighFrequencySmallAmountRule.java
鈹?  鈹斺攢鈹€ RewardWithdrawAbuseRule.java
鈹溾攢鈹€ device
鈹?  鈹溾攢鈹€ SharedDeviceClusterRule.java
鈹?  鈹斺攢鈹€ DeviceSwitchSpikeRule.java
鈹溾攢鈹€ security
鈹?  鈹溾攢鈹€ RapidProfileChangeRule.java
鈹?  鈹斺攢鈹€ SecurityChangeBeforeWithdrawRule.java
鈹溾攢鈹€ graph
鈹?  鈹溾攢鈹€ HighRiskNeighborClusterRule.java
鈹?  鈹斺攢鈹€ CollectorAccountRule.java
鈹斺攢鈹€ composite
    鈹溾攢鈹€ AtoSuspicionRule.java
    鈹斺攢鈹€ PromotionAbuseCompositeRule.java

20. 鎺ㄨ崘绗竴鎵逛笂绾胯鍒?
浣犵涓€鐗堜笉闇€瑕?100 鏉¤鍒欍€?
寤鸿鍏堜笂 12 鏉￠珮浠峰€艰鍒欙細
    1. HIGH_RISK_IP_LOGIN
    2. VPN_LOGIN_SPIKE
    3. LOGIN_FAILURE_BURST
    4. RAPID_WITHDRAW_AFTER_DEPOSIT
    5. HIGH_FREQUENCY_SMALL_AMOUNT
    6. REWARD_WITHDRAW_ABUSE
    7. SHARED_DEVICE_CLUSTER
    8. DEVICE_SWITCH_SPIKE
    9. RAPID_PROFILE_CHANGE
    10. SECURITY_CHANGE_BEFORE_WITHDRAW
    11. HIGH_RISK_NEIGHBOR_CLUSTER
    12. ATO_SUSPICION_COMPOSITE
杩欐壒宸茬粡瓒冲鏋勬垚寮烘湁鍔涚殑绗竴鐗堢郴缁熴€?

21. 鏁版嵁琛ㄦ渶灏忛泦
濡傛灉浣犺涓€涓渶灏忎絾姝ｇ‘鐨勮惤鍦版柟妗堬紝Rule Engine 鑷冲皯闇€瑕侊細
    鈥?rule_definition
    鈥?rule_version
    鈥?rule_hit_log
杩?3 寮犺〃灏卞璧锋銆?

22. 鏈€缁堣惤鍦板缓璁?
浣犵殑 Rule Engine 绗竴鐗堟渶绋崇殑瀹炵幇鏂瑰紡鏄細
鈥淛ava 绫诲疄鐜拌鍒欓€昏緫 + PostgreSQL 瀛樿鍒欏厓鏁版嵁涓庡弬鏁?+ JVM 鍐呯紦瀛樼儹鍔犺浇 + 瑙勫垯鍛戒腑鏃ュ織瀹¤鈥?
瀹冨吋椤句簡锛?
    鈥?鍙В閲?
    鈥?鍙璁?
    鈥?鍙儹鏇存柊
    鈥?浣庝緷璧?
    鈥?閫傚悎 Spring JDBC
    鈥?鑳戒粠绂荤嚎骞虫粦婕旇繘鍒板疄鏃?
涓嬩竴姝ユ渶閫傚悎缁х画鍋氱殑鏄袱浠藉伐绋嬪寲浜х墿锛?
    1. rule_engine_schema.sql
    2. fraud-rule-engine 妯″潡 AI 浠ｇ爜鐢熸垚 Prompt
涓嬩竴鏉℃垜鍙互鐩存帴鎶婅繖涓や唤瀹屾暣缁欎綘銆?

