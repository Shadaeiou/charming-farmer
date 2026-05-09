package com.shadaeiou.charmingfarmer.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shadaeiou.charmingfarmer.data.room.AppDatabase
import com.shadaeiou.charmingfarmer.data.room.KitchenRunEntity
import com.shadaeiou.charmingfarmer.data.room.LegacyMigrator
import com.shadaeiou.charmingfarmer.service.LocalNotifier
import kotlin.random.Random

/**
 * One ingredient on a recipe — the crop it comes from and how many
 * pieces are needed. The displayed cost per recipe is the sum of each
 * ingredient's [CropType.sellPrice] times its quantity, so a recipe
 * that calls for top-shelf produce costs the player the same coins it
 * would have netted them at market.
 */
data class RecipeIngredient(val crop: CropType, val quantity: Int)

/**
 * Cooking appliances. Each recipe is assigned to exactly one. The
 * kitchen has one of each, so up to three recipes can be cooking at
 * the same time (one per appliance), plus one prep slot shared across
 * the whole kitchen.
 */
enum class Appliance(val displayName: String, val emoji: String) {
    STOVE("Stove", "🍳"),
    OVEN("Oven", "🔥"),
    AIR_FRYER("Air Fryer", "🌬️"),
}

/**
 * Cooking happens in two stages:
 *   - PREP: knife work / mixing / kneading. 5-45 s per recipe. Only
 *     one PREP can be in flight across the whole kitchen.
 *   - COOK: applying heat at the appliance. One COOK per appliance.
 */
enum class CookStage { PREP, COOK }

/**
 * A kitchen recipe. Phase 1 is coin-priced — paying a recipe's cost
 * stands in for buying the ingredients. The dish drops into the FARM
 * silo as an artisan good with a quality roll, so it can be sold like
 * any other ItemStack.
 */
enum class KitchenRecipe(
    val displayName: String,
    val outputType: ItemType,
    val ingredients: List<RecipeIngredient>,
    val basePrice: Int,
    val cookEnergy: Int,
    /** Cook duration applied during [CookStage.COOK]. Halved standard
     *  vs. previous releases — a 4 m soup is now 2 m. */
    val cookDurationMs: Long,
    /** Prep duration during [CookStage.PREP] — between 5 and 45 s. */
    val prepDurationMs: Long,
    /** Player-facing prep verb (e.g. "Chop", "Knead"). */
    val prepLabel: String,
    /** Best-fit emoji for the prep step. Falls back to 🧑‍🍳 when
     *  there's no close match. */
    val prepEmoji: String,
    val appliance: Appliance,
) {
    // ----- Existing recipes (cook times halved, prep step added) -----
    CARROT_SOUP(
        displayName = "Carrot Soup",
        outputType = ItemType.DISH_CARROT_SOUP,
        ingredients = listOf(RecipeIngredient(CropType.CARROT, 5)),
        basePrice = 110,
        cookEnergy = 3,
        cookDurationMs = 2 * 60_000L,
        prepDurationMs = 20_000L,
        prepLabel = "Chop",
        prepEmoji = "🔪",
        appliance = Appliance.STOVE,
    ),
    MASHED_POTATOES(
        displayName = "Mashed Potatoes",
        outputType = ItemType.DISH_MASHED_POTATOES,
        ingredients = listOf(RecipeIngredient(CropType.POTATO, 5)),
        basePrice = 170,
        cookEnergy = 3,
        cookDurationMs = (2.5 * 60_000L).toLong(),
        prepDurationMs = 25_000L,
        prepLabel = "Peel",
        prepEmoji = "🔪",
        appliance = Appliance.STOVE,
    ),
    STRAWBERRY_JAM(
        displayName = "Strawberry Jam",
        outputType = ItemType.DISH_STRAWBERRY_JAM,
        ingredients = listOf(RecipeIngredient(CropType.STRAWBERRY, 6)),
        basePrice = 2_200,
        cookEnergy = 4,
        cookDurationMs = 4 * 60_000L,
        prepDurationMs = 30_000L,
        prepLabel = "Mash",
        prepEmoji = "🥄",
        appliance = Appliance.STOVE,
    ),
    VEGGIE_STEW(
        displayName = "Veggie Stew",
        outputType = ItemType.DISH_VEGGIE_STEW,
        ingredients = listOf(
            RecipeIngredient(CropType.POTATO, 3),
            RecipeIngredient(CropType.CARROT, 2),
            RecipeIngredient(CropType.ONION, 1),
            RecipeIngredient(CropType.BROCCOLI, 1),
        ),
        basePrice = 1_000,
        cookEnergy = 5,
        cookDurationMs = 6 * 60_000L,
        prepDurationMs = 35_000L,
        prepLabel = "Dice",
        prepEmoji = "🔪",
        appliance = Appliance.STOVE,
    ),
    STUFFED_PEPPERS(
        displayName = "Stuffed Peppers",
        outputType = ItemType.DISH_STUFFED_PEPPER,
        ingredients = listOf(
            RecipeIngredient(CropType.PEPPER, 3),
            RecipeIngredient(CropType.CORN, 2),
            RecipeIngredient(CropType.BEAN, 2),
            RecipeIngredient(CropType.TOMATO, 1),
        ),
        basePrice = 22_000,
        cookEnergy = 6,
        cookDurationMs = 9 * 60_000L,
        prepDurationMs = 40_000L,
        prepLabel = "Stuff",
        prepEmoji = "🧑‍🍳",
        appliance = Appliance.OVEN,
    ),
    PUMPKIN_PIE(
        displayName = "Pumpkin Pie",
        outputType = ItemType.DISH_PUMPKIN_PIE,
        ingredients = listOf(
            RecipeIngredient(CropType.PUMPKIN, 1),
            RecipeIngredient(CropType.WHEAT, 2),
        ),
        basePrice = 4_900,
        cookEnergy = 5,
        cookDurationMs = 7 * 60_000L,
        prepDurationMs = 45_000L,
        prepLabel = "Knead",
        prepEmoji = "🥄",
        appliance = Appliance.OVEN,
    ),
    TRUFFLE_RISOTTO(
        displayName = "Truffle Risotto",
        outputType = ItemType.DISH_TRUFFLE_RISOTTO,
        ingredients = listOf(
            RecipeIngredient(CropType.TRUFFLE, 1),
            RecipeIngredient(CropType.MUSHROOM, 2),
            RecipeIngredient(CropType.GARLIC, 1),
            RecipeIngredient(CropType.ONION, 1),
        ),
        basePrice = 400_000,
        cookEnergy = 8,
        cookDurationMs = 15 * 60_000L,
        prepDurationMs = 45_000L,
        prepLabel = "Shave",
        prepEmoji = "🔪",
        appliance = Appliance.STOVE,
    ),

    // ----- 30 new recipes -----
    ROASTED_CARROTS(
        displayName = "Roasted Carrots",
        outputType = ItemType.DISH_ROASTED_CARROTS,
        ingredients = listOf(RecipeIngredient(CropType.CARROT, 3)),
        basePrice = 70,
        cookEnergy = 2,
        cookDurationMs = 90_000L,
        prepDurationMs = 15_000L,
        prepLabel = "Peel",
        prepEmoji = "🔪",
        appliance = Appliance.OVEN,
    ),
    HASH_BROWNS(
        displayName = "Hash Browns",
        outputType = ItemType.DISH_HASH_BROWNS,
        ingredients = listOf(
            RecipeIngredient(CropType.POTATO, 3),
            RecipeIngredient(CropType.ONION, 1),
        ),
        basePrice = 200,
        cookEnergy = 3,
        cookDurationMs = 90_000L,
        prepDurationMs = 25_000L,
        prepLabel = "Grate",
        prepEmoji = "🧀",
        appliance = Appliance.STOVE,
    ),
    CARAMELIZED_ONIONS(
        displayName = "Caramelized Onions",
        outputType = ItemType.DISH_CARAMELIZED_ONIONS,
        ingredients = listOf(RecipeIngredient(CropType.ONION, 4)),
        basePrice = 320,
        cookEnergy = 3,
        cookDurationMs = 4 * 60_000L,
        prepDurationMs = 25_000L,
        prepLabel = "Slice",
        prepEmoji = "🔪",
        appliance = Appliance.STOVE,
    ),
    GARLIC_BREAD(
        displayName = "Garlic Bread",
        outputType = ItemType.DISH_GARLIC_BREAD,
        ingredients = listOf(
            RecipeIngredient(CropType.GARLIC, 1),
            RecipeIngredient(CropType.WHEAT, 1),
        ),
        basePrice = 19_500,
        cookEnergy = 3,
        cookDurationMs = 90_000L,
        prepDurationMs = 25_000L,
        prepLabel = "Mince",
        prepEmoji = "🔪",
        appliance = Appliance.OVEN,
    ),
    MUSHROOM_RISOTTO(
        displayName = "Mushroom Risotto",
        outputType = ItemType.DISH_MUSHROOM_RISOTTO,
        ingredients = listOf(
            RecipeIngredient(CropType.MUSHROOM, 4),
            RecipeIngredient(CropType.ONION, 1),
            RecipeIngredient(CropType.GARLIC, 1),
        ),
        basePrice = 22_000,
        cookEnergy = 5,
        cookDurationMs = 4 * 60_000L,
        prepDurationMs = 30_000L,
        prepLabel = "Slice",
        prepEmoji = "🔪",
        appliance = Appliance.STOVE,
    ),
    TOMATO_SOUP(
        displayName = "Tomato Soup",
        outputType = ItemType.DISH_TOMATO_SOUP,
        ingredients = listOf(
            RecipeIngredient(CropType.TOMATO, 6),
            RecipeIngredient(CropType.ONION, 1),
            RecipeIngredient(CropType.GARLIC, 1),
        ),
        basePrice = 28_000,
        cookEnergy = 4,
        cookDurationMs = 3 * 60_000L,
        prepDurationMs = 25_000L,
        prepLabel = "Dice",
        prepEmoji = "🔪",
        appliance = Appliance.STOVE,
    ),
    THREE_BEAN_CHILI(
        displayName = "Three-Bean Chili",
        outputType = ItemType.DISH_THREE_BEAN_CHILI,
        ingredients = listOf(
            RecipeIngredient(CropType.BEAN, 4),
            RecipeIngredient(CropType.TOMATO, 2),
            RecipeIngredient(CropType.PEPPER, 1),
            RecipeIngredient(CropType.ONION, 1),
        ),
        basePrice = 11_000,
        cookEnergy = 5,
        cookDurationMs = 5 * 60_000L,
        prepDurationMs = 35_000L,
        prepLabel = "Dice",
        prepEmoji = "🔪",
        appliance = Appliance.STOVE,
    ),
    CORNBREAD(
        displayName = "Cornbread",
        outputType = ItemType.DISH_CORNBREAD,
        ingredients = listOf(
            RecipeIngredient(CropType.CORN, 3),
            RecipeIngredient(CropType.WHEAT, 1),
        ),
        basePrice = 700,
        cookEnergy = 3,
        cookDurationMs = 3 * 60_000L,
        prepDurationMs = 30_000L,
        prepLabel = "Mix",
        prepEmoji = "🥄",
        appliance = Appliance.OVEN,
    ),
    STEAMED_BROCCOLI(
        displayName = "Steamed Broccoli",
        outputType = ItemType.DISH_STEAMED_BROCCOLI,
        ingredients = listOf(
            RecipeIngredient(CropType.BROCCOLI, 3),
            RecipeIngredient(CropType.GARLIC, 1),
        ),
        basePrice = 21_000,
        cookEnergy = 2,
        cookDurationMs = 60_000L,
        prepDurationMs = 10_000L,
        prepLabel = "Trim",
        prepEmoji = "🔪",
        appliance = Appliance.STOVE,
    ),
    ZUCCHINI_FRITTERS(
        displayName = "Zucchini Fritters",
        outputType = ItemType.DISH_ZUCCHINI_FRITTERS,
        ingredients = listOf(
            RecipeIngredient(CropType.ZUCCHINI, 3),
            RecipeIngredient(CropType.ONION, 1),
            RecipeIngredient(CropType.WHEAT, 1),
        ),
        basePrice = 3_500,
        cookEnergy = 3,
        cookDurationMs = 90_000L,
        prepDurationMs = 25_000L,
        prepLabel = "Grate",
        prepEmoji = "🧀",
        appliance = Appliance.AIR_FRYER,
    ),
    STRAWBERRY_CRISP(
        displayName = "Strawberry Crisp",
        outputType = ItemType.DISH_STRAWBERRY_CRISP,
        ingredients = listOf(
            RecipeIngredient(CropType.STRAWBERRY, 4),
            RecipeIngredient(CropType.OATS_CROP, 1),
        ),
        basePrice = 1_400,
        cookEnergy = 3,
        cookDurationMs = 2 * 60_000L,
        prepDurationMs = 20_000L,
        prepLabel = "Slice",
        prepEmoji = "🔪",
        appliance = Appliance.OVEN,
    ),
    BLUEBERRY_MUFFINS(
        displayName = "Blueberry Muffins",
        outputType = ItemType.DISH_BLUEBERRY_MUFFINS,
        ingredients = listOf(
            RecipeIngredient(CropType.BLUEBERRY, 4),
            RecipeIngredient(CropType.WHEAT, 1),
        ),
        basePrice = 9_000,
        cookEnergy = 3,
        cookDurationMs = 3 * 60_000L,
        prepDurationMs = 30_000L,
        prepLabel = "Mix",
        prepEmoji = "🥄",
        appliance = Appliance.OVEN,
    ),
    GRAPE_JELLY(
        displayName = "Grape Jelly",
        outputType = ItemType.DISH_GRAPE_JELLY,
        ingredients = listOf(RecipeIngredient(CropType.GRAPE, 6)),
        basePrice = 19_000,
        cookEnergy = 4,
        cookDurationMs = 4 * 60_000L,
        prepDurationMs = 35_000L,
        prepLabel = "Mash",
        prepEmoji = "🥄",
        appliance = Appliance.STOVE,
    ),
    PUMPKIN_SOUP(
        displayName = "Pumpkin Soup",
        outputType = ItemType.DISH_PUMPKIN_SOUP,
        ingredients = listOf(
            RecipeIngredient(CropType.PUMPKIN, 1),
            RecipeIngredient(CropType.ONION, 1),
            RecipeIngredient(CropType.GARLIC, 1),
        ),
        basePrice = 24_000,
        cookEnergy = 4,
        cookDurationMs = 3 * 60_000L,
        prepDurationMs = 40_000L,
        prepLabel = "Peel & Dice",
        prepEmoji = "🔪",
        appliance = Appliance.STOVE,
    ),
    PEPPER_STIR_FRY(
        displayName = "Pepper Stir-Fry",
        outputType = ItemType.DISH_PEPPER_STIR_FRY,
        ingredients = listOf(
            RecipeIngredient(CropType.PEPPER, 3),
            RecipeIngredient(CropType.ONION, 1),
            RecipeIngredient(CropType.GARLIC, 1),
        ),
        basePrice = 24_000,
        cookEnergy = 3,
        cookDurationMs = 90_000L,
        prepDurationMs = 20_000L,
        prepLabel = "Slice",
        prepEmoji = "🔪",
        appliance = Appliance.STOVE,
    ),
    PINEAPPLE_UPSIDE_DOWN_CAKE(
        displayName = "Pineapple Upside-Down Cake",
        outputType = ItemType.DISH_PINEAPPLE_UPSIDE_DOWN_CAKE,
        ingredients = listOf(
            RecipeIngredient(CropType.PINEAPPLE, 1),
            RecipeIngredient(CropType.WHEAT, 1),
        ),
        basePrice = 12_000,
        cookEnergy = 4,
        cookDurationMs = 4 * 60_000L,
        prepDurationMs = 35_000L,
        prepLabel = "Slice",
        prepEmoji = "🔪",
        appliance = Appliance.OVEN,
    ),
    GARLIC_CONFIT(
        displayName = "Garlic Confit",
        outputType = ItemType.DISH_GARLIC_CONFIT,
        ingredients = listOf(RecipeIngredient(CropType.GARLIC, 6)),
        basePrice = 120_000,
        cookEnergy = 4,
        cookDurationMs = 6 * 60_000L,
        prepDurationMs = 25_000L,
        prepLabel = "Peel",
        prepEmoji = "🔪",
        appliance = Appliance.OVEN,
    ),
    KIWI_TART(
        displayName = "Kiwi Tart",
        outputType = ItemType.DISH_KIWI_TART,
        ingredients = listOf(
            RecipeIngredient(CropType.KIWI, 4),
            RecipeIngredient(CropType.WHEAT, 1),
        ),
        basePrice = 115_000,
        cookEnergy = 4,
        cookDurationMs = 2 * 60_000L,
        prepDurationMs = 25_000L,
        prepLabel = "Slice",
        prepEmoji = "🔪",
        appliance = Appliance.OVEN,
    ),
    PEANUT_BRITTLE(
        displayName = "Peanut Brittle",
        outputType = ItemType.DISH_PEANUT_BRITTLE,
        ingredients = listOf(RecipeIngredient(CropType.PEANUT, 5)),
        basePrice = 200_000,
        cookEnergy = 3,
        cookDurationMs = 90_000L,
        prepDurationMs = 20_000L,
        prepLabel = "Crush",
        prepEmoji = "🧑‍🍳",
        appliance = Appliance.STOVE,
    ),
    HOT_SAUCE(
        displayName = "Hot Sauce",
        outputType = ItemType.DISH_HOT_SAUCE,
        ingredients = listOf(
            RecipeIngredient(CropType.HOT_PEPPER, 3),
            RecipeIngredient(CropType.GARLIC, 2),
            RecipeIngredient(CropType.ONION, 1),
        ),
        basePrice = 220_000,
        cookEnergy = 4,
        cookDurationMs = 3 * 60_000L,
        prepDurationMs = 30_000L,
        prepLabel = "Mince",
        prepEmoji = "🔪",
        appliance = Appliance.STOVE,
    ),
    SNAP_PEA_STIR_FRY(
        displayName = "Snap Pea Stir-Fry",
        outputType = ItemType.DISH_SNAP_PEA_STIR_FRY,
        ingredients = listOf(
            RecipeIngredient(CropType.SNAP_PEA, 5),
            RecipeIngredient(CropType.GARLIC, 1),
        ),
        basePrice = 425_000,
        cookEnergy = 3,
        cookDurationMs = 60_000L,
        prepDurationMs = 15_000L,
        prepLabel = "Snap",
        prepEmoji = "🧑‍🍳",
        appliance = Appliance.STOVE,
    ),
    SAFFRON_RICE(
        displayName = "Saffron Rice",
        outputType = ItemType.DISH_SAFFRON_RICE,
        ingredients = listOf(
            RecipeIngredient(CropType.SAFFRON, 1),
            RecipeIngredient(CropType.WHEAT, 1),
            RecipeIngredient(CropType.ONION, 1),
        ),
        basePrice = 145_000,
        cookEnergy = 4,
        cookDurationMs = 3 * 60_000L,
        prepDurationMs = 40_000L,
        prepLabel = "Infuse",
        prepEmoji = "🧑‍🍳",
        appliance = Appliance.STOVE,
    ),
    VANILLA_CUSTARD(
        displayName = "Vanilla Custard",
        outputType = ItemType.DISH_VANILLA_CUSTARD,
        ingredients = listOf(
            RecipeIngredient(CropType.VANILLA, 1),
            RecipeIngredient(CropType.WHEAT, 1),
        ),
        basePrice = 200_000,
        cookEnergy = 4,
        cookDurationMs = 4 * 60_000L,
        prepDurationMs = 30_000L,
        prepLabel = "Whisk",
        prepEmoji = "🥄",
        appliance = Appliance.OVEN,
    ),
    YAM_FRIES(
        displayName = "Yam Fries",
        outputType = ItemType.DISH_YAM_FRIES,
        ingredients = listOf(RecipeIngredient(CropType.PURPLE_YAM, 2)),
        basePrice = 525_000,
        cookEnergy = 3,
        cookDurationMs = 90_000L,
        prepDurationMs = 20_000L,
        prepLabel = "Slice",
        prepEmoji = "🔪",
        appliance = Appliance.AIR_FRYER,
    ),
    TRUFFLE_MAC_AND_CHEESE(
        displayName = "Truffle Mac & Cheese",
        outputType = ItemType.DISH_TRUFFLE_MAC_AND_CHEESE,
        ingredients = listOf(
            RecipeIngredient(CropType.TRUFFLE, 1),
            RecipeIngredient(CropType.WHEAT, 1),
        ),
        basePrice = 425_000,
        cookEnergy = 6,
        cookDurationMs = 4 * 60_000L,
        prepDurationMs = 35_000L,
        prepLabel = "Grate",
        prepEmoji = "🧀",
        appliance = Appliance.OVEN,
    ),
    ONION_RINGS(
        displayName = "Onion Rings",
        outputType = ItemType.DISH_ONION_RINGS,
        ingredients = listOf(
            RecipeIngredient(CropType.ONION, 3),
            RecipeIngredient(CropType.WHEAT, 1),
        ),
        basePrice = 400,
        cookEnergy = 3,
        cookDurationMs = 90_000L,
        prepDurationMs = 25_000L,
        prepLabel = "Slice",
        prepEmoji = "🔪",
        appliance = Appliance.AIR_FRYER,
    ),
    STUFFED_MUSHROOMS(
        displayName = "Stuffed Mushrooms",
        outputType = ItemType.DISH_STUFFED_MUSHROOMS,
        ingredients = listOf(
            RecipeIngredient(CropType.MUSHROOM, 5),
            RecipeIngredient(CropType.GARLIC, 1),
            RecipeIngredient(CropType.WHEAT, 1),
        ),
        basePrice = 22_500,
        cookEnergy = 4,
        cookDurationMs = 2 * 60_000L,
        prepDurationMs = 30_000L,
        prepLabel = "Hollow",
        prepEmoji = "🥄",
        appliance = Appliance.OVEN,
    ),
    PUMPKIN_SPICE_BREAD(
        displayName = "Pumpkin Spice Bread",
        outputType = ItemType.DISH_PUMPKIN_SPICE_BREAD,
        ingredients = listOf(
            RecipeIngredient(CropType.PUMPKIN, 1),
            RecipeIngredient(CropType.WHEAT, 1),
        ),
        basePrice = 5_500,
        cookEnergy = 4,
        cookDurationMs = 4 * 60_000L,
        prepDurationMs = 45_000L,
        prepLabel = "Knead",
        prepEmoji = "🥄",
        appliance = Appliance.OVEN,
    ),
    VEGGIE_TEMPURA(
        displayName = "Veggie Tempura",
        outputType = ItemType.DISH_VEGGIE_TEMPURA,
        ingredients = listOf(
            RecipeIngredient(CropType.ZUCCHINI, 1),
            RecipeIngredient(CropType.BROCCOLI, 1),
            RecipeIngredient(CropType.WHEAT, 1),
        ),
        basePrice = 2_400,
        cookEnergy = 4,
        cookDurationMs = 90_000L,
        prepDurationMs = 35_000L,
        prepLabel = "Batter",
        prepEmoji = "🥄",
        appliance = Appliance.AIR_FRYER,
    ),
    VEGETABLE_LASAGNA(
        displayName = "Vegetable Lasagna",
        outputType = ItemType.DISH_VEGETABLE_LASAGNA,
        ingredients = listOf(
            RecipeIngredient(CropType.TOMATO, 2),
            RecipeIngredient(CropType.ZUCCHINI, 1),
            RecipeIngredient(CropType.MUSHROOM, 2),
            RecipeIngredient(CropType.WHEAT, 1),
        ),
        basePrice = 5_500,
        cookEnergy = 6,
        cookDurationMs = 5 * 60_000L,
        prepDurationMs = 40_000L,
        prepLabel = "Layer",
        prepEmoji = "🧑‍🍳",
        appliance = Appliance.OVEN,
    ),
    EGGPLANT_PARM(
        displayName = "Eggplant Parmesan",
        outputType = ItemType.DISH_EGGPLANT_PARM,
        ingredients = listOf(
            RecipeIngredient(CropType.ZUCCHINI, 2),
            RecipeIngredient(CropType.TOMATO, 2),
            RecipeIngredient(CropType.WHEAT, 1),
        ),
        basePrice = 3_500,
        cookEnergy = 5,
        cookDurationMs = 4 * 60_000L,
        prepDurationMs = 35_000L,
        prepLabel = "Bread",
        prepEmoji = "🥄",
        appliance = Appliance.OVEN,
    ),
    BLUEBERRY_PANCAKES(
        displayName = "Blueberry Pancakes",
        outputType = ItemType.DISH_BLUEBERRY_PANCAKES,
        ingredients = listOf(
            RecipeIngredient(CropType.BLUEBERRY, 3),
            RecipeIngredient(CropType.WHEAT, 1),
        ),
        basePrice = 7_000,
        cookEnergy = 3,
        cookDurationMs = 90_000L,
        prepDurationMs = 25_000L,
        prepLabel = "Whisk",
        prepEmoji = "🥄",
        appliance = Appliance.STOVE,
    ),
    OATMEAL_COOKIES(
        displayName = "Oatmeal Cookies",
        outputType = ItemType.DISH_OATMEAL_COOKIES,
        ingredients = listOf(
            RecipeIngredient(CropType.OATS_CROP, 2),
            RecipeIngredient(CropType.WHEAT, 1),
        ),
        basePrice = 250,
        cookEnergy = 3,
        cookDurationMs = 2 * 60_000L,
        prepDurationMs = 30_000L,
        prepLabel = "Mix",
        prepEmoji = "🥄",
        appliance = Appliance.OVEN,
    ),
    POTATO_GRATIN(
        displayName = "Potato Gratin",
        outputType = ItemType.DISH_POTATO_GRATIN,
        ingredients = listOf(
            RecipeIngredient(CropType.POTATO, 4),
            RecipeIngredient(CropType.GARLIC, 1),
            RecipeIngredient(CropType.ONION, 1),
        ),
        basePrice = 19_500,
        cookEnergy = 5,
        cookDurationMs = 5 * 60_000L,
        prepDurationMs = 35_000L,
        prepLabel = "Slice",
        prepEmoji = "🔪",
        appliance = Appliance.OVEN,
    ),
    CORN_CHOWDER(
        displayName = "Corn Chowder",
        outputType = ItemType.DISH_CORN_CHOWDER,
        ingredients = listOf(
            RecipeIngredient(CropType.CORN, 4),
            RecipeIngredient(CropType.POTATO, 2),
            RecipeIngredient(CropType.ONION, 1),
        ),
        basePrice = 800,
        cookEnergy = 4,
        cookDurationMs = 3 * 60_000L,
        prepDurationMs = 30_000L,
        prepLabel = "Dice",
        prepEmoji = "🔪",
        appliance = Appliance.STOVE,
    ),
    ;

    /** Coin price the player pays to start this recipe — sum of each
     *  ingredient's farm sell price times its quantity. */
    val ingredientCoinCost: Int get() =
        ingredients.sumOf { it.crop.sellPrice * it.quantity }

    /** Total time from "start" tap to "ready in barn". */
    val totalDurationMs: Long get() = prepDurationMs + cookDurationMs
}

