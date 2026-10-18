package com.thalison.botfinanceiro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class FinanceManager {

    private static final Path FILE =
            Path.of("dados_financeiros.json");

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private final List<Expense> expenses =
            new ArrayList<>();

    private final double monthlyIncome = 1800.00;

    private final double rent = 550.00;

    private final double internet = 150.00;

    private final double electricity = 70.00;

    public FinanceManager() {
        load();
    }

    public synchronized void addExpense(
            double amount,
            String category,
            String description) {

        expenses.add(
                new Expense(
                        amount,
                        category,
                        description
                )
        );

        save();
    }

    public synchronized List<Expense>
    getCurrentMonthExpenses() {

        YearMonth current =
                YearMonth.now();

        List<Expense> result =
                new ArrayList<>();

        for (Expense e : expenses) {

            if (e.getDate() == null) {
                continue;
            }

            try {

                YearMonth month =
                        YearMonth.from(
                                LocalDateTime.parse(
                                        e.getDate()
                                )
                        );

                if (month.equals(current)) {
                    result.add(e);
                }

            } catch (Exception ignored) {
            }
        }

        return result;
    }

    public double getVariableTotal(
            String category) {

        return getCurrentMonthExpenses()
                .stream()
                .filter(e ->
                        e.getCategory() != null
                                && e.getCategory()
                                .equalsIgnoreCase(category)
                )
                .mapToDouble(
                        Expense::getAmount
                )
                .sum();
    }

    public double getVariableTotalAll() {

        return getCurrentMonthExpenses()
                .stream()
                .mapToDouble(
                        Expense::getAmount
                )
                .sum();
    }

    public double getFixedTotal() {

        return rent +
                internet +
                electricity;
    }

    public double getMonthlyTotal() {

        return getFixedTotal() +
                getVariableTotalAll();
    }

    public double getBalance() {

        return monthlyIncome -
                getMonthlyTotal();
    }

    public String summary() {

        double uber =
                getVariableTotal("uber");

        double bebidas =
                getVariableTotal("bebidas");

        double outros =
                getVariableTotalAll()
                        - uber
                        - bebidas;

        return String.format(

                "📊 RESUMO DO MÊS%n%n" +

                "💰 Renda: R$ %.2f%n%n" +

                "🏠 Aluguel: R$ %.2f%n" +

                "🌐 Internet: R$ %.2f%n" +

                "💡 Luz: R$ %.2f%n" +

                "🚗 Uber: R$ %.2f%n" +

                "🍺 Bebidas: R$ %.2f%n" +

                "📦 Outros gastos: R$ %.2f%n%n" +

                "💸 Total gasto: R$ %.2f%n" +

                "💵 Saldo restante: R$ %.2f",

                monthlyIncome,
                rent,
                internet,
                electricity,
                uber,
                bebidas,
                outros,
                getMonthlyTotal(),
                getBalance()
        );
    }

    public String fixedExpenses() {

        return String.format(

                "📌 GASTOS FIXOS%n%n" +

                "🏠 Aluguel: R$ %.2f%n" +

                "🌐 Internet: R$ %.2f%n" +

                "💡 Luz: R$ %.2f%n%n" +

                "Total fixo: R$ %.2f",

                rent,
                internet,
                electricity,
                getFixedTotal()
        );
    }

    public String history() {

        List<Expense> current =
                getCurrentMonthExpenses();

        if (current.isEmpty()) {

            return
                    "📭 Nenhum gasto variável " +
                    "registrado neste mês.";
        }

        StringBuilder sb =
                new StringBuilder(
                        "🧾 GASTOS DO MÊS\n\n"
                );

        for (Expense e : current) {

            sb.append(
                    String.format(
                            "• R$ %.2f — %s — %s%n",
                            e.getAmount(),
                            e.getCategory(),
                            e.getDescription()
                    )
            );
        }

        sb.append(
                String.format(
                        "\nTotal variável: R$ %.2f",
                        getVariableTotalAll()
                )
        );

        return sb.toString();
    }

    public synchronized void clearCurrentMonth() {

        YearMonth current =
                YearMonth.now();

        expenses.removeIf(e -> {

            if (e.getDate() == null) {
                return false;
            }

            try {

                return YearMonth.from(
                        LocalDateTime.parse(
                                e.getDate()
                        )
                ).equals(current);

            } catch (Exception ignored) {

                return false;
            }
        });

        save();
    }

    private void load() {

        try {

            if (!Files.exists(FILE)) {
                return;
            }

            String json =
                    Files.readString(
                            FILE,
                            StandardCharsets.UTF_8
                    );

            Type type =
                    new TypeToken<List<Expense>>() {
                    }.getType();

            List<Expense> loaded =
                    GSON.fromJson(
                            json,
                            type
                    );

            if (loaded != null) {
                expenses.addAll(loaded);
            }

        } catch (Exception e) {

            System.err.println(
                    "Erro carregando dados: "
                            + e.getMessage()
            );
        }
    }

    private void save() {

        try {

            Files.writeString(
                    FILE,
                    GSON.toJson(expenses),
                    StandardCharsets.UTF_8
            );

        } catch (IOException e) {

            System.err.println(
                    "Erro salvando dados: "
                            + e.getMessage()
            );
        }
    }
}