package org.peidevs.waro.actor;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

import java.util.*;
import java.util.stream.Stream;

import org.peidevs.waro.util.Timer;
import org.peidevs.waro.config.ConfigInfo;

public class Supervisor extends AbstractBehavior<Supervisor.Command> {
    private static ConfigInfo configInfo;

    private static final String TRACER = "TRACER Supervisor ";

    public static Behavior<Supervisor.Command> create(ConfigInfo configInfo) {
        Supervisor.configInfo = configInfo;
        return Behaviors.setup(Supervisor::new);
    }

    private Supervisor(ActorContext<Supervisor.Command> context) {
        super(context);
    }

    public sealed interface Command permits StartCommand, BeginTourneyAckEvent {}

    // this is a "prime the pump" command and not request/response idiom
    public static final class StartCommand implements Command {
        final long requestId;

        public StartCommand(long requestId) {
            this.requestId = requestId;
        }
    }

    public static final class BeginTourneyAckEvent implements Command {
        final long tourneyRequestId;

        public BeginTourneyAckEvent(long tourneyRequestId) {
            this.tourneyRequestId = tourneyRequestId;
        }
    }

    @Override
    public Receive<Supervisor.Command> createReceive() {
        return newReceiveBuilder()
                   .onMessage(StartCommand.class, this::onStartCommand)
                   .onMessage(BeginTourneyAckEvent.class, this::onBeginTourneyAckEvent)
                   .onSignal(PostStop.class, signal -> onPostStop())
                   .build();
    }

    private Behavior<Supervisor.Command> onStartCommand(StartCommand command) {
        try {
            var timer = new Timer();

            // create tourney
            ActorRef<Tourney.Command> worker = getContext().spawn(Tourney.create(Supervisor.configInfo), "tourney");

            // assign blocks to workers
            long tourneyRequestId = 6160;
            ActorRef<Supervisor.Command> self = getContext().getSelf();
            int numGames = configInfo.numGames();
            var beginTourneyCommand = new Tourney.BeginTourneyCommand(tourneyRequestId, numGames, self);
            worker.tell(beginTourneyCommand);

            getContext().getLog().info(TRACER + "{}", timer.getElapsed("onStartCommand"));

        } catch (Exception ex) {
            getContext().getLog().error(TRACER + "caught exception! ex: {}", ex.getMessage());
        }

        return this;
    }

    private Behavior<Supervisor.Command> onBeginTourneyAckEvent(Supervisor.BeginTourneyAckEvent command) {
        long tourneyRequestId = command.tourneyRequestId;
        getContext().getLog().info(TRACER + "received ACK for tourneyRequestId: {}", tourneyRequestId);
        return this;
    }

    private Behavior<Command> onPostStop() {
         getContext().getLog().info(TRACER + "STOPPED");
         return Behaviors.stopped();
    }
}
