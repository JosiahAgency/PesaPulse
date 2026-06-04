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
    WITHDRAWAL("Withdrawal", "🏧"),
    PAYBILL("Pay Bill", "🧾"),
    BUY_GOODS("Buy Goods", "🏪"),
    FULIZA("Fuliza M-PESA", "🔄"),
    MSHWARI("M-Shwari", "🏦"),
    KCB_MPESA("KCB M-PESA", "🏦"),
    AGENT_DEPOSIT("Agent Deposit", "💵"),

    // ── INCOMING: TOP-LEVEL ───────────────────────────────────────────────

    SALARY("Salary & Payroll", "💼"),
    BUSINESS_INCOME("Business Income", "🏪"),
    RECEIVED_MONEY("Received Money", "📲"),
    LOAN_DISBURSEMENT("Loan Received", "🤝"),
    HUSTLER_FUND("Hustler Fund", "🇰🇪"),
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
    HOME_SUPPLIES("Home & Hardware", "🏠", parentId = "SHOPPING"),   // ★ new

    // ── SAVINGS SUBCATEGORIES ─────────────────────────────────────────────

    BANK_TRANSFER("Bank Transfer", "🏦", parentId = "SAVINGS"),      // ★ new
    SACCO("SACCO", "🤝", parentId = "SAVINGS"),                      // ★ new

    // ── FALLBACK / SYSTEM ─────────────────────────────────────────────────

    OTHER("Other", "📦"),
    BALANCE_INQUIRY("Balance Inquiry", "🔍");

    /** True for categories that represent internal wallet movements, not real spending. */
    val isTransfer: Boolean
        get() = this in setOf(
            MSHWARI, KCB_MPESA, BANK_TRANSFER, SACCO, SAVINGS, AGENT_DEPOSIT
        )
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

// ─────────────────────────────────────────────────────────────────────────────
// guessCategory
//
// Resolution order (first match wins):
//   0. Exact user correction           (highest priority — explicit human feedback)
//   1. Custom category keywords
//   2. M-PESA system types             (Fuliza, M-Shwari, KCB M-PESA, Hustler Fund, Paybill,
//                                       Buy Goods, Agent Deposit, Withdrawal, Balance Inquiry)
//   3. Income-specific rules
//   4. Airtime & Data
//   5. Food  (groceries → delivery → fast food → restaurants)
//   6. Parking
//   7. Fuel
//   8. Car Maintenance  (wash → tyres → service)
//   9. Savings & Banking  (SACCO → bank → M-Shwari savings)
//  10. Fuliza / Loan Repayment
//  11. Government & Tax
//  12. Insurance
//  13. Utilities  (electricity → water → internet → gas)
//  14. Education
//  15. Health  (pharmacy → hospital → gym)
//  16. Tolls
//  17. Transport  (ride-hailing → train → public)
//  18. Entertainment
//  19. Shopping  (online → clothing → electronics → home → generic)
//  20. Send Money  (person-name heuristic)
//  21. OTHER
// ─────────────────────────────────────────────────────────────────────────────

