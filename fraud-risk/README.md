# fraud-risk

`fraud-risk` 鏄闄╄瘎鍒嗕笌鍐崇瓥缂栨帓妯″潡锛岃礋璐ｆ妸瑙勫垯寮曟搸銆佸浘璋便€丮L 鍜岃涓虹壒寰佷俊鍙疯仛鍚堜负缁熶竴椋庨櫓缁撴灉銆?
## Architecture Overview

妯″潡褰撳墠鍒嗕负浠ヤ笅鍑犲眰锛?
- `model`
  - 瀹氫箟椋庨櫓杈撳叆銆佹潈閲嶉厤缃€佽瘎鍒嗘媶瑙ｃ€佺粨鏋滃拰鎵逛换鍔℃ā鍨?- `scoring`
  - 鏄惧紡瀹炵幇琛屼负鍒嗚绠椼€佹潈閲嶉€夋嫨鍜屾渶缁堥闄╁垎鍏紡
- `classification`
  - 鏍规嵁鏈€缁堝垎璁＄畻椋庨櫓绛夌骇
- `reason`
  - 姹囨€诲苟鎺掑簭 top reason codes
- `repository`
  - 浣跨敤 Spring JDBC 鍜屾樉寮?SQL 璇诲啓 `risk_weight_profile`銆乣risk_score_result`銆乣risk_reason_mapping`銆乣risk_evaluation_job`
- `service`
  - 鎻愪緵鍗曡处鍙峰拰鎵归噺椋庨櫓璇勪及缂栨帓
- `job`
  - 鎻愪緵鎵归噺椋庨櫓璇勪及 runner

澶栭儴鎺ㄨ崘鍏ュ彛鏄?[RiskEngineFacade.java](/d:/03_projects/suk/asu-trading-analysis/fraud-risk/src/main/java/me/asu/ta/risk/service/RiskEngineFacade.java)銆?
## Input Signals

褰撳墠 `fraud-risk` 娑堣垂 4 绫讳俊鍙凤細

- 瑙勫垯淇″彿
  - 鏉ヨ嚜 [RuleEngineResult.java](/d:/03_projects/suk/asu-trading-analysis/fraud-rule-engine/src/main/java/me/asu/ta/rule/model/RuleEngineResult.java)
- 鍥捐氨淇″彿
  - 鏉ヨ嚜 [GraphRiskSignal.java](/d:/03_projects/suk/asu-trading-analysis/fraud-risk/src/main/java/me/asu/ta/risk/model/GraphRiskSignal.java)
- ML 淇″彿
  - 鏉ヨ嚜 [MlAnomalySignal.java](/d:/03_projects/suk/asu-trading-analysis/fraud-risk/src/main/java/me/asu/ta/risk/model/MlAnomalySignal.java)
- 琛屼负淇″彿
  - 鐢?[BehaviorScoreCalculator.java](/d:/03_projects/suk/asu-trading-analysis/fraud-risk/src/main/java/me/asu/ta/risk/scoring/BehaviorScoreCalculator.java) 鍩轰簬 [AccountFeatureSnapshot.java](/d:/03_projects/suk/asu-trading-analysis/fraud-feature/src/main/java/me/asu/ta/feature/model/AccountFeatureSnapshot.java) 璁＄畻

`RiskEvaluationService` 涔熸敮鎸?fallback锛?
- 鏈樉寮忎紶鍏?`GraphRiskSignal` 鏃讹紝浠?feature snapshot 鐨勫浘璋辩浉鍏冲瓧娈垫帹瀵?- 鏈樉寮忎紶鍏?`MlAnomalySignal` 鏃讹紝浠?`anomaly_score_last` 鎺ㄥ

## Scoring Formula

鏈€缁堥闄╁垎鐢?[RiskScoreCalculator.java](/d:/03_projects/suk/asu-trading-analysis/fraud-risk/src/main/java/me/asu/ta/risk/scoring/RiskScoreCalculator.java) 鏄惧紡璁＄畻锛屾墍鏈夊垎鏁伴兘鎸?`0-100` 澶勭悊骞舵渶缁堝皝椤跺埌 `100`銆?
榛樿鏀寔涓や釜 profile锛?
- `DEFAULT`
  - `0.40 * rule + 0.25 * graph + 0.20 * anomaly + 0.15 * behavior`
- `NO_ML`
  - `0.55 * rule + 0.30 * graph + 0.00 * anomaly + 0.15 * behavior`

鏉冮噸鏉ユ簮锛?
- 浼樺厛浠?`risk_weight_profile` 琛ㄨ鍙?- 璇讳笉鍒版椂鐢?[RiskWeightProfileService.java](/d:/03_projects/suk/asu-trading-analysis/fraud-risk/src/main/java/me/asu/ta/risk/scoring/RiskWeightProfileService.java) 浣跨敤浠ｇ爜鍐?fallback

## Behavior Score Logic

[BehaviorScoreCalculator.java](/d:/03_projects/suk/asu-trading-analysis/fraud-risk/src/main/java/me/asu/ta/risk/scoring/BehaviorScoreCalculator.java) 鐩墠浣跨敤浠ヤ笅鐗瑰緛瀛楁鍋氭樉寮忓姞鍒嗭細

- `login_failure_rate_24h > 0.8` -> `+15`
- `high_risk_ip_login_count_24h >= 1` -> `+20`
- `withdraw_after_deposit_delay_avg_24h > 0 && <= 30` -> `+20`
- `shared_device_accounts_7d >= 5` -> `+20`
- `security_change_before_withdraw_flag_24h = true` -> `+25`

鏈€缁堣涓哄垎锛?
- 灏侀《 `100`
- 鍚屾椂鐢熸垚琛屼负 evidence
- 鍚屾椂鐢熸垚琛屼负 reason codes锛屼究浜庢浠惰В閲?
## Risk Level Thresholds

[RiskLevelClassifier.java](/d:/03_projects/suk/asu-trading-analysis/fraud-risk/src/main/java/me/asu/ta/risk/classification/RiskLevelClassifier.java) 浣跨敤鍥哄畾闃堝€硷細

- `LOW`: `0-29`
- `MEDIUM`: `30-59`
- `HIGH`: `60-79`
- `CRITICAL`: `80-100`

## How ML Is Optional

ML 鍦ㄥ綋鍓嶈璁￠噷鏄彲閫夎緭鍏ワ紝涓嶆槸寮轰緷璧栵細

- 濡傛灉璋冪敤鏂逛紶鍏?`MlAnomalySignal`锛岄闄╄瘎浼颁紭鍏堜娇鐢ㄨ淇″彿
- 濡傛灉娌℃湁浼犲叆锛屼絾 feature snapshot 涓瓨鍦?`anomaly_score_last`锛屾湇鍔′細鎺ㄥ涓€涓畝鍖栫増 ML 淇″彿
- 濡傛灉涓よ€呴兘娌℃湁锛屽垯鑷姩鍒囨崲 `NO_ML` profile

杩欐剰鍛崇潃鍗充娇 ML 鏈嶅姟鏆傛椂鏈帴鍏ワ紝`fraud-risk` 涔熷彲浠ョ户缁伐浣溿€?
## Batch vs Realtime Mode

涓ょ妯″紡鍏变韩鍚屼竴濂楅闄╄瘎鍒嗛€昏緫锛屽樊寮備富瑕佸湪璋冪敤鍏ュ彛鍜屾墽琛屾柟寮忥細

- `REALTIME`
  - 鎺ㄨ崘浣跨敤 `RiskEngineFacade.evaluateAccount(...)`
  - 閫氬父鐢卞疄鏃朵簨浠舵垨鍦ㄧ嚎鍐崇瓥瑙﹀彂
- `BATCH`
  - 鎺ㄨ崘浣跨敤 `RiskEngineFacade.evaluateBatch(...)`
  - 鎵逛换鍔″叆鍙ｆ槸 [RiskEvaluationJobRunner.java](/d:/03_projects/suk/asu-trading-analysis/fraud-risk/src/main/java/me/asu/ta/risk/job/RiskEvaluationJobRunner.java)
  - 榛樿姣忔壒 `1000` 涓处鍙凤紝閬垮厤涓€娆℃€у姞杞藉叏閲忔暟鎹?
鏃犺鍝妯″紡锛岄兘浼氳緭鍑虹粺涓€鐨?[RiskScoreResult.java](/d:/03_projects/suk/asu-trading-analysis/fraud-risk/src/main/java/me/asu/ta/risk/model/RiskScoreResult.java)銆?
## How To Add a New Weight Profile

1. 鍦?`risk_weight_profile` 涓柊澧炰竴琛?profile 閰嶇疆銆?2. 纭繚鍥涗釜鏉冮噸瀛楁涔嬪拰绗﹀悎浣犱滑鐨勭瓥鐣ヨ姹傘€?3. 濡傛灉闇€瑕佷唬鐮佹樉寮忚瘑鍒繖涓?profile锛屽啀鎵╁睍 [RiskWeightProfileService.java](/d:/03_projects/suk/asu-trading-analysis/fraud-risk/src/main/java/me/asu/ta/risk/scoring/RiskWeightProfileService.java) 鐨勯€夋嫨閫昏緫銆?4. 鎵ц `mvn verify`銆?
濡傛灉 profile 鍙槸鍙傛暟鍙樺寲鑰屼笉鏄€夋嫨閫昏緫鍙樺寲锛岄€氬父涓嶉渶瑕佹敼 Java 浠ｇ爜銆?
## Initialization SQL

鍒濆鍖栬剼鏈綅浜庯細

- [01_risk_weight_profile_init.sql](/d:/03_projects/suk/asu-trading-analysis/fraud-risk/sql/01_risk_weight_profile_init.sql)
- [02_risk_reason_mapping_init.sql](/d:/03_projects/suk/asu-trading-analysis/fraud-risk/sql/02_risk_reason_mapping_init.sql)