/**
 * One in-flight cooking session. Honors [DebugSettings.skipTimers] so
 * the debug toggle works for kitchen the same as malthouse / brewery.
 */
data class CookingRun(
    val id: Long,
    val recipe: KitchenRecipe,
    val stage: CookStage,
    val stageStartedMs: Long,
) {
    private fun stageDurationMs(): Long = when (stage) {
        CookStage.PREP -> recipe.prepDurationMs
        CookStage.COOK -> recipe.cookDurationMs
    }

    fun progress(nowMs: Long): Float {
        if (DebugSettings.skipTimers) return 1f
        return ((nowMs - stageStartedMs).toFloat() / stageDurationMs()).coerceIn(0f, 1f)
    }

    fun isComplete(nowMs: Long): Boolean =
        DebugSettings.skipTimers || nowMs - stageStartedMs >= stageDurationMs()

    fun remainingMs(nowMs: Long): Long {
        if (DebugSettings.skipTimers) return 0L
        return (stageStartedMs + stageDurationMs() - nowMs).coerceAtLeast(0)
    }
}

/**
 * Kitchen equipment progression — caps quality.
 */
enum class KitchenTier(
    val displayName: String,
    val qualityCap: Int,
    val unlockCost: Int,
) {
    STOVETOP("Home stovetop", 70, 0),
    PRO_RANGE("Pro range", 85, 5_000),
    CHEFS_KITCHEN("Chef's kitchen", 95, 30_000),
}

