package me.tntpablo.blockshuffle;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    public ShuffleCore shuffleCore;
    @Override
    public void onEnable(){
        shuffleCore = new ShuffleCore(this);
        commandManager();
    }
    @Override
    public void onDisable(){

    }


    public void commandManager(){
        this.getCommand("blockshuffle").setExecutor(new ShuffleCommand(this));
        this.getCommand("blockshuffle").setTabCompleter(new ShuffleTab());
    }

}