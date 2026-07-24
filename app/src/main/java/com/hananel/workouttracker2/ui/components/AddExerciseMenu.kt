package com.hananel.workouttracker2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.ui.theme.rememberGlassColors

/**
 * Bottom sheet that lets the user pick how to add an exercise — create from scratch
 * or pull from the catalog. Replaces the old centered AlertDialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseMenu(
    onDismiss: () -> Unit,
    onCreateNew: () -> Unit,
    onChooseFromCatalog: () -> Unit
) {
    GlassSheet(
        title = stringResource(R.string.add_exercise),
        icon = Icons.Default.Add,
        onDismiss = onDismiss
    ) {
            AddExerciseOptionTile(
                icon = Icons.AutoMirrored.Filled.List,
                tint = MaterialTheme.colorScheme.secondary,
                title = stringResource(R.string.choose_from_catalog),
                subtitle = stringResource(R.string.choose_from_catalog_desc),
                onClick = {
                    onDismiss()
                    onChooseFromCatalog()
                }
            )

            AddExerciseOptionTile(
                icon = Icons.Default.Add,
                tint = MaterialTheme.colorScheme.primary,
                title = stringResource(R.string.create_new_exercise),
                subtitle = stringResource(R.string.create_new_exercise_desc),
                onClick = {
                    onDismiss()
                onCreateNew()
            }
        )
    }
}

@Composable
private fun AddExerciseOptionTile(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val glass = rememberGlassColors()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = glass.onGlass.copy(alpha = 0.05f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
