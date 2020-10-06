package org.peidevs.waro.config;

import org.peidevs.waro.player.Player;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

public class ConfigService {
    private final AnnotationConfigApplicationContext context;

    public ConfigService() {
        context = new AnnotationConfigApplicationContext(Config.class);
    }

    public int getNumPlayers() { return context.getBean(Config.BEAN_NUM_PLAYERS, Integer.class); }
    public int getNumCards() { return context.getBean(Config.BEAN_NUM_CARDS, Integer.class); }
    public int getNumGames() { return context.getBean(Config.BEAN_NUM_GAMES, Integer.class); }
    public boolean isVerbose() { return context.getBean(Config.BEAN_IS_VERBOSE, Boolean.class); }

    public List<Player> getPlayers() {
        return context.getBean(Config.BEAN_PLAYERS, List.class);
    }

    public ConfigInfo getConfigInfo() {
        int numPlayers = getNumPlayers();
        int numCards = getNumCards();
        int numGames = getNumGames();
        boolean isVerbose = isVerbose();
        var players = getPlayers();

        ConfigInfo configInfo = new ConfigInfo(numPlayers, numCards, numGames, isVerbose, players);
        return configInfo;
    }
}
