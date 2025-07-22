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
            Material.STRIPPED_WARPED_STEM,
            Material.OAK_WOOD,
            Material.SPRUCE_WOOD,
            Material.BIRCH_WOOD,
            Material.JUNGLE_WOOD,
            Material.ACACIA_WOOD,
            Material.DARK_OAK_WOOD,
            Material.MANGROVE_WOOD,
            Material.CHERRY_WOOD,
            Material.CRIMSON_HYPHAE,
            Material.WARPED_HYPHAE,
            Material.STRIPPED_OAK_WOOD,
            Material.STRIPPED_SPRUCE_WOOD,
            Material.STRIPPED_BIRCH_WOOD,
            Material.STRIPPED_JUNGLE_WOOD,
            Material.STRIPPED_ACACIA_WOOD,
            Material.STRIPPED_DARK_OAK_WOOD,
            Material.STRIPPED_MANGROVE_WOOD,
            Material.STRIPPED_CHERRY_WOOD,
            Material.STRIPPED_CRIMSON_HYPHAE,
            Material.STRIPPED_WARPED_HYPHAE
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

        if (player.isSneaking() && isAxe(player.getInventory().getItemInMainHand().getType())) {
            if (isLog(block.getType())) {
                event.setCancelled(true);
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
            final Material originalLogType = startBlock.getType();

            @Override
            public void run() {
                if (blocksToCheck.isEmpty()) {
                    blocksToCheck.add(startBlock);
                    checkedBlocks.add(startBlock);
                }

                int blocksProcessed = 0;
                int maxBlocksPerTick = 5;
                int maxTreeHeight = 50;
                int maxTreeWidth = 5;

                while (!blocksToCheck.isEmpty() && blocksProcessed < maxBlocksPerTick) {
                    Block current = blocksToCheck.poll();
                    blocksProcessed++;

                    Material type = current.getType();

                    if (isLog(type) && type == originalLogType) {
                        // Damage the axe for logs only
                        if (axe != null && axe.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable) {
                            org.bukkit.inventory.meta.Damageable meta = (org.bukkit.inventory.meta.Damageable) axe.getItemMeta();
                            meta.setDamage(meta.getDamage() + 1);
                            if (meta.getDamage() >= axe.getType().getMaxDurability()) {
                                player.getInventory().setItemInMainHand(null);
                                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                                break;
                            }
                            axe.setItemMeta(meta);
                        }

                        current.breakNaturally(axe);
                        player.giveExp(expToDrop);

                    } else if (isLeaf(type)) {
                        current.breakNaturally();
                    }

                    // Scan nearby blocks
                    for (int x = -1; x <= 1; x++) {
                        for (int y = 0; y <= 1; y++) {
                            for (int z = -1; z <= 1; z++) {
                                if (x == 0 && y == 0 && z == 0) continue;

                                Block relative = current.getRelative(x, y, z);
                                Material relativeType = relative.getType();

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

                if (blocksToCheck.isEmpty()) {
                    cancel();
                }
            }
        }.runTaskTimer(this, 0, 1);
    }
}