package org.peidevs.waro.table;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.*;

import java.util.stream.*;
import static java.util.stream.Collectors.toList;

public class HandTest {
    
    @Test
    public void testSelect_Basic() {
        var cards = IntStream.range(1,10+1).boxed().collect(toList());
        var hand = new Hand(cards);
        
        // test
        var result = hand.select(5);
        
        assertEquals(9, result.cardsAsIntStream().boxed().count());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testSelect_Illegal() {
        var cards = IntStream.range(1,10+1).boxed().collect(toList());
        var hand = new Hand(cards);
        
        // test
        var result = hand.select(18);
    }
    
}
