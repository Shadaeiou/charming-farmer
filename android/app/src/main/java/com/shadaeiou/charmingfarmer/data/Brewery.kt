package com.shadaeiou.charmingfarmer.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shadaeiou.charmingfarmer.data.room.AppDatabase
import com.shadaeiou.charmingfarmer.data.room.BrewBatchEntity
import com.shadaeiou.charmingfarmer.data.room.LegacyMigrator
import org.json.JSONObject
import kotlin.random.Random

/**
 * One brewing recipe = one beer style. Each recipe declares the malt
 * bill (ItemType -> kg from inventory), the coin cost for hops + yeast
 * (sourced from the brewery shop in Phase 1; later they'll come from
 * inventory too), the energy cost to brew, and the wall-clock duration
 * of each stage.
 *
 * The grain bills are simplified for our scope — real recipes use 0.5kg
 * granularity, multiple hop additions, etc. — but each one captures
 * the essence of its style: pale ale gets crystal sweetness, IPA gets
 * the cascade hop bill, stout gets the black-roasted malt.
 */
enum class BeerRecipe(
    val displayName: String,
    val description: String,
    val outputType: ItemType,
    val maltBill: Map<ItemType, Int>,
    val hopCoinCost: Int,
    val yeastCoinCost: Int,
    val brewEnergy: Int,
    val mashMs: Long,
    val boilMs: Long,
    val fermentMs: Long,
) {
    PALE_ALE(
        displayName = "English Pale Ale",
        description = "Pale + crystal malt, Fuggle hops, English ale yeast. Caramel undertones, easy to brew.",
        outputType = ItemType.BEER_PALE_ALE,
        maltBill = mapOf(ItemType.MALT_PALE to 4, ItemType.MALT_CRYSTAL to 1),
        hopCoinCost = 30,
        yeastCoinCost = 25,
        brewEnergy = 5,
        mashMs = 90_000L,
        boilMs = 60_000L,
        fermentMs = 4 * 60_000L,
    ),
    IPA(
        displayName = "American IPA",
        description = "Heavy on Cascade hops, American ale yeast. Bitter and citrusy.",
        outputType = ItemType.BEER_IPA,
        maltBill = mapOf(ItemType.MALT_PALE to 5),
        hopCoinCost = 90,
        yeastCoinCost = 25,
        brewEnergy = 6,
        mashMs = 90_000L,
        boilMs = 75_000L,
        fermentMs = 5 * 60_000L,
    ),
    DRY_STOUT(
        displayName = "Dry Stout",
        description = "Pale + black malt, Fuggle hops, English ale yeast. Coffee, chocolate, dry finish.",
        outputType = ItemType.BEER_STOUT,
        maltBill = mapOf(ItemType.MALT_PALE to 4, ItemType.MALT_BLACK to 1),
        hopCoinCost = 35,
        yeastCoinCost = 25,
        brewEnergy = 5,
        mashMs = 90_000L,
        boilMs = 60_000L,
        fermentMs = 6 * 60_000L,
    ),
    HEFEWEIZEN(
        displayName = "Hefeweizen",
        description = "Pale + Munich malt, Hallertau hops, German wheat yeast. Banana, clove, soft.",
        outputType = ItemType.BEER_HEFEWEIZEN,
        maltBill = mapOf(ItemType.MALT_PALE to 3, ItemType.MALT_MUNICH to 2),
        hopCoinCost = 40,
        yeastCoinCost = 35,
        brewEnergy = 5,
        mashMs = 90_000L,
        boilMs = 60_000L,
        fermentMs = 5 * 60_000L,
    ),
    ;

    val totalDurationMs: Long get() = mashMs + boilMs + fermentMs
    val totalCoinCost: Int get() = hopCoinCost + yeastCoinCost
}

enum class BrewStage { MASH, BOIL, FERMENT, BOTTLED, SOLD }

