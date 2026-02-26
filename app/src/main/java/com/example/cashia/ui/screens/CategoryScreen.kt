package com.example.cashia.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.models.Category
import com.example.cashia.models.CategoryViewModel

@Composable
fun CategoryScreen(viewModel: CategoryViewModel) {
	   val categories by viewModel.categories.collectAsState(emptyList())
	   var showAddDialog by remember { mutableStateOf(false) }
	   var editingCategory by remember { mutableStateOf<Category?>(null) }
	   
	   Scaffold(
			containerColor = androidx.compose.ui.graphics.Color.Transparent,
			floatingActionButton = {
				   FloatingActionButton(
						onClick = { showAddDialog = true },
						containerColor = MaterialTheme.colorScheme.primary
				   ) {
						Icon(Icons.Rounded.Add, contentDescription = "Add Category")
				   }
			}) { padding ->
			LazyColumn(
				   contentPadding = padding,
				   modifier = Modifier.padding(horizontal = 16.dp),
				   verticalArrangement = Arrangement.spacedBy(8.dp)
			) {
				   item { Spacer(Modifier.height(8.dp)) }
				   items(categories, key = { it.id }) { cat ->
						CategoryItem(
							   category = cat,
							   onEdit = { editingCategory = cat },
							   onDelete = {
									if (!cat.isDefault) viewModel.deleteCategory(
										   cat
									)
							   })
				   }
				   item { Spacer(Modifier.height(80.dp)) } // FAB clearance
			}
			
			if (showAddDialog) {
				   AddEditCategoryDialog(
						title = "New Category",
						onConfirm = { name, emoji, keywords ->
							   viewModel.addCategory(name, emoji, keywords)
							   showAddDialog = false
						},
						onDismiss = { showAddDialog = false })
			}
			
			editingCategory?.let { cat ->
				   AddEditCategoryDialog(
						title = "Edit Category",
						initialName = cat.name,
						initialEmoji = cat.emoji,
						initialKeywords = cat.keywords,
						onConfirm = { name, emoji, keywords ->
							   viewModel.updateCategory(
									cat.copy(
										   name = name,
										   emoji = emoji,
										   keywords = keywords
									)
							   )
							   editingCategory = null
						},
						onDismiss = { editingCategory = null })
			}
	   }
}

@Composable
fun CategoryItem(
	   category: Category, onEdit: () -> Unit, onDelete: () -> Unit
) {
	   val colorScheme = MaterialTheme.colorScheme
	   
	   Surface(
			modifier = Modifier.fillMaxWidth(),
			shape = RoundedCornerShape(20.dp),
			color = colorScheme.surface.copy(alpha = 0.6f),
			border = BorderStroke(0.5.dp, colorScheme.onSurface.copy(alpha = 0.08f))
	   ) {
			Row(
				   modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 16.dp, vertical = 12.dp),
				   verticalAlignment = Alignment.CenterVertically
			) {
				   // Emoji badge
				   Box(
						contentAlignment = Alignment.Center,
						modifier = Modifier
							   .size(44.dp)
							   .background(
									colorScheme.primary.copy(alpha = 0.1f),
									CircleShape
							   )
				   ) {
						Text(text = category.emoji, fontSize = 20.sp)
				   }
				   
				   Spacer(Modifier.width(12.dp))
				   
				   // Name + keywords
				   Column(modifier = Modifier.weight(1f)) {
						Text(
							   text = category.name,
							   style = MaterialTheme.typography.bodyLarge.copy(
									fontWeight = FontWeight.Bold
							   ),
							   color = colorScheme.onSurface
						)
						if (category.keywords.isNotBlank()) {
							   Text(
									text = category.keywords,
									style = MaterialTheme.typography.labelSmall,
									color = colorScheme.onSurfaceVariant.copy(
										   alpha = 0.6f
									),
									maxLines = 1,
									overflow = TextOverflow.Ellipsis
							   )
						}
						if (category.isDefault) {
							   Text(
									text = "DEFAULT",
									style = MaterialTheme.typography.labelSmall.copy(
										   fontWeight = FontWeight.Black,
										   letterSpacing = 1.sp
									),
									color = colorScheme.primary.copy(alpha = 0.7f)
							   )
						}
				   }
				   
				   // Actions
				   IconButton(onClick = onEdit) {
						Icon(
							   Icons.Rounded.Edit,
							   contentDescription = "Edit",
							   tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
							   modifier = Modifier.size(18.dp)
						)
				   }
				   if (!category.isDefault) {
						IconButton(onClick = onDelete) {
							   Icon(
									Icons.Rounded.Delete,
									contentDescription = "Delete",
									tint = colorScheme.error.copy(alpha = 0.7f),
									modifier = Modifier.size(18.dp)
							   )
						}
				   }
			}
	   }
}

@Composable
fun AddEditCategoryDialog(
	   title: String,
	   initialName: String = "",
	   initialEmoji: String = "📦",
	   initialKeywords: String = "",
	   onConfirm: (name: String, emoji: String, keywords: String) -> Unit,
	   onDismiss: () -> Unit
) {
	   var name by remember { mutableStateOf(initialName) }
	   var emoji by remember { mutableStateOf(initialEmoji) }
	   var keywords by remember { mutableStateOf(initialKeywords) }
	   val isValid = name.isNotBlank() && emoji.isNotBlank()
	   
	   AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(24.dp), title = {
			Text(title, fontWeight = FontWeight.Black)
	   }, text = {
			Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
				   Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
						OutlinedTextField(
							   value = emoji,
							   onValueChange = {
									if (it.length <= 2) emoji = it
							   },
							   label = { Text("Icon") },
							   modifier = Modifier.width(72.dp),
							   singleLine = true,
							   shape = RoundedCornerShape(12.dp)
						)
						OutlinedTextField(
							   value = name,
							   onValueChange = { name = it },
							   label = { Text("Name") },
							   modifier = Modifier.weight(1f),
							   singleLine = true,
							   shape = RoundedCornerShape(12.dp)
						)
				   }
				   OutlinedTextField(
						value = keywords,
						onValueChange = { keywords = it },
						label = { Text("Auto-match keywords") },
						placeholder = {
							   Text(
									"e.g. petrol, shell, total", fontSize = 12.sp
							   )
						},
						supportingText = {
							   Text(
									"Comma-separated. Transactions whose recipient contains any keyword will be auto-assigned.",
									style = MaterialTheme.typography.labelSmall,
									color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
										   alpha = 0.6f
									)
							   )
						},
						modifier = Modifier.fillMaxWidth(),
						shape = RoundedCornerShape(12.dp),
						minLines = 2
				   )
			}
	   }, confirmButton = {
			Button(
				   onClick = {
						onConfirm(
							   name.trim(), emoji.trim(), keywords.lowercase().trim()
						)
				   }, enabled = isValid, shape = RoundedCornerShape(12.dp)
			) {
				   Text("Save", fontWeight = FontWeight.Bold)
			}
	   }, dismissButton = {
			TextButton(onClick = onDismiss) {
				   Text("Cancel")
			}
	   })
}