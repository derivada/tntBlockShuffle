package me.tntpablo.blockshuffle;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

public class Utils {
	public static String error(String msg) {
		return ChatColor.translateAlternateColorCodes('&',
				"&l&a[ &3Block &4&lShuffle&l&a ]&r&f &4&l ERROR: &r&f" + msg);
	}

	public static String chat(String msg) {
		// Mensajes generales
		return ChatColor.translateAlternateColorCodes('&', "&l&a[ &3Block &4&lShuffle&l&a ]&r&f " + msg);
	}

	public static String color(String msg) {
		return ChatColor.translateAlternateColorCodes('&', "&f" + msg);
	}

	public static String consoleChat(String msg) {
		return "[The Bridge] " + msg;
	}

	public static String pluginMsg(String label) {

		// Mensajes por excepciones, errores del usuario...
		switch (label.toLowerCase()) {
			case "usage":
				return chat("Uso correcto: /blockshuffle <comando>");
			case "lowplayer":
				return chat("No hay suficientes jugadores!");
			case "noimplement":
				return chat("Comando aun no implementado!");
			case "full":
				return chat("La partida esta llena!");
			case "notenoughplayers":
				return chat("No hay suficientes jugadores para empezar la partida");
			case "stopping":
				return chat("El juego esta terminando!");
		}
		return null;
	}

	public static String timeReminder(int n) {

		if ((n > 60) && (n % 60 == 0))
			// 60, 120, 180...
			return "&a";
		else if (n > 15 && n <= 60 && (n % 15 == 0))
			// 45 30 15
			return "&e";
		else if (n > 5 && n <= 15 && (n % 5 == 0))
			// 10 5
			return "&6";
		else if (n >= 1 && n <= 5)
			// 4 3 2 1
			return "&c";
		else
			return null;
	}

	public static String formattedString(String s) {
		String firstLetter = s.substring(0, 1);
		String nextLetters = s.substring(1);
		return firstLetter.toUpperCase() + nextLetters.toLowerCase().replace('_', ' ');
	}

	public static void setScoreboard(Player p, String scoreboardName, List<String> entries) {
		Scoreboard board = p.getScoreboard();
		try {
			Objective oldObj = board.getObjective("BlockShuffle");
			oldObj.unregister();
		} catch (Exception e) {
		}

		Objective obj = board.registerNewObjective("BlockShuffle", "dummy");
		obj.setDisplayName(Utils.color(scoreboardName));
		obj.setDisplaySlot(DisplaySlot.SIDEBAR);
		int i = entries.size();
		int sepcount = 0;
		for (String entry : entries) {
			if (entry.equalsIgnoreCase("SPACE")) {
				String sep = " ";
				for (int j = 0; j < sepcount; j++)
					sep = sep.concat(" ");
				sepcount++;
				Score separator = obj.getScore(sep);
				separator.setScore(i);
				i--;
			} else {
				Score score = obj.getScore(Utils.color(entry));
				score.setScore(i);
				i--;
			}
		}
		p.setScoreboard(board);
	}

	public static String warningColor(int value, int maxValue) {
		double percentage = (double) value / maxValue;

		if (percentage > (2/3))
			return "&a";

		else if (percentage <= (2/3) && percentage > (1/3))
			return "&e";

		return "&c";
	}
}
