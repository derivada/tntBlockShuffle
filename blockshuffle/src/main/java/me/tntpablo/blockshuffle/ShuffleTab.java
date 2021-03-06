package me.tntpablo.blockshuffle;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class ShuffleTab implements TabCompleter {

    private List<String> subcommandList = new ArrayList<String>();

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {

		if (subcommandList.isEmpty()) {
			// Crear lista
			createList();
		}
		if(args.length==1) {
		List<String> finalList = new ArrayList<String>();
		
		// Comprueba si el subcomando aun esta disponible en cada caracter que se añade
		for (String s : subcommandList) {
			if (s.toLowerCase().startsWith(args[0].toLowerCase()))
				finalList.add(s);
		}

		return finalList;
		}
		return null;
	}

	void createList() {
		
		// Crear la lista de subcomandos
		subcommandList.add("join");
		subcommandList.add("leave");
		subcommandList.add("start");
		subcommandList.add("stop");
		subcommandList.add("state");
		subcommandList.add("forcestart");
		subcommandList.add("reload");
		// ...
	}
}
