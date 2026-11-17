package core.launcher.server;

import core.launcher.common.config.CliArgs;

import java.nio.file.Path;
import java.util.Properties;

public record ServerConfig(
        String bindHost,
        int port,
        long heartbeatIntervalMs,
        int suspectedMultiplier,
        int offlineMultiplier,
        long defaultTaskTimeoutMs,
        int maxOutputBytes,
        Path resultsDir,
        Path walFile,
        String authToken,
        String logLevel
) {

    public static final int DEFAULT_PORT = 54321;

    public ServerConfig {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("TCP port must be in range 1..65535, got: " + port);
        }
        if (heartbeatIntervalMs < 200) {
            throw new IllegalArgumentException("Heartbeat interval must be at least 200 ms, got: " + heartbeatIntervalMs);
        }
        if (suspectedMultiplier < 1 || offlineMultiplier <= suspectedMultiplier) {
            throw new IllegalArgumentException("Failure detection multipliers must satisfy 1 <= suspected < offline");
        }
    }

    public static ServerConfig load(String[] args) {
        CliArgs cli = CliArgs.parse(args);
        String customConfigPath = cli.get("config", null, null, null);
        Properties props = CliArgs.loadProperties("server.properties", customConfigPath);
        return fromPropertiesAndArgs(props, cli);
    }

    public static ServerConfig fromPropertiesAndArgs(Properties props, CliArgs cli) {
        String host = cli.get("host", props, "server.host", "0.0.0.0");
        int port = cli.getInt("port", props, "server.port", DEFAULT_PORT);
        long hbIntervalMs = cli.getDurationMs("heartbeat", props, "server.heartbeat.interval.ms", 3000L);
        int suspectedMult = cli.getInt("suspected-mult", props, "server.heartbeat.suspected.multiplier", 2);
        int offlineMult = cli.getInt("offline-mult", props, "server.heartbeat.offline.multiplier", 4);
        long defaultTimeoutMs = cli.getDurationMs("default-timeout", props, "server.task.default.timeout.ms", 60_000L);
        int maxOutputBytes = cli.getInt("max-output-bytes", props, "server.task.max.output.bytes", 1_048_576);
        String resultsDirStr = cli.get("results-dir", props, "server.results.dir", "results");
        String walFileStr = cli.get("wal-file", props, "server.wal.file", "data/tasks-wal.jsonl");
        String authToken = cli.get("token", props, "server.auth.token", "");
        String logLevel = cli.hasFlag("debug")
                ? "DEBUG"
                : cli.get("log-level", props, "server.log.level", "INFO");

        return new ServerConfig(
                host,
                port,
                hbIntervalMs,
                suspectedMult,
                offlineMult,
                defaultTimeoutMs,
                maxOutputBytes,
                Path.of(resultsDirStr),
                Path.of(walFileStr),
                authToken,
                logLevel
        );
    }

    public long suspectedTimeoutMs() {
        return heartbeatIntervalMs * suspectedMultiplier;
    }

    public long offlineTimeoutMs() {
        return heartbeatIntervalMs * offlineMultiplier;
    }
}
