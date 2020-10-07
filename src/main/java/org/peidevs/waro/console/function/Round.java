package org.peidevs.waro.console.function;

import org.peidevs.waro.player.*;
import org.peidevs.waro.table.*;

import java.util.*;
import java.util.stream.*;
import java.util.function.*;
import static java.util.stream.Collectors.toList;
import static java.util.Comparator.comparing;

@Deprecated
class Round implements UnaryOperator<Stream<Player>> {
    private final int prizeCard;

    protected Round(int prizeCard) {
        this.prizeCard = prizeCard;
    }

    protected Round() {
        this.prizeCard = -1;
    }

    @Override
    public Stream<Player> apply(Stream<Player> players) {
        var bids = getAllBids(players, prizeCard);

        var winningBid = findWinningBid(bids);

        var newWinner = winningBid.bidder().winsRound(winningBid);

        var winner = winningBid.bidder().getName();

        var result = Stream.concat( Stream.of(newWinner),
                                    bids.stream()
                                        .filter(b -> ! b.bidder().getName().equals(winner))
                                        .map(b -> b.bidder().losesRound(b)));

        return result;
    }

    // --------- internal

    protected List<Bid> getAllBids(Stream<Player> players, int prizeCard) {
        return players.map(p -> p.getBid(prizeCard)).collect(toList());
    }

    protected Bid findWinningBid(List<Bid> bids) {
        var winningBid = bids.stream().max( comparing(Bid::offer) ).get();
        return winningBid;
    }
}
