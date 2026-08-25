package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phone_rules")
data class PhoneRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val contactName: String = "",
    val relationshipTier: String = "Standard", // "Inner Circle", "Standard", "Nuisance"
    val isVip: Boolean = false,
    val isDivert: Boolean = false,
    val isForward: Boolean = false
)
