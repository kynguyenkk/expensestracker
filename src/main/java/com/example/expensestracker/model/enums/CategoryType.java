package com.example.expensestracker.model.enums;

public enum CategoryType {
    income ("income"),
    expense ("expense");
    private final String categoryType;

    CategoryType(String categoryType) {
        this.categoryType = categoryType;
    }

    public String getCategoryType() {
        return categoryType;
    }
    public static CategoryType fromString(String categoryType) {
        for (CategoryType type : CategoryType.values()) {
            if (type.getCategoryType().equalsIgnoreCase(categoryType)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid category type: " + categoryType);
    }
}
