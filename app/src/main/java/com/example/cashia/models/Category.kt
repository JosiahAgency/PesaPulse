package com.example.cashia.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionCategory(
    val label: String,
    val emoji: String,
    val isSystem: Boolean = true,
    val parentId: String? = null
) {

    // ── OUTGOING: TOP-LEVEL ───────────────────────────────────────────────

    FOOD("Food & Drinks", "🍽️"),
    TRANSPORT("Matatu & Rides", "🚌"),
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
    GOVERNMENT_REFUND("Gov't Refund / NHIF", "🏦"),

    // ── OUTGOING: FOOD SUBCATEGORIES ──────────────────────────────────────

    GROCERIES("Groceries & Supermarket", "🛒", parentId = "FOOD"),
    RESTAURANTS("Restaurants & Cafes", "🍴", parentId = "FOOD"),
    FAST_FOOD("Fast Food", "🍟", parentId = "FOOD"),
    FOOD_DELIVERY("Food Delivery", "🛵", parentId = "FOOD"),

    // ── OUTGOING: TRANSPORT SUBCATEGORIES ────────────────────────────────

    RIDE_HAILING("Ride Hailing (Uber/Bolt)", "🚕", parentId = "TRANSPORT"),
    MATATU("Matatu & Bus", "🚌", parentId = "TRANSPORT"),
    PARKING("Parking", "🅿️", parentId = "TRANSPORT"),
    SGR("SGR / Train", "🚆", parentId = "TRANSPORT"),

    // ── OUTGOING: FUEL SUBCATEGORIES ─────────────────────────────────────

    PETROL("Petrol", "⛽", parentId = "FUEL"),
    DIESEL("Diesel", "🛢️", parentId = "FUEL"),

    // ── OUTGOING: CAR MAINTENANCE SUBCATEGORIES ───────────────────────────

    CAR_SERVICE("Car Service / Garage", "🔩", parentId = "CAR_MAINTENANCE"),
    CAR_WASH("Car Wash", "🚿", parentId = "CAR_MAINTENANCE"),
    TYRES("Tyres & Wheels", "🛞", parentId = "CAR_MAINTENANCE"),
    INSURANCE_CAR("Vehicle Insurance", "🛡️", parentId = "CAR_MAINTENANCE"),

    // ── OUTGOING: UTILITIES SUBCATEGORIES ────────────────────────────────

    ELECTRICITY("Electricity (KPLC)", "⚡", parentId = "UTILITIES"),
    WATER("Water Bill", "💧", parentId = "UTILITIES"),
    INTERNET("Internet & TV", "📡", parentId = "UTILITIES"),
    COOKING_GAS("Cooking Gas", "🔥", parentId = "UTILITIES"),

    // ── OUTGOING: HEALTH SUBCATEGORIES ───────────────────────────────────

    PHARMACY("Pharmacy & Chemist", "💊", parentId = "HEALTH"),
    HOSPITAL_VISIT("Hospital / Clinic", "🩺", parentId = "HEALTH"),
    GYM("Gym & Fitness", "🏋️", parentId = "HEALTH"),

    // ── OUTGOING: SHOPPING SUBCATEGORIES ─────────────────────────────────

    ONLINE_SHOPPING("Online Shopping", "📦", parentId = "SHOPPING"),
    CLOTHING("Clothing & Fashion", "👗", parentId = "SHOPPING"),
    ELECTRONICS("Electronics & Gadgets", "📱", parentId = "SHOPPING"),

    // ── LOAN / FULIZA (incoming disbursement) ─────────────────────────────

    FULIZA_LOAN("Fuliza / M-Shwari Loan", "💰", parentId = "LOAN_DISBURSEMENT"),
    MOBILE_LOAN("Mobile Loan (Tala/Branch)", "🤳", parentId = "LOAN_DISBURSEMENT"),

    // ── FALLBACK ──────────────────────────────────────────────────────────

    OTHER("Other", "📦"),

    // ── SYSTEM / NEUTRAL ──────────────────────────────────────────────────────
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

data class ResolvedCategory(
    val id: String,          // TransactionCategory.name OR CustomCategory.id
    val label: String,
    val emoji: String,
    val isSystem: Boolean,
    val parentId: String? = null
)

fun TransactionCategory.toResolved() = ResolvedCategory(name, label, emoji, true, parentId)
fun CustomCategory.toResolved() = ResolvedCategory(id, label, emoji, false, parentId)

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
            // Salary
            r.containsAny(
                "salary", "payroll", "wage", "basic pay", "net pay",
                "stipend", "allowance"
            ) -> TransactionCategory.SALARY.name

            // Business / till
            r.containsAny(
                "till", "paybill", "lipa na mpesa", "lipa mpesa",
                "business", "merchant", "pos receipt", "buy goods"
            ) -> TransactionCategory.BUSINESS_INCOME.name

            // Investment returns
            r.containsAny(
                "dividend", "interest earned", "returns", "profit",
                "unit trust", "mmf", "money market", "t-bill"
            ) -> TransactionCategory.INVESTMENT_RETURNS.name

            // Government refunds / NHIF
            r.containsAny(
                "nhif refund", "nssf refund", "kra refund", "tax refund",
                "government refund", "county refund"
            ) -> TransactionCategory.GOVERNMENT_REFUND.name

            // Loan disbursements
            r.containsAny(
                "fuliza", "m-shwari loan", "mshwari loan",
                "kcb m-pesa loan", "hustler fund"
            ) -> TransactionCategory.FULIZA_LOAN.name

            r.containsAny(
                "tala", "branch", "zenka", "okash", "okolea",
                "timiza loan", "haraka loan", "berry loan"
            ) -> TransactionCategory.MOBILE_LOAN.name

            // Generic received (person-to-person)
            else -> TransactionCategory.RECEIVED_MONEY.name
        }
    }

    // ── 3. Airtime & Data ─────────────────────────────────────────────────
    if (r == "safaricom airtime" || r.containsAny(
            "airtime", "data bundle", "safaricom data", "airtel data",
            "telkom data", "faiba data", "youtube go", "bonga points",
            "internet bundle"
        )
    ) return TransactionCategory.AIRTIME.name

    // ── 4. FUEL — checked BEFORE generic transport ────────────────────────
    if (r.containsAny(
            // Petrol stations
            "shell", "energies", "kenol", "oil libya", "rubis",
            "lake oil", "hashi energy", "galana", "vivo energy",
            "petro station", "gulf energy", "total",
            "petrol", "diesel", "fuel", "pump", "litre of fuel",
        )
    ) {
        return if (r.containsAny("diesel", "gasoil"))
            TransactionCategory.DIESEL.name
        else
            TransactionCategory.PETROL.name
    }

    // ── 5. CAR MAINTENANCE ────────────────────────────────────────────────
    if (r.containsAny(
            // Garages / service
            "garage", "mechanic", "auto service", "car service",
            "vehicle service", "service centre", "oil change",
            "engine oil", "spare parts", "spares", "auto parts",
            "car repair", "panel beating", "body work",
            // Car wash
            "car wash", "autowash", "auto wash", "hand wash",
            // Tyres
            "tyre", "tire", "wheel alignment", "wheel balance",
            "puncture", "rim",
            // Parking (own-car context, not ride-hail)
            "parking fee", "parking ticket", "nairobi parking",
            "county parking", "e-parking"
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

    // ── 6. WITHDRAWAL ─────────────────────────────────────────────────────
    if (r.containsAny(
            "withdraw", "agent", "atm", "pochi la biashara",
            "cash out", "cashout", "mpesa agent"
        )
    ) return TransactionCategory.WITHDRAW.name

    // ── 7. SAVINGS & BANKING ──────────────────────────────────────────────
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

    // ── 8. FULIZA REPAYMENT (outgoing) ────────────────────────────────────
    if (r.containsAny(
            "fuliza repayment", "fuliza payment", "loan repayment",
            "repay fuliza", "repay loan", "m-shwari repayment"
        )
    ) return TransactionCategory.FULIZA_REPAYMENT.name

    // ── 9. MOBILE LOANS (outgoing repayment via expense) ─────────────────
    if (type == "expense" && r.containsAny(
            "tala repayment", "branch repayment", "zenka repayment",
            "okash repayment", "okolea repayment", "haraka repayment"
        )
    ) return TransactionCategory.FULIZA_REPAYMENT.name

    // ── 10. GOVERNMENT & TAX ──────────────────────────────────────────────
    if (r.containsAny(
            "kra", "kenya revenue", "ecitizen", "e-citizen", "ntsa",
            "huduma", "nhif", "nssf", "nairobi city county", "county",
            "government", "passport", "driving licence", "birth certificate",
            "title deed", "land rates", "nairobi water sewerage",
            "nema", "kebs", "knec"
        )
    ) return TransactionCategory.GOVERNMENT.name

    // ── 11. INSURANCE ─────────────────────────────────────────────────────
    if (r.containsAny(
            "insurance", "jubilee", "britam", "madison", "cic insurance",
            "aaa insurance", "resolution insurance", "sanlam", "aar",
            "heritage insurance", "pioneer assurance", "icea lion",
            "liberty life", "old mutual", "ga insurance",
            "motor insurance", "comprehensive", "third party"
        )
    ) {
        // Vehicle insurance → under CAR_MAINTENANCE tree
        return if (r.containsAny("motor", "vehicle", "car", "third party", "comprehensive"))
            TransactionCategory.INSURANCE_CAR.name
        else
            TransactionCategory.INSURANCE.name
    }

    // ── 12. INVESTMENT ────────────────────────────────────────────────────
    if (r.containsAny(
            "nse", "nairobi securities", "cdsc", "cma",
            "cytonn", "genghis", "stocksmaster", "zimele", "amana",
            "britam asset", "old mutual invest", "cic asset",
            "equity investment", "nabo", "sanlam invest",
            "mmf", "money market", "unit trust", "t-bill", "treasury bill",
            "treasury bond", "fixed deposit"
        )
    ) return TransactionCategory.INVESTMENT.name

    // ── 13. UTILITIES ─────────────────────────────────────────────────────
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
            "net2phone", "dstv", "gotv", "azam", "showmax", "startimes"
        )
    ) return TransactionCategory.INTERNET.name

    if (r.containsAny(
            "gas", "afrigas", "k-gas", "total gas", "shell gas", "lake gas",
            "pro gas", "dolgas", "orion gas", "lpg", "cooking gas"
        )
    ) return TransactionCategory.COOKING_GAS.name

    // ── 14. EDUCATION ─────────────────────────────────────────────────────
    if (r.containsAny(
            "school", "college", "university", "tuition", "education",
            "academy", "institute", "polytechnic", "tvet", "helb",
            "knec", "kcse", "kcpe", "exam fee", "school fees",
            "kindergarten", "nursery", "playgroup",
            "uon", "ku", "mku", "kenyatta university", "strathmore",
            "usiu", "daystar", "catholic university", "pwani university",
            "kabarak", "zetech", "multimedia university"
        )
    ) return TransactionCategory.EDUCATION.name

    // ── 15. HEALTH ────────────────────────────────────────────────────────
    if (r.containsAny(
            "pharmacy", "chemist", "goodlife pharmacy", "haltons",
            "medisel", "sapphire pharmacy", "lifecare pharmacy",
            "dawa", "drug store"
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

    // ── 16. TRANSPORT (ride-hailing & public transit) ─────────────────────
    if (r.containsAny(
            // Ride-hailing
            "uber", "bolt", "little cab", "littlecab", "faras",
            "mondo ride", "wasili", "ndovu",
            // Nairobi expressway / toll
            "nairobi expressway", "westlands expressway", "mlolongo",
            // SGR / train
            "sgr", "madaraka express", "kenya railways",
            // Ferry
            "likoni ferry", "ferry",
            // Airport
            "jkia", "wilson airport", "airport transfer",
            // Matatu / PSV keywords
            "matatu", "psvs", "bus fare", "shuttle", "pss",
            "citi hoppa", "kbs", "2nk", "forward travellers",
            "easy coach", "modern coast", "dreamline",
            "mash", "guardian angel", "trans nzoia"
        )
    ) {
        return when {
            r.containsAny("uber", "bolt", "little cab", "littlecab", "faras", "wasili", "ndovu")
                -> TransactionCategory.RIDE_HAILING.name

            r.containsAny("sgr", "madaraka", "kenya railways")
                -> TransactionCategory.SGR.name

            r.containsAny(
                "parking fee",
                "parking ticket",
                "nairobi parking",
                "county parking",
                "e-parking"
            )
                -> TransactionCategory.PARKING.name

            else -> TransactionCategory.MATATU.name
        }
    }

    // ── 17. FOOD ──────────────────────────────────────────────────────────
    // Supermarkets → GROCERIES
    if (r.containsAny(
            "naivas", "quickmart", "carrefour", "chandarana", "tuskys",
            "cleanshelf", "eastmatt", "highrise", "food plus",
            "market", "supermarket", "grocery", "mini mart", "duka"
        )
    ) return TransactionCategory.GROCERIES.name

    // Delivery platforms → FOOD_DELIVERY
    if (r.containsAny(
            "glovo", "jumia food", "uber eats", "food delivery",
            "chowdeck", "sendy food"
        )
    ) return TransactionCategory.FOOD_DELIVERY.name

    // Fast food chains → FAST_FOOD
    if (r.containsAny(
            "kfc", "pizza inn", "pizza hut", "chicken inn",
            "galitos", "debonairs", "burger king", "subway",
            "steers", "fishaways", "hardee's", "bfc", "smokie",
            "nyama choma"
        )
    ) return TransactionCategory.FAST_FOOD.name

    // Sit-down restaurants & cafes → RESTAURANTS
    if (r.containsAny(
            "java", "artcaffe", "dormans", "java house", "k'osewe",
            "carnivore", "restaurant", "cafe", "coffee", "bakery",
            "hotel dining", "food court", "canteen", "mess", "eatery",
            "mama's", "brew bistro", "roast house"
        )
    ) return TransactionCategory.RESTAURANTS.name

    // ── 18. ENTERTAINMENT ─────────────────────────────────────────────────
    if (r.containsAny(
            "netflix", "prime video", "spotify", "youtube premium",
            "apple music", "deezer", "boomplay", "tidal",
            "cinema", "imax", "century cinemas", "silverbird",
            "club", "lounge", "bar", "pub", "nightclub",
            "ticket", "concert", "event", "festival",
            "gaming", "playstation", "xbox", "steam", "ea games",
            "sport", "sport pesa", "betika", "odibets", "mcheza"
        )
    ) return TransactionCategory.ENTERTAINMENT.name

    // ── 19. SHOPPING ─────────────────────────────────────────────────────
    // Online → ONLINE_SHOPPING
    if (r.containsAny(
            "jumia", "kilimall", "jiji", "masoko", "amazon",
            "aliexpress", "shein", "temu"
        )
    ) return TransactionCategory.ONLINE_SHOPPING.name

    // Clothing → CLOTHING
    if (r.containsAny(
            "clothing", "fashion", "wear", "boutique", "clothes",
            "shoes", "footwear", "apparel", "mtumba", "second hand"
        )
    ) return TransactionCategory.CLOTHING.name

    // Electronics → ELECTRONICS
    if (r.containsAny(
            "electronics", "phone", "laptop", "computer", "gadget",
            "samsung store", "apple store", "techno", "infinix",
            "iphone", "tablet"
        )
    ) return TransactionCategory.ELECTRONICS.name

    // Generic shopping (malls, shops)
    if (r.containsAny(
            "shop", "store", "mall", "centre", "plaza",
            "two rivers", "garden city", "westgate", "the junction",
            "village market", "sarit centre", "galleria", "hub karen"
        )
    ) return TransactionCategory.SHOPPING.name

    // ── 20. SEND MONEY (person-to-person outgoing) ────────────────────────
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