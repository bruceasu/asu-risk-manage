package me.asu.ta.util;
import java.util.HashMap;
import java.util.Map;

public class SymbolMapper {
    private final Map<Integer, String> idToName = new HashMap<>();
    private final Map<String, Integer> nameToId = new HashMap<>();
    
    public SymbolMapper() {
        // TODO: initialization can be done here or via a separate method
    }
    public void register(int id, String name) {
        idToName.put(id, name);
        nameToId.put(name, id);
    }
    
   public boolean contains(int id) {
        return idToName.containsKey(id);
    }
    
    public String getName(int id) {
        return idToName.getOrDefault(id, "UNKNOWN:"+id);
    }
    
    public int getId(String name) {
        return nameToId.getOrDefault(name, 0);
    }
    
    // 示例：初始化常见货币对
    // static SymbolMapper createDefault() {
    //     SymbolMapper mapper = new SymbolMapper();
    //     mapper.register(1, "EURUSD");
    //     mapper.register(2, "GBPUSD");
    //     mapper.register(3, "USDJPY");
    //     mapper.register(4, "USDCHF");
    //     // ... 其他货币对
    //     return mapper;
    // }
}