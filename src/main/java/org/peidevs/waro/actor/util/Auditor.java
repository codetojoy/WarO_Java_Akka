package org.peidevs.waro.actor.util;

import java.util.*;
import java.util.stream.*;

import org.peidevs.waro.table.Hand;

public class Auditor {
    private Set<Integer> gameObservedBids = new HashSet<>();
    private Set<Integer> gameExpectedBids = new HashSet<>();

    private Map<String, Set<Integer>> expectedBidsPerPlayer = new HashMap<>();

    private int expectedKittySum = 0;
    private List<Integer> observedTotals = new ArrayList<>();

    private static final String PREFIX = "Auditor ";

    public Auditor(Hand kitty, int numCards) {
        expectedKittySum = kitty.cardsAsIntStream().sum();
        var kittyCards = kitty.cardsAsIntStream().boxed().collect(Collectors.toSet());
        gameExpectedBids = IntStream.rangeClosed(1, numCards)
                                    .boxed()
                                    .filter(i -> ! kittyCards.contains(i))
                                    .collect(Collectors.toSet());
    }

    public void setExpectedBidsForPlayer(String playerName, Hand hand) {
        var bids = hand.cardsAsIntStream().boxed().collect(Collectors.toSet());
        expectedBidsPerPlayer.put(playerName, bids);
    }

    public void setObservedBidForPlayer(String playerName, int bid) {
        var bids = expectedBidsPerPlayer.get(playerName);

        if (bids.contains(bid)) {
            bids.remove(bid);
        } else {
            throw new IllegalStateException("INTERNAL ERROR on observed bids per player [set]");
        }

        setGameObservedBid(bid);
    }

    public String confirmBidsForPlayers() {
        String result = PREFIX + " bidsForPlayers CONFIRMED";

        for (var bids : expectedBidsPerPlayer.values()) {
            if (! bids.isEmpty()) {
                throw new IllegalStateException("INTERNAL ERROR on observed bids per player [confirm]");
            }
        }

        return result;
    }

    protected void setGameObservedBid(int bid) {
        gameObservedBids.add(bid);
    }

    public String confirmGameBids() {
        String result = PREFIX + " bids CONFIRMED";

        if (! areSetsEqual(gameExpectedBids, gameObservedBids)) {
            throw new IllegalStateException("INTERNAL ERROR on bids");
        }

        return result;
    }

    public String confirmKitty() {
        String result = PREFIX + " kitty CONFIRMED";

        int observedSum = observedTotals.stream().mapToInt(Integer::intValue).sum();

        if (expectedKittySum != observedSum) {
            throw new IllegalStateException("INTERNAL ERROR on kitty");
        }

        return result;
    }

    public void setObservedTotal(Integer total) {
        observedTotals.add(total);
    }

    protected boolean areSetsEqual(Set<Integer> s, Set<Integer> t) {
        boolean result = (s.equals(t));
        return result;
    }
}
