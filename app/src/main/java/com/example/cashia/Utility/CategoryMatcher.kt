package com.example.cashia.Utility

import com.example.cashia.models.Category
import com.example.cashia.models.MpesaTransactions

object CategoryMatcher {
	   fun autoAssign(tx: MpesaTransactions, categories: List<Category>): String {
			if (tx.category.isNotBlank() && tx.category != "cat_uncategorized") {
				   return tx.category
			}
			val recipientLower = tx.recipient.lowercase()
			for (category in categories) {
				   if (category.keywords.isBlank()) continue
				   val keywords = category.keywords.split(",").map { it.trim().lowercase() }
				   if (keywords.any { kw -> kw.isNotEmpty() && recipientLower.contains(kw) }) {
						return category.id
				   }
			}
			return "cat_uncategorized"
	   }
	   
	   fun reCategorizeAll(
			transactions: List<MpesaTransactions>, categories: List<Category>
	   ): List<MpesaTransactions> {
			return transactions.map { tx ->
				   tx.copy(category = autoAssign(tx, categories))
			}
	   }
}
