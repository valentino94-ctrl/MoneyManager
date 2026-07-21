package com.v94studio.moneymanager.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? {
        return value?.let { TransactionType.valueOf(it) }
    }

    @TypeConverter
    fun fromTransactionType(type: TransactionType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toCategoryType(value: String?): CategoryType? {
        return value?.let { CategoryType.valueOf(it) }
    }

    @TypeConverter
    fun fromCategoryType(type: CategoryType?): String? {
        return type?.name
    }
}
