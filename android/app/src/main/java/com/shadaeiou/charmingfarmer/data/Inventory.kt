package com.shadaeiou.charmingfarmer.data

import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/**
 * The universal quality model. Every produced or processed item in the game
 * — crops, hops, malts, bottled beer, eventually cooked dishes — carries
 * a [score], a [tier], and a quantity. Score is fine-grained; tier is a
 * rare-roll modifier; the [grade] property buckets the score for display.
 *
 * Mirrors how real-world grading systems work (BJCP for beer, USDA for
 * produce, SCA for coffee): a numeric score plus an optional tier modifier
 * for outliers (giant pumpkins, championship tomatoes).
 *
 * Inventories store lists of ItemStacks. Same item type with different
 * scores or tiers stays as separate stacks so quality information isn't
 * lost on merge.
 */
data class ItemStack(
    val type: ItemType,
    val quantity: Int,
    val score: Int = 50,
    val tier: ItemTier = ItemTier.NORMAL,
    val createdMs: Long = 0L,
) {
    val grade: ItemGrade get() = ItemGrade.fromScore(score)

    fun withQuantity(q: Int): ItemStack = copy(quantity = q)
    fun add(amount: Int): ItemStack = copy(quantity = quantity + amount)
    fun subtract(amount: Int): ItemStack = copy(quantity = (quantity - amount).coerceAtLeast(0))

    /** Sale price scales quadratically with score so quality matters a lot. */
    fun unitSellPrice(basePrice: Int): Int {
        val scoreFactor = (score / 100.0).coerceIn(0.0, 1.0)
        val priceFromScore = basePrice * scoreFactor * scoreFactor
        return (priceFromScore * tier.priceMultiplier).toInt().coerceAtLeast(1)
    }
}

enum class ItemTier(val displayName: String, val emojiSuffix: String, val priceMultiplier: Double) {
    NORMAL("Normal", "", 1.0),
    MEGA("Mega", "✨", 1.5),
    GOLDEN("Golden", "💛", 3.0),
    PERFECT("Perfect", "🏆", 8.0),
    ;

    companion object {
        /** Roll a tier on production. Independent of score. */
        fun roll(): ItemTier {
            val r = Random.nextInt(10_000)
            return when {
                r < 10 -> PERFECT       // 0.10%
                r < 100 -> GOLDEN       // 0.90%
                r < 600 -> MEGA         // 5.00%
                else -> NORMAL          // 94.00%
            }
        }
    }
}

enum class ItemGrade(val display: String, val color: Long) {
    F("F",  0xFFA94B4B),
    D("D",  0xFFB07A2A),
    C("C",  0xFF8B7E2A),
    B("B",  0xFF4F7A2F),
    A("A",  0xFF1F5B85),
    S("S",  0xFFB07A1F),
    ;

    companion object {
        fun fromScore(score: Int): ItemGrade = when {
            score >= 95 -> S
            score >= 85 -> A
            score >= 70 -> B
            score >= 55 -> C
            score >= 40 -> D
            else -> F
        }
    }
}

/**
 * One enum to identify any item moving through the supply chain. Adding
 * a new type is non-breaking: load() defaults missing types to GRASS-like
 * empties.
 */
enum class ItemType(val displayName: String, val emoji: String) {
    // Raw grains harvested from the farm
    BARLEY("Barley", "🌾"),
    WHEAT_GRAIN("Wheat", "🌾"),
    OATS("Oats", "🌾"),
    RYE("Rye", "🌾"),
    HOPS("Hops", "🌿"),

    // Malted grain produced at the malthouse
    MALT_PALE("Pale Malt", "🟡"),
    MALT_MUNICH("Munich Malt", "🟠"),
    MALT_CRYSTAL("Crystal Malt", "🟤"),
    MALT_CHOCOLATE("Chocolate Malt", "🟫"),
    MALT_BLACK("Black Malt", "⚫"),

    // Hop varieties (treated as distinct items so recipes can specify them)
    HOPS_CASCADE("Cascade Hops", "🌿"),
    HOPS_SAAZ("Saaz Hops", "🌿"),
    HOPS_FUGGLE("Fuggle Hops", "🌿"),
    HOPS_CITRA("Citra Hops", "🌿"),

    // Yeast strains
    YEAST_ENGLISH_ALE("English Ale Yeast", "🦠"),
    YEAST_AMERICAN_ALE("American Ale Yeast", "🦠"),
    YEAST_GERMAN_LAGER("German Lager Yeast", "🦠"),
    YEAST_GERMAN_WHEAT("German Wheat Yeast", "🦠"),

    // Bottled beer styles
    BEER_PALE_ALE("Pale Ale", "🍺"),
    BEER_IPA("IPA", "🍺"),
    BEER_STOUT("Dry Stout", "🍺"),
    BEER_HEFEWEIZEN("Hefeweizen", "🍺"),

    // Cooked dishes (artisan goods produced at the kitchen)
    DISH_CARROT_SOUP("Carrot Soup", "🥣"),
    DISH_MASHED_POTATOES("Mashed Potatoes", "🥔"),
    DISH_GARDEN_SALAD("Garden Salad", "🥗"),
    DISH_STRAWBERRY_JAM("Strawberry Jam", "🍓"),
    DISH_VEGGIE_STEW("Veggie Stew", "🍲"),
    DISH_STUFFED_PEPPER("Stuffed Peppers", "🫑"),
    DISH_PUMPKIN_PIE("Pumpkin Pie", "🥧"),
    DISH_PINEAPPLE_SALSA("Pineapple Salsa", "🌶️"),
    DISH_WATERMELON_SORBET("Watermelon Sorbet", "🍧"),
    DISH_TRUFFLE_RISOTTO("Truffle Risotto", "🍚"),
    ;

