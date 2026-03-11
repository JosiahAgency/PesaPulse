package com.example.cashia.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionCategory(val label: String, val emoji: String, val isSystem: Boolean = true) {
    FOOD("Food & Drinks", "🍔"),
    TRANSPORT("Transport", "🚗"),
    UTILITIES("Utilities & Bills", "💡"),
    SHOPPING("Shopping", "🛍️"),
    AIRTIME("Airtime & Data", "📱"),
    ENTERTAINMENT("Entertainment", "🎬"),
    HEALTH("Health & Medical", "💊"),
    SAVINGS("Savings & Banking", "🏦"),
    SALARY("Salary & Income", "💼"),
    SEND_MONEY("Send Money", "💸"),
    WITHDRAW("Withdrawal", "🏧"),
    RECEIVED("Received", "📥"),
    EDUCATION("Education", "📚"),
    GOVERNMENT("Government & Tax", "🏛️"),
    INSURANCE("Insurance", "🛡️"),
    INVESTMENT("Investment", "📈"),
    BALANCE_INQUIRY("Balance Inquiry", "🔍"),
    OTHER("Other", "📦");

}

@Entity(tableName = "CustomCategories")
data class CustomCategory(
    @PrimaryKey val id: String,
    val label: String,
    val emoji: String,
    val keywords: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class ResolvedCategory(
    val id: String,          // TransactionCategory.name OR CustomCategory.id
    val label: String,
    val emoji: String,
    val isSystem: Boolean
)

fun TransactionCategory.toResolved() = ResolvedCategory(name, label, emoji, true)
fun CustomCategory.toResolved() = ResolvedCategory(id, label, emoji, false)


fun guessCategory(
    recipient: String,
    type: String,
    customCategories: List<CustomCategory> = emptyList()
): String {
    val r = recipient.lowercase().trim()

    // ── 1. Check user custom categories first ──────────────────────────────
    for (custom in customCategories) {
        val keywords =
            custom.keywords.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        if (keywords.any { kw -> r.contains(kw) }) {
            return custom.id
        }
    }

    // ── 2. Income types ────────────────────────────────────────────────────
    if (type == "income") {
        return when {
            r.containsAny(
                "salary",
                "payroll",
                "wage",
                "basic pay",
                "net pay"
            ) -> TransactionCategory.SALARY.name

            r.containsAny(
                "dividend",
                "interest",
                "returns",
                "profit"
            ) -> TransactionCategory.INVESTMENT.name

            r.containsAny("nhif", "nssf", "kra", "revenue") -> TransactionCategory.GOVERNMENT.name
            else -> TransactionCategory.RECEIVED.name
        }
    }

    // ── 3. Airtime & Data ─────────────────────────────────────────────────
    if (r == "safaricom airtime" || r.containsAny(
            "airtime", "data bundle", "safaricom data", "airtel data",
            "telkom data", "faiba", "youtube go", "bonga"
        )
    ) return TransactionCategory.AIRTIME.name

    // ── 4. Withdrawal ─────────────────────────────────────────────────────
    if (r.containsAny(
            "withdraw", "agent", "atm", "pos", "pochi la biashara",
            "cash out", "cashout"
        )
    ) return TransactionCategory.WITHDRAW.name

    // ── 5. Savings & Banking ──────────────────────────────────────────────
    if (r.containsAny(
            // Major Kenya Banks
            "equity bank", "equity", "kcb", "co-op bank", "cooperative bank",
            "absa", "stanbic", "stanchart", "standard chartered", "ncba",
            "dtb", "diamond trust", "family bank", "prime bank", "sidian",
            "credit bank", "gulf african", "ib africa", "housing finance",
            "hfc", "victoria commercial",
            // SACCOs & MFIs
            "sacco", "mwalimu", "unaitas", "stima sacco", "harambee sacco",
            "tower sacco", "metropolitan", "jamii bora",
            // Mobile savings / wallets
            "m-shwari", "mshwari", "kcb m-pesa", "fuliza", "hustler fund",
            "tala", "branch", "zenka", "okash", "okolea", "timiza",
            "kwara", "faulu", "letshego"
        )
    ) return TransactionCategory.SAVINGS.name

    // ── 6. Government & Tax ───────────────────────────────────────────────
    if (r.containsAny(
            "kra", "kenya revenue", "ecitizen", "e-citizen", "ntsa",
            "huduma", "nhif", "nssf", "nairobi city county", "county",
            "government", "passport", "driving licence", "birth certificate",
            "title deed", "land rates", "nairobi water", "sewerage"
        )
    ) return TransactionCategory.GOVERNMENT.name

    // ── 7. Insurance ──────────────────────────────────────────────────────
    if (r.containsAny(
            "insurance", "jubilee", "britam", "madison", "cic insurance",
            "aaa insurance", "resolution insurance", "sanlam", "aar",
            "heritage insurance", "pioneer assurance", "icea lion",
            "liberty life", "old mutual", "nhif"
        )
    ) return TransactionCategory.INSURANCE.name

    // ── 8. Investment ─────────────────────────────────────────────────────
    if (r.containsAny(
            "nse", "nairobi securities", "cdsc", "cma", "xplicit",
            "cytonn", "genghis", "stocksmaster", "zimele", "amana",
            "britam asset", "old mutual invest", "cic asset",
            "equity investment", "nabo", "sanlam invest",
            "mmf", "money market", "unit trust", "t-bill", "treasury"
        )
    ) return TransactionCategory.INVESTMENT.name

    // ── 9. Utilities & Bills ──────────────────────────────────────────────
    if (r.containsAny(
            // Electricity
            "kplc", "kenya power", "umeme",
            // Water
            "nairobi water", "nwsc", "mombasa water", "kikuyu water",
            "mavoko water", "eldoret water", "kisumu water",
            // Internet / TV
            "zuku", "faiba", "safaricom home", "airtel home", "telkom",
            "poa internet", "net2phone", "liquid home", "jti broadband",
            "dstv", "gotv", "azam", "showmax", "startimes",
            // Cooking gas
            "gas", "afrigas", "k-gas", "total gas", "shell gas", "lake gas",
            "pro gas", "dolgas", "orion gas"
        )
    ) return TransactionCategory.UTILITIES.name

    // ── 10. Education ─────────────────────────────────────────────────────
    if (r.containsAny(
            "school", "college", "university", "tuition", "education",
            "academy", "institute", "polytechnic", "tvet", "helb",
            "higher education", "knec", "kcse", "kcpe", "exam fee",
            "school fees", "kindergarten", "nursery", "playgroup",
            "uon", "ku", "mku", "kenyatta university", "strathmore",
            "usiu", "daystar", "catholic university", "pwani university"
        )
    ) return TransactionCategory.EDUCATION.name

    // ── 11. Health & Medical ──────────────────────────────────────────────
    if (r.containsAny(
            "hospital", "clinic", "pharmacy", "chemist", "health",
            "medical", "doctor", "dispensary", "nursing home", "dentist",
            "optician", "laboratory", "lab", "radiology", "physiotherapy",
            // Specific Kenya facilities
            "knh", "nairobi hospital", "aga khan", "mater", "mp shah",
            "karen hospital", "gertrudes", "coptic", "premier hospital",
            "coast general", "kisumu county", "eldoret referral",
            "goodlife pharmacy", "haltons", "medisel", "sapphire",
            "lifecare", "nhif"
        )
    ) return TransactionCategory.HEALTH.name

    // ── 12. Transport ─────────────────────────────────────────────────────
    if (r.containsAny(
            // Ride-hailing
            "uber", "bolt", "little cab", "littlecab", "faras", "mondo",
            "ndovu", "wasili",
            // Fuel / petrol stations
            "shell", "total", "kenol", "oil libya", "oil com", "rubis",
            "lake oil", "hashi energy", "petro", "fuel", "petrol",
            "galana", "vivo energy",
            // Matatu / public transport
            "matatu", "bus", "shuttle", "pss", "ez link", "bemobile",
            "nairobi expressway", "sgr", "madaraka express", "ferry",
            "parking", "airport"
        )
    ) return TransactionCategory.TRANSPORT.name

    // ── 13. Food & Drinks ─────────────────────────────────────────────────
    if (r.containsAny(
            // Supermarkets
            "naivas", "quickmart", "carrefour", "chandarana", "tuskys",
            "nakumatt", "cleanshelf", "eastmatt", "highrise", "food plus",
            "market", "supermarket", "grocery",
            // Restaurants & fast food
            "kfc", "java", "pizza inn", "pizza hut", "chicken inn",
            "galitos", "debonairs", "hardee's", "burger king", "subway",
            "steers", "fishaways", "mama's", "artcaffe", "dormans",
            "java house", "k'osewe", "carnivore", "the hub",
            "restaurant", "cafe", "coffee", "bakery", "hotel",
            "food court", "canteen", "mess", "eatery",
            // Delivery platforms
            "glovo", "jumia food", "uber eats", "food", "meals"
        )
    ) return TransactionCategory.FOOD.name

    // ── 14. Entertainment ─────────────────────────────────────────────────
    if (r.containsAny(
            "netflix", "showmax", "spotify", "youtube premium", "apple music",
            "deezer", "boomplay", "tidal",
            "cinema", "imax", "century cinemas", "silverbird",
            "club", "lounge", "bar", "pub", "nightclub",
            "ticket", "concert", "event", "festival",
            "gaming", "playstation", "xbox", "steam", "ea games",
            "sport", "gym", "fitness", "yoga", "swimming"
        )
    ) return TransactionCategory.ENTERTAINMENT.name

    // ── 15. Shopping ─────────────────────────────────────────────────────
    if (r.containsAny(
            // Online marketplaces
            "jumia", "kilimall", "jiji", "masoko", "amazon",
            "aliexpress", "shein", "temu",
            // Clothing
            "clothing", "fashion", "wear", "boutique", "clothes",
            "shoes", "footwear", "apparel",
            // Electronics
            "electronics", "phone", "laptop", "computer", "gadget",
            "samsung", "apple store", "techno", "infinix",
            // Furniture / home
            "furniture", "home", "ikea", "decor",
            "shop", "store", "mall", "centre", "plaza"
        )
    ) return TransactionCategory.SHOPPING.name

    if (type == "expense" && looksLikePersonName(r)) {
        return TransactionCategory.SEND_MONEY.name
    }

    return TransactionCategory.OTHER.name
}

private fun String.containsAny(vararg keywords: String): Boolean =
    keywords.any { this.contains(it, ignoreCase = true) }

private fun looksLikePersonName(r: String): Boolean {
    val noDigits = r.none { it.isDigit() }
    val words = r.trim().split("\\s+".toRegex())
    val isShortWords = words.all { it.length in 2..15 }
    return noDigits && words.size in 1..4 && isShortWords
}