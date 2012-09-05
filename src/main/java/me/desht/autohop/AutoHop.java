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
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.material.Stairs;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class AutoHop extends JavaPlugin implements Listener {

	private static Set<Integer> passable = new HashSet<Integer>();

	static {
		passable.add(Material.AIR.getId());
		passable.add(Material.WATER.getId());
		passable.add(Material.STATIONARY_WATER.getId());
		passable.add(Material.SAPLING.getId());
		passable.add(Material.POWERED_RAIL.getId());
		passable.add(Material.DETECTOR_RAIL.getId());
		passable.add(Material.WEB.getId());
		passable.add(Material.LONG_GRASS.getId());
		passable.add(Material.DEAD_BUSH.getId());
		passable.add(Material.YELLOW_FLOWER.getId());
		passable.add(Material.RED_ROSE.getId());
		passable.add(Material.BROWN_MUSHROOM.getId());
		passable.add(Material.RED_MUSHROOM.getId());
		passable.add(Material.TORCH.getId());
		passable.add(Material.FIRE.getId());
		passable.add(Material.REDSTONE_WIRE.getId());
		passable.add(Material.CROPS.getId());
		passable.add(Material.SIGN_POST.getId());
		passable.add(Material.LADDER.getId());
		passable.add(Material.RAILS.getId());
		passable.add(Material.WALL_SIGN.getId());
		passable.add(Material.LEVER.getId());
		passable.add(Material.STONE_PLATE.getId());
		passable.add(Material.WOOD_PLATE.getId());
		passable.add(Material.REDSTONE_TORCH_OFF.getId());
		passable.add(Material.REDSTONE_TORCH_ON.getId());
		passable.add(Material.STONE_BUTTON.getId());
		passable.add(Material.SNOW.getId());
		passable.add(Material.SUGAR_CANE.getId());
		passable.add(Material.PORTAL.getId());
		passable.add(Material.DIODE_BLOCK_OFF.getId());
		passable.add(Material.DIODE_BLOCK_ON.getId());
		passable.add(Material.PUMPKIN_STEM.getId());
		passable.add(Material.MELON_STEM.getId());
		passable.add(Material.VINE.getId());
		passable.add(Material.WATER_LILY.getId());
		passable.add(Material.NETHER_WARTS.getId());
		passable.add(Material.ENDER_PORTAL.getId());
		passable.add(Material.TRIPWIRE.getId());
		passable.add(Material.TRIPWIRE_HOOK.getId());
		// yeah, fences aren't passable, but this prevents players attempting to jump them at all
		passable.add(Material.FENCE.getId());
	}

	@Override
	public void onDisable() {
	}

	@Override
	public void onEnable() { 
		PluginManager pm = this.getServer().getPluginManager();

		pm.registerEvents(this, this);
		
		try {
			MetricsLite metrics = new MetricsLite(this);
			metrics.start();
		} catch (IOException e) {
			this.getLogger().log(Level.WARNING, "Couldn't submit metrics stats: " + e.getMessage());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
		//		long s0 = System.nanoTime();

		if (!event.getPlayer().hasPermission("autohop.hop"))
			return;

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

		// System.out.println("yaw = " + t.getYaw() + " dx = " + dx + " dz = " + dz + " nextX = " + nextX + " tx = " + tx + " nextZ = " + nextZ + " tz = " + tz);

		float yaw = to.getYaw() % 360;
		if (yaw < 0) yaw += 360;

		BlockFace face = null;
		if (yaw >= 45 && yaw < 135 && dx <= 0.0 && tx < 0.3001) {
			face = BlockFace.NORTH;
		} else if (yaw >= 135 && yaw < 225 && dz <= 0.0 && tz < 0.3001) {
			face = BlockFace.EAST;
		} else if (yaw >= 225 && yaw < 315 && dx >= 0.0 && tx > 0.6999) {
			face = BlockFace.SOUTH;
		} else if ((yaw >= 315 || yaw < 45) && dz >= 0.0 && tz > 0.6999) {
			face = BlockFace.WEST;
		} else {
			return;
		}

		// the block we're trying to move into
		Block b = to.getBlock().getRelative(face);
		// System.out.println("check block " + face + " type = " + b.getType());

		boolean climbable = false;
		if (isStairs(b.getTypeId())) {
			Stairs s = (Stairs)b.getState().getData();
			climbable = s.getAscendingDirection() == face;
			// System.out.println("see some stairs: climbable = " + climbable);
		} else if (isSlab(b.getTypeId())) {
			climbable = true;
		}

		if (!climbable && !passable.contains(b.getTypeId())) {
			// trying to move into a non-passable or climbable block
			// see if we're able to jump onto it
			
			int id1 = b.getRelative(BlockFace.UP).getTypeId();
			int id2 = b.getRelative(BlockFace.UP, 2).getTypeId();
			
			// ensure there's room above the block we want to jump on
			// if there's a slab on that block, we can still jump, iff we're already on a slab
			if ((passable.contains(id1) || isSlab(id1) && standingOnSlab(from)) && passable.contains(id2)) {
				
				// is player standing on solid ground or on (including partway up) some stairs or a slab?
				if (from.getY() % 1 < 0.0001 && !passable.contains(from.getBlock().getRelative(BlockFace.DOWN).getTypeId())
						|| isStairs(from.getBlock().getTypeId())
						|| standingOnSlab(from)) {
					
					Vector v = event.getPlayer().getVelocity();
					// System.out.println("current velocity = " + v + ", y pos = " + f.getY() + "->" + t.getY());
					v.setY(0.37);
					event.getPlayer().setVelocity(v);
				}
			}
		}

		// System.out.println("event handler: " + (System.nanoTime() - s0));
	}
	
	private boolean isStairs(int id) {
		return id == Material.COBBLESTONE_STAIRS.getId() || id == Material.WOOD_STAIRS.getId();
	}
	
	private boolean isSlab(int id) {
		return id == Material.STEP.getId() || id == Material.WOOD_STEP.getId();
	}
	
	private boolean standingOnSlab(Location l) {
		return isSlab(l.getBlock().getTypeId()) && l.getY() % 1 <= 0.51;
	}
}