fun guessCategory(
    recipient: String,
    type: String,
    customCategories: List<CustomCategory> = emptyList(),
    userCorrections: Map<String, String> = emptyMap()
): String {
    val r = recipient.lowercase().trim()

    // ── 0. Explicit user correction ───────────────────────────────────────
    userCorrections[r]?.let { return it }

    // ── 1. Custom category keywords ───────────────────────────────────────
    for (custom in customCategories) {
        val keywords = custom.keywords
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        if (keywords.any { kw -> r.contains(kw) }) return custom.id
    }

    // ── 2. M-PESA system / structural types ───────────────────────────────
    // These recipients are set by buildTransaction (not user names), so they
    // are reliable anchors that must be checked before any keyword guessing.

    when {
        r == "balance inquiry" ->
            return TransactionCategory.BALANCE_INQUIRY.name

        r.startsWith("fuliza m-pesa") || r == "fuliza m-pesa repayment" ->
            return TransactionCategory.FULIZA.name

        r == "m-shwari account" || r == "m-shwari withdrawal" || r == "m-shwari loan" ->
            return TransactionCategory.MSHWARI.name

        r == "kcb m-pesa account" || r == "kcb m-pesa" ->
            return TransactionCategory.KCB_MPESA.name

        r == "hustler fund loan" ->
            return TransactionCategory.HUSTLER_FUND.name

        r.startsWith("deposited via") || r == "agent deposit" ->
            return TransactionCategory.AGENT_DEPOSIT.name

        r.startsWith("reversal") ->
            return TransactionCategory.RECEIVED_MONEY.name

        r.startsWith("refund:") ->
            return TransactionCategory.RECEIVED_MONEY.name

        // ★ Paybill recipients carry "(Acc: XXXX)" suffix from the parser
        r.contains("(acc:") ->
            return TransactionCategory.PAYBILL.name
    }

    // ── 3. Income types ───────────────────────────────────────────────────
    if (type == "income") {
        return when {
            r.containsAny(
                "salary", "payroll", "wage", "basic pay", "net pay",
                "stipend", "allowance", "remuner"
            ) -> TransactionCategory.SALARY.name

            r.containsAny(
                "dividend", "interest earned", "returns", "profit",
                "unit trust", "mmf", "money market", "t-bill", "ziidi",
                "cytonn", "britam asset", "sanlam investments"
            ) -> TransactionCategory.INVESTMENT_RETURNS.name

            r.containsAny(
                "nhif refund", "nssf refund", "kra refund", "tax refund",
                "government refund", "county refund", "nhif"
            ) -> TransactionCategory.GOVERNMENT_REFUND.name

            r.containsAny(
                "hustler fund", "hustler loan"
            ) -> TransactionCategory.HUSTLER_FUND.name

            r.containsAny(
                "fuliza", "m-shwari loan", "mshwari loan",
                "kcb m-pesa loan", "kcb mpesa loan",
                "tala", "branch loan", "zenka", "okash", "okolea",
                "timiza loan", "haraka loan", "berry loan", "zash loan",
                "ipesa", "kashway", "pesazone", "stawika", "eazzy loan",
                "mcoopcash", "mco-opcash", "hf whizz", "pesapap loan"
            ) -> TransactionCategory.LOAN_DISBURSEMENT.name

            r.containsAny(
                "till", "lipa na mpesa", "lipa mpesa", "lipa na m-pesa",
                "merchant", "pos receipt", "buy goods"
            ) -> TransactionCategory.BUSINESS_INCOME.name

            r.containsAny(
                "business", "enterprise", "limited", "ltd", "company",
                "supplies", "services", "agencies", "shop", "store"
            ) -> TransactionCategory.BUSINESS_INCOME.name

            else -> TransactionCategory.RECEIVED_MONEY.name
        }
    }

    // ── 4. Airtime & Data ─────────────────────────────────────────────────
    if (r == "safaricom airtime" ||
        r.containsAny(
            "airtime", "data bundle", "internet bundle", "bonga points",
            "safaricom airtime", "airtel airtime", "telkom airtime",
            "faiba airtime"
        )
    ) return TransactionCategory.AIRTIME.name

    // Safaricom/Airtel/Telkom as generic paybills without more context → Airtime
    if (r.containsAny("safaricom", "airtel", "telkom ") &&
        !r.containsAny("home", "fiber", "fibre", "broadband")
    ) return TransactionCategory.AIRTIME.name

    // ── 5. Food ───────────────────────────────────────────────────────────

    // Groceries / supermarkets
    if (r.containsAny(
            "naivas", "quickmart", "carrefour", "chandarana", "tuskys",
            "cleanshelf", "eastmatt", "uchumi", "nakumatt", "supermarket",
            "mini mart", "grocery", "groceries", "duka", "kinyozi na duka",
            "food market", "fresh market", "vegetable", "butchery",
            "green grocer", "greengrocer", "provisions", "general store",
            "mini market", "convenient store", "convenience store"
        )
    ) return TransactionCategory.GROCERIES.name

    // Food delivery platforms
    if (r.containsAny(
            "glovo", "jumia food", "uber eats", "ubereats",
            "food delivery", "chowdeck", "sendy food", "yummy",
            "instashop", "order food", "door dash"
        )
    ) return TransactionCategory.FOOD_DELIVERY.name

    // Fast food chains (Kenyan + international)
    if (r.containsAny(
            "kfc", "pizza inn", "pizza hut", "chicken inn", "galitos",
            "debonairs", "burger king", "subway", "steers", "fishaways",
            "smokie", "nyama choma", "nyama", "smokies",
            "hardees", "dominos", "domino's", "mcdonald", "mcdonalds",
            "big square", "the burger", "barcelos", "nando's", "nandos",
            "fry's", "creamy inn", "cold stone", "baskin", "haagen",
            "pop inn", "kentucky", "hot box", "food court kiosk"
        )
    ) return TransactionCategory.FAST_FOOD.name

    // Sit-down restaurants / cafes
    if (r.containsAny(
            "java", "artcaffe", "art caffe", "dormans", "java house",
            "k'osewe", "carnivore", "restaurant", "cafe", "coffee house",
            "bakery", "hotel dining", "food court", "canteen", "mess",
            "eatery", "mama's", "brew bistro", "roast house",
            "trattoria", "tamambo", "harvest", "kikwetu", "the grove",
            "olepolos", "diamond plaza", "kaldis", "cup of joe",
            "cafeteria", "bistro", "grille", "diner", "kitchen"
        )
    ) return TransactionCategory.RESTAURANTS.name

    // ── 6. Parking ────────────────────────────────────────────────────────
    if (r.containsAny(
            "parking", "e-parking", "county parking",
            "nairobi parking", "parking fee", "parking ticket",
            "evopay", "park n ride", "smart park"
        )
    ) return TransactionCategory.PARKING.name

    // ── 7. Fuel ───────────────────────────────────────────────────────────
    if (r.containsAny(
            "shell", "totalenergies", "total energies", "kenol", "oil libya",
            "rubis", "lake oil", "hashi energy", "galana", "vivo energy",
            "petro station", "gulf energy", "total", "oilibya",
            "petrol", "diesel", "fuel", "gas station", "filling station",
            "petroleum", "mpesa fuel", "fuel card"
        )
    ) return TransactionCategory.FUEL.name

    // ── 8. Car Maintenance ────────────────────────────────────────────────
    if (r.containsAny(
            "garage", "mechanic", "auto service", "car service",
            "vehicle service", "service centre", "oil change",
            "engine oil", "spare parts", "spares", "auto parts",
            "car repair", "panel beating", "body work", "auto electrician",
            "car wash", "autowash", "auto wash", "hand wash", "jetwash",
            "tyre", "tire", "wheel alignment", "wheel balance",
            "puncture", "rim", "radiator", "brake", "clutch",
            "suspension", "exhaust", "battery", "shock absorber",
            "windscreen", "car glass", "auto glass", "nrb motors",
            "toyota kenya", "general motors", "cfao motors"
        )
    ) {
        return when {
            r.containsAny("wash") -> TransactionCategory.CAR_WASH.name
            r.containsAny("tyre", "tire", "wheel", "puncture", "rim")
                -> TransactionCategory.TYRES.name

            else -> TransactionCategory.CAR_SERVICE.name
        }
    }

    // ── 9. Savings & Banking ──────────────────────────────────────────────

    // SACCOs — before generic bank check
    if (r.containsAny(
            "sacco", "mwalimu sacco", "unaitas", "stima sacco",
            "harambee sacco", "tower sacco", "metropolitan sacco",
            "kenya police sacco", "afya sacco", "airport sacco",
            "kuscco", "co-op sacco", "equity sacco", "chai sacco",
            "bandari sacco", "telecom sacco", "nation sacco",
            "safaricom sacco", "kenya post sacco", "imarika sacco"
        )
    ) return TransactionCategory.SACCO.name

    // Banks & mobile banking products
    if (r.containsAny(
            "equity bank", "equity ", "kcb", "co-op bank", "cooperative bank",
            "absa", "stanbic", "stanchart", "standard chartered", "ncba",
            "dtb", "diamond trust", "family bank", "prime bank", "sidian",
            "credit bank", "gulf african", "ib africa", "housing finance",
            "hfc", "victoria commercial", "consolidated bank",
            "ecobank", "guaranty trust", "gt bank",
            "m-shwari", "mshwari", "kcb m-pesa", "timiza",
            "kwara", "faulu", "letshego",
            "bank transfer", "bank deposit", "transfer to bank",
            "equity mobile", "eazzy banking", "kcb app"
        )
    ) return TransactionCategory.BANK_TRANSFER.name

    // ── 10. Fuliza / loan repayments ──────────────────────────────────────
    if (r.containsAny(
            "fuliza repayment", "fuliza payment", "repay fuliza",
            "loan repayment", "repay loan", "m-shwari repayment",
            "tala repayment", "branch repayment", "zenka repayment",
            "okash repayment", "okolea repayment", "haraka repayment",
            "ipesa repayment", "kashway repayment", "stawika repayment"
        )
    ) return TransactionCategory.FULIZA.name

    // ── 11. Government & Tax ──────────────────────────────────────────────
    if (r.containsAny(
            "kra", "kenya revenue", "ecitizen", "e-citizen", "ntsa",
            "huduma", "nhif", "nssf", "sha ", "nairobi city county",
            "county ", "government", "passport", "driving licence",
            "birth certificate", "title deed", "land rates",
            "nairobi water sewerage", "nema", "kebs", "knec",
            "helb", "higher education loans", "immigration",
            "housing levy", "affordable housing", "nca ", "epra",
            "national lands", "lands registry", "business permit"
        )
    ) return TransactionCategory.GOVERNMENT.name

    // ── 12. Insurance ─────────────────────────────────────────────────────
    if (r.containsAny(
            "insurance", "jubilee", "britam", "aar health", "madison",
            "kenindia", "sanlam", "aig", "resolution insurance",
            "first assurance", "old mutual", "cic insurance",
            "heritage insurance", "trident insurance", "cannon",
            "ga insurance", "pacis", "real insurance", "pioneer",
            "medical cover", "health cover", "life assurance",
            "motor insurance", "comprehensive cover", "insurance premium"
        )
    ) return TransactionCategory.INSURANCE.name

    // ── 13. Investment ────────────────────────────────────────────────────
    if (r.containsAny(
            "nse ", "nairobi securities", "cdsc", "stockbroker",
            "unit trust", "money market fund", "mmf", "t-bill",
            "treasury bill", "treasury bond", "cytonn", "britam asset",
            "sanlam investments", "old mutual investments",
            "apollo investments", "genghis", "kingdom securities",
            "ziidi", "m-akiba", "safaricom shares"
        )
    ) return TransactionCategory.INVESTMENT.name

    // ── 14. Utilities ─────────────────────────────────────────────────────

    if (r.containsAny(
            "kplc", "kenya power", "umeme", "stima", "prepaid token",
            "power token", "electricity token", "token purchase"
        )
    ) return TransactionCategory.ELECTRICITY.name

    if (r.containsAny(
            "nairobi water", "nwsc", "mombasa water", "kikuyu water",
            "mavoko water", "eldoret water", "kisumu water",
            "nakuru water", "nyeri water", "water bill", "sewerage"
        )
    ) return TransactionCategory.WATER.name

    if (r.containsAny(
            "zuku", "faiba home", "safaricom home", "airtel home",
            "telkom home", "poa internet", "liquid home", "jti broadband",
            "dstv", "gotv", "azam tv", "showmax", "startimes",
            "surfline", "seacom home", "jamii telecom", "icolo",
            "internet ", "wifi ", "broadband", "fiber ", "fibre "
        )
    ) return TransactionCategory.INTERNET.name

    if (r.containsAny(
            "afrigas", "k-gas", "total gas", "shell gas", "lake gas",
            "pro gas", "dolgas", "orion gas", "lpg", "cooking gas",
            "gas cylinder", "gas refill", "gas delivery", " gas"
        )
    ) return TransactionCategory.COOKING_GAS.name

    // ── 15. Education ─────────────────────────────────────────────────────
    if (r.containsAny(
            "school", "college", "university", "tuition", "education",
            "academy", "institute", "polytechnic", "tvet", "helb",
            "kcse", "kcpe", "exam fee", "school fees", "school fee",
            "kindergarten", "nursery", "playgroup", "ecd ", "pre-school",
            "uon", "ku", "mku", "kenyatta university", "strathmore",
            "usiu", "daystar", "catholic university", "pwani university",
            "kabarak", "zetech", "multimedia university", "umma",
            "mount kenya university", "mkumkum", "rongo university",
            "egerton university", "moi university", "maseno university",
            "training centre", "training center", "short course",
            "online course", "udemy", "coursera", "masterclass"
        )
    ) return TransactionCategory.EDUCATION.name

    // ── 16. Health ────────────────────────────────────────────────────────

    if (r.containsAny(
            "pharmacy", "chemist", "goodlife pharmacy", "haltons",
            "medisel", "sapphire pharmacy", "lifecare pharmacy",
            "dawa", "drug store", "drugstore", "mediplus",
            "ozone pharmacy", "peoples pharmacy", "healthplus",
            "wellcare pharmacy", "remedy pharmacy", "pharma"
        )
    ) return TransactionCategory.PHARMACY.name

    if (r.containsAny(
            "hospital", "clinic", "health centre", "medical", "doctor",
            "dispensary", "nursing home", "dentist", "optician",
            "laboratory", "radiology", "physiotherapy", "optometrist",
            "ophthalmologist", "specialist", "health facility",
            "knh", "nairobi hospital", "aga khan", "mater", "mp shah",
            "karen hospital", "gertrudes", "coptic", "premier hospital",
            "coast general", "kisumu county referral", "eldoret referral",
            "kenyatta national", "lancet", "metroplex health",
            "avenue hospital", "meridian health", "inclusive health",
            "highland hospital", "rift valley hospital"
        )
    ) return TransactionCategory.HOSPITAL_VISIT.name

    if (r.containsAny(
            "gym", "fitness", "yoga", "swimming", "crossfit",
            "planet fitness", "the gym", "snap fitness",
            "lift gym", "powerhouse gym", "lifestyle fitness",
            "elite fitness", "my gym", "anytime fitness", "f45"
        )
    ) return TransactionCategory.GYM.name

    // ── 17. Tolls ─────────────────────────────────────────────────────────
    if (r.containsAny(
            "expressway", "toll", "nairobi expressway",
            "mombasa road toll", "thika road", "toll gate"
        )
    ) return TransactionCategory.TOLLS.name

    // ── 18. Transport ─────────────────────────────────────────────────────
    if (r.containsAny(
            "uber", "bolt", "little cab", "littlecab", "faras",
            "wasili", "ndovu", "rova", "hailabus",
            "matatu", "bus fare", "shuttle", "citi hoppa",
            "kbs", "2nk", "easy coach", "modern coast",
            "tahmeed coach", "dreamline", "riverside shuttle",
            "likoni ferry", "ferry", "jkia", "airport transfer",
            "airport express", "sgr", "train", "metro",
            "boda boda", "boda", "tuktuk", "tuk tuk", "piki piki",
            "transport", "cab fare", "taxi"
        )
    ) {
        return when {
            r.containsAny(
                "uber", "bolt", "little cab", "littlecab", "faras",
                "wasili", "ndovu", "rova", "hailabus", "cab fare", "taxi"
            ) -> TransactionCategory.RIDE_HAILING.name

            r.containsAny("sgr", "train", "metro")
                -> TransactionCategory.TRAIN.name

            else -> TransactionCategory.PUBLIC_TRANSPORT.name
        }
    }

    // ── 19. Entertainment ─────────────────────────────────────────────────
    if (r.containsAny(
            "netflix", "prime video", "amazon prime", "spotify",
            "youtube premium", "apple music", "deezer", "boomplay", "tidal",
            "showmax", "disney+", "disney plus", "apple tv",
            "canal plus", "canal+",
            "cinema", "imax", "century cinemas", "silverbird", "cineplex",
            "club", "lounge", "bar", "pub", "nightclub", "rooftop bar",
            "ticket", "concert", "event", "festival", "show",
            "gaming", "playstation", "xbox", "steam", "ea play",
            "sportpesa", "betika", "odibets", "mcheza", "betin",
            "premiumbets", "bangbet", "1xbet",
            "spa", "massage", "salon", "beauty", "nail art",
            "barber", "barbershop", "hair salon"
        )
    ) return TransactionCategory.ENTERTAINMENT.name

    // ── 20. Shopping ──────────────────────────────────────────────────────

    if (r.containsAny(
            "jumia", "kilimall", "jiji", "masoko", "amazon",
            "aliexpress", "shein", "temu", "ebay", "wish",
            "online shop", "e-commerce", "shop online",
            "noon.com", "noon ", "pigiame"
        )
    ) return TransactionCategory.ONLINE_SHOPPING.name

    if (r.containsAny(
            "clothing", "fashion", "wear", "boutique", "clothes",
            "shoes", "footwear", "apparel", "mtumba", "second hand",
            "secondhand", "thrift", "vaa ", "attire", "garment",
            "kitenge", "ankara", "uniform", "sportswear"
        )
    ) return TransactionCategory.CLOTHING.name

    if (r.containsAny(
            "electronics", "gadget", "samsung store", "apple store",
            "phone shop", "laptop shop", "computer", "techno",
            "infinix", "iphone", "tablet", "accessories",
            "phones", "computers", "printers", "cameras",
            "speaker", "headphones", "earphones", "smartwatch",
            "itel", "nokia", "huawei", "xiaomi", "oppo", "vivo"
        )
    ) return TransactionCategory.ELECTRONICS.name

    if (r.containsAny(
            "hardware", "building materials", "cement", "iron sheets",
            "timber", "paint", "plumbing", "electrical supply",
            "furniture", "decor", "curtains", "bedding",
            "mattress", "kitchen ware", "kitchenware", "cookware",
            "crockery", "cleaning supplies", "detergent",
            "home goods", "household", "ikea"
        )
    ) return TransactionCategory.HOME_SUPPLIES.name

    if (r.containsAny(
            "shop", "store", "mall", "centre", "center", "plaza",
            "two rivers", "garden city", "westgate", "the junction",
            "village market", "sarit centre", "galleria",
            "hub karen", "prestige plaza", "lavington mall",
            "nextgen mall", "great rift valley lodge", "yaya centre",
            "abc place", "karen shopping", "crossroads"
        )
    ) return TransactionCategory.SHOPPING.name

    // ── 21. Send Money ────────────────────────────────────────────────────
    if (type == "expense" && looksLikePersonName(r) && !r.containsAny(
            "shop", "store", "enterprise", "limited", "ltd",
            "agency", "services", "supplies", "hardware",
            "paybill", "till", "merchant", "company"
        )
    ) return TransactionCategory.SEND_MONEY.name

    return TransactionCategory.OTHER.name
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun String.containsAny(vararg keywords: String): Boolean =
    keywords.any { this.contains(it, ignoreCase = true) }

private fun looksLikePersonName(r: String): Boolean {
    val noDigits = r.none { it.isDigit() }
    val words = r.trim().split("\\s+".toRegex())
    return noDigits && words.size in 1..4 && words.all { it.length in 2..20 }
}