package org.peidevs.waro.actor;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

import java.util.*;
import static java.util.Comparator.comparing;

import org.peidevs.waro.actor.util.*;
import org.peidevs.waro.player.Player;
import org.peidevs.waro.config.ConfigInfo;
import org.peidevs.waro.table.Hand;

public class Dealer extends AbstractBehavior<Dealer.Command> {
    private static ConfigInfo configInfo;

    private IdGenerator idGenerator = new IdGenerator(200L);

    private Map<String, ActorRef<PlayerActor.Command>> playerActorMap = new HashMap<>();
    private RequestTracker newHandRequestTracker = new RequestTracker();

    private RequestTracker bidRequestTracker = new RequestTracker();
    private Set<BidInfo> bids = new HashSet<>();

    private RequestTracker totalRequestTracker = new RequestTracker();
    private Set<TotalInfo> totals = new HashSet<>();

    private RequestTracker roundOverRequestTracker = new RequestTracker();
    private RequestTracker gameOverRequestTracker = new RequestTracker();

    private Hand kitty = null;
    private int prizeCard = 0;

    private static final String TRACER = "TRACER dealer ";

    public static Behavior<Dealer.Command> create(ConfigInfo configInfo) {
        Dealer.configInfo = configInfo;
        return Behaviors.setup(Dealer::new);
    }

    private Dealer(ActorContext<Dealer.Command> context) {
        super(context);
    }

    public sealed interface Command
        permits PlayGameCommand, NewHandAckEvent, BidEvent,
                RoundOverAckEvent, GetTotalEvent, GameOverAckEvent {}

    public static final class PlayGameCommand implements Command {
        final long gameRequestId;
        final ActorRef<Tourney.Command> replyTo;

        public PlayGameCommand(long gameRequestId, ActorRef<Tourney.Command> replyTo) {
            this.gameRequestId = gameRequestId;
            this.replyTo = replyTo;
        }
    }

    static abstract class AckEvent {
        final long requestId;

        AckEvent(long requestId) {
            this.requestId = requestId;
        }
    }

    public static final class NewHandAckEvent extends AckEvent implements Command {
        public NewHandAckEvent(long requestId) {
            super(requestId);
        }
    }

    public static final class BidEvent implements Command {
        final long bidRequestId;
        final int offer;
        final String playerName;

        public BidEvent(long bidRequestId, int offer, String playerName) {
            this.bidRequestId = bidRequestId;
            this.offer = offer;
            this.playerName = playerName;
        }
    }

    public static final class GetTotalEvent implements Command {
        final long totalRequestId;
        final int total;
        final String playerName;

        public GetTotalEvent(long totalRequestId, int total, String playerName) {
            this.totalRequestId = totalRequestId;
            this.total = total;
            this.playerName = playerName;
        }
    }

    public static final class RoundOverAckEvent extends AckEvent implements Command {
        public RoundOverAckEvent(long requestId) {
            super(requestId);
        }
    }

    public static final class GameOverAckEvent extends AckEvent implements Command {
        public GameOverAckEvent(long requestId) {
            super(requestId);
        }
    }

    @Override
    public Receive<Dealer.Command> createReceive() {
        return newReceiveBuilder()
                   .onMessage(PlayGameCommand.class, this::onPlayGameCommand)
                   .onMessage(NewHandAckEvent.class, this::onNewHandAckEvent)
                   .onMessage(BidEvent.class, this::onBidEvent)
                   .onMessage(RoundOverAckEvent.class, this::onRoundOverAckEvent)
                   .onMessage(GetTotalEvent.class, this::onGetTotalEvent)
                   .onMessage(GameOverAckEvent.class, this::onGameOverAckEvent)
                   .onSignal(PostStop.class, signal -> onPostStop())
                   .build();
    }

    // ---------- begin message handlers

    private Behavior<Dealer.Command> onPlayGameCommand(PlayGameCommand command) {

        int numPlayers = configInfo.numPlayers();
        int numCards = configInfo.numCards();
        var players = configInfo.players();

        // create Players
        for (var player : players) {
            var playerName = player.getName();
            ActorRef<PlayerActor.Command> playerActor = getContext().spawn(PlayerActor.create(configInfo), playerName);
            playerActorMap.put(playerName, playerActor);
        }

        // create hands
        var playersStream = players.stream();
        var legacyDealer = new org.peidevs.waro.table.Dealer();
        var table = legacyDealer.deal(numPlayers, numCards, playersStream);
        kitty = table.kitty();
        var playersWithHand = table.players();

        // deal hands

        for (var player : playersWithHand) {
            long requestId = idGenerator.nextId();
            var playerName = player.getName();
            var strategy = player.getStrategy();
            var hand = player.getHand();
            var playerActor = playerActorMap.get(playerName);
            var self = getContext().getSelf();
            var newHandCommand = new PlayerActor.NewHandCommand(requestId, playerName,
                                                                strategy, hand, self);
            playerActor.tell(newHandCommand);
            newHandRequestTracker.put(requestId, playerName);
        }

        // example of response
        command.replyTo.tell(new Tourney.PlayGameAckEvent(command.gameRequestId));

        return this;
    }

    private int getRoundIndex() {
        int numCardsPerPlayer = configInfo.numCards() / (configInfo.numPlayers() + 1);
        int roundIndex = (numCardsPerPlayer - kitty.size()) + 1;
        return roundIndex;
    }

    private Behavior<Dealer.Command> onNewHandAckEvent(NewHandAckEvent event) {
        long requestId = event.requestId;
        newHandRequestTracker.ackReceived(requestId);

        if (newHandRequestTracker.isAllReceived()) {
            logState("new hand complete {k:" + kitty.size() + " req:" + requestId + "}");

            playRound();
        }

        return this;
    }