private const val META_KITCHEN_TIER = "kitchen_tier"
private const val META_NEXT_COOK_ID = "kitchen_next_cook_id"

class Kitchen private constructor(private val appContext: Context) {

    private val db = AppDatabase.get(appContext).also {
        LegacyMigrator.migrateIfNeeded(appContext, it)
    }

    val activeRuns = mutableStateListOf<CookingRun>()
    var tier: KitchenTier by mutableStateOf(KitchenTier.STOVETOP)
        private set
    private var nextRunId: Long = 1L

    var revisionTick: Int by mutableStateOf(0)
        private set
    var feedback: String? by mutableStateOf(null)
        private set
    var feedbackBad: Boolean by mutableStateOf(false)
        private set

    init { load() }

    /** True if the prep slot is currently open (no recipe in PREP stage). */
    fun prepSlotFree(): Boolean = activeRuns.none { it.stage == CookStage.PREP }

    /** True if [appliance] currently has no recipe assigned (prep or cook). */
    fun applianceFree(appliance: Appliance): Boolean =
        activeRuns.none { it.recipe.appliance == appliance }

    /**
     * Start a recipe. Pays coins (ingredient cost) + energy up front;
     * the dish drops into FARM silo when [tick] sees the cook stage
     * complete. Returns null on any failure (slot full, can't afford
     * coins, can't afford energy) and surfaces a message via [feedback].
     */
    fun startCooking(
        recipe: KitchenRecipe,
        nowMs: Long,
        spendEnergy: (Int) -> Boolean,
        spendCoins: (Int) -> Boolean,
    ): CookingRun? {
        if (!prepSlotFree()) {
            fail("Already prepping a dish — finish that first")
            return null
        }
        if (!applianceFree(recipe.appliance)) {
            fail("${recipe.appliance.emoji} ${recipe.appliance.displayName} is busy")
            return null
        }
        val coinCost = recipe.ingredientCoinCost
        if (!spendCoins(coinCost)) {
            fail("Need 🪙$coinCost for ingredients")
            return null
        }
        if (!spendEnergy(recipe.cookEnergy)) {
            fail("Need ⚡${recipe.cookEnergy}")
            return null
        }
        val run = CookingRun(
            id = nextRunId++,
            recipe = recipe,
            stage = CookStage.PREP,
            stageStartedMs = nowMs,
        )
        db.runInTransaction {
            db.kitchenRuns().insert(run.toEntity())
            db.systemMeta().put(META_NEXT_COOK_ID, nextRunId.toString())
        }
        activeRuns += run
        if (!DebugSettings.skipTimers) {
            LocalNotifier.schedule(
                context = appContext,
                channel = NotificationSettings.Channel.CRAFT_DONE,
                delayMs = recipe.totalDurationMs,
                title = "${recipe.outputType.emoji} ${recipe.displayName} ready",
                body = "Tap to head to the kitchen and plate it.",
                uniqueTag = "cook_${run.id}",
            )
        }
        note("${recipe.prepEmoji} ${recipe.prepLabel} for ${recipe.displayName}…")
        bump()
        return run
    }

