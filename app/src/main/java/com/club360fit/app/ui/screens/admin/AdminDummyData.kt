package com.club360fit.app.ui.screens.admin

data class ClientSummary(
    val id: String,
    val name: String,
    val goal: String,
    val lastActive: String
)

data class MealPlan(
    val title: String,
    val description: String,
    val mealsPerDay: List<String>
)

data class WorkoutPlan(
    val title: String,
    val description: String,
    val sessionsPerWeek: List<String>
)

data class ClientDetails(
    val id: String,
    val name: String,
    val age: Int?,
    val heightCm: Int?,
    val weightKg: Int?,
    val phone: String?,
    val email: String,
    val overallGoal: String,
    val medicalConditions: String?,
    val foodRestrictions: String?,
    val paleoMealPlan: MealPlan,
    val workoutPlan: WorkoutPlan
)

// Dummy paleo-style data
val demoClients = listOf(
    ClientDetails(
        id = "1",
        name = "John Hunter",
        age = 34,
        heightCm = 180,
        weightKg = 82,
        phone = "555-123-4567",
        email = "john.hunter@example.com",
        overallGoal = "Lean out and improve energy",
        medicalConditions = "Mild gluten sensitivity",
        foodRestrictions = "Avoid dairy, gluten",
        paleoMealPlan = MealPlan(
            title = "Paleo Fat-Loss Week 1",
            description = "Whole foods, lean proteins, veggies, and healthy fats.",
            mealsPerDay = listOf(
                "Breakfast: Scrambled eggs in olive oil, spinach, avocado, black coffee.",
                "Lunch: Grilled chicken breast, mixed greens, olive oil & lemon, handful of almonds.",
                "Snack: Apple + tablespoon almond butter.",
                "Dinner: Baked salmon, roasted sweet potato, steamed broccoli."
            )
        ),
        workoutPlan = WorkoutPlan(
            title = "3-Day Strength + Conditioning",
            description = "Full-body strength 3x/week with light conditioning.",
            sessionsPerWeek = listOf(
                "Day 1: Full-body strength (squats, push-ups, rows) + 10 min brisk walk.",
                "Day 3: Deadlifts (light), overhead press, planks + 5 x 30s easy bike.",
                "Day 5: Kettlebell swings, lunges, farmer carries + 15 min walk."
            )
        )
    ),
    ClientDetails(
        id = "2",
        name = "Sarah Wright",
        age = 29,
        heightCm = 165,
        weightKg = 63,
        phone = "555-987-6543",
        email = "sarah.wright@example.com",
        overallGoal = "Build strength and tone",
        medicalConditions = null,
        foodRestrictions = "No peanuts",
        paleoMealPlan = MealPlan(
            title = "Paleo Performance Week 1",
            description = "Higher-carb paleo for training days.",
            mealsPerDay = listOf(
                "Breakfast: Omelet with mushrooms, peppers, onions, berries on the side.",
                "Lunch: Grass-fed beef burger (no bun), avocado, baked sweet potato fries.",
                "Snack: Banana + handful cashews.",
                "Dinner: Roast chicken thighs, quinoa-style cauliflower rice, asparagus."
            )
        ),
        workoutPlan = WorkoutPlan(
            title = "4-Day Split",
            description = "Upper/lower strength split with one conditioning day.",
            sessionsPerWeek = listOf(
                "Day 1: Upper body push/pull (bench, rows, overhead press).",
                "Day 2: Lower body (squats, hip thrusts, core).",
                "Day 4: Upper body accessories + sled pushes or prowler.",
                "Day 5: 20–30 min zone-2 run or bike."
            )
        )
    )
)

val demoClientSummaries: List<ClientSummary> =
    demoClients.map {
        ClientSummary(
            id = it.id,
            name = it.name,
            goal = it.overallGoal,
            lastActive = "Today"
        )
    }

fun findClientDetails(id: String): ClientDetails? =
    demoClients.firstOrNull { it.id == id }
