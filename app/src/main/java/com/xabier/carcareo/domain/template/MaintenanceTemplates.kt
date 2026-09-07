package com.xabier.carcareo.domain.template

import com.xabier.carcareo.data.entity.MaintenanceTask
import com.xabier.carcareo.data.entity.VehicleCategory

/**
 * A single line of a built-in plan template (spec section 6). Bilingual by
 * design: the name is carried in both languages and resolved once, at the moment
 * the template is applied. After that the task name is user data and is never
 * re-translated (spec 2.1).
 *
 * These are generic starting points, NOT manufacturer truth — the UI shows a
 * disclaimer when applying them.
 */
data class TemplateTask(
    val nameEs: String,
    val nameEn: String,
    val intervalKm: Int?,
    val intervalMonths: Int?,
) {
    fun localizedName(spanish: Boolean): String = if (spanish) nameEs else nameEn

    fun toEntity(vehicleId: Long, spanish: Boolean, sortOrder: Int): MaintenanceTask =
        MaintenanceTask(
            vehicleId = vehicleId,
            name = localizedName(spanish),
            intervalKm = intervalKm,
            intervalMonths = intervalMonths,
            sortOrder = sortOrder,
        )
}

object MaintenanceTemplates {

    fun forCategory(category: VehicleCategory): List<TemplateTask> = when (category) {
        VehicleCategory.MOTO_TERMICA -> motoTermica
        VehicleCategory.COCHE_TERMICO -> cocheTermico
        VehicleCategory.COCHE_ELECTRICO -> cocheElectrico
    }

    /** Instantiate the whole template as persistable tasks for [vehicleId]. */
    fun instantiate(
        category: VehicleCategory,
        vehicleId: Long,
        spanish: Boolean,
    ): List<MaintenanceTask> =
        forCategory(category).mapIndexed { index, t -> t.toEntity(vehicleId, spanish, index) }

    // --- Section 6.1 — Petrol motorbike ---
    private val motoTermica = listOf(
        TemplateTask("Aceite motor y filtro", "Engine oil and filter", 8_000, 12),
        TemplateTask("Filtro de aire", "Air filter", 12_000, 24),
        TemplateTask("Bujías", "Spark plugs", 16_000, null),
        TemplateTask("Líquido de frenos", "Brake fluid", null, 24),
        TemplateTask("Líquido refrigerante", "Coolant", 30_000, 24),
        TemplateTask("Limpieza y tensado de cadena", "Chain clean and tension", 1_000, null),
        TemplateTask("Sustitución de kit de transmisión", "Drive chain kit replacement", 28_000, null),
        TemplateTask("Inspección de pastillas de freno", "Brake pad inspection", 6_000, 12),
        TemplateTask("Neumáticos", "Tyres", 15_000, 60),
        TemplateTask("Revisión general en taller", "General workshop service", 12_000, 12),
    )

    // --- Section 6.2 — Petrol car ---
    private val cocheTermico = listOf(
        TemplateTask("Aceite motor y filtro", "Engine oil and filter", 15_000, 12),
        TemplateTask("Filtro de aire", "Air filter", 30_000, 24),
        TemplateTask("Filtro de habitáculo", "Cabin filter", 15_000, 12),
        TemplateTask("Filtro de combustible", "Fuel filter", 40_000, 48),
        TemplateTask("Bujías (gasolina)", "Spark plugs (petrol)", 60_000, null),
        TemplateTask("Líquido de frenos", "Brake fluid", null, 24),
        TemplateTask("Líquido refrigerante", "Coolant", 60_000, 48),
        TemplateTask("Correa de distribución", "Timing belt", 120_000, 84),
        TemplateTask("Pastillas de freno", "Brake pads", 40_000, null),
        TemplateTask("Discos de freno", "Brake discs", 80_000, null),
        TemplateTask("Neumáticos", "Tyres", 40_000, 72),
        TemplateTask("Batería 12 V", "12 V battery", null, 60),
        TemplateTask("Escobillas limpiaparabrisas", "Wiper blades", null, 12),
        TemplateTask("ITV", "Roadworthiness test", null, 12),
    )

    // --- Section 6.3 — Electric car ---
    private val cocheElectrico = listOf(
        TemplateTask("Filtro de habitáculo", "Cabin filter", 15_000, 12),
        TemplateTask("Líquido de frenos", "Brake fluid", null, 24),
        TemplateTask(
            "Refrigerante de batería / circuito térmico",
            "Battery / thermal-circuit coolant",
            100_000, 60,
        ),
        TemplateTask("Aceite de reductora", "Reduction-gear oil", 100_000, null),
        TemplateTask("Inspección y engrase de frenos", "Brake inspection and lubrication", 20_000, 12),
        TemplateTask("Pastillas de freno", "Brake pads", 60_000, null),
        TemplateTask("Neumáticos", "Tyres", 30_000, 60),
        TemplateTask("Rotación de neumáticos", "Tyre rotation", 10_000, null),
        TemplateTask("Batería 12 V / auxiliar", "12 V / auxiliary battery", null, 48),
        TemplateTask("Revisión de cable y equipo de carga", "Charging cable and equipment check", null, 12),
        TemplateTask("Escobillas limpiaparabrisas", "Wiper blades", null, 12),
        TemplateTask("ITV", "Roadworthiness test", null, 12),
    )
}
