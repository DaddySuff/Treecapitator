package com.suff.treecapitator;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Treecapitator extends JavaPlugin implements Listener {

    private final Set<Material> AXES = EnumSet.of(
            Material.WOODEN_AXE,
            Material.STONE_AXE,
            Material.IRON_AXE,
            Material.GOLDEN_AXE,
            Material.DIAMOND_AXE,
            Material.NETHERITE_AXE
    );

    private final Set<Material> LOGS = EnumSet.of(
            Material.OAK_LOG,
            Material.SPRUCE_LOG,
            Material.BIRCH_LOG,
            Material.JUNGLE_LOG,
            Material.ACACIA_LOG,
            Material.DARK_OAK_LOG,
            Material.MANGROVE_LOG,
            Material.CHERRY_LOG,
            Material.CRIMSON_STEM,
            Material.WARPED_STEM,
            Material.STRIPPED_OAK_LOG,
            Material.STRIPPED_SPRUCE_LOG,
            Material.STRIPPED_BIRCH_LOG,
            Material.STRIPPED_JUNGLE_LOG,
            Material.STRIPPED_ACACIA_LOG,
            Material.STRIPPED_DARK_OAK_LOG,
            Material.STRIPPED_MANGROVE_LOG,
            Material.STRIPPED_CHERRY_LOG,
            Material.STRIPPED_CRIMSON_STEM,
            Material.STRIPPED_WARPED_STEM
    );

    private final Set<Material> LEAVES = EnumSet.of(
            Material.OAK_LEAVES,
            Material.SPRUCE_LEAVES,
            Material.BIRCH_LEAVES,
            Material.JUNGLE_LEAVES,
            Material.ACACIA_LEAVES,
            Material.DARK_OAK_LEAVES,
            Material.MANGROVE_LEAVES,
            Material.CHERRY_LEAVES,
            Material.NETHER_WART_BLOCK,
            Material.WARPED_WART_BLOCK,
            Material.AZALEA_LEAVES,
            Material.FLOWERING_AZALEA_LEAVES
    );

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Treecapitator enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Treecapitator disabled.");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Check if player is sneaking and holding an axe
        if (player.isSneaking() && isAxe(player.getInventory().getItemInMainHand().getType())) {
            // Check if the broken block is a log
            if (isLog(block.getType())) {
                event.setCancelled(true); // Cancel the original block break
                breakTree(block, player, event.getExpToDrop());
            }
        }
    }

    private boolean isAxe(Material material) {
        return AXES.contains(material);
    }

    private boolean isLog(Material material) {
        return LOGS.contains(material);
    }

    private boolean isLeaf(Material material) {
        return LEAVES.contains(material);
    }

    private void breakTree(Block startBlock, Player player, int expToDrop) {
        new BukkitRunnable() {
            final Set<Block> checkedBlocks = new HashSet<>();
            final Queue<Block> blocksToCheck = new LinkedList<>();
            final ItemStack axe = player.getInventory().getItemInMainHand();
            final World world = startBlock.getWorld();
            final Material originalLogType = startBlock.getType(); // Store the type of the first log

            @Override
            public void run() {
                if (blocksToCheck.isEmpty()) {
                    // First run - start with the initial block
                    blocksToCheck.add(startBlock);
                    checkedBlocks.add(startBlock);
                }

                int blocksProcessed = 0;
                int maxBlocksPerTick = 5; // Process up to 5 blocks per tick for performance
                int maxTreeHeight = 50; // Maximum height of a tree to prevent breaking huge structures
                int maxTreeWidth = 3; // Maximum width to prevent breaking large areas

                while (!blocksToCheck.isEmpty() && blocksProcessed < maxBlocksPerTick) {
                    Block current = blocksToCheck.poll();
                    blocksProcessed++;

                    // Break the block if it's a log of the same type as the original
                    if (isLog(current.getType()) && current.getType() == originalLogType) {
                        // Damage the axe
                        if (axe != null) {
                            if (axe.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable) {
                                org.bukkit.inventory.meta.Damageable meta = (org.bukkit.inventory.meta.Damageable) axe.getItemMeta();
                                meta.setDamage(meta.getDamage() + 1);
                                if (meta.getDamage() >= axe.getType().getMaxDurability()) {
                                    player.getInventory().setItemInMainHand(null);
                                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                                    break;
                                }
                                axe.setItemMeta(meta);
                            }
                        }

                        // Break the block naturally to drop items
                        current.breakNaturally(axe);
                        player.giveExp(expToDrop);
                    }

                    // Check adjacent blocks (only up to maxTreeWidth blocks away horizontally)
                    for (int x = -1; x <= 1; x++) {
                        for (int y = 0; y <= 1; y++) { // Only check above and same level
                            for (int z = -1; z <= 1; z++) {
                                if (x == 0 && y == 0 && z == 0) continue;

                                Block relative = current.getRelative(x, y, z);
                                Material relativeType = relative.getType();

                                // Only check logs of the same type and leaves that haven't been checked yet
                                if ((relativeType == originalLogType || isLeaf(relativeType)) &&
                                        !checkedBlocks.contains(relative) &&
                                        Math.abs(relative.getX() - startBlock.getX()) <= maxTreeWidth &&
                                        Math.abs(relative.getZ() - startBlock.getZ()) <= maxTreeWidth &&
                                        (relative.getY() - startBlock.getY()) <= maxTreeHeight) {

                                    blocksToCheck.add(relative);
                                    checkedBlocks.add(relative);
                                }
                            }
                        }
                    }
                }

                // If there are no more blocks to process, cancel the task
                if (blocksToCheck.isEmpty()) {
                    cancel();
                }
            }
        }.runTaskTimer(this, 0, 1);
    }
}