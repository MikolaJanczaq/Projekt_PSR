package core.launcher.common.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public final class CliArgs {

    private final Map<String, String> options;

    private CliArgs(Map<String, String> options) {
        this.options = Collections.unmodifiableMap(options);
    }

    public static CliArgs parse(String[] args) {
        Map<String, String> parsed = new LinkedHashMap<>();
        if (args == null) {
            return new CliArgs(parsed);
        }
        for (int i = 0; i < args.length; i++) {
            String token = args[i];
            if (token == null || !token.startsWith("--")) {
                continue;
            }
            String body = token.substring(2);
            int eqIdx = body.indexOf('=');
            if (eqIdx > 0) {
                String key = body.substring(0, eqIdx).trim();
                String val = body.substring(eqIdx + 1).trim();
                parsed.put(key, val);
            } else {
                String key = body.trim();
                if (i + 1 < args.length && args[i + 1] != null && !args[i + 1].startsWith("--")) {
                    parsed.put(key, args[++i].trim());
                } else {
                    parsed.put(key, "true");
                }
            }
        }
        return new CliArgs(parsed);
    }

    public boolean hasFlag(String key) {
        return options.containsKey(key);
    }

    public String get(String cliKey, Properties props, String propKey, String defaultValue) {
        if (options.containsKey(cliKey)) {
            return options.get(cliKey);
        }
        if (props != null && propKey != null && props.getProperty(propKey) != null) {
            return props.getProperty(propKey).trim();
        }
        return defaultValue;
    }

    public int getInt(String cliKey, Properties props, String propKey, int defaultValue) {
        String raw = get(cliKey, props, propKey, null);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid integer value for '" + cliKey + "/" + propKey + "': " + raw, e);
        }
    }

    public long getDurationMs(String cliKey, Properties props, String propKey, long defaultValueMs) {
        String raw = get(cliKey, props, propKey, null);
        if (raw == null || raw.isBlank()) {
            return defaultValueMs;
        }
        return parseDurationMs(raw);
    }

    public static long parseDurationMs(String text) {
        String s = text.trim().toLowerCase();
        try {
            if (s.endsWith("ms")) {
                return Long.parseLong(s.substring(0, s.length() - 2).trim());
            }
            if (s.endsWith("s")) {
                return Long.parseLong(s.substring(0, s.length() - 1).trim()) * 1000L;
            }
            if (s.endsWith("m")) {
                return Long.parseLong(s.substring(0, s.length() - 1).trim()) * 60_000L;
            }
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid duration format: '" + text + "' (expected e.g. 3000, 3s, 500ms, 1m)", e);
        }
    }

    public static Properties loadProperties(String resourceName, String externalFilePath) {
        Properties props = new Properties();
        if (resourceName != null && !resourceName.isBlank()) {
            try (InputStream is = CliArgs.class.getClassLoader().getResourceAsStream(resourceName)) {
                if (is != null) {
                    props.load(is);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load classpath resource: " + resourceName, e);
            }
        }
        if (externalFilePath != null && !externalFilePath.isBlank()) {
            Path path = Path.of(externalFilePath);
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("Configuration file does not exist: " + path.toAbsolutePath());
            }
            try (InputStream fis = new FileInputStream(path.toFile())) {
                props.load(fis);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read configuration file: " + path, e);
            }
        }
        return props;
    }
}