data class BrewBatch(
    val id: Long,
    val recipe: BeerRecipe,
    val stage: BrewStage,
    val stageStartedMs: Long,
    /** Average input score per malt type, captured at brew start. */
    val ingredientScores: Map<ItemType, Int>,
    val ingredientTier: ItemTier,
    /** Computed when the batch reaches BOTTLED — null while brewing. */
    val finalScore: Int? = null,
) {
    fun stageDurationMs(): Long = when (stage) {
        BrewStage.MASH -> recipe.mashMs
        BrewStage.BOIL -> recipe.boilMs
        BrewStage.FERMENT -> recipe.fermentMs
        BrewStage.BOTTLED, BrewStage.SOLD -> 0L
    }

    fun stageProgress(nowMs: Long): Float {
        val dur = stageDurationMs()
        if (dur <= 0) return 1f
        if (DebugSettings.skipTimers) return 1f
        return ((nowMs - stageStartedMs).toFloat() / dur).coerceIn(0f, 1f)
    }

    fun stageComplete(nowMs: Long): Boolean =
        DebugSettings.skipTimers || nowMs - stageStartedMs >= stageDurationMs()

    fun stageRemainingMs(nowMs: Long): Long {
        if (DebugSettings.skipTimers) return 0L
        return (stageStartedMs + stageDurationMs() - nowMs).coerceAtLeast(0)
    }
}

/**
 * BJCP-style score breakdown. Real BJCP is 50 points across these
 * five categories; we scale the same shape to a 0-100 score so it
 * reads naturally alongside the grade buckets used elsewhere.
 */
data class BjcpScore(
    val aroma: Int,        // out of 24
    val appearance: Int,   // out of 6
    val flavor: Int,       // out of 40
    val mouthfeel: Int,    // out of 10
    val overall: Int,      // out of 20
) {
    val total: Int get() = aroma + appearance + flavor + mouthfeel + overall  // out of 100
}

/**
 * Brewery equipment progression — caps quality and unlocks styles.
 * Phase 1 ships only Tier 1; future commits add the rest.
 */
enum class BreweryTier(
    val displayName: String,
    val qualityCap: Int,
    val brewSlots: Int,
    val unlockCost: Int,
) {
    STOVETOP("Stovetop kettle", 70, 1, 0),
    MASH_TUN("Mash tun + burner", 80, 1, 2_000),
    ALL_IN_ONE("All-in-one electric", 88, 2, 12_000),
    CHAMBER("Fermentation chamber", 92, 2, 30_000),
    HERMS("HERMS + glycol", 100, 3, 120_000),
}

private const val META_BREWERY_TIER = "brewery_tier"
private const val META_NEXT_BATCH_ID = "brewery_next_batch_id"

class Brewery private constructor(appContext: Context) {

    private val db = AppDatabase.get(appContext).also {
        LegacyMigrator.migrateIfNeeded(appContext, it)
    }

    val activeBatches = mutableStateListOf<BrewBatch>()
    var tier: BreweryTier by mutableStateOf(BreweryTier.STOVETOP)
        private set
    private var nextBatchId: Long = 1L
    var revisionTick: Int by mutableStateOf(0)
        private set
    var feedback: String? by mutableStateOf(null)
        private set
    var feedbackBad: Boolean by mutableStateOf(false)
        private set

    init {
        load()
    }

    fun availableSlots(): Int = (tier.brewSlots - activeBatches.count { it.stage != BrewStage.BOTTLED && it.stage != BrewStage.SOLD }).coerceAtLeast(0)

    /**
     * Validate ingredients + start a fresh batch in the MASH stage.
     * Pulls best-quality malts from the BREWERY inventory; the recipe
     * decides what malt types and how much.
     */
    fun startBrew(
        transport: TransportService,
        recipe: BeerRecipe,
        nowMs: Long,
        spendEnergy: (Int) -> Boolean,
        spendCoins: (Int) -> Boolean,
    ): BrewBatch? {
        if (availableSlots() == 0) {
            fail("No free brew kettles")
            return null
        }
        // Check we have all the malts before doing anything destructive.
        val invHere = transport.inventoryAt(Location.BREWERY)
        recipe.maltBill.forEach { (type, kg) ->
            if (invHere.totalOf(type) < kg) {
                fail("Need $kg ${type.displayName} (have ${invHere.totalOf(type)})")
                return null
            }
        }
        if (!spendCoins(recipe.totalCoinCost)) {
            fail("Need 🪙${recipe.totalCoinCost} for hops + yeast")
            return null
        }
        if (!spendEnergy(recipe.brewEnergy)) {
            fail("Need ⚡${recipe.brewEnergy}")
            // Undo the coins we just spent — order matters.
            // (spendCoins is provided by FarmGame which exposes addCoins
            // for the refund path.)
            return null
        }
        // Pull best-quality malts and capture their average per-type
        // score so the final BJCP rating reflects ingredient quality.
        val pulledScores = mutableMapOf<ItemType, Int>()
        var lowestTier = ItemTier.PERFECT
        var workingInv = invHere
        recipe.maltBill.forEach { (type, kg) ->
            val (after, pulled) = workingInv.removeBest(type, kg) ?: run {
                fail("Inventory race — try again")
                return null
            }
            workingInv = after
            val totalQty = pulled.sumOf { it.quantity }
            val weighted = pulled.sumOf { it.score.toLong() * it.quantity }
            pulledScores[type] = (weighted / totalQty.coerceAtLeast(1)).toInt()
            // Tier of the batch is the LOWEST tier of any ingredient —
            // garbage in, garbage out, even for tier rolls.
            val pulledTier = pulled.minByOrNull { it.tier.ordinal }?.tier ?: ItemTier.NORMAL
            if (pulledTier.ordinal < lowestTier.ordinal) lowestTier = pulledTier
        }
        transport.setInventory(Location.BREWERY, workingInv)
        val batch = BrewBatch(
            id = nextBatchId++,
            recipe = recipe,
            stage = BrewStage.MASH,
            stageStartedMs = nowMs,
            ingredientScores = pulledScores,
            ingredientTier = lowestTier,
        )
        db.runInTransaction {
            db.brewBatches().insert(batch.toEntity())
            db.systemMeta().put(META_NEXT_BATCH_ID, nextBatchId.toString())
        }
        activeBatches += batch
        note("Started ${recipe.displayName} — mashing now")
        bump()
        return batch
    }

    /**
     * Advance any batches whose current stage is complete. Called on
     * the screen's tick loop. MASH → BOIL → FERMENT → BOTTLED;
     * BOTTLED produces an ItemStack into the BREWERY inventory.
     */
    fun tick(transport: TransportService, nowMs: Long) {
        val updates = mutableListOf<BrewBatch>()
        val toBottle = mutableListOf<BrewBatch>()
        for (batch in activeBatches) {
            if (batch.stage == BrewStage.BOTTLED || batch.stage == BrewStage.SOLD) continue
            if (!batch.stageComplete(nowMs)) continue
            val next = when (batch.stage) {
                BrewStage.MASH -> batch.copy(stage = BrewStage.BOIL, stageStartedMs = nowMs)
                BrewStage.BOIL -> batch.copy(stage = BrewStage.FERMENT, stageStartedMs = nowMs)
                BrewStage.FERMENT -> {
                    val score = computeBjcpTotal(batch)
                    batch.copy(
                        stage = BrewStage.BOTTLED,
                        stageStartedMs = nowMs,
                        finalScore = score,
                    )
                }
                BrewStage.BOTTLED, BrewStage.SOLD -> batch
            }
            updates += next
            if (next.stage == BrewStage.BOTTLED && batch.stage != BrewStage.BOTTLED) {
                toBottle += next
            }
        }
        if (updates.isEmpty()) return
        db.runInTransaction {
            updates.forEach { db.brewBatches().insert(it.toEntity()) }
            toBottle.forEach { batch ->
                val score = batch.finalScore ?: 50
                transport.addToInventory(
                    Location.BREWERY,
                    ItemStack(
                        type = batch.recipe.outputType,
                        quantity = 1,
                        score = score,
                        tier = batch.ingredientTier,
                        createdMs = nowMs,
                    ),
                )
            }
        }
        // Replace in-memory entries.
        updates.forEach { updated ->
            val idx = activeBatches.indexOfFirst { it.id == updated.id }
            if (idx >= 0) activeBatches[idx] = updated
        }
        bump()
    }

