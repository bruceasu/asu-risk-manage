package me.asu.ta.util;
import java.util.HashMap;
import java.util.Map;

public class SimpleCli {
    Map<String, String> args = new HashMap<>();

    public SimpleCli put(String k, String v) {
        args.put(k, v);
        return this;
    }

    public String get(String k) {
        return args.get(k);
    }

    public String get(String k, String d) {
        String v = args.get(k);
        return v == null ? d : v;
    }

    public String require(String key) {
        String v = args.get(key);
        if (v == null || v.isBlank())
            throw new IllegalArgumentException("Missing arg: " + key);
        return v;
    }

   public int intv(String k, int d) {
        if (!args.containsKey(k))
            return d;
        try {
            return Integer.parseInt(args.get(k).trim());
        } catch (Exception e) {
            return d;
        }
    }

    public boolean hasHelp() {
        return args.containsKey("--help") || args.containsKey("-h");
    }

    public boolean has(String key) {
        return args.containsKey(key);
    }

    public boolean isTrue(String key) {
        String v = args.get(key);
        return v != null && (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes")
                || v.equalsIgnoreCase("1"));
    }

    public long longv(String k, long d) {
        if (!args.containsKey(k))
            return d;
        try {
            return Long.parseLong(args.get(k).trim());
        } catch (Exception e) {
            return d;
        }
    }

    public static SimpleCli parseArgs(String[] args) {
        SimpleCli cli = new SimpleCli();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--")) {
                String v = (i + 1 < args.length && !args[i + 1].startsWith("--")) ? args[++i]
                        : "true";
                cli.put(a, v);
            }
        }
        return cli;
    }
}