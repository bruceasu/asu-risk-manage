package me.asu.ta;
import java.util.*;
import me.asu.ta.dto.EventText;

/**
 * 客户端指纹分析器
 * 追踪客户端类型、版本、登录名、IP、备注等信息
 * 用于检测账户共享、批量机器人、异常客户端
 */
public final class ClientFingerprintAnalyzer {
    private final Set<String> clientTypes = new HashSet<>(4);
    private final Set<String> clientVersions = new HashSet<>(8);
    private final Set<String> loginNames = new HashSet<>(4);
    private final Set<String> ips = new HashSet<>(8);
    private String primaryIp = null;
    private int primaryIpCount = 0;
    private int totalOrders = 0;
    private boolean hasBotKeyword = false;
    
    /**
     * 添加一笔订单的客户端信息
     */
    public void add(EventText et) {
        if (et == null) return;
        
        totalOrders++;
        
        if (et.clientType != null) {
            clientTypes.add(et.clientType);
        }
        
        if (et.clientVersion != null) {
            clientVersions.add(et.clientVersion);
        }
        
        if (et.loginName != null) {
            loginNames.add(et.loginName);
        }
        
        if (et.ip != null) {
            ips.add(et.ip);
            // 更新主要 IP
            if (primaryIp == null) {
                primaryIp = et.ip;
                primaryIpCount = 1;
            } else if (primaryIp.equals(et.ip)) {
                primaryIpCount++;
            }
        }
        
        if (et.containsBotKeyword()) {
            hasBotKeyword = true;
        }
    }
    
    /**
     * 使用的客户端类型数量
     * 可疑阈值：> 3（频繁切换客户端）
     */
    public int uniqueClientTypes() {
        return clientTypes.size();
    }
    
    /**
     * 使用的客户端版本数量
     * 可疑阈值：> 5
     */
    public int uniqueClientVersions() {
        return clientVersions.size();
    }
    
    /**
     * 使用的登录名数量
     * 可疑阈值：> 1（账户共享）
     */
    public int uniqueLoginNames() {
        return loginNames.size();
    }
    
    /**
     * 使用的唯一 IP 数量
     * 可疑阈值：> 10（频繁切换 IP）
     */
    public int uniqueIps() {
        return ips.size();
    }
    
    /**
     * 主要 IP（使用最多的 IP）
     */
    public String primaryIp() {
        return primaryIp;
    }
    
    /**
     * 主要 IP 的订单占比
     * 可疑模式：< 0.5（IP 分散）
     */
    public double primaryIpRatio() {
        return totalOrders > 0 ? primaryIpCount / (double) totalOrders : 0;
    }
    
    /**
     * IP 切换率（平均每单切换 IP 的频率）
     */
    public double ipSwitchRate() {
        return totalOrders > 0 ? ips.size() / (double) totalOrders : 0;
    }
    
    /**
     * 备注中是否包含机器人关键词
     */
    public boolean hasBotKeyword() {
        return hasBotKeyword;
    }
    
    /**
     * 获取所有使用的 IP 列表
     */
    public Set<String> getAllIps() {
        return new HashSet<>(ips);
    }
}