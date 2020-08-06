package me.tntpablo.blockshuffle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ShuffleCore {
    private GameState gameState = GameState.OFFLINE;
    private Main plugin;
    private List<Player> players = new ArrayList<Player>();
    private List<Player> alivePlayers = new ArrayList<Player>();
    private Map<Player, Material> objectiveBlocks = new HashMap<Player, Material>();
    private Map<Player, Integer> lives = new HashMap<Player, Integer>();
    // Cada bloques se mapea a true si ya ha salido
    private List<Material> blocks = new ArrayList<Material>();
    private List<Material> availableBlocks = new ArrayList<Material>();

    public ShuffleConfig config = new ShuffleConfig();

    ShuffleCore(Main plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.logger.info("Cargando configuracion!");
        FileConfiguration configFile;
        FileConfiguration blocksFile;
        try {
            configFile = this.plugin.config.getConfig();
            blocksFile = this.plugin.blocks.getConfig();
        } catch (NullPointerException e) {
            plugin.logger.log(Level.SEVERE, "NO SE HAN PODIDO CARGAR LOS ARCHIVOS DE CONFIGURACION");
            throw e;
        }
        try {

            plugin.logger.info("Cargando configuracion adicional!");
            this.config.lives = configFile.getInt("lives");
            plugin.logger.info("Cargado numero de vidas");
            this.config.startingTime = configFile.getInt("starting-time");
            plugin.logger.info("Cargado tiempo de inicio");
            this.config.roundTime = configFile.getInt("round-time");
            plugin.logger.info("Cargado tiempo de ronda");
            this.config.difficulty = configFile.getString("difficulty");
            plugin.logger.info("Dificultad cargada");
            this.config.competitive = configFile.getBoolean("competitive");
            plugin.logger.info("Cargado modo competitivo");
            plugin.logger.info("Cargando configuracion de bloques!");

            for (String s : blocksFile.getStringList("blocks")) {
                Material mat = Material.matchMaterial(s);
                if (mat != null) {
                    if (mat.isBlock()) {
                        blocks.add(Material.matchMaterial(s));
                    } else {
                        plugin.logger.info(Utils.formattedString(s) + "no es un bloque!");
                    }
                } else {
                    plugin.logger.info(Utils.formattedString(s) + "no es un material!");
                }
            }
            plugin.logger.info("Cargada configuracion de bloques!");
        } catch (Exception e) {
            plugin.logger.log(Level.SEVERE, "ERROR CARGANDO CONFIGURACION");
            throw e;
        }
        plugin.logger.info("Configuracion cargada con exito!");
    }

    public void playerJoin(Player p) {
        if (!players.contains(p)) {
            players.add(p);
            p.sendMessage(Utils.chat(("Te has unido a la partida!")));
            updateScoreboard(p);
            return;
        }
        p.sendMessage(Utils.chat("Ya estas en la partida!"));
    }

    public void playerEliminate(Player p) {
        alivePlayers.remove(p);
        if (p.isOnline()) {
            p.sendMessage("Has sido eliminado!");
        }
        p.setGameMode(GameMode.SPECTATOR);
        updateScoreboard(p);
    }

    public void playerLeave(Player p) {
        if (!players.contains(p)) {
            p.sendMessage(Utils.chat("No estas en la partida!"));
            return;
        }
        players.remove(p);
        alivePlayers.remove(p);
        objectiveBlocks.remove(p);
        lives.remove(p);
        p.sendMessage(Utils.chat("Has salido de la partida!"));
        if (gameState != GameState.OFFLINE)
            checkList();
    }

    public void checkList() {
        if (alivePlayers.size() == 1){
            msgAll("El jugador &l" +alivePlayers.get(0).getName()+ " &rha ganado! Enhorabuena!");
            stop();
        }
        if(alivePlayers.size()<=0){
            msgAll("Tenemos un empate! Gracias por jugar!");
            stop();
        }
    }

    public void start(Player sender) {
        // Condiciones para poder empezar la partida

        if (this.gameState != GameState.OFFLINE) {
            sender.sendMessage(Utils.chat("La partida ya ha comenzado, no se puede empezar otra!"));
            return;
        }
        if (players.size() <= 1) {
            sender.sendMessage(
                    Utils.chat("No se han unido suficientes jugadores! Usa &k/blockshuffle &rjoin para unirte"));
            return;
        }
        availableBlocks.addAll(blocks);
        msgAll("&lComenzando la partida!");
        // Añadir a la lista de jugadores vivos y darles sus vidas
        alivePlayers.addAll(players);
        for (Player p : players) {
            lives.put(p, 3);
        }

        roundStart();
    }

    private void roundStart() {
        this.gameState = GameState.STARTING;

        for (Player p : alivePlayers) {
            updateScoreboard(p);
        }

        new BukkitRunnable() {
            int timeLeft = config.startingTime;

            @Override
            public void run() {
                if (Utils.timeReminder(timeLeft) != null)
                    msgAll("Quedan " + Utils.timeReminder(timeLeft) + timeLeft + "&f segundos!");

                timeLeft--;
                if (gameState != GameState.STARTING)
                    this.cancel();
                if (timeLeft == 0) {
                    roundRun();
                    this.cancel();
                }

                for (Player p : alivePlayers)
                    updateScoreboardTime(p, timeLeft);
            }

        }.runTaskTimer(plugin, 20, 20);
    }

    private void roundRun() {
        this.gameState = GameState.RUNNING;

        giveBlocks();

        for (Player p : alivePlayers) {
            updateScoreboard(p);
        }
        new BukkitRunnable() {
            int timeLeft = config.roundTime;
            List<Player> finished = new ArrayList<Player>();

            @Override
            public void run() {
                if (Utils.timeReminder(timeLeft) != null)
                    msgAll("Quedan " + Utils.timeReminder(timeLeft) + timeLeft + "&f segundos!");
                if (gameState != GameState.RUNNING)
                    this.cancel();
                timeLeft--;
                if (finished.size() == alivePlayers.size()) {
                    msgAll("Todos los jugadores han encontrado su bloque! Comenzando siguiente ronda...");
                    roundStart();
                    this.cancel();
                }
                if (timeLeft == 0) {
                    msgAll("Se ha acabado el tiempo! Comenzando siguiente ronda...");
                    roundStart();
                    // Quitar vidas y eliminar
                    for (Iterator<Player> iter = alivePlayers.listIterator(); iter.hasNext();) {
                        Player p = iter.next();
                        if (!finished.contains(p)) {
                            lives.put(p, lives.get(p) - 1);
                            p.sendMessage(Utils.chat("Te quedan &4&l" + lives.get(p) + "&r&f vidas!"));
                        }
                        if (lives.get(p) <= 0) {
                            ShuffleCore.this.playerEliminate(p);
                            p.sendMessage(Utils.chat("Has sido eliminado!"));
                        }
                    }
                    checkList();
                    this.cancel();
                }
                for (Player p : alivePlayers) {
                    // Comprueba si ha obtenido el bloque
                    if (p.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().equals(objectiveBlocks.get(p))
                            && !finished.contains(p)) {
                        finished.add(p);
                        p.sendMessage(Utils.chat("Has encontrado el bloque!"));
                    }

                    updateScoreboardTime(p, timeLeft);
                }

            }

        }.runTaskTimer(plugin, 0, 20);
    }

    public void stop() {
        msgAll("Finalizando la partida! Gracias por jugar :)");
        gameState = GameState.OFFLINE;
        new BukkitRunnable() {
            @Override
            public void run() {
                gameState = GameState.OFFLINE;
                players.clear();
                alivePlayers.clear();
                lives.clear();
                objectiveBlocks.clear();
                Bukkit.broadcastMessage(Utils.chat("Partida de BlockShuffle finalizada!"));
            }
        }.runTaskLater(plugin, 20);
    }

    private void giveBlocks() {
        // Dar los bloques a los jugadores

        Random r = new Random(System.currentTimeMillis());
        if (config.competitive == true) {
            if (availableBlocks.size() == 0) {
                msgAll("Ya han salido todos los bloques, ahora se repetiran!");
                availableBlocks.addAll(blocks);
            }
            int index = r.nextInt(availableBlocks.size());
            Material mat = availableBlocks.get(index);
            availableBlocks.remove(index);
            for (Player p : alivePlayers) {
                objectiveBlocks.put(p, mat);
            }
            msgAll("Los jugadores deben encontrar: &l" + Utils.formattedString(mat.toString()));
        } else {
            if (availableBlocks.size() == alivePlayers.size()) {
                msgAll("Ya han salido todos los bloques, ahora se repetiran!");
                availableBlocks.addAll(blocks);
            }
            for (Player p : alivePlayers) {
                int index = r.nextInt(availableBlocks.size());
                Material mat = availableBlocks.get(index);
                availableBlocks.remove(index);
                objectiveBlocks.put(p, mat);
                p.sendMessage(Utils.chat("Tu debes encontrar: &l" + Utils.formattedString(mat.toString())));
            }
        }
    }

    public void updateScoreboard(Player p) {

    }

    private void updateScoreboardTime(Player p, int timeLeft) {

    }

    public GameState getGameState() {
        return this.gameState;
    }

    public void msgAll(String msg) {
        for (Player p : players) {
            p.sendMessage(Utils.chat(msg));
        }
    }

    public boolean isAlive(Player p) {
        return alivePlayers.contains(p) ? true : false;
    }

    public class ShuffleConfig {
        /*
         * lives: 3 starting-time: 5 round-time: 30 difficulty: easy competitive: true
         */
        public int lives, startingTime, roundTime;
        public String difficulty;
        public boolean competitive;
    }

}
