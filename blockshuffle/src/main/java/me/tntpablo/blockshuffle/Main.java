package me.tntpablo.blockshuffle;

import java.util.logging.Level;

import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;

import me.tntpablo.blockshuffle.files.DataManager;

public class Main extends JavaPlugin {
    public DataManager config, blocks;
    public ShuffleCore shuffleCore;
    public PluginLogger logger;

    @Override
    public void onEnable() {
        this.logger = new PluginLogger(this);
        try {
            this.config = new DataManager(this, "otherconfig.yml");
            this.blocks = new DataManager(this, "blocks.yml");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "ERROR ABRIENDO LOS ARCHIVOS DE CONFIGURACION");
            e.printStackTrace();
        }
        try {
            shuffleCore = new ShuffleCore(this);
        } catch (Exception e) {
            this.logger.log(Level.SEVERE, "NO SE PUDO INICIALIZAR EL PLUGIN");
            e.printStackTrace();
        }
        commandManager();
    }

    @Override
    public void onDisable() {
        this.logger.info("Desactivando plugin...");
        shuffleCore.resetScoreboards();
        this.logger.info("Plugin desactivado!");
    }

    public void commandManager() {
        this.getCommand("blockshuffle").setExecutor(new ShuffleCommand(this));
        this.getCommand("blockshuffle").setTabCompleter(new ShuffleTab());
    }

}