package org.peidevs.waro.actor;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

import org.peidevs.waro.config.ConfigInfo;

public class Tourney extends AbstractBehavior<Tourney.Command> {
    private static ConfigInfo configInfo;

    private static final String TRACER = "TRACER Tourney ";

    public static Behavior<Tourney.Command> create(ConfigInfo configInfo) {
        Tourney.configInfo = configInfo;
        return Behaviors.setup(Tourney::new);
    }

    private Tourney(ActorContext<Tourney.Command> context) {
        super(context);
    }

    public sealed interface Command permits BeginTourneyCommand, PlayGameAckEvent {}

    public static final class BeginTourneyCommand implements Command {
        final long tourneyRequestId;
        final int numGames;
        final ActorRef<Supervisor.Command> replyTo;

        public BeginTourneyCommand(long tourneyRequestId, int numGames,
                                   ActorRef<Supervisor.Command> replyTo) {
            this.tourneyRequestId = tourneyRequestId;
            this.numGames = numGames;
            this.replyTo = replyTo;
        }
    }

    public static final class PlayGameAckEvent implements Command {
        final long gameRequestId;

        public PlayGameAckEvent(long gameRequestId) {
            this.gameRequestId = gameRequestId;
        }
    }

    @Override
    public Receive<Tourney.Command> createReceive() {
        return newReceiveBuilder()
                   .onMessage(BeginTourneyCommand.class, this::onBeginTourneyCommand)
                   .onMessage(PlayGameAckEvent.class, this::onPlayGameAckEvent)
                   .onSignal(PostStop.class, signal -> onPostStop())
                   .build();
    }

    private Behavior<Tourney.Command> onBeginTourneyCommand(BeginTourneyCommand command) {
        var gameRequestId = 1L;

        ActorRef<Tourney.Command> self = getContext().getSelf();

        for (int gameIndex = 1; gameIndex <= command.numGames; gameIndex++) {
            ActorRef<Dealer.Command> dealer = getContext().spawn(Dealer.create(Tourney.configInfo), "dealer_" + gameIndex);
            var playGameCommand = new Dealer.PlayGameCommand(gameRequestId, self);
            dealer.tell(playGameCommand);

            gameRequestId++;

            getContext().getLog().info(TRACER + "sleeping... req: " + gameRequestId);
            try { Thread.sleep(3000); } catch (Exception ex) {}
        }

        // example of response
        command.replyTo.tell(new Supervisor.BeginTourneyAckEvent(command.tourneyRequestId));

        return this;
    }

    private Behavior<Tourney.Command> onPlayGameAckEvent(Tourney.PlayGameAckEvent command) {
        long gameRequestId = command.gameRequestId;
        getContext().getLog().info(TRACER + "received ACK for gameRequestId: {}", gameRequestId);
        return this;
    }

    private Behavior<Command> onPostStop() {
         getContext().getLog().info(TRACER + "STOPPED");
         return Behaviors.stopped();
    }
}