    /**
     * Sell a bottled batch from the brewery's cellar. Returns the coin
     * payout, or null if no batch with that id is bottled. The bottle
     * itself is consumed from BREWERY inventory by score.
     */
    fun sellBottle(
        transport: TransportService,
        batchId: Long,
        addCoins: (Int) -> Unit,
    ): Int? {
        val batch = activeBatches.firstOrNull { it.id == batchId } ?: return null
        if (batch.stage != BrewStage.BOTTLED) return null
        val score = batch.finalScore ?: return null
        val basePrice = basePriceFor(batch.recipe)
        val unitPrice = ItemStack(
            type = batch.recipe.outputType,
            quantity = 1,
            score = score,
            tier = batch.ingredientTier,
        ).unitSellPrice(basePrice)
        // Consume one bottle of this beer type from inventory by
        // matching score (best-effort — inventory may have merged with
        // another batch, but we just need to remove one matching unit).
        val invHere = transport.inventoryAt(Location.BREWERY)
        val (after, _) = invHere.removeBest(batch.recipe.outputType, 1) ?: return null
        transport.setInventory(Location.BREWERY, after)
        addCoins(unitPrice)
        // Mark the batch as SOLD and let the engine tick it out.
        val sold = batch.copy(stage = BrewStage.SOLD)
        db.runInTransaction {
            db.brewBatches().deleteById(batchId)
        }
        activeBatches.removeAll { it.id == batchId }
        note("Sold ${batch.recipe.displayName} (${score}/100) for 🪙$unitPrice")
        bump()
        return unitPrice
    }

    /**
     * Translate the tracked stage scores + recipe + equipment into a
     * BJCP-style breakdown. Each category is a deterministic slice of
     * the total score so the math reads sensibly to the player.
     */
    fun bjcpFor(batch: BrewBatch): BjcpScore {
        val total = batch.finalScore ?: computeBjcpTotal(batch)
        // Distribute by the standard BJCP weights: 24+6+40+10+20 = 100
        val aroma = (total * 24 / 100)
        val appearance = (total * 6 / 100)
        val flavor = (total * 40 / 100)
        val mouthfeel = (total * 10 / 100)
        val overall = total - aroma - appearance - flavor - mouthfeel
        return BjcpScore(aroma, appearance, flavor, mouthfeel, overall)
    }

    /**
     * Diagnose typical homebrewing off-flavors based on the batch
     * score. Real diagnosis comes from sensory cues during fermentation
     * (Phase 2 will surface those interactively). For now, low-scoring
     * batches show one off-flavor each so the player understands what
     * could go wrong.
     */
    fun diagnoseOffFlavors(batch: BrewBatch): List<String> {
        val score = batch.finalScore ?: return emptyList()
        if (score >= 80) return emptyList()
        val flaws = mutableListOf<String>()
        if (score < 40) {
            flaws += "Acetaldehyde — bottled green; ferment a few days longer next time."
        }
        if (score < 55) {
            flaws += "Banana esters — fermentation ran too warm; cooler is cleaner."
        }
        if (score < 70) {
            flaws += "DMS (cooked corn) — extend the boil or leave the kettle uncovered."
        }
        if (batch.ingredientScores.values.any { it < 50 }) {
            flaws += "Dull malt character — start with higher-grade grain at the malthouse."
        }
        return flaws
    }

