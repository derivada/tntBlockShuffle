package me.tntpablo.blockshuffle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
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
    private List<Material> availableBlocks = new ArrayList<Material>();
    private List<Player> registeredScoreboards = new ArrayList<Player>();
    private List<Player> finished = new ArrayList<Player>();
    private Map<Player, GameMode> previousGameMode = new HashMap<Player, GameMode>();

    private int countdown = 0;
    private int rounds = 0;

    public ShuffleConfig config;

    ShuffleCore(Main plugin) {
        this.plugin = plugin;
        plugin.logger.info("Cargando configuracion!");
        config = new ShuffleConfig(this.plugin.config.getConfig(), this.plugin.blocks.getConfig());
    }

    public void playerJoin(Player p) {
        if (!players.contains(p)) {
            players.add(p);
            p.sendMessage(Utils.chat(("Te has unido a la partida!")));
            previousGameMode.put(p, p.getGameMode());
            for (Player pl : players) {
                updateScoreboard(pl);
            }
            return;
        }
        p.sendMessage(Utils.chat("Ya estas en la partida!"));
    }

    public void playerEliminate(Player p) {
        boolean removed = alivePlayers.remove(p);
        if (p.isOnline()) {
            p.sendMessage(Utils.chat("Has sido eliminado!"));
        }
        for (Player pl : players) {
            updateScoreboard(pl);
        }
        if (gameState != GameState.OFFLINE && removed == true)
            checkList();
        updateScoreboard(p);
    }

    public void playerLeave(Player p, boolean checkList) {
        if (!players.contains(p)) {
            p.sendMessage(Utils.chat("No estas en la partida!"));
            return;
        }
        players.remove(p);
        alivePlayers.remove(p);
        objectiveBlocks.remove(p);
        lives.remove(p);
        p.setGameMode(previousGameMode.get(p));
        previousGameMode.remove(p);
        updateScoreboard(p);
        p.sendMessage(Utils.chat("Has salido de la partida!"));
        if (gameState != GameState.OFFLINE && checkList == true)
            checkList();
        for (Player pl : players) {
            updateScoreboard(pl);
        }
    }

    public void checkList() {
        if (alivePlayers.size() == 1) {
            msgAll("El jugador &l" + alivePlayers.get(0).getName() + " &rha ganado! Enhorabuena!");
            stop();
        } else if (alivePlayers.size() <= 0) {
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
                    Utils.chat("No se han unido suficientes jugadores! Usa &o/blockshuffle join&r&f para unirte"));
            return;
        }
        availableBlocks.addAll(config.blocks);
        msgAll("&lComenzando la partida!");
        // Añadir a la lista de jugadores vivos y darles sus vidas
        alivePlayers.addAll(players);
        for (Player p : players) {
            lives.put(p, 3);
        }
        rounds = 0;
        roundStart();
    }

    private void roundStart() {
        this.gameState = GameState.STARTING;
        countdown = config.startingtime;
        for (Player p : alivePlayers)
            updateScoreboard(p);
        new BukkitRunnable() {

            @Override
            public void run() {
                countdown--;
                if (gameState != GameState.STARTING)
                    this.cancel();
                if (countdown == 0) {
                    roundRun();
                    this.cancel();
                }
                for (Player p : alivePlayers)
                    updateScoreboard(p);
            }

        }.runTaskTimer(plugin, 20, 20);
    }

    private void roundRun() {
        this.gameState = GameState.RUNNING;
        countdown = (int) Math.floor(config.roundtime * Math.pow(config.timeShrink, rounds));
        giveBlocks();
        finished.clear();
        for (Player p : alivePlayers)
            updateScoreboard(p);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (gameState != GameState.RUNNING)
                    this.cancel();
                countdown--;
                if (finished.size() == alivePlayers.size()) {
                    checkList();
                    msgAll("Todos los jugadores han encontrado su bloque! Comenzando siguiente ronda...");
                    rounds++;
                    roundStart();
                    this.cancel();
                }
                if (countdown == 0) {
                    msgAll("Se ha acabado el tiempo! Comenzando siguiente ronda...");
                    rounds++;
                    roundStart();
                    // Quitar vidas y eliminar
                    ListIterator<Player> iter = alivePlayers.listIterator();
                    while (iter.hasNext()) {
                        Player p = iter.next();
                        if (!finished.contains(p)) {
                            lives.put(p, lives.get(p) - 1);
                            p.sendMessage(Utils.chat("Te quedan &4&l" + lives.get(p) + "&r&f vidas!"));
                        }
                        if (lives.get(p) <= 0) {
                            iter.remove();
                            ShuffleCore.this.playerEliminate(p);
                        }
                    }
                    checkList();
                    this.cancel();
                }
                for (Player p : alivePlayers) {
                    // Comprueba si ha obtenido el bloque
                    if ((p.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().equals(objectiveBlocks.get(p))
                            || p.getLocation().add(0, 1, 0).getBlock().getRelative(BlockFace.DOWN).getType()
                                    .equals(objectiveBlocks.get(p)))

                            && !finished.contains(p)) {
                        finished.add(p);
                        p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1.0f, 1.0f);
                        p.sendMessage(Utils.chat("Has encontrado el bloque!"));
                    }
                    updateScoreboard(p);
                }
            }
        }.runTaskTimer(plugin, 20, 20);
    }

    public void stop() {
        msgAll("Finalizando la partida! Gracias por jugar!");
        gameState = GameState.OFFLINE;
        new BukkitRunnable() {
            @Override
            public void run() {
                gameState = GameState.OFFLINE;
                alivePlayers.clear();

                players.clear();
                lives.clear();
                objectiveBlocks.clear();

                for (Player p : Bukkit.getOnlinePlayers()) {
                    updateScoreboard(p);
                }

                for (Player p : previousGameMode.keySet())
                    p.setGameMode(previousGameMode.get(p));
                previousGameMode.clear();

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
                availableBlocks.addAll(config.blocks);
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
                availableBlocks.addAll(config.blocks);
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
        if (!registeredScoreboards.contains(p))
            registeredScoreboards.add(p);

        if (!players.contains(p)) {
            // Quitar scoreboard si el jugador no esta en la lista de jugadores
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            return;
        }
        List<String> scoreboardEntries = new ArrayList<String>();
        String timeLeft = String.format("%02d:%02d", countdown / 60, countdown % 60);

        switch (gameState) {
            case OFFLINE:
                scoreboardEntries.add("Esperando...");
                scoreboardEntries.add("SPACE");
                scoreboardEntries.add("Jugadores: &b" + players.size());
                break;
            case STARTING:
                scoreboardEntries.add("Jugadores vivos: &b" + this.alivePlayers.size() + "/" + this.players.size());
                scoreboardEntries.add("SPACE");
                scoreboardEntries.add("Siguiente ronda en: &b" + timeLeft);
                scoreboardEntries.add("SPACE");
                if (alivePlayers.contains(p)) {
                    scoreboardEntries.add(Utils.hpString(lives.get(p), config.lives));
                } else {
                    scoreboardEntries.add("Has sido eliminado!");
                }
                break;
            case RUNNING:
                scoreboardEntries.add("Jugadores vivos: &b" + this.alivePlayers.size() + "/" + this.players.size());
                scoreboardEntries.add("SPACE");
                scoreboardEntries.add("Tiempo restante: &b" + timeLeft);
                scoreboardEntries.add("SPACE");

                String nombreBloque = Utils.formattedString(objectiveBlocks.get(p).toString());

                if (nombreBloque.length() > 16 && nombreBloque.length() < 60) {
                    String[] tokens = nombreBloque.split("\\W+");
                    String primeraParte = new String(tokens[0]);
                    int i = 1;
                    String temp = primeraParte.concat("Bloque: &o" + tokens[i]);
                    while (temp.length() < 16) {
                        primeraParte = temp;
                        i++;
                        temp = primeraParte.concat(" " + tokens[i]);
                    }
                    String segundaParte = " " + nombreBloque.substring(primeraParte.length() + 1);
                    scoreboardEntries.add(Utils.color("Bloque: &o" + primeraParte));
                    scoreboardEntries.add(Utils.color("&o" + segundaParte));
                } else if (nombreBloque.length() <= 16) {
                    scoreboardEntries.add(Utils.color("Bloque: &o" + nombreBloque));
                } else {
                    scoreboardEntries.add(Utils.color("Bloque: &oVer Chat"));
                }
                scoreboardEntries.add("SPACE");
                scoreboardEntries.add("Progreso:");
                for (Player player : alivePlayers) {
                    if (finished.contains(player))
                        scoreboardEntries.add("&a&l \u2713 &r&a" + player.getName());
                    else
                        scoreboardEntries.add("&c&l \u2717 &r&c" + player.getName());
                }
                scoreboardEntries.add("SPACE");
                if (alivePlayers.contains(p)) {
                    scoreboardEntries.add(Utils.hpString(lives.get(p), config.lives));
                } else {
                    scoreboardEntries.add("Has sido eliminado!");
                }
                scoreboardEntries.add("SPACE");
                scoreboardEntries.add("Ronda: &b" + (this.rounds + 1));
                break;
        }
        Utils.setScoreboard(p, "&e&lBlock&f&lShuffle", scoreboardEntries);
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

    public void resetScoreboards() {
        for (Player p : registeredScoreboards) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            plugin.logger.info("Se ha reseteado el scoreboard de " + p.getName());
        }
        plugin.logger.info("Scoreboards desactivados!");
    }

    public void reloadConfig(Player sender) {
        if (this.gameState != GameState.OFFLINE) {
            sender.sendMessage(Utils.chat("El juego debe estar parado para recargar la configuracion!"));
            plugin.logger.log(Level.SEVERE, "El juego debe estar parado para recargar la configuracion!");
            return;
        }
        // Recargar la configuracion
        sender.sendMessage(Utils.chat("Recargando la configuracion!"));
        plugin.logger.info("Recargando la configuracion!");
        try {
            plugin.blocks.reloadConfig();
            plugin.config.reloadConfig();
            config = new ShuffleConfig(this.plugin.config.getConfig(), this.plugin.blocks.getConfig());
        } catch (Exception e) {
            sender.sendMessage(Utils.chat("Error al recargar la configuracion!"));
            plugin.logger.info("Error al recargar la configuracion!");
            e.printStackTrace();
        }
        sender.sendMessage(Utils.chat("Configuracion recargada!"));
        plugin.logger.info("Configuracion recargada!");

    }

    public class ShuffleConfig {
        /*
         * lives: 3 starting-time: 5 round-time: 30 difficulty: easy competitive: true
         */
        final public int lives, startingtime, roundtime;
        final public boolean competitive;
        final public double timeShrink;
        final public List<Material> blocks;

        public ShuffleConfig(FileConfiguration config, FileConfiguration blocksFile) {

            // Variables temporales del constructor porque no se pueden asignar final en
            // estos try-catch

            int templives, tempstartingtime, temproundtime;
            boolean tempcompetitive;
            double temptimeShrink;

            plugin.logger.info("Cargando configuracion principal!");

            try {
                templives = config.getInt("lives");
                plugin.logger.info("Cargado numero de vidas: " + templives);
                if (templives > 10 || templives < 1)
                    throw new Exception();
            } catch (Exception e) {
                templives = 3;
                plugin.logger
                        .info("No se pudo cargar el numero de vidas o no es adecuado, cargando default: " + templives);
            }
            try {
                tempstartingtime = config.getInt("starting-time");
                plugin.logger.info("Cargado tiempo de inicio: " + tempstartingtime);
                if (tempstartingtime < 1)
                    throw new Exception();
            } catch (Exception e) {
                tempstartingtime = 5;
                plugin.logger.info("No se pudo cargar el tiempo de inicio o no es adecuado, cargando default: "
                        + tempstartingtime);
            }
            try {
                temproundtime = config.getInt("round-time");
                plugin.logger.info("Cargado tiempo de ronda: " + temproundtime);
                if (temproundtime < 1)
                    throw new Exception();
            } catch (Exception e) {
                temproundtime = 300;
                plugin.logger.info(
                        "No se pudo cargar el tiempo de ronda o no es adecuado, cargando default: " + temproundtime);
            }
            try {
                tempcompetitive = config.getBoolean("competitive");
                plugin.logger.info("Cargado modo competitivo: " + tempcompetitive);
            } catch (Exception e) {
                tempcompetitive = true;
                plugin.logger.info("No se pudo cargar el modo competitivo, cargando default: " + tempcompetitive);
            }
            try {
                temptimeShrink = config.getDouble("time-shrink");
                plugin.logger.info("Cargado modificador de tiempo por ronda: " + temptimeShrink);
                if (temptimeShrink <= 0)
                    throw new Exception();
            } catch (Exception e) {
                temptimeShrink = 0.90;
                plugin.logger.info("No se pudo cargar el modo competitivo, cargando default: " + temptimeShrink);
            }

            // Asignando finales

            lives = templives;
            startingtime = tempstartingtime;
            roundtime = temproundtime;
            competitive = tempcompetitive;
            timeShrink = temptimeShrink;

            plugin.logger.info("Configuracion principal cargada!");

            plugin.logger.info("Cargando configuracion de bloques!");
            List<Material> blockstemp = new ArrayList<Material>();
            try {
                for (String s : blocksFile.getStringList("blocks")) {
                    Material mat = Material.matchMaterial(s);
                    if (mat != null) {
                        if (mat.isBlock()) {
                            blockstemp.add(Material.matchMaterial(s));
                        } else {
                            plugin.logger.info(Utils.formattedString(s) + "no es un bloque!");
                        }
                    } else {
                        plugin.logger.info(Utils.formattedString(s) + "no es un material!");
                    }
                }
            } catch (Exception e) {
                plugin.logger.log(Level.SEVERE, "ERROR CARGANDO CONFIGURACION DE BLOQUES");
                // Finalizar el programa (no se pueden dar defaults)
                throw e;
            }

            blocks = blockstemp;
            plugin.logger.info("Cargada configuracion de bloques!");
            plugin.logger.info("Configuracion cargada correctamente!");
        }
    }

}