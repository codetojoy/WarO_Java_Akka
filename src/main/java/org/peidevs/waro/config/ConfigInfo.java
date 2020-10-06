package org.peidevs.waro.config;

import java.util.List;
import org.peidevs.waro.player.Player;

public record ConfigInfo (int numPlayers, int numCards, int numGames,
                          boolean isVerbose, List<Player> players) {}