    /**
     * Drain finished cooking sessions into the FARM silo as artisan
     * goods. Quality scales with kitchen tier + a small skill roll;
     * tier rolls (Mega/Golden/Perfect) come from the standard helper.
     * Also transitions PREP-complete runs into COOK stage.
     */
    fun tick(transport: TransportService, nowMs: Long) {
        if (activeRuns.isEmpty()) return
        var changed = false
        val finishedCooks = mutableListOf<CookingRun>()
        val updated = mutableListOf<Pair<Int, CookingRun>>()
        for ((idx, run) in activeRuns.withIndex()) {
            if (!run.isComplete(nowMs)) continue
            when (run.stage) {
                CookStage.PREP -> {
                    val next = run.copy(stage = CookStage.COOK, stageStartedMs = nowMs)
                    updated += idx to next
                    changed = true
                }
                CookStage.COOK -> {
                    finishedCooks += run
                    changed = true
                }
            }
        }
        if (!changed) return

        for (run in finishedCooks) {
            val skillBonus = Random.nextInt(0, 16)
            val producedScore = computeOutputScore(
                inputAverage = 60,
                skillBonus = skillBonus,
                equipmentCap = tier.qualityCap,
            )
            transport.addToInventory(
                Location.FARM,
                ItemStack(
                    type = run.recipe.outputType,
                    quantity = 1,
                    score = producedScore,
                    tier = ItemTier.roll(),
                    createdMs = nowMs,
                ),
            )
        }

        db.runInTransaction {
            finishedCooks.forEach { db.kitchenRuns().deleteById(it.id) }
            updated.forEach { (_, run) -> db.kitchenRuns().insert(run.toEntity()) }
        }
        // Apply to in-memory list: transitions in place, then drop completes.
        for ((idx, next) in updated) {
            activeRuns[idx] = next
        }
        if (finishedCooks.isNotEmpty()) {
            activeRuns.removeAll(finishedCooks)
        }
        bump()
    }

