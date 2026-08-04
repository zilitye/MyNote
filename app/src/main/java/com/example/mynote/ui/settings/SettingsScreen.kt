package com.example.mynote.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mynote.data.SortOrder
import com.example.mynote.data.ThemeMode
import com.example.mynote.data.ViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    viewMode: ViewMode,
    sortOrder: SortOrder,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetViewMode: (ViewMode) -> Unit,
    onSetSortOrder: (SortOrder) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            SectionHeader("Appearance")
            OptionRow("System default", themeMode == ThemeMode.SYSTEM) { onSetThemeMode(ThemeMode.SYSTEM) }
            OptionRow("Light", themeMode == ThemeMode.LIGHT) { onSetThemeMode(ThemeMode.LIGHT) }
            OptionRow("Dark", themeMode == ThemeMode.DARK) { onSetThemeMode(ThemeMode.DARK) }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            SectionHeader("Default notes view")
            OptionRow("Grid", viewMode == ViewMode.GRID) { onSetViewMode(ViewMode.GRID) }
            OptionRow("List", viewMode == ViewMode.LIST) { onSetViewMode(ViewMode.LIST) }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            SectionHeader("Default sort order")
            OptionRow("Date modified", sortOrder == SortOrder.DATE_MODIFIED) { onSetSortOrder(SortOrder.DATE_MODIFIED) }
            OptionRow("Date created", sortOrder == SortOrder.DATE_CREATED) { onSetSortOrder(SortOrder.DATE_CREATED) }
            OptionRow("Title (A-Z)", sortOrder == SortOrder.TITLE_ASC) { onSetSortOrder(SortOrder.TITLE_ASC) }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen(
            themeMode = ThemeMode.SYSTEM,
            viewMode = ViewMode.GRID,
            sortOrder = SortOrder.DATE_MODIFIED,
            onSetThemeMode = {},
            onSetViewMode = {},
            onSetSortOrder = {},
            onBack = {}
        )
    }
}
