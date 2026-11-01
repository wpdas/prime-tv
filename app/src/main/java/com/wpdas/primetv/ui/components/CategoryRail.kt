package com.wpdas.primetv.ui.components

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.wpdas.primetv.ui.theme.PrimeFocus
import com.wpdas.primetv.ui.theme.PrimeTheme

data class RailCategory(val id: String, val label: String, val count: Int? = null)

/** Trilho vertical de categorias (Ao vivo, Configurações). A selecionada fica em `selected`. */
@Composable
fun CategoryRail(
    categories: List<RailCategory>,
    selectedId: String?,
    onSelect: (RailCategory) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 200.dp,
) {
    val colors = PrimeTheme.colors
    val shape = PrimeTheme.shapes.card
    LazyColumn(
        modifier = modifier.width(width).focusRestorer().focusGroup(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
    ) {
        items(categories, key = { it.id }) { cat ->
            val selected = cat.id == selectedId
            Surface(
                onClick = { onSelect(cat) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = PrimeFocus.shape(shape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (selected) colors.selected else colors.background,
                    contentColor = if (selected) colors.text else colors.textBody,
                    focusedContainerColor = colors.selected,
                    focusedContentColor = colors.text,
                    pressedContainerColor = colors.selected,
                    pressedContentColor = colors.text,
                ),
                scale = PrimeFocus.noScale(),
                border = PrimeFocus.border(shape),
            ) {
                Row(
                    Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        cat.label,
                        style = if (selected) PrimeTheme.typography.bodyStrong else PrimeTheme.typography.buttonSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (cat.count != null) {
                        Text(cat.count.toString(), style = PrimeTheme.typography.caption, color = colors.textSecondary, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}
