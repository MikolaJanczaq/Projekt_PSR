package core.launcher.agent;

import core.launcher.common.config.CliArgs;
import core.launcher.common.util.AppLogger;

import java.util.logging.Logger;

public final class AgentMain {

    public static void main(String[] args) {
        CliArgs cli = CliArgs.parse(args);
        if (cli.hasFlag("help")) {
            printUsage();
            return;
        }

        AgentConfig config = AgentConfig.load(args);
        AppLogger.configure(config.logLevel());
        Logger log = AppLogger.getLogger(AgentMain.class);

        log.info(() -> String.format(
                "Starting PSR Launcher Agent (server=%s:%d, name=%s, maxTasks=%d)",
                config.serverHost(),
                config.serverPort(),
                config.nodeName(),
                config.maxConcurrentTasks()
        ));
    }

    private static void printUsage() {
        System.out.println("""
                Usage: java -jar launcher-agent.jar [options]
                  --config <file>             Path to external .properties configuration file
                  --server <host:port>        Server address and port (e.g. 192.168.1.10:54321)
                  --host <host>               Server IP address or hostname (default: 127.0.0.1)
                  --port <port>               Server TCP port (default: 54321)
                  --node-id <id>              Persistent node identifier (optional)
                  --name <name>               Human-readable execution node name
                  --max-tasks <N>             Maximum number of concurrent tasks (default: 4)
                  --charset <encoding>        Process output character encoding (e.g. CP852, UTF-8)
                  --token <secret>            Shared authentication token
                  --debug                     Enable verbose DEBUG logging
                  --help                      Display this help message
                """);
    }
}
