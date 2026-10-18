package com.thalison.botfinanceiro;

import java.time.LocalDateTime;

public class Expense {

    private double amount;

    private String category;

    private String description;

    private String date;

    public Expense(
            double amount,
            String category,
            String description) {

        this.amount = amount;

        this.category = category;

        this.description = description;

        this.date =
                LocalDateTime.now().toString();
    }

    public double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getDate() {
        return date;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDate(String date) {
        this.date = date;
    }
}