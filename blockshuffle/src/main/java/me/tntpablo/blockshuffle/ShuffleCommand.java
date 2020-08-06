package me.tntpablo.blockshuffle;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShuffleCommand implements CommandExecutor {
    private Main plugin;

    ShuffleCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (args.length >= 1) {
                switch (args[0].toLowerCase()) {
                    case "start":
                        plugin.shuffleCore.start(p);
                        break;
                    case "stop":
                        plugin.shuffleCore.stop();
                        break;
                    case "join":
                        plugin.shuffleCore.playerJoin(p);
                        break;
                    case "leave":
                        plugin.shuffleCore.playerLeave(p);
                        break;
                    case "state":
                        p.sendMessage(Utils.chat(
                                "Estado del juego: " + plugin.shuffleCore.getGameState().toString().substring(0, 1)
                                        + plugin.shuffleCore.getGameState().toString().substring(1).toLowerCase()));
                        break;
                    default:
                        p.sendMessage(Utils.pluginMsg("noimplement"));
                        break;
                }
                return true;
            }
            p.sendMessage(Utils.pluginMsg("usage"));
            return true;
        }

        return false;
    }

}
