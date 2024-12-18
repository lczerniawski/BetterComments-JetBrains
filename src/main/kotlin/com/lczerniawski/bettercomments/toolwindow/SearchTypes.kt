package com.lczerniawski.bettercomments.toolwindow

enum class SearchTypes(val description: String) {
    ProjectFiles("Project Files Tracked by Git"),
    RecentlyChangedFiles("Changed Files"),
    OpenFiles("Open Files"),
    CurrentFile("Current File");

    companion object {
        fun fromString(description: String): SearchTypes? {
            return values().find { it.description == description }
        }
    }
}