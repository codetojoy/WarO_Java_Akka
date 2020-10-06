package org.peidevs.waro.actor;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

import org.peidevs.waro.config.ConfigInfo;
import org.peidevs.waro.player.*;
import org.peidevs.waro.strategy.Strategy;
import org.peidevs.waro.table.Hand;

// Thoughts:
// - don't allow legacy Player object leak out of this (e.g. in a message)
// - don't reference ConfigInfo.players() in here
// - pass Hand, Strategy, state as necessary in messages?

public class PlayerActor extends AbstractBehavior<PlayerActor.Command> {
    private static int numCards;
    private static int maxCard;
    private static boolean isVerbose;

    private Player player;

    private static final String TRACER = "TRACER PlayerActor ";

    public static Behavior<PlayerActor.Command> create(ConfigInfo configInfo) {
        PlayerActor.numCards = configInfo.numCards();
        PlayerActor.maxCard = PlayerActor.numCards;
        PlayerActor.isVerbose = configInfo.isVerbose();
        return Behaviors.setup(PlayerActor::new);
    }

    private PlayerActor(ActorContext<PlayerActor.Command> context) {
        super(context);
    }

    public sealed interface Command
        permits LogStateCommand, NewHandCommand, MakeBidCommand, RoundOverCommand {}

    public static final class LogStateCommand implements Command {
        final String prefix;

        public LogStateCommand(String prefix) {
            this.prefix = prefix;
        }
    }

    public static final class NewHandCommand implements Command {
        final long handRequestId;
        final String playerName;
        final Strategy strategy;
        final Hand hand;
        final ActorRef<Dealer.Command> replyTo;

        public NewHandCommand(long handRequestId, String playerName,
                              Strategy strategy, Hand hand, ActorRef<Dealer.Command> replyTo) {
            this.handRequestId = handRequestId;
            this.playerName = playerName;
            this.strategy = strategy;
            this.hand = hand;
            this.replyTo = replyTo;
        }
    }

    public static final class MakeBidCommand implements Command {
        final long bidRequestId;
        final int prizeCard;
        final ActorRef<Dealer.Command> replyTo;

        public MakeBidCommand(long bidRequestId, int prizeCard,
                              ActorRef<Dealer.Command> replyTo) {
            this.bidRequestId = bidRequestId;
            this.prizeCard = prizeCard;
            this.replyTo = replyTo;
        }
    }

    public static final class RoundOverCommand implements Command {
        final long roundOverRequestId;
        final int prizeCard;
        final int offer;
        final boolean isWinner;
        final ActorRef<Dealer.Command> replyTo;

        public RoundOverCommand(long roundOverRequestId, int prizeCard, int offer,
                                boolean isWinner, ActorRef<Dealer.Command> replyTo) {
            this.roundOverRequestId = roundOverRequestId;
            this.prizeCard = prizeCard;
            this.offer = offer;
            this.isWinner = isWinner;
            this.replyTo = replyTo;
        }
    }

    @Override
    public Receive<PlayerActor.Command> createReceive() {
        return newReceiveBuilder()
                   .onMessage(LogStateCommand.class, this::onLogStateCommand)
                   .onMessage(NewHandCommand.class, this::onNewHandCommand)
                   .onMessage(MakeBidCommand.class, this::onMakeBidCommand)
                   .onMessage(RoundOverCommand.class, this::onRoundOverCommand)
                   .onSignal(PostStop.class, signal -> onPostStop())
                   .build();
    }

    private Behavior<PlayerActor.Command> onLogStateCommand(LogStateCommand command) {
        var prefix = command.prefix;
        getContext().getLog().info(TRACER + prefix + " STATE {}", player.toString());
        return this;
    }

    private Behavior<PlayerActor.Command> onNewHandCommand(NewHandCommand command) {
        var playerName = command.playerName;
        var hand = command.hand;
        var strategy = command.strategy;
        player = new Player(playerName, strategy, maxCard, hand);

        getContext().getLog().info(TRACER + "playerActor STATE {}", player.toString());

        // example of response
        command.replyTo.tell(new Dealer.NewHandAckEvent(command.handRequestId));

        return this;
    }

    private Behavior<PlayerActor.Command> onMakeBidCommand(MakeBidCommand command) {
        var playerName = player.getName();
        var bid = player.getBid(command.prizeCard);
        var offer = bid.offer();

        // example of response
        command.replyTo.tell(new Dealer.BidEvent(command.bidRequestId, offer, playerName));

        return this;
    }

    private Behavior<PlayerActor.Command> onRoundOverCommand(RoundOverCommand command) {
        var isWinner = command.isWinner;
        var bid = new Bid(command.prizeCard, command.offer, player);

        if (isWinner) {
            player = player.winsRound(bid);
        } else {
            player = player.losesRound(bid);
        }

        // example of response
        command.replyTo.tell(new Dealer.RoundOverAckEvent(command.roundOverRequestId));

        return this;
    }

    private Behavior<Command> onPostStop() {
         getContext().getLog().info(TRACER + "STOPPED");
         return Behaviors.stopped();
    }
}