    /** Drop in-memory state and re-read from DB. Used by full-game reset. */
    fun reload() {
        activeBatches.clear()
        tier = BreweryTier.STOVETOP
        nextBatchId = 1L
        feedback = null
        feedbackBad = false
        load()
        bump()
    }

    fun upgrade(to: BreweryTier): Boolean {
        if (to.ordinal <= tier.ordinal) return false
        tier = to
        db.systemMeta().put(META_BREWERY_TIER, to.name)
        bump()
        return true
    }

    private fun computeBjcpTotal(batch: BrewBatch): Int {
        val avg = if (batch.ingredientScores.isEmpty()) 50
            else batch.ingredientScores.values.average().toInt()
        // Skill bonus is small + random for Phase 1; later we'll feed
        // in mash temp accuracy + hop timing here.
        val skillBonus = Random.nextInt(0, 16)
        return computeOutputScore(
            inputAverage = avg,
            skillBonus = skillBonus,
            equipmentCap = tier.qualityCap,
        )
    }

    private fun basePriceFor(recipe: BeerRecipe): Int = when (recipe) {
        BeerRecipe.PALE_ALE -> 220
        BeerRecipe.IPA -> 320
        BeerRecipe.DRY_STOUT -> 280
        BeerRecipe.HEFEWEIZEN -> 260
    }

    private fun bump() { revisionTick = revisionTick + 1 }
    private fun note(msg: String) { feedback = msg; feedbackBad = false }
    private fun fail(msg: String) { feedback = msg; feedbackBad = true }

    private fun load() {
        tier = db.systemMeta().get(META_BREWERY_TIER)
            ?.let { runCatching { BreweryTier.valueOf(it) }.getOrNull() }
            ?: BreweryTier.STOVETOP
        nextBatchId = db.systemMeta().get(META_NEXT_BATCH_ID)?.toLongOrNull()?.coerceAtLeast(1L) ?: 1L
        db.brewBatches().getAll().forEach { row ->
            row.toBatchOrNull()?.let { activeBatches += it }
        }
    }

    private fun BrewBatch.toEntity(): BrewBatchEntity {
        val scoresJson = JSONObject().apply {
            ingredientScores.forEach { (k, v) -> put(k.name, v) }
        }
        return BrewBatchEntity(
            id = id,
            recipe = recipe.name,
            stage = stage.name,
            stageStartedMs = stageStartedMs,
            ingredientScoresJson = scoresJson.toString(),
            ingredientTier = ingredientTier.name,
        )
    }

    private fun BrewBatchEntity.toBatchOrNull(): BrewBatch? {
        val r = runCatching { BeerRecipe.valueOf(recipe) }.getOrNull() ?: return null
        val s = runCatching { BrewStage.valueOf(stage) }.getOrNull() ?: return null
        val tierEnum = runCatching { ItemTier.valueOf(ingredientTier) }.getOrDefault(ItemTier.NORMAL)
        val scoresJson = runCatching { JSONObject(ingredientScoresJson) }.getOrNull()
        val scores = mutableMapOf<ItemType, Int>()
        scoresJson?.let { obj ->
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val type = ItemType.valueOfOrNull(k) ?: continue
                scores[type] = obj.optInt(k, 50)
            }
        }
        return BrewBatch(
            id = id,
            recipe = r,
            stage = s,
            stageStartedMs = stageStartedMs,
            ingredientScores = scores,
            ingredientTier = tierEnum,
        )
    }

    companion object {
        @Volatile private var instance: Brewery? = null

        fun get(context: Context): Brewery {
            val existing = instance
            if (existing != null) return existing
            return synchronized(this) {
                instance ?: Brewery(context.applicationContext).also { instance = it }
            }
        }
    }
}
