package core.launcher.server;

import core.launcher.common.config.CliArgs;
import core.launcher.common.util.AppLogger;

import java.util.logging.Logger;

public final class ServerMain {

    public static void main(String[] args) {
        CliArgs cli = CliArgs.parse(args);
        if (cli.hasFlag("help")) {
            printUsage();
            return;
        }

        ServerConfig config = ServerConfig.load(args);
        AppLogger.configure(config.logLevel());
        Logger log = AppLogger.getLogger(ServerMain.class);

        log.info(() -> String.format(
                "Starting PSR Launcher Server (bind=%s:%d, heartbeat=%dms, suspected=%dms, offline=%dms, resultsDir=%s)",
                config.bindHost(),
                config.port(),
                config.heartbeatIntervalMs(),
                config.suspectedTimeoutMs(),
                config.offlineTimeoutMs(),
                config.resultsDir()
        ));
    }

    private static void printUsage() {
        System.out.println("""
                Usage: java -jar launcher-server.jar [options]
                  --config <file>             Path to external .properties configuration file
                  --host <ip>                 TCP bind address (default: 0.0.0.0)
                  --port <port>               TCP listen port (default: 54321)
                  --heartbeat <duration>      Heartbeat interval, e.g. 3s or 3000ms
                  --default-timeout <dur>     Default task execution timeout, e.g. 60s
                  --results-dir <dir>         Directory for storing task results (default: results)
                  --token <secret>            Optional shared authentication token for agents
                  --debug                     Enable verbose DEBUG logging
                  --help                      Display this help message
                """);
    }
}