    companion object {
        fun valueOfOrNull(name: String): ItemType? =
            runCatching { valueOf(name) }.getOrNull()
    }
}

/**
 * A bag of stacks held at one location. Adding the same (type, score, tier)
 * combination merges quantities; otherwise we keep separate stacks so we
 * don't average away high-quality items.
 */
data class Inventory(val stacks: List<ItemStack> = emptyList()) {

    fun totalOf(type: ItemType): Int = stacks.filter { it.type == type }.sumOf { it.quantity }

    /** Weighted average score of all stacks of [type]; null if none. */
    fun averageScore(type: ItemType): Int? {
        val matching = stacks.filter { it.type == type }
        val totalQty = matching.sumOf { it.quantity }
        if (totalQty == 0) return null
        val weighted = matching.sumOf { it.score.toLong() * it.quantity }
        return (weighted / totalQty).toInt()
    }

    fun add(stack: ItemStack): Inventory {
        if (stack.quantity <= 0) return this
        val existingIdx = stacks.indexOfFirst {
            it.type == stack.type && it.score == stack.score && it.tier == stack.tier
        }
        return if (existingIdx >= 0) {
            val merged = stacks[existingIdx].add(stack.quantity)
            Inventory(stacks.toMutableList().also { it[existingIdx] = merged })
        } else {
            Inventory(stacks + stack)
        }
    }

    /**
     * Take [amount] of [type] from this inventory, drawing from the
     * lowest-quality stacks first (so the user keeps their best stuff).
     * Returns the inventory after removal and the actual stacks pulled.
     * If insufficient quantity, returns null.
     */
    fun remove(type: ItemType, amount: Int): Pair<Inventory, List<ItemStack>>? {
        if (amount <= 0) return this to emptyList()
        if (totalOf(type) < amount) return null
        val sorted = stacks.filter { it.type == type }.sortedBy { it.score }
        val others = stacks.filter { it.type != type }
        var remaining = amount
        val pulled = mutableListOf<ItemStack>()
        val keep = mutableListOf<ItemStack>()
        for (s in sorted) {
            if (remaining <= 0) {
                keep += s
                continue
            }
            if (s.quantity <= remaining) {
                pulled += s
                remaining -= s.quantity
            } else {
                pulled += s.withQuantity(remaining)
                keep += s.subtract(remaining)
                remaining = 0
            }
        }
        return Inventory(others + keep) to pulled
    }

    /**
     * Take [amount] of [type] picking the BEST quality first. Used by recipes
     * where the player has explicitly committed top-shelf ingredients.
     */
    fun removeBest(type: ItemType, amount: Int): Pair<Inventory, List<ItemStack>>? {
        if (amount <= 0) return this to emptyList()
        if (totalOf(type) < amount) return null
        val sorted = stacks.filter { it.type == type }.sortedByDescending { it.score }
        val others = stacks.filter { it.type != type }
        var remaining = amount
        val pulled = mutableListOf<ItemStack>()
        val keep = mutableListOf<ItemStack>()
        for (s in sorted) {
            if (remaining <= 0) {
                keep += s
                continue
            }
            if (s.quantity <= remaining) {
                pulled += s
                remaining -= s.quantity
            } else {
                pulled += s.withQuantity(remaining)
                keep += s.subtract(remaining)
                remaining = 0
            }
        }
        return Inventory(others + keep) to pulled
    }

    fun toJson(): JSONArray {
        val arr = JSONArray()
        for (s in stacks) {
            arr.put(JSONObject()
                .put("type", s.type.name)
                .put("quantity", s.quantity)
                .put("score", s.score)
                .put("tier", s.tier.name)
                .put("createdMs", s.createdMs))
        }
        return arr
    }

    companion object {
        fun fromJson(arr: JSONArray?): Inventory {
            if (arr == null) return Inventory()
            val out = mutableListOf<ItemStack>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val type = ItemType.valueOfOrNull(o.optString("type")) ?: continue
                val qty = o.optInt("quantity", 0)
                if (qty <= 0) continue
                out += ItemStack(
                    type = type,
                    quantity = qty,
                    score = o.optInt("score", 50).coerceIn(0, 100),
                    tier = runCatching { ItemTier.valueOf(o.optString("tier", "NORMAL")) }
                        .getOrDefault(ItemTier.NORMAL),
                    createdMs = o.optLong("createdMs", 0L),
                )
            }
            return Inventory(out)
        }
    }
}

/**
 * Compute the score of a produced item from its inputs. Used by malting,
 * brewing, cooking — anywhere a recipe takes ingredients and emits a new
 * stack. Output is bounded by the equipment quality cap.
 *
 * - inputAverage: weighted average of input ingredient scores
 * - skillBonus: 0..30 from how well the player handled the process (mash
 *   temp accuracy, hop timing, fermentation stability, etc.)
 * - equipmentCap: maximum score the equipment can produce (e.g. tier-1
 *   stovetop kettle caps at 70)
 */
fun computeOutputScore(
    inputAverage: Int,
    skillBonus: Int,
    equipmentCap: Int,
): Int {
    val raw = (inputAverage * 0.7 + skillBonus * 1.0 + 30 * 0.3).toInt()
    return raw.coerceIn(0, equipmentCap)
}
