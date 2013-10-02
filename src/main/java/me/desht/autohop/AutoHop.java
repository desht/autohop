package me.desht.autohop;

/*
    This file is part of autohop

    AutoHop is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    AutoHop is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with AutoHop.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.material.Stairs;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.mcstats.MetricsLite;

public class AutoHop extends JavaPlugin implements Listener {

	private final Set<String> noHoppers = new HashSet<String>();

	private static final Set<Material> passableBlocks = new HashSet<Material>();
	private static final Set<Material> stairBlocks = new HashSet<Material>();

	private BukkitTask saveTask = null;

	private static BlockFace NORTH = BlockFace.NORTH;
	private static BlockFace EAST = BlockFace.EAST;
	private static BlockFace SOUTH = BlockFace.SOUTH;
	private static BlockFace WEST = BlockFace.WEST;

	static {
		passableBlocks.add(Material.AIR);
		passableBlocks.add(Material.WATER);
		passableBlocks.add(Material.STATIONARY_WATER);
		passableBlocks.add(Material.SAPLING);
		passableBlocks.add(Material.POWERED_RAIL);
		passableBlocks.add(Material.DETECTOR_RAIL);
		passableBlocks.add(Material.WEB);
		passableBlocks.add(Material.LONG_GRASS);
		passableBlocks.add(Material.DEAD_BUSH);
		passableBlocks.add(Material.YELLOW_FLOWER);
		passableBlocks.add(Material.RED_ROSE);
		passableBlocks.add(Material.BROWN_MUSHROOM);
		passableBlocks.add(Material.RED_MUSHROOM);
		passableBlocks.add(Material.TORCH);
		passableBlocks.add(Material.FIRE);
		passableBlocks.add(Material.REDSTONE_WIRE);
		passableBlocks.add(Material.CROPS);
		passableBlocks.add(Material.SIGN_POST);
		passableBlocks.add(Material.LADDER);
		passableBlocks.add(Material.RAILS);
		passableBlocks.add(Material.WALL_SIGN);
		passableBlocks.add(Material.LEVER);
		passableBlocks.add(Material.STONE_PLATE);
		passableBlocks.add(Material.WOOD_PLATE);
		passableBlocks.add(Material.REDSTONE_TORCH_OFF);
		passableBlocks.add(Material.REDSTONE_TORCH_ON);
		passableBlocks.add(Material.STONE_BUTTON);
		passableBlocks.add(Material.SNOW);
		passableBlocks.add(Material.SUGAR_CANE);
		passableBlocks.add(Material.PORTAL);
		passableBlocks.add(Material.DIODE_BLOCK_OFF);
		passableBlocks.add(Material.DIODE_BLOCK_ON);
		passableBlocks.add(Material.PUMPKIN_STEM);
		passableBlocks.add(Material.MELON_STEM);
		passableBlocks.add(Material.VINE);
		passableBlocks.add(Material.WATER_LILY);
		passableBlocks.add(Material.NETHER_WARTS);
		passableBlocks.add(Material.ENDER_PORTAL);
		passableBlocks.add(Material.TRIPWIRE);
		passableBlocks.add(Material.TRIPWIRE_HOOK);
		passableBlocks.add(Material.CARROT);
		passableBlocks.add(Material.POTATO);
		passableBlocks.add(Material.FLOWER_POT);
		passableBlocks.add(Material.SKULL);
		passableBlocks.add(Material.GOLD_PLATE);
		passableBlocks.add(Material.IRON_PLATE);
		passableBlocks.add(Material.DAYLIGHT_DETECTOR);
		passableBlocks.add(Material.CARPET);
		// yeah, fences/cobble-walls aren't passable, but this prevents players attempting to jump them at all
		passableBlocks.add(Material.FENCE);
		passableBlocks.add(Material.COBBLE_WALL);

		for (Material m : Material.values()) {
			if (m.toString().endsWith("_STAIRS")) {
				stairBlocks.add(m);
			}
		}
	}

	@Override
	public void onDisable() {
		saveConf();
	}

	@Override
	public void onEnable() {
		PluginManager pm = this.getServer().getPluginManager();

		pm.registerEvents(this, this);

		if (BlockFace.NORTH.getModX() == -1) {
			// legacy support - breaking BlockFace change as of Dec 4th 2012
			// https://github.com/Bukkit/Bukkit/commit/e468a8b391336f292d3642ffa4c45b4600e91b64
			NORTH = BlockFace.EAST;
			EAST = BlockFace.SOUTH;
			SOUTH = BlockFace.WEST;
			WEST = BlockFace.NORTH;
		}

		try {
			MetricsLite metrics = new MetricsLite(this);
			metrics.start();
		} catch (IOException e) {
			this.getLogger().log(Level.WARNING, "Couldn't submit metrics stats: " + e.getMessage());
		}

		if (getConfig().contains("nohop")) {
			for (String s : getConfig().getStringList("nohop")) {
				noHoppers.add(s);
			}
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equals("ahop")) {
			if (sender instanceof Player) {
				String name = sender.getName();
				if (noHoppers.contains(name)) {
					sender.sendMessage(ChatColor.YELLOW + "Autohop ENABLED!");
					noHoppers.remove(name);
				} else {
					sender.sendMessage(ChatColor.YELLOW + "Autohop DISABLED!");
					noHoppers.add(name);
				}
				if (saveTask == null) {
					saveTask = Bukkit.getScheduler().runTaskLater(this, new Runnable() {
						@Override
						public void run() {
							saveConf();
						}
					}, 1200L);
				}
			} else {
				sender.sendMessage(ChatColor.RED + "This command can't be used from the console.");
			}
			return true;
		} else {
			return false;
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
		//		long s0 = System.nanoTime();

		if (!event.getPlayer().hasPermission("autohop.hop") || noHoppers.contains(event.getPlayer().getName())) {
			return;
		}

		Location from = event.getFrom();
		Location to = event.getTo();

		// delta X and Z - which way the player is going
		double dx = to.getX() - from.getX();
		double dz = to.getZ() - from.getZ();
		// extrapolation of next X and Z the player will get to
		double nextX = to.getX() + dx;
		double nextZ = to.getZ() + dz;
		// X and Z position within a block - a player pushing against a wall will 
		// have X or Z either < ~0.3 or > ~0.7 due to player entity bounding box size
		double tx = nextX - Math.floor(nextX);
		double tz = nextZ - Math.floor(nextZ);

		//		 System.out.println("yaw = " + to.getYaw() + " dx = " + dx + " dz = " + dz + " nextX = " + nextX + " tx = " + tx + " nextZ = " + nextZ + " tz = " + tz);

		float yaw = to.getYaw() % 360;
		if (yaw < 0) yaw += 360;

		BlockFace face;
		if (yaw >= 45 && yaw < 135 && dx <= 0.0 && tx < 0.3001) {
			face = WEST;
		} else if (yaw >= 135 && yaw < 225 && dz <= 0.0 && tz < 0.3001) {
			face = NORTH;
		} else if (yaw >= 225 && yaw < 315 && dx >= 0.0 && tx > 0.6999) {
			face = EAST;
		} else if ((yaw >= 315 || yaw < 45) && dz >= 0.0 && tz > 0.6999) {
			face = SOUTH;
		} else {
            // impossible!
			return;
		}

		// the block we're trying to move into
		Block b = to.getBlock().getRelative(face);
		//		 System.out.println("check block " + face + " type = " + b.getType());

		boolean climbable = false;
		if (stairBlocks.contains(b.getType())) {
			Stairs s = (Stairs)b.getState().getData();
            climbable = s.getAscendingDirection() == face;
			// System.out.println("see some stairs: climbable = " + climbable);
		} else if (isSlab(b)) {
			climbable = true;
		}

		if (!climbable && !isPassable(b)) {
			// trying to move into a non-passable or climbable block; see if we're able to jump onto it
			Block above = b.getRelative(BlockFace.UP);
			boolean onSlab = standingOnSlab(from);
			if (isPassable(above) && ((Entity)event.getPlayer()).isOnGround() && !onSlab) {
				event.setTo(new Location(from.getWorld(), nextX, from.getY() + 1, nextZ, from.getYaw(), from.getPitch()));
			} else if (isSlab(above) && onSlab && isPassable(above.getRelative(BlockFace.UP))) {
				event.setTo(new Location(from.getWorld(), nextX, above.getY() + getBlockThickness(above), nextZ, from.getYaw(), from.getPitch()));
			}
		}

		// System.out.println("event handler: " + (System.nanoTime() - s0));
	}

	private double getBlockThickness(Block b) {
		if (b.getType() == Material.SNOW && b.getData() >= 3) {
			return b.getData() * 0.1250025;
		} else if (isSlab(b)) {
			return 0.5;
		} else {
			return 1.0;
		}
	}

	private boolean isPassable(Block b) {
		if (b.getType() == Material.SNOW) {
			return b.getData() < 5;
		} else {
			return passableBlocks.contains(b.getType());
		}
	}

	private boolean isSlab(Block b) {
		return b.getType() == Material.STEP || b.getType() == Material.WOOD_STEP || b.getType() == Material.SNOW && (b.getData() == 3 || b.getData() == 4);
	}

	private boolean standingOnSlab(Location location) {
		Block b = location.getBlock();
		if (b.getType() == Material.SNOW && b.getData() >= 3) {
			return location.getY() % 1 <= b.getData() * 0.1250025;
		} else if (isSlab(b)) {
			return location.getY() % 1 <= 0.501;
		} else {
			return false;
		}
	}

	private void saveConf() {
		List<String> l = new ArrayList<String>();
		for (String s : noHoppers) {
			l.add(s);
		}
		getConfig().set("nohop", l);
		saveConfig();
		saveTask = null;
	}
}
