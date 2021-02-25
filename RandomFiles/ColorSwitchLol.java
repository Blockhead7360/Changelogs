package com.blockhead7360.mc.colorswitchlol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class ColorSwitchLol extends JavaPlugin implements Listener {

//test

	private final int PLATFORM_LENGTH = 3;
	private final int ARENA_LENGTH = 48;
	private final int DEC_EVERY_ROUND = 1;

	//IN 2s (seconds * 10)
	private final int START_TICKS = 80;
	private final int DEC_TICKS = 2;
	private final int MIN_TICKS = 10;
	private final int WAIT_TICKS = 40;

	private final String[] COLORS = {"WHITE_WOOL", "PURPLE_WOOL", "YELLOW_WOOL",
			"PINK_WOOL", "BLUE_WOOL", "LIGHT_BLUE_WOOL", "LIME_WOOL", "GRAY_WOOL"};

	private List<String> players;
	private List<String> dead;
	private List<Material> platform;

	private Location mapLoc;
	private Location joinLoc;
	private Location leaveLoc;

	private boolean running;

	public void onEnable() {
		saveDefaultConfig();

		mapLoc = getConfig().getLocation("mapLoc");
		joinLoc = getConfig().getLocation("joinLoc");
		leaveLoc = getConfig().getLocation("leaveLoc");


		this.players = new ArrayList<String>();
		this.dead = new ArrayList<String>();
		this.platform = new ArrayList<Material>();

		running = false;


		for (String color : COLORS) {

			for (int i = 0; i < (ARENA_LENGTH / PLATFORM_LENGTH) * 2; i++) {

				platform.add(Material.getMaterial(color));

			}

		}

		this.getServer().getPluginManager().registerEvents(this, this);

	}




	public void onDisable() {
		saveConfig();
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {

		if (running) {

			if (players.contains(e.getPlayer().getName()) && !dead.contains(e.getPlayer().getName())) {

				if (e.getPlayer().getLocation().getY() <= mapLoc.getBlockY()) {

					dead(e.getPlayer());
					return;

				}

			}

		}

	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {

		if (players.contains(e.getPlayer().getName())) {
			leaveArena(e.getPlayer());
		}

	}

	public void startGame() {

		Bukkit.getServer().broadcastMessage(ChatColor.WHITE + "A game of ColorSwitchLol has started.");

		if (running) return;

		running = true;

		Random r = new Random();

		Material starter = Material.getMaterial(COLORS[r.nextInt(COLORS.length)]);

		World world = mapLoc.getWorld();
		final int startX = mapLoc.getBlockX();
		final int startZ = mapLoc.getBlockZ();
		final int endX = startX + ARENA_LENGTH;
		final int endZ = startZ + ARENA_LENGTH;
		final int y = mapLoc.getBlockY();

		for (int x = startX; x < endX; x++) {

			for (int z = startZ; z < endZ; z++) {

				Location loc = new Location(world, x, y, z);

				loc.getBlock().setType(starter);

			}

		}

		for (String s : players) {

			getServer().getPlayer(s).teleport(joinLoc);

		}

		new BukkitRunnable() {

			int round = 0;
			int totalTime = START_TICKS;
			int currentTime = WAIT_TICKS;
			Material color = null;


			//0, waiting; 0, playing; -1, operation in progress
			int mode = 0;

			public void run() {

				if (!running) {
					cancel();
				}

				if (currentTime % 10 == 0) {

					String text = (mode == 1 ? "Time: " : "Waiting: ");

					for (String s : players) {

						getServer().getPlayer(s).spigot().sendMessage(ChatMessageType.ACTION_BAR,
								TextComponent.fromLegacyText(ChatColor.WHITE + text + ChatColor.GREEN + "" + ChatColor.BOLD + (currentTime/10) + " second(s)"));

					}

				}

				if (mode == 1 && currentTime == 0) {

					mode = -1;
					
					for (String s : players) {

						Player p = getServer().getPlayer(s);
						p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

					}


					for (int x = startX; x < endX; x++) {

						for (int z = startZ; z < endZ; z++) {

							Block b = world.getBlockAt(x, y, z);

							if (b.getType() != color) {
								b.setType(Material.AIR);
							}

						}

					}

					mode = 0;
					currentTime = WAIT_TICKS;


				}

				if (mode == 0 && currentTime == 0) {

					mode = -1;

					Collections.shuffle(platform);

					//fill arena

					color = Material.getMaterial(COLORS[r.nextInt(COLORS.length)]);

					//						List<Material> list = new ArrayList<Material>();
					//						list.addAll(platform);


					int size = ARENA_LENGTH / PLATFORM_LENGTH;

					int index = 0;

					for (int i = 0; i < size; i++) {
						for (int j = 0; j < size; j++) {
							Material color = platform.get(index);

							int iStartX = startX + (i * PLATFORM_LENGTH);
							int iStartZ = startZ + (j * PLATFORM_LENGTH);
							int iEndX = iStartX + PLATFORM_LENGTH;
							int iEndZ = iStartZ + PLATFORM_LENGTH;

							for (int x = iStartX; x < iEndX; x++) {
								for (int z = iStartZ; z < iEndZ; z++) {
									world.getBlockAt(x, y, z).setType(color);
								}	
							}

							index++;
						}
					}

					mode = 1;
					round++;
					if (round % DEC_EVERY_ROUND == 0) {
						totalTime = START_TICKS - (DEC_TICKS * (round - 1));

						if (totalTime < MIN_TICKS) totalTime = MIN_TICKS;
					}

					for (String s : players) {
						Player p = getServer().getPlayer(s);
						for (int i = 0; i < 9; i++) {
							p.getInventory().setItem(i, new ItemStack(color, 1));
						}

						p.updateInventory();
						p.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Round " + round);
						p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

					}

					currentTime = totalTime;

				}

				currentTime--;

			}

		}.runTaskTimer(this, 0, 2L);


	}

	public void joinArena(Player player) {

		players.add(player.getName());

		Bukkit.getServer().broadcastMessage(ChatColor.GREEN + player.getName() + ChatColor.WHITE + " joined the ColorSwitchLol arena! (total: " + players.size() + ")");

	}

	public void leaveArena(Player player) {

		players.remove(player.getName());

		if (dead.contains(player.getName())) {
			dead.remove(player.getName());
		} else {
			if (player.isOnline()) {
				player.teleport(leaveLoc);
				for (int i = 0; i < 9; i++) {

					player.getInventory().clear(i);

				}
				player.updateInventory();
			}
		}

		Bukkit.getServer().broadcastMessage(ChatColor.RED + player.getName() + ChatColor.WHITE + " left the ColorSwitchLol arena! (total: " + players.size() + ")");

		if (players.size() == 0) {

			if (running) {
				running = false;
				Bukkit.getServer().broadcastMessage(ChatColor.WHITE + "The game of ColorSwitchLol has been terminated.");
			}
		}

	}

	public void dead(Player player) {

		dead.add(player.getName());

		Bukkit.getServer().broadcastMessage(ChatColor.BLUE + player.getName() + ChatColor.WHITE + " has died in the ColorSwitchLol game! (remaining: " + (players.size() - dead.size()) + ")");
		
		for (String s : players) {

			Player p = getServer().getPlayer(s);
			p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1);

		}

		player.teleport(leaveLoc);

		for (int i = 0; i < 9; i++) {

			player.getInventory().clear(i);

		}
		player.updateInventory();

		if (dead.size() == players.size()) {

			for (String s : players) {
				
				Player p = getServer().getPlayer(s);

				p.sendTitle(ChatColor.GREEN + player.getName(), ChatColor.WHITE + "won the game!", 10, 60, 10);

				p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 1);
				
			}

			Bukkit.getServer().broadcastMessage(ChatColor.GREEN + player.getName() + ChatColor.WHITE + " won the ColorSwitchLol game.");
			running = false;
			dead.clear();

		}

	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (cmd.getName().equalsIgnoreCase("csl")) {

			if (args.length == 0) {

				sender.sendMessage(ChatColor.GRAY + "This server is running " + ChatColor.AQUA + getDescription().getName()
						+ ChatColor.GRAY + " version " + ChatColor.AQUA + getDescription().getVersion()
						+ ChatColor.GRAY + " by " + ChatColor.AQUA + getDescription().getAuthors().get(0));
				sender.sendMessage(ChatColor.GRAY + getDescription().getDescription());
				return true;

			}

			if (!(sender instanceof Player)) {
				sender.sendMessage("Only players can use this command.");
				return true;
			}

			Player player = (Player) sender;

			if (args[0].equalsIgnoreCase("join")) {

				if (running) {
					player.sendMessage("The game has already started. Wait for it to finish before you try joining.");
					return true;
				}

				if (players.contains(player.getName())) {
					player.sendMessage("You're already in the game. Wait for others to join if you want and then type '/csl start' to start it.");
					return true;
				}

				for (int i = 0; i < 9; i++) {
					if (player.getInventory().getItem(i) != null && !(player.getInventory().getItem(i).getType() != Material.AIR)) {

						player.sendMessage("Your hotbar must be clear before you can play.");
						return true;

					}

				}


				joinArena(player);

				return true;

			}

			if (args[0].equalsIgnoreCase("leave")) {

				if (!players.contains(player.getName())) {
					sender.sendMessage("You aren't in the game. Join it with '/csl join'.");
					return true;
				}

				leaveArena(player);
				return true;

			}

			if (args[0].equalsIgnoreCase("start")) {

				if (players.size() == 0) {

					player.sendMessage("No one has joined the game. Join it with '/csl join'.");
					return true;

				}

				if (!players.contains(player.getName())) {

					player.sendMessage("You have to join the game to start it. Join it with '/csl join'.");
					return true;

				}

				if (running) {

					player.sendMessage("The game is already running. To stop it, have all players do '/csl leave'.");
					return true;

				}

				startGame();


				return true;

			}

			if (args[0].equalsIgnoreCase("maploc")) {

				if (!player.hasPermission("colorswitchlol.admin")) {

					player.sendMessage("You don't have permission.");
					return true;

				}

				Block block = player.getTargetBlockExact(4);
				if (block == null || block.getType() == Material.AIR) {

					player.sendMessage("Invalid block type. Look at a solid block");
					return true;

				}

				Location loc = block.getLocation();

				int x = loc.getBlockX();
				int y = loc.getBlockY();
				int z = loc.getBlockZ();

				for (int x1 = 0; x1 < ARENA_LENGTH; x1++) {
					for (int z1 = 1; z1 < ARENA_LENGTH; z1++) {

						if (loc.getWorld().getBlockAt(x + x1, y, z + z1).getType() != Material.AIR) {

							player.sendMessage("Block interference detected. Make sure you have an empty " + ARENA_LENGTH + "x" + ARENA_LENGTH
									+ " area on this y level in the positive x and z directions.");
							return true;

						}
					}
				}



				for (int i = 0; i < ARENA_LENGTH; i++) {

					loc.getWorld().getBlockAt(x + i, y, z).setType(Material.GLASS);
					loc.getWorld().getBlockAt(x + i, y, z + ARENA_LENGTH - 1).setType(Material.GLASS);
					loc.getWorld().getBlockAt(x, y, z + i).setType(Material.GLASS);
					loc.getWorld().getBlockAt(x + ARENA_LENGTH - 1, y, z + i).setType(Material.GLASS);

				}

				this.mapLoc = loc;
				getConfig().set("mapLoc", this.mapLoc);
				saveConfig();

				player.sendMessage("Successfully set the arena bounds. Check the glass outline to make sure it's what you want.");

				return true;

			}

			if (args[0].equalsIgnoreCase("joinloc")) {

				if (!player.hasPermission("colorswitchlol.admin")) {

					player.sendMessage("You don't have permission.");
					return true;

				}

				this.joinLoc = player.getLocation().getBlock().getLocation();
				getConfig().set("joinLoc", this.joinLoc);
				saveConfig();
				player.sendMessage("Successfully set the join location.");
				return true;

			}

			if (args[0].equalsIgnoreCase("leaveloc")) {

				if (!player.hasPermission("colorswitchlol.admin")) {

					player.sendMessage("You don't have permission.");
					return true;

				}

				this.leaveLoc = player.getLocation().getBlock().getLocation();
				getConfig().set("leaveLoc", this.leaveLoc);
				saveConfig();
				player.sendMessage("Successfully set the leave location.");
				return true;

			}

			else {

				player.sendMessage("Unknown command.");
				return true;

			}


		}

		return true;

	}

}
