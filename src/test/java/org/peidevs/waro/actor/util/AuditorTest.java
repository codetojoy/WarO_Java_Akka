package org.peidevs.waro.actor.util;

import org.peidevs.waro.table.*;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.*;

public class AuditorTest {
    private Auditor auditor = null;

    private static final int NUM_CARDS = 12;
    private static final String PLAYER_NAME = "Edward_Van_Halen";
    private static final Hand EMPTY_KITTY = new Hand(List.of());

    @Test(expected = IllegalStateException.class)
    public void testConfirmBidsForPlayers_Fail() {
        var kitty = new Hand(List.of(2,3,5));
        auditor = new Auditor(kitty, NUM_CARDS);
        var hand = new Hand(List.of(1,6,9));
        auditor.setExpectedBidsForPlayer(PLAYER_NAME, hand);
        auditor.setObservedBidForPlayer(PLAYER_NAME, 1);
        auditor.setObservedBidForPlayer(PLAYER_NAME, 6);
        // auditor.setObservedBidForPlayer(PLAYER_NAME, 9);

        // test
        var result = auditor.confirmBidsForPlayers();
    }

    @Test
    public void testConfirmBidsForPlayers_Basic() {
        var kitty = new Hand(List.of(2,3,5));
        auditor = new Auditor(kitty, NUM_CARDS);
        var hand = new Hand(List.of(1,6,9));
        auditor.setExpectedBidsForPlayer(PLAYER_NAME, hand);
        auditor.setObservedBidForPlayer(PLAYER_NAME, 1);
        auditor.setObservedBidForPlayer(PLAYER_NAME, 6);
        auditor.setObservedBidForPlayer(PLAYER_NAME, 9);

        // test
        var result = auditor.confirmBidsForPlayers();

        // success == no exception
    }

    @Test
    public void testAreSetsEqual_basic() {
        auditor = new Auditor(EMPTY_KITTY, NUM_CARDS);
        var s = Set.of(2,3,5);
        var t = Set.of(5,3,2);

        // test
        var result = auditor.areSetsEqual(s, t);

        assertTrue(result);
    }
}
