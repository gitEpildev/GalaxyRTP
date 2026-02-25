package com.galaxyrealms.api;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;

/**
 * Ensures the vanilla crafting table recipe (4 planks in 2x2 -> crafting table) exists.
 * Runs delayed after server load so it overrides any plugin that removed or changed it.
 */
public class CraftingTableRecipeFix {

    public static void runDelayed(JavaPlugin plugin) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> apply(plugin), 40L);
    }

    private static void apply(JavaPlugin plugin) {
        // Remove any existing recipes that produce a crafting table (so our correct one is the only one)
        removeCraftingTableRecipes();

        // Add vanilla recipe: 4 planks in 2x2 -> 1 crafting table
        NamespacedKey key = new NamespacedKey(plugin, "crafting_table_fix");
        ItemStack result = new ItemStack(Material.CRAFTING_TABLE, 1);
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape("XX", "XX");
        RecipeChoice planks = new RecipeChoice.MaterialChoice(
            Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS,
            Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS,
            Material.MANGROVE_PLANKS, Material.CHERRY_PLANKS, Material.BAMBOO_PLANKS,
            Material.CRIMSON_PLANKS, Material.WARPED_PLANKS
        );
        recipe.setIngredient('X', planks);

        Bukkit.addRecipe(recipe);
        plugin.getLogger().info("Crafting table recipe fix applied (4 planks -> crafting table)");
    }

    private static void removeCraftingTableRecipes() {
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe r = it.next();
            if (r != null && r.getResult().getType() == Material.CRAFTING_TABLE && r instanceof Keyed) {
                try {
                    Bukkit.removeRecipe(((Keyed) r).getKey());
                } catch (Throwable ignored) {
                    // removeRecipe might not exist on all servers
                }
            }
        }
    }
}
