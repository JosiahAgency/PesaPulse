package com.example.cashia.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionCategory(
    val label: String,
    val emoji: String,
    val parentId: String? = null
) {
    // ── OUTGOING: TOP-LEVEL ───────────────────────────────────────────────

    FOOD("Food & Drinks", "🍽️"),
    TRANSPORT("Transport", "🚌"),
    FUEL("Fuel & Petrol", "⛽"),
    CAR_MAINTENANCE("Car Maintenance", "🔧"),
    UTILITIES("Utilities & Bills", "💡"),
    SHOPPING("Shopping", "🛍️"),
    AIRTIME("Airtime & Data", "📶"),
    ENTERTAINMENT("Entertainment", "🎬"),
    HEALTH("Health & Medical", "🏥"),
    SAVINGS("Savings & Banking", "🏦"),
    EDUCATION("Education", "🎓"),
    GOVERNMENT("Government & Tax", "🏛️"),
    INSURANCE("Insurance", "🛡️"),
    INVESTMENT("Investment", "📈"),
    SEND_MONEY("Send Money", "💸"),
    WITHDRAW("Withdrawal", "🏧"),
    FULIZA_REPAYMENT("Fuliza Repayment", "🔄"),

    // ── INCOMING: TOP-LEVEL ───────────────────────────────────────────────

    SALARY("Salary & Payroll", "💼"),
    BUSINESS_INCOME("Business Income", "🏪"),
    RECEIVED_MONEY("Received Money", "📲"),
    LOAN_DISBURSEMENT("Loan Received", "🤝"),
    INVESTMENT_RETURNS("Investment Returns", "💹"),
    GOVERNMENT_REFUND("Gov't Refund / NHIF", "🏛️"),

    // ── FOOD SUBCATEGORIES ────────────────────────────────────────────────

    GROCERIES("Groceries", "🛒", parentId = "FOOD"),
    RESTAURANTS("Restaurants & Cafes", "🍴", parentId = "FOOD"),
    FAST_FOOD("Fast Food", "🍟", parentId = "FOOD"),
    FOOD_DELIVERY("Food Delivery", "🛵", parentId = "FOOD"),

    // ── TRANSPORT SUBCATEGORIES ───────────────────────────────────────────

    RIDE_HAILING("Ride Hailing", "🚕", parentId = "TRANSPORT"),
    PUBLIC_TRANSPORT("Matatu & Bus", "🚌", parentId = "TRANSPORT"),
    TRAIN("Train / SGR", "🚆", parentId = "TRANSPORT"),
    PARKING("Parking", "🅿️", parentId = "TRANSPORT"),
    TOLLS("Tolls & Expressway", "🛣️", parentId = "TRANSPORT"),

    // ── CAR MAINTENANCE SUBCATEGORIES ─────────────────────────────────────

    CAR_SERVICE("Car Service", "🔩", parentId = "CAR_MAINTENANCE"),
    CAR_WASH("Car Wash", "🚿", parentId = "CAR_MAINTENANCE"),
    TYRES("Tyres & Wheels", "🛞", parentId = "CAR_MAINTENANCE"),

    // ── UTILITIES SUBCATEGORIES ───────────────────────────────────────────

    ELECTRICITY("Electricity (KPLC)", "⚡", parentId = "UTILITIES"),
    WATER("Water Bill", "💧", parentId = "UTILITIES"),
    INTERNET("Internet & TV", "📡", parentId = "UTILITIES"),
    COOKING_GAS("Cooking Gas", "🔥", parentId = "UTILITIES"),

    // ── HEALTH SUBCATEGORIES ──────────────────────────────────────────────

    PHARMACY("Pharmacy", "💊", parentId = "HEALTH"),
    HOSPITAL_VISIT("Hospital / Clinic", "🩺", parentId = "HEALTH"),
    GYM("Gym & Fitness", "🏋️", parentId = "HEALTH"),

    // ── SHOPPING SUBCATEGORIES ────────────────────────────────────────────

    ONLINE_SHOPPING("Online Shopping", "📦", parentId = "SHOPPING"),
    CLOTHING("Clothing & Fashion", "👗", parentId = "SHOPPING"),
    ELECTRONICS("Electronics", "📱", parentId = "SHOPPING"),

    // ── FALLBACK / SYSTEM ─────────────────────────────────────────────────

    OTHER("Other", "📦"),
    BALANCE_INQUIRY("Balance Inquiry", "🔍");
}

@Entity(tableName = "CustomCategories")
data class CustomCategory(
    @PrimaryKey val id: String,
    val label: String,
    val emoji: String,
    val keywords: String,
    val parentId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "UserCorrections")
