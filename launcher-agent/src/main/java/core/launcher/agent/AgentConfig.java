package core.launcher.agent;

import core.launcher.common.config.CliArgs;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public record AgentConfig(
        String serverHost,
        int serverPort,
        String nodeId,
        String nodeName,
        int maxConcurrentTasks,
        long reconnectInitialDelayMs,
        long reconnectMaxDelayMs,
        String processCharset,
        String authToken,
        Set<String> allowedCommands,
        String logLevel
) {

    public AgentConfig {
        if (serverHost == null || serverHost.isBlank()) {
            throw new IllegalArgumentException("Server host address must not be blank");
        }
        if (serverPort <= 0 || serverPort > 65535) {
            throw new IllegalArgumentException("Server TCP port must be in range 1..65535, got: " + serverPort);
        }
        if (maxConcurrentTasks < 1) {
            throw new IllegalArgumentException("Max concurrent tasks must be at least 1, got: " + maxConcurrentTasks);
        }
    }

    public static AgentConfig load(String[] args) {
        CliArgs cli = CliArgs.parse(args);
        String customConfigPath = cli.get("config", null, null, null);
        Properties props = CliArgs.loadProperties("agent.properties", customConfigPath);
        return fromPropertiesAndArgs(props, cli);
    }

    public static AgentConfig fromPropertiesAndArgs(Properties props, CliArgs cli) {
        String host = cli.get("host", props, "agent.server.host", "127.0.0.1");
        int port = cli.getInt("port", props, "agent.server.port", 54321);

        String serverEndpoint = cli.get("server", null, null, null);
        if (serverEndpoint != null && !serverEndpoint.isBlank()) {
            int colonIdx = serverEndpoint.lastIndexOf(':');
            if (colonIdx > 0 && colonIdx < serverEndpoint.length() - 1) {
                host = serverEndpoint.substring(0, colonIdx).trim();
                port = Integer.parseInt(serverEndpoint.substring(colonIdx + 1).trim());
            } else {
                host = serverEndpoint.trim();
            }
        }

        String nodeId = cli.get("node-id", props, "agent.node.id", "");
        String rawName = cli.get("name", props, "agent.node.name", "");
        String nodeName = (rawName == null || rawName.isBlank()) ? resolveDefaultHostname() : rawName.trim();

        int defaultSlots = Math.max(2, Runtime.getRuntime().availableProcessors());
        int maxTasks = cli.getInt("max-tasks", props, "agent.max.tasks", defaultSlots);
        long reconnectInitialMs = cli.getDurationMs("reconnect-initial", props, "agent.reconnect.initial.ms", 1000L);
        long reconnectMaxMs = cli.getDurationMs("reconnect-max", props, "agent.reconnect.max.ms", 30_000L);
        String charset = cli.get("charset", props, "agent.process.charset", "");
        String authToken = cli.get("token", props, "agent.auth.token", "");

        String allowedRaw = cli.get("allowed-commands", props, "agent.allowed.commands", "");
        Set<String> allowedCommands = parseAllowedCommands(allowedRaw);

        String logLevel = cli.hasFlag("debug")
                ? "DEBUG"
                : cli.get("log-level", props, "agent.log.level", "INFO");

        return new AgentConfig(
                host,
                port,
                nodeId,
                nodeName,
                maxTasks,
                reconnectInitialMs,
                reconnectMaxMs,
                charset,
                authToken,
                allowedCommands,
                logLevel
        );
    }

    private static Set<String> parseAllowedCommands(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String resolveDefaultHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown-host";
        }
    }
}
