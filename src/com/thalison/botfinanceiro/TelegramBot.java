package com.thalison.botfinanceiro;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TelegramBot {

    private final String apiUrl;

    private final HttpClient client =
            HttpClient.newHttpClient();

    private final FinanceManager finance =
            new FinanceManager();

    private long offset = 0;

    public TelegramBot(String token) {

        this.apiUrl =
                "https://api.telegram.org/bot" + token;
    }

    public void start() {

        System.out.println(
                "Bot online. Ctrl+C para parar."
        );

        while (true) {

            try {

                JsonObject root =
                        JsonParser
                                .parseString(getUpdates())
                                .getAsJsonObject();

                if (!root.get("ok").getAsBoolean()) {

                    System.err.println(
                            "Erro Telegram: " + root
                    );

                    Thread.sleep(3000);

                    continue;
                }

                for (var update :
                        root.getAsJsonArray("result")) {

                    JsonObject u =
                            update.getAsJsonObject();

                    offset =
                            u.get("update_id")
                                    .getAsLong() + 1;

                    processUpdate(u);
                }

            } catch (Exception e) {

                System.err.println(
                        "Erro: " + e.getMessage()
                );

                try {

                    Thread.sleep(3000);

                } catch (InterruptedException ignored) {

                    Thread.currentThread()
                            .interrupt();

                    return;
                }
            }
        }
    }

    private String getUpdates()
            throws IOException, InterruptedException {

        HttpRequest request =
                HttpRequest.newBuilder()

                        .uri(
                                URI.create(
                                        apiUrl +
                                        "/getUpdates?offset=" +
                                        offset +
                                        "&timeout=25"
                                )
                        )

                        .GET()

                        .build();

        return client.send(
                request,
                HttpResponse.BodyHandlers
                        .ofString()
        ).body();
    }

    private void processUpdate(
            JsonObject update)
            throws IOException, InterruptedException {

        if (!update.has("message")) {
            return;
        }

        JsonObject message =
                update.getAsJsonObject("message");

        if (!message.has("chat")
                || !message.has("text")) {

            return;
        }

        long chatId =
                message
                        .getAsJsonObject("chat")
                        .get("id")
                        .getAsLong();

        String text =
                message
                        .get("text")
                        .getAsString()
                        .trim();

        if (text.equals("/start")
                || text.equals("/ajuda")) {

            sendMessage(
                    chatId,
                    help()
            );

        } else if (text.equals("/resumo")) {

            sendMessage(
                    chatId,
                    finance.summary()
            );

        } else if (text.equals("/fixos")) {

            sendMessage(
                    chatId,
                    finance.fixedExpenses()
            );

        } else if (text.equals("/historico")) {

            sendMessage(
                    chatId,
                    finance.history()
            );

        } else if (text.equals("/saldo")) {

            sendMessage(
                    chatId,
                    String.format(
                            "💵 Saldo estimado neste mês: R$ %.2f",
                            finance.getBalance()
                    )
            );

        } else if (text.equals("/limparmes")) {

            finance.clearCurrentMonth();

            sendMessage(
                    chatId,
                    "🗑️ Gastos variáveis deste mês apagados."
            );

        } else if (text.startsWith("/gasto ")) {

            registerExpense(
                    chatId,
                    text.substring(7).trim()
            );

        } else {

            sendMessage(
                    chatId,
                    "❓ Comando não reconhecido. Use /ajuda."
            );
        }
    }

    private void registerExpense(
            long chatId,
            String input)
            throws IOException, InterruptedException {

        Matcher matcher =
                Pattern.compile(
                        "^(\\d+(?:[.,]\\d{1,2})?)\\s+([^\\s]+)(?:\\s+(.*))?$"
                )
                .matcher(input);

        if (!matcher.matches()) {

            sendMessage(
                    chatId,
                    "❌ Formato inválido.\n\n" +
                    "Exemplos:\n" +
                    "/gasto 30 uber\n" +
                    "/gasto 20 bebidas\n" +
                    "/gasto 15 outros lanche"
            );

            return;
        }

        double amount =
                Double.parseDouble(
                        matcher.group(1)
                                .replace(",", ".")
                );

        String category =
                matcher.group(2)
                        .toLowerCase();

        String description =
                matcher.group(3) == null
                        ? "sem descrição"
                        : matcher.group(3);

        if (amount <= 0) {

            sendMessage(
                    chatId,
                    "❌ O valor precisa ser maior que zero."
            );

            return;
        }

        if (category.equals("uber")) {

            category = "uber";

        } else if (
                category.equals("bebida")
                        || category.equals("bebidas")) {

            category = "bebidas";

        } else {

            category = "outros";
        }

        finance.addExpense(
                amount,
                category,
                description
        );

        sendMessage(
                chatId,
                String.format(
                        "✅ Gasto registrado!\n\n" +
                        "💰 Valor: R$ %.2f\n" +
                        "📂 Categoria: %s\n" +
                        "📝 Descrição: %s\n\n" +
                        "📊 Total variável do mês: R$ %.2f\n" +
                        "💵 Saldo estimado: R$ %.2f",

                        amount,
                        category,
                        description,
                        finance.getVariableTotalAll(),
                        finance.getBalance()
                )
        );
    }

    private String help() {

        return
                "🤖 BOT FINANCEIRO\n\n" +

                "💰 Renda mensal: R$ 1.800,00\n\n" +

                "📌 GASTOS FIXOS\n" +
                "/fixos — aluguel, internet e luz\n\n" +

                "➕ REGISTRAR GASTO\n" +
                "/gasto 30 uber\n" +
                "/gasto 20 bebidas\n" +
                "/gasto 15 outros lanche\n\n" +

                "📊 CONSULTAS\n" +
                "/resumo — resumo completo\n" +
                "/saldo — saldo restante\n" +
                "/historico — gastos do mês\n\n" +

                "🗑️ /limparmes — apaga gastos variáveis do mês\n\n" +

                "💾 Os gastos ficam salvos em dados_financeiros.json.";
    }

    private void sendMessage(
            long chatId,
            String text)
            throws IOException, InterruptedException {

        String body =
                "chat_id=" +
                URLEncoder.encode(
                        String.valueOf(chatId),
                        StandardCharsets.UTF_8
                ) +

                "&text=" +
                URLEncoder.encode(
                        text,
                        StandardCharsets.UTF_8
                );

        HttpRequest request =
                HttpRequest.newBuilder()

                        .uri(
                                URI.create(
                                        apiUrl +
                                        "/sendMessage"
                                )
                        )

                        .header(
                                "Content-Type",
                                "application/x-www-form-urlencoded"
                        )

                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(body)
                        )

                        .build();

        client.send(
                request,
                HttpResponse.BodyHandlers
                        .ofString()
        );
    }
}