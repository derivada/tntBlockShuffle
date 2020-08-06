package me.tntpablo.blockshuffle;

import org.bukkit.ChatColor;

public class Utils {
	public static String error(String msg) {
		return ChatColor.translateAlternateColorCodes('&', "&l&a[ &3Block &4&lShuffle&l&a ]&r&f &4&l ERROR: &r&f" + msg);
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

	public static String formattedString(String s){
		String firstLetter = s.substring(0,1);
		String nextLetters = s.substring(1);
		return firstLetter.toUpperCase()+nextLetters.toLowerCase().replace('_', ' ');
	}
}
