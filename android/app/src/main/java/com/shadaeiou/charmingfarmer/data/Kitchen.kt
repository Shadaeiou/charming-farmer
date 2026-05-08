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
 * A kitchen recipe. Phase 1 is coin-priced — paying a recipe's cost
 * stands in for buying the ingredients. Players see the ingredient
 * list as flavor and to know what farms they should be expanding into.
 * The dish drops into the FARM silo as an artisan good with a quality
 * roll, so it can be sold like any other ItemStack.
 */
enum class KitchenRecipe(
    val displayName: String,
    val description: String,
    val outputType: ItemType,
    val ingredients: List<RecipeIngredient>,
    val basePrice: Int,
    val cookEnergy: Int,
    val cookDurationMs: Long,
) {
    // basePrice is roughly 2.5× the ingredient coin cost so an A-grade
    // cook (~score 85 → 0.72× multiplier) clears a healthy margin and a
    // C-grade dish still breaks even. Kitchen tier upgrades push the
    // average score up, which compounds with the quadratic price curve
    // in ItemStack.unitSellPrice.
    CARROT_SOUP(
        displayName = "Carrot Soup",
        description = "Velvety, slow-simmered carrots with a crack of black pepper.",
        outputType = ItemType.DISH_CARROT_SOUP,
        ingredients = listOf(RecipeIngredient(CropType.CARROT, 5)),
        basePrice = 110,
        cookEnergy = 3,
        cookDurationMs = 4 * 60_000L,
    ),
    MASHED_POTATOES(
        displayName = "Mashed Potatoes",
        description = "Buttery, fluffy, the kind that ruin all other mashed potatoes.",
        outputType = ItemType.DISH_MASHED_POTATOES,
        ingredients = listOf(RecipeIngredient(CropType.POTATO, 5)),
        basePrice = 170,
        cookEnergy = 3,
        cookDurationMs = 5 * 60_000L,
    ),
    GARDEN_SALAD(
        displayName = "Garden Salad",
        description = "Crisp lettuce with shaved carrot and onion — fresh, clean, easy.",
        outputType = ItemType.DISH_GARDEN_SALAD,
        ingredients = listOf(
            RecipeIngredient(CropType.LETTUCE, 3),
            RecipeIngredient(CropType.CARROT, 2),
            RecipeIngredient(CropType.ONION, 1),
        ),
        basePrice = 290,
        cookEnergy = 2,
        cookDurationMs = 3 * 60_000L,
    ),
    STRAWBERRY_JAM(
        displayName = "Strawberry Jam",
        description = "Six punnets of strawberries reduced low and slow with a kiss of sugar.",
        outputType = ItemType.DISH_STRAWBERRY_JAM,
        ingredients = listOf(RecipeIngredient(CropType.STRAWBERRY, 6)),
        basePrice = 2_200,
        cookEnergy = 4,
        cookDurationMs = 8 * 60_000L,
    ),
    VEGGIE_STEW(
        displayName = "Veggie Stew",
        description = "Hearty potato-and-broccoli stew with onion and carrot. A winter classic.",
        outputType = ItemType.DISH_VEGGIE_STEW,
        ingredients = listOf(
            RecipeIngredient(CropType.POTATO, 3),
            RecipeIngredient(CropType.CARROT, 2),
            RecipeIngredient(CropType.ONION, 1),
            RecipeIngredient(CropType.BROCCOLI, 1),
        ),
        basePrice = 1_000,
        cookEnergy = 5,
        cookDurationMs = 12 * 60_000L,
    ),
    STUFFED_PEPPERS(
        displayName = "Stuffed Peppers",
        description = "Bell peppers packed with corn, beans, and tomato. Charred just right.",
        outputType = ItemType.DISH_STUFFED_PEPPER,
        ingredients = listOf(
            RecipeIngredient(CropType.PEPPER, 3),
            RecipeIngredient(CropType.CORN, 2),
            RecipeIngredient(CropType.BEAN, 2),
            RecipeIngredient(CropType.TOMATO, 1),
        ),
        basePrice = 22_000,
        cookEnergy = 6,
        cookDurationMs = 18 * 60_000L,
    ),
    PUMPKIN_PIE(
        displayName = "Pumpkin Pie",
        description = "Spiced pumpkin custard in a flaky wheat-flour crust. Festival favorite.",
        outputType = ItemType.DISH_PUMPKIN_PIE,
        ingredients = listOf(
            RecipeIngredient(CropType.PUMPKIN, 1),
            RecipeIngredient(CropType.WHEAT, 2),
        ),
        basePrice = 4_900,
        cookEnergy = 5,
        cookDurationMs = 14 * 60_000L,
    ),
    PINEAPPLE_SALSA(
        displayName = "Pineapple Salsa",
        description = "Sweet pineapple chunks, hot pepper kick, sharp onion bite.",
        outputType = ItemType.DISH_PINEAPPLE_SALSA,
        ingredients = listOf(
            RecipeIngredient(CropType.PINEAPPLE, 1),
            RecipeIngredient(CropType.HOT_PEPPER, 1),
            RecipeIngredient(CropType.ONION, 1),
        ),
        basePrice = 70_000,
        cookEnergy = 4,
        cookDurationMs = 10 * 60_000L,
    ),
    WATERMELON_SORBET(
        displayName = "Watermelon Sorbet",
        description = "Pure-fruit sorbet — sweet, slushy, served in a chilled glass.",
        outputType = ItemType.DISH_WATERMELON_SORBET,
        ingredients = listOf(RecipeIngredient(CropType.WATERMELON, 4)),
        basePrice = 55_000,
        cookEnergy = 5,
        cookDurationMs = 20 * 60_000L,
    ),
    TRUFFLE_RISOTTO(
        displayName = "Truffle Risotto",
        description = "Stirred slowly with porcini, garlic, and shaved black truffle. Restaurant-grade.",
        outputType = ItemType.DISH_TRUFFLE_RISOTTO,
        ingredients = listOf(
            RecipeIngredient(CropType.TRUFFLE, 1),
            RecipeIngredient(CropType.MUSHROOM, 2),
            RecipeIngredient(CropType.GARLIC, 1),
            RecipeIngredient(CropType.ONION, 1),
        ),
        basePrice = 400_000,
        cookEnergy = 8,
        cookDurationMs = 30 * 60_000L,
    ),
    ;

    /** Coin price the player pays to start this recipe — sum of each
     *  ingredient's farm sell price times its quantity. */
    val ingredientCoinCost: Int get() =
        ingredients.sumOf { it.crop.sellPrice * it.quantity }
}

/**
 * One in-flight cooking session. Honors [DebugSettings.skipTimers] so
 * the debug toggle works for kitchen the same as malthouse / brewery.
 */
data class CookingRun(
    val id: Long,
    val recipe: KitchenRecipe,
    val startMs: Long,
) {
    fun progress(nowMs: Long): Float {
        if (DebugSettings.skipTimers) return 1f
        return ((nowMs - startMs).toFloat() / recipe.cookDurationMs).coerceIn(0f, 1f)
    }

    fun isComplete(nowMs: Long): Boolean =
        DebugSettings.skipTimers || nowMs - startMs >= recipe.cookDurationMs

    fun remainingMs(nowMs: Long): Long {
        if (DebugSettings.skipTimers) return 0L
        return (startMs + recipe.cookDurationMs - nowMs).coerceAtLeast(0)
    }
}

/**
 * Kitchen equipment progression — caps quality and unlocks burner
 * slots. Phase 1 ships the basic stovetop only.
 */
enum class KitchenTier(
    val displayName: String,
    val qualityCap: Int,
    val cookSlots: Int,
    val unlockCost: Int,
) {
    STOVETOP("Home stovetop", 70, 1, 0),
    PRO_RANGE("Pro range", 85, 2, 5_000),
    CHEFS_KITCHEN("Chef's kitchen", 95, 3, 30_000),
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

    fun availableSlots(): Int = (tier.cookSlots - activeRuns.size).coerceAtLeast(0)

    /**
     * Start a recipe. Pays coins (ingredient cost) + energy up front;
     * the dish drops into FARM silo when [tick] sees the run complete.
     * Returns null on any failure (slot full, can't afford coins, can't
     * afford energy) and surfaces a message via [feedback].
     */
    fun startCooking(
        recipe: KitchenRecipe,
        nowMs: Long,
        spendEnergy: (Int) -> Boolean,
        spendCoins: (Int) -> Boolean,
    ): CookingRun? {
        if (availableSlots() == 0) {
            fail("All burners are busy")
            return null
        }
        val coinCost = recipe.ingredientCoinCost
        if (!spendCoins(coinCost)) {
            fail("Need 🪙$coinCost for ingredients")
            return null
        }
        if (!spendEnergy(recipe.cookEnergy)) {
            fail("Need ⚡${recipe.cookEnergy}")
            // Refund handled by caller — Kitchen mirrors Brewery's contract.
            return null
        }
        val run = CookingRun(
            id = nextRunId++,
            recipe = recipe,
            startMs = nowMs,
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
                delayMs = recipe.cookDurationMs,
                title = "${recipe.outputType.emoji} ${recipe.displayName} ready",
                body = "Tap to head to the kitchen and plate it.",
                uniqueTag = "cook_${run.id}",
            )
        }
        note("Started cooking ${recipe.displayName}")
        bump()
        return run
    }

    /**
     * Drain finished cooking sessions into the FARM silo as artisan
     * goods. Quality scales with kitchen tier + a small skill roll;
     * tier rolls (Mega/Golden/Perfect) come from the standard helper.
     */
    fun tick(transport: TransportService, nowMs: Long) {
        val done = activeRuns.filter { it.isComplete(nowMs) }
        if (done.isEmpty()) return
        for (run in done) {
            val skillBonus = Random.nextInt(0, 16)
            val producedScore = computeOutputScore(
                inputAverage = 60,    // Phase 1: ingredients are off-screen, treat as B-grade
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
            done.forEach { db.kitchenRuns().deleteById(it.id) }
        }
        activeRuns.removeAll(done)
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
        startMs = startMs,
    )

    private fun KitchenRunEntity.toRunOrNull(): CookingRun? {
        val r = runCatching { KitchenRecipe.valueOf(recipe) }.getOrNull() ?: return null
        return CookingRun(id = id, recipe = r, startMs = startMs)
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
