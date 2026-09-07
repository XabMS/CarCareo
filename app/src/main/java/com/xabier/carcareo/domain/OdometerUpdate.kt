package com.xabier.carcareo.domain

/**
 * Classifies a manual odometer correction — spec section 4.1.
 *
 *  - [OK]              new value is >= the previous confirmed value
 *  - [MINOR_DECREASE]  new value is lower, but by 10% or less: warn, allow
 *  - [MAJOR_DECREASE]  new value is lower by more than 10%: require explicit
 *                      confirmation before saving
 */
enum class KmChangeSeverity { OK, MINOR_DECREASE, MAJOR_DECREASE }

object OdometerUpdate {

    private const val MAJOR_DECREASE_FRACTION = 0.10

    fun classify(previousConfirmedKm: Int, newKm: Int): KmChangeSeverity = when {
        newKm >= previousConfirmedKm -> KmChangeSeverity.OK
        newKm < previousConfirmedKm * (1.0 - MAJOR_DECREASE_FRACTION) ->
            KmChangeSeverity.MAJOR_DECREASE
        else -> KmChangeSeverity.MINOR_DECREASE
    }
}
