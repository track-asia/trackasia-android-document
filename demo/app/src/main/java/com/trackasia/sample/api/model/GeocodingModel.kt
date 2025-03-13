package com.trackasia.sample.api.model

data class APIResponse(
    val type: String,
    val features: List<Feature>
)

data class Feature(
    val properties: Properties,
    val geometry: Geometry
)

data class Properties(
    val name: String,
    val street: String,
    val distance: Double,
    val country: String,
    val region: String,
    val county: String,
    val locality: String,
    val label: String
)

data class Geometry(
    val coordinates: List<Double>
)

data class AutoSuggestionResponse(
    val features: List<Feature>
)