    /** Drop in-memory state and re-read from DB. Used by full-game reset. */
    fun reload() {
        activeRuns.clear()
        tier = KitchenTier.STOVETOP
        nextRunId = 1L
        feedback = null
        feedbackBad = false
        load()
        bump()
    }

    fun upgrade(to: KitchenTier): Boolean {
        if (to.ordinal <= tier.ordinal) return false
        tier = to
        db.systemMeta().put(META_KITCHEN_TIER, to.name)
        bump()
        return true
    }

    private fun bump() { revisionTick = revisionTick + 1 }
    private fun note(msg: String) { feedback = msg; feedbackBad = false }
    private fun fail(msg: String) { feedback = msg; feedbackBad = true }

    private fun load() {
        tier = db.systemMeta().get(META_KITCHEN_TIER)
            ?.let { runCatching { KitchenTier.valueOf(it) }.getOrNull() }
            ?: KitchenTier.STOVETOP
        nextRunId = db.systemMeta().get(META_NEXT_COOK_ID)?.toLongOrNull()?.coerceAtLeast(1L) ?: 1L
        db.kitchenRuns().getAll().forEach { row ->
            row.toRunOrNull()?.let { activeRuns += it }
        }
    }

    private fun CookingRun.toEntity(): KitchenRunEntity = KitchenRunEntity(
        id = id,
        recipe = recipe.name,
        startMs = stageStartedMs,
        stage = stage.name,
    )

    private fun KitchenRunEntity.toRunOrNull(): CookingRun? {
        val r = runCatching { KitchenRecipe.valueOf(recipe) }.getOrNull() ?: return null
        val s = runCatching { CookStage.valueOf(stage) }.getOrDefault(CookStage.COOK)
        return CookingRun(id = id, recipe = r, stage = s, stageStartedMs = startMs)
    }

    companion object {
        @Volatile private var instance: Kitchen? = null

        fun get(context: Context): Kitchen {
            val existing = instance
            if (existing != null) return existing
            return synchronized(this) {
                instance ?: Kitchen(context.applicationContext).also { instance = it }
            }
        }
    }
}
