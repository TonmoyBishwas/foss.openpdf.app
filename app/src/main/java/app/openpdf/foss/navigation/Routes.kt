package app.openpdf.foss.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object SettingsRoute

@Serializable
data class ViewerRoute(val uri: String)

@Serializable
data class OrganizeRoute(val uri: String)
