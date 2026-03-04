package me.asu.ta.dto;

/**
 * 报价事件数据。统一的报价数据结构，用于表示行情事件。
 *
 * <p><b>字段说明</b>：</p>
 * <ul>
 *   <li>{@code symbolId} — 品种 ID（整数编码）
 *     <ul>
 *       <li>&gt; 0：特定品种的实时事件（online 风险系统）</li>
 *       <li>= 0：通用报价数据，symbol 由上下文管理（offline 分析）</li>
 *     </ul>
 *   </li>
 *   <li>{@code ts} — 报价时间戳（Unix epoch 毫秒）</li>
 *   <li>{@code mid} — 中间价（(bid + ask) / 2）</li>
 * </ul>
 *
 * <p><b>使用场景</b>：</p>
 * <ul>
 *   <li>Online：RiskLoop 从事件流中提取 QuoteEvent 处理风险</li>
 *   <li>Offline：FxReplayEngine 创建 symbolId=0 的 QuoteEvent 进行历史分析</li>
 * </ul>
 */
public record QuoteEvent(int symbolId, String symbol, long ts, double mid) implements Event {}