data class UserCorrection(
    @PrimaryKey val recipient: String,
    val categoryId: String,
    val hitCount: Int = 1,
    val updatedAt: Long = System.currentTimeMillis()
)

data class ResolvedCategory(
    val id: String,
    val label: String,
    val emoji: String,
    val isSystem: Boolean,
    val parentId: String? = null
)

fun TransactionCategory.toResolved() = ResolvedCategory(name, label, emoji, true, parentId)
fun CustomCategory.toResolved() = ResolvedCategory(id, label, emoji, false, parentId)

/**
 * Guess the best category for a transaction.
 *
 * Resolution order (first match wins):
 *   0. Exact user correction  (highest priority – explicit human feedback)
 *   1. Custom category keywords
 *   2. Income-specific rules
 *   3. Airtime & Data
 *   4. **Food** (groceries → delivery → fast food → restaurants)  ← moved UP
 *   5. Parking                                                     ← strengthened
 *   6. Fuel                                                        ← before generic transport
 *   7. Car maintenance
 *   8. Withdrawal
 *   9. Savings & Banking
 *  10. Fuliza repayment
 *  11. Government & Tax
 *  12. Insurance
 *  13. Investment
 *  14. Utilities
 *  15. Education
 *  16. Health
 *  17. Tolls / Expressway
 *  18. Transport (ride-hailing → public)
 *  19. Entertainment
 *  20. Shopping  ← now AFTER food; no risk of pizza bleeding into shopping
 *  21. Send Money (person name heuristic)
 *  22. OTHER
 */
fun guessCategory(
    recipient: String,
    type: String,
    customCategories: List<CustomCategory> = emptyList(),
    userCorrections: Map<String, String> = emptyMap()
): String {
    val r = recipient.lowercase().trim()

    userCorrections[r]?.let { return it }

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
                "salary", "payroll", "wage", "basic pay", "net pay", "stipend", "allowance"
            ) -> TransactionCategory.SALARY.name

            r.containsAny(
                "till",
                "paybill",
                "lipa na mpesa",
                "lipa mpesa",
                "business",
                "merchant",
                "pos receipt"
            ) -> TransactionCategory.BUSINESS_INCOME.name

            r.containsAny(
                "dividend", "interest earned", "returns", "profit",
                "unit trust", "mmf", "money market", "t-bill"
            ) -> TransactionCategory.INVESTMENT_RETURNS.name

            r.containsAny(
                "nhif refund", "nssf refund", "kra refund", "tax refund",
                "government refund", "county refund"
            ) -> TransactionCategory.GOVERNMENT_REFUND.name

            r.containsAny(
                "fuliza",
                "m-shwari loan",
                "mshwari loan",
                "kcb m-pesa loan",
                "hustler fund",
                "tala",
                "branch",
                "zenka",
                "okash",
                "okolea",
                "timiza loan",
                "haraka loan",
                "berry loan"
            ) -> TransactionCategory.LOAN_DISBURSEMENT.name

            else -> TransactionCategory.RECEIVED_MONEY.name
        }
    }

    if (type == "expense" && r.contains("buy goods"))
        return TransactionCategory.GROCERIES.name

    // ── 3. Airtime & Data ─────────────────────────────────────────────────
    if (r == "safaricom airtime" || r.containsAny(
            "airtime", "data bundle", "safaricom", "airtel",
            "telkom ", "faiba", "bonga points", "internet bundle"
        )
    ) return TransactionCategory.AIRTIME.name

    // ── 4. Food ───────────────────────────────────────────────────────────

    if (r.containsAny(
            "naivas", "quickmart", "carrefour", "chandarana",
            "tuskys", "cleanshelf", "eastmatt",
            "supermarket", "mini mart", "grocery", "duka"
        )
    ) return TransactionCategory.GROCERIES.name

    if (r.containsAny(
            "glovo", "jumia food", "uber eats", "food delivery", "chowdeck", "sendy food"
        )
    ) return TransactionCategory.FOOD_DELIVERY.name

    if (r.containsAny(
            "kfc", "pizza inn", "pizza hut", "chicken inn",
            "galitos", "debonairs", "burger king", "subway",
            "steers", "fishaways", "smokie", "nyama choma"
        )
    ) return TransactionCategory.FAST_FOOD.name

    if (r.containsAny(
            "java", "artcaffe", "dormans", "java house", "k'osewe",
            "carnivore", "restaurant", "cafe", "coffee", "bakery",
            "hotel dining", "food court", "canteen", "mess", "eatery",
            "mama's", "brew bistro", "roast house"
        )
    ) return TransactionCategory.RESTAURANTS.name

    // ── 5. Parking ────────────────────────────────────────────────────────
    if (r.containsAny(
            "parking", "e-parking", "county parking",
            "nairobi parking", "parking fee", "parking ticket", "evopay"
        )
    ) return TransactionCategory.PARKING.name

    // ── 6. Fuel ───────────────────────────────────────────────────────────
    if (r.containsAny(
            "shell", "energies", "kenol", "oil libya", "rubis",
            "lake oil", "hashi energy", "galana", "vivo energy",
            "petro station", "gulf energy", "total",
            "petrol", "diesel", "fuel"
        )
    ) return TransactionCategory.FUEL.name

    // ── 7. Car Maintenance ────────────────────────────────────────────────
    if (r.containsAny(
            "garage", "mechanic", "auto service", "car service",
            "vehicle service", "service centre", "oil change",
            "engine oil", "spare parts", "spares", "auto parts",
            "car repair", "panel beating", "body work",
            "car wash", "autowash", "auto wash", "hand wash",
            "tyre", "tire", "wheel alignment", "wheel balance",
            "puncture", "rim"
        )
    ) {
        return when {
            r.containsAny("wash") -> TransactionCategory.CAR_WASH.name
            r.containsAny(
                "tyre",
                "tire",
                "wheel",
                "puncture",
                "rim"
            ) -> TransactionCategory.TYRES.name

            else -> TransactionCategory.CAR_SERVICE.name
        }
    }

    // ── 8. Withdrawal ─────────────────────────────────────────────────────
    if (r.containsAny(
            "withdraw", "agent", "atm", "pochi la biashara",
            "cash out", "cashout", "mpesa agent", "pochi"
        )
    ) return TransactionCategory.WITHDRAW.name

    // ── 9. Savings & Banking ──────────────────────────────────────────────
    if (r.containsAny(
            "equity bank", "equity ", "kcb", "co-op bank", "cooperative bank",
            "absa", "stanbic", "stanchart", "standard chartered", "ncba",
            "dtb", "diamond trust", "family bank", "prime bank", "sidian",
            "credit bank", "gulf african", "ib africa", "housing finance",
            "hfc", "victoria commercial",
            "sacco", "mwalimu", "unaitas", "stima sacco", "harambee sacco",
            "tower sacco", "metropolitan sacco",
            "m-shwari", "mshwari", "kcb m-pesa", "timiza", "kwara",
            "faulu", "letshego", "bank transfer", "bank deposit"
        )
    ) return TransactionCategory.SAVINGS.name

    // ── 10. Fuliza / Loan Repayment ───────────────────────────────────────
    if (r.containsAny(
            "fuliza repayment", "fuliza payment", "loan repayment",
            "repay fuliza", "repay loan", "m-shwari repayment",
            "tala repayment", "branch repayment", "zenka repayment",
            "okash repayment", "okolea repayment", "haraka repayment"
        )
    ) return TransactionCategory.FULIZA_REPAYMENT.name

    // ── 11. Government & Tax ──────────────────────────────────────────────
    if (r.containsAny(
            "kra", "kenya revenue", "ecitizen", "e-citizen", "ntsa",
            "huduma", "nhif", "nssf", "nairobi city county", "county",
            "government", "passport", "driving licence", "birth certificate",
            "title deed", "land rates", "nairobi water sewerage",
            "nema", "kebs", "knec"
        )
    ) return TransactionCategory.GOVERNMENT.name

    // ── 12. Insurance ─────────────────────────────────────────────────────
    if (r.containsAny(
            "insurance", "jubilee", "britam", "aar", "madison",
            "kenindia", "sanlam", "aig", "resolution insurance",
            "first assurance", "old mutual", "CIC insurance"
        )
    ) return TransactionCategory.INSURANCE.name

    // ── 13. Utilities ─────────────────────────────────────────────────────
    if (r.containsAny("kplc", "kenya power", "umeme", "stima"))
        return TransactionCategory.ELECTRICITY.name

    if (r.containsAny(
            "nairobi water", "nwsc", "mombasa water", "kikuyu water",
            "mavoko water", "eldoret water", "kisumu water", "water bill"
        )
    ) return TransactionCategory.WATER.name

    if (r.containsAny(
            "zuku", "faiba home", "safaricom home", "airtel home",
            "telkom home", "poa internet", "liquid home", "jti broadband",
            "dstv", "gotv", "azam", "showmax", "startimes"
        )
    ) return TransactionCategory.INTERNET.name

    if (r.containsAny(
            "gas", "afrigas", "k-gas", "total gas", "shell gas", "lake gas",
            "pro gas", "dolgas", "orion gas", "lpg", "cooking gas"
        )
    ) return TransactionCategory.COOKING_GAS.name

    // ── 14. Education ─────────────────────────────────────────────────────
    if (r.containsAny(
            "school", "college", "university", "tuition", "education",
            "academy", "institute", "polytechnic", "tvet", "helb",
            "kcse", "kcpe", "exam fee", "school fees",
            "kindergarten", "nursery", "playgroup",
            "uon", "ku", "mku", "kenyatta university", "strathmore",
            "usiu", "daystar", "catholic university", "pwani university",
            "kabarak", "zetech", "multimedia university"
        )
    ) return TransactionCategory.EDUCATION.name

    // ── 15. Health ────────────────────────────────────────────────────────
    if (r.containsAny(
            "pharmacy", "chemist", "goodlife pharmacy", "haltons",
            "medisel", "sapphire pharmacy", "lifecare pharmacy", "dawa", "drug store"
        )
    ) return TransactionCategory.PHARMACY.name

    if (r.containsAny(
            "hospital", "clinic", "health", "medical", "doctor",
            "dispensary", "nursing home", "dentist", "optician",
            "laboratory", "radiology", "physiotherapy",
            "knh", "nairobi hospital", "aga khan", "mater", "mp shah",
            "karen hospital", "gertrudes", "coptic", "premier hospital",
            "coast general", "kisumu county referral", "eldoret referral"
        )
    ) return TransactionCategory.HOSPITAL_VISIT.name

    if (r.containsAny(
            "gym", "fitness", "yoga", "swimming", "crossfit",
            "planet fitness", "the gym", "snap fitness"
        )
    ) return TransactionCategory.GYM.name

    // ── 16. Tolls ─────────────────────────────────────────────────────────
    if (r.containsAny("expressway", "toll", "nairobi expressway"))
        return TransactionCategory.TOLLS.name

    // ── 17. Transport ─────────────────────────────────────────────────────
    if (r.containsAny(
            "uber", "bolt", "little cab", "faras",
            "matatu", "bus fare", "shuttle", "citi hoppa",
            "kbs", "2nk", "easy coach", "modern coast",
            "likoni ferry", "ferry", "jkia", "airport transfer",
            "sgr", "train"
        )
    ) {
        return when {
            r.containsAny("uber", "bolt", "little cab", "littlecab", "faras", "wasili", "ndovu")
                -> TransactionCategory.RIDE_HAILING.name

            r.containsAny("sgr", "train")
                -> TransactionCategory.TRAIN.name

            else -> TransactionCategory.PUBLIC_TRANSPORT.name
        }
    }

    // ── 18. Entertainment ─────────────────────────────────────────────────
    if (r.containsAny(
            "netflix", "prime video", "spotify", "youtube premium",
            "apple music", "deezer", "boomplay", "tidal",
            "cinema", "imax", "century cinemas", "silverbird",
            "club", "lounge", "bar", "pub", "nightclub",
            "ticket", "concert", "event", "festival",
            "gaming", "playstation", "xbox", "steam",
            "sportpesa", "betika", "odibets", "mcheza"
        )
    ) return TransactionCategory.ENTERTAINMENT.name

    // ── 19. Shopping ──────────────────────────────────────────────────────
    if (r.containsAny(
            "jumia",
            "kilimall",
            "jiji",
            "masoko",
            "amazon",
            "aliexpress",
            "shein",
            "temu"
        )
    )
        return TransactionCategory.ONLINE_SHOPPING.name

    if (r.containsAny(
            "clothing",
            "fashion",
            "wear",
            "boutique",
            "clothes",
            "shoes",
            "footwear",
            "apparel",
            "mtumba",
            "second hand"
        )
    )
        return TransactionCategory.CLOTHING.name

    if (r.containsAny(
            "electronics",
            "phone",
            "laptop",
            "computer",
            "gadget",
            "samsung store",
            "apple store",
            "techno",
            "infinix",
            "iphone",
            "tablet"
        )
    )
        return TransactionCategory.ELECTRONICS.name

    if (r.containsAny(
            "shop",
            "store",
            "mall",
            "centre",
            "plaza",
            "two rivers",
            "garden city",
            "westgate",
            "the junction",
            "village market",
            "sarit centre",
            "galleria",
            "hub karen"
        )
    )
        return TransactionCategory.SHOPPING.name

    // ── 20. Send Money ────────────────────────────────────────────────────
    if (type == "expense" && looksLikePersonName(r) && !r.containsAny(
            "shop", "store", "enterprise", "limited", "ltd",
            "agency", "services", "supplies", "hardware"
        )
    ) return TransactionCategory.SEND_MONEY.name

    return TransactionCategory.OTHER.name
}

private fun String.containsAny(vararg keywords: String): Boolean =
    keywords.any { this.contains(it, ignoreCase = true) }

private fun looksLikePersonName(r: String): Boolean {
    val noDigits = r.none { it.isDigit() }
    val words = r.trim().split("\\s+".toRegex())
    return noDigits && words.size in 1..4 && words.all { it.length in 2..20 }
}