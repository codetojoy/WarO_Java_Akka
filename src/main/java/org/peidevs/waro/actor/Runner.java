package org.peidevs.waro.actor;

import akka.actor.typed.ActorSystem;
import java.io.*;
import java.util.List;

import org.peidevs.waro.config.*;

public class Runner {
    public static void main(String[] args) {
        var configService = new ConfigService();
        var configInfo = configService.getConfigInfo();

        ActorSystem<Supervisor.Command> supervisor = ActorSystem.create(Supervisor.create(configInfo), "supervisor");
        long requestId = 5150;
        supervisor.tell(new Supervisor.StartCommand(requestId));

        try {
            promptForUserInput();
        } catch (Exception ignored) {
        } finally {
            supervisor.terminate();
        }
    }

    static void promptForUserInput() {
        try {
            System.out.println(">>> Press ENTER to exit <<<");
            System.in.read();
        } catch (Exception ex) {
        }
    }
}