    private Behavior<Dealer.Command> onRoundOverAckEvent(RoundOverAckEvent event) {
        long requestId = event.requestId;
        roundOverRequestTracker.ackReceived(requestId);

        if (roundOverRequestTracker.isAllReceived()) {
            logState("ROUND OVER");

            if (kitty.isEmpty()) {
                getTotals();
            } else {
                playRound();
            }
        }

        return this;
    }

    private Behavior<Dealer.Command> onGameOverAckEvent(GameOverAckEvent event) {
        long requestId = event.requestId;
        gameOverRequestTracker.ackReceived(requestId);

        if (gameOverRequestTracker.isAllReceived()) {
            logState("GAME OVER");

            // TODO: tell tourney?
        }

        return this;
    }

    private Behavior<Dealer.Command> onBidEvent(BidEvent event) {
        long bidRequestId = event.bidRequestId;
        bidRequestTracker.ackReceived(bidRequestId);
        var playerName = event.playerName;
        var offer = event.offer;
        var bidInfo = new BidInfo(offer, playerName);
        bids.add(bidInfo);

        if (bidRequestTracker.isAllReceived()) {
            getContext().getLog().info(TRACER + "bids complete");
            determineRoundWinner();
        }

        return this;
    }

    private Behavior<Dealer.Command> onGetTotalEvent(GetTotalEvent event) {
        long totalRequestId = event.totalRequestId;
        totalRequestTracker.ackReceived(totalRequestId);
        var playerName = event.playerName;
        var total = event.total;
        var totalInfo = new TotalInfo(total, playerName);
        totals.add(totalInfo);

        if (totalRequestTracker.isAllReceived()) {
            getContext().getLog().info(TRACER + "totals complete");
            determineGameWinner();
        }

        return this;
    }

    // ---------- end message handlers

    private void determineGameWinner() {
        getContext().getLog().info(TRACER + "determine game winner TODO here");
        var highestTotal = totals.stream().max( comparing(TotalInfo::total) ).get();
        var gameWinner = highestTotal.playerName();

        for (var playerName : playerActorMap.keySet()) {
            var playerActor = playerActorMap.get(playerName);
            var isGameWinner = gameWinner.equals(playerName);
            var requestId = idGenerator.nextId();
            gameOverRequestTracker.put(requestId, playerName);

            if (isGameWinner) {
                getContext().getLog().info(TRACER + "{} WINS GAME", playerName);
            }

            var self = getContext().getSelf();
            var gameOverCommand = new PlayerActor.GameOverCommand(requestId, isGameWinner, self);
            playerActor.tell(gameOverCommand);
        }
    }

    private void determineRoundWinner() {
        getContext().getLog().info(TRACER + "determine round winner");
        roundOverRequestTracker.clear();
        var winningBid = bids.stream().max( comparing(BidInfo::offer) ).get();
        var winner = winningBid.playerName();

        for (var playerName : playerActorMap.keySet()) {
            var playerActor = playerActorMap.get(playerName);
            var offer = bids.stream().filter(b -> b.playerName().equals(playerName)).findFirst().get().offer();
            var isWinner = winner.equals(playerName);
            var requestId = idGenerator.nextId();
            roundOverRequestTracker.put(requestId, playerName);

            if (isWinner) {
                getContext().getLog().info(TRACER + "{} WINS ROUND bid: {}  prize: {} kitty: {}", playerName, offer, prizeCard, kitty.toString());
            }

            var self = getContext().getSelf();
            var roundOverCommand = new PlayerActor.RoundOverCommand(requestId, prizeCard, offer, isWinner, self);
            playerActor.tell(roundOverCommand);
        }
    }

    private void playRound() {
        if (! kitty.isEmpty()) {
            prizeCard = kitty.take();
            kitty = kitty.select(prizeCard);
            getBids();
        } else {
            // getContext().getLog().info(TRACER + "GAME OVER");
            prizeCard = 0;
            logState("GAME OVER");
        }
    }

    private void getBids() {
        bidRequestTracker.clear();
        bids.clear();

        for (var playerName : playerActorMap.keySet()) {
            var playerActor = playerActorMap.get(playerName);
            long bidRequestId = idGenerator.nextId();
            bidRequestTracker.put(bidRequestId, playerName);
            var self = getContext().getSelf();
            var makeBidCommand = new PlayerActor.MakeBidCommand(bidRequestId, prizeCard, self);
            playerActor.tell(makeBidCommand);
        }
    }

    private void getTotals() {
        totalRequestTracker.clear();
        totals.clear();

        for (var playerName : playerActorMap.keySet()) {
            var playerActor = playerActorMap.get(playerName);
            long totalRequestId = idGenerator.nextId();
            totalRequestTracker.put(totalRequestId, playerName);
            var self = getContext().getSelf();
            var getTotalCommand = new PlayerActor.GetTotalCommand(totalRequestId, self);
            playerActor.tell(getTotalCommand);
        }
    }

    private void logState(String prefix) {
         getContext().getLog().info(TRACER + prefix + " prizeCard {} kitty {}", prizeCard, kitty.toString());
         tellPlayersToLogState(prefix);
    }

    private void tellPlayersToLogState(String prefix) {
        for (var playerActor : playerActorMap.values()) {
            var logStateCommand = new PlayerActor.LogStateCommand(prefix);
            playerActor.tell(logStateCommand);
        }
    }

    private Behavior<Command> onPostStop() {
         getContext().getLog().info(TRACER + "STOPPED");
         return Behaviors.stopped();
    }
}

record BidInfo (int offer, String playerName) {}

record TotalInfo (int total, String playerName) {}
