package com.xabier.carcareo.domain

import com.xabier.carcareo.data.entity.VehicleCategory
import com.xabier.carcareo.domain.template.MaintenanceTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintenanceTemplatesTest {

    @Test
    fun `template sizes match the spec tables`() {
        assertEquals(10, MaintenanceTemplates.forCategory(VehicleCategory.MOTO_TERMICA).size)
        assertEquals(14, MaintenanceTemplates.forCategory(VehicleCategory.COCHE_TERMICO).size)
        assertEquals(12, MaintenanceTemplates.forCategory(VehicleCategory.COCHE_ELECTRICO).size)
    }

    @Test
    fun `every template task has at least one interval`() {
        VehicleCategory.entries.forEach { category ->
            MaintenanceTemplates.forCategory(category).forEach { task ->
                assertTrue(
                    "${category}: ${task.nameEn}",
                    MaintenanceTaskRules.intervalsValid(task.intervalKm, task.intervalMonths),
                )
            }
        }
    }

    @Test
    fun `electric car template has no engine oil or fuel filter`() {
        val names = MaintenanceTemplates.forCategory(VehicleCategory.COCHE_ELECTRICO)
            .map { it.nameEn.lowercase() }
        assertTrue(names.none { it.contains("engine oil") })
        assertTrue(names.none { it.contains("fuel filter") })
        assertTrue(names.none { it.contains("spark plug") })
    }

    @Test
    fun `instantiate assigns vehicle id and sequential sort order`() {
        val tasks = MaintenanceTemplates.instantiate(
            category = VehicleCategory.MOTO_TERMICA,
            vehicleId = 42,
            spanish = false,
        )
        assertEquals(10, tasks.size)
        tasks.forEachIndexed { index, task ->
            assertEquals(42L, task.vehicleId)
            assertEquals(index, task.sortOrder)
        }
        assertEquals("Engine oil and filter", tasks.first().name)
    }

    @Test
    fun `instantiate picks the Spanish name when spanish is true`() {
        val tasks = MaintenanceTemplates.instantiate(
            category = VehicleCategory.MOTO_TERMICA,
            vehicleId = 1,
            spanish = true,
        )
        assertEquals("Aceite motor y filtro", tasks.first().name)
    }
}
