package com.thalison.botfinanceiro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BotFinanceiro extends TelegramLongPollingBot {

    private static final double RENDA_MENSAL = 1800.00;
    private static final double ALUGUEL = 550.00;
    private static final double INTERNET = 150.00;
    private static final double LUZ = 70.00;

    private static final String ARQUIVO = "financeiro.json";

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private Map<String, MesFinanceiro> meses;

    private final NumberFormat moeda =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public BotFinanceiro() {
        carregarDados();
    }

    @Override
    public String getBotUsername() {
        return "srferreirabot";
    }

    @Override
    public String getBotToken() {
        return "8755121317:AAGG5ZOeINPWQMfTPB63papRE9Keh6TGCWU";
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String mensagem = update.getMessage().getText().trim();
        String comando = mensagem.toLowerCase();

        System.out.println("Mensagem recebida: " + mensagem);

        if (comando.equals("/start")) {

            responder(update,
                    "💰 BOT FINANCEIRO\n\n"
                    + "Olá! Vou controlar seus gastos.\n\n"
                    + "Exemplos:\n"
                    + "30 reais de uber\n"
                    + "20 reais de bebidas\n\n"
                    + "Comandos:\n"
                    + "/resumo\n"
                    + "/gastos\n"
                    + "/ajuda\n"
                    + "/limpar");

            return;
        }

        if (comando.equals("/ajuda")) {

            responder(update,
                    "📋 COMANDOS\n\n"
                    + "💸 Registrar gasto:\n"
                    + "30 reais de uber\n"
                    + "20 reais de bebidas\n\n"
                    + "📊 /resumo\n"
                    + "Mostra seu resumo financeiro.\n\n"
                    + "📋 /gastos\n"
                    + "Mostra Uber e bebidas.\n\n"
                    + "🗑️ /limpar\n"
                    + "Apaga os gastos variáveis do mês.");

            return;
        }

        if (comando.equals("/resumo")) {

            responder(update, gerarResumo());

            return;
        }

        if (comando.equals("/gastos")) {

            responder(update, gerarGastos());

            return;
        }

        if (comando.equals("/limpar")) {

            meses.put(getMesAtual(), new MesFinanceiro());

            salvarDados();

            responder(update,
                    "🗑️ Os gastos variáveis deste mês foram apagados.");

            return;
        }

        registrarGasto(update, mensagem);
    }

    private void registrarGasto(Update update, String mensagem) {

        String regex =
                "(?i)^\\s*(\\d+(?:[\\.,]\\d{1,2})?)\\s*(?:reais?)?\\s*(?:de\\s+)?(.+?)\\s*$";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(mensagem);

        if (!matcher.matches()) {

            responder(update,
                    "❓ Não entendi.\n\n"
                    + "Use, por exemplo:\n"
                    + "30 reais de uber\n"
                    + "20 reais de bebidas");

            return;
        }

        double valor;

        try {

            String valorTexto = matcher.group(1)
                    .replace(",", ".");

            valor = Double.parseDouble(valorTexto);

        } catch (NumberFormatException e) {

            responder(update,
                    "❌ Não consegui entender o valor.");

            return;
        }

        if (valor <= 0) {

            responder(update,
                    "❌ O valor precisa ser maior que zero.");

            return;
        }

        String categoria = matcher.group(2)
                .trim()
                .toLowerCase();

        if (categoria.contains("uber")) {

            categoria = "uber";

        } else if (categoria.contains("bebida")) {

            categoria = "bebidas";

        } else {

            responder(update,
                    "❌ Categoria não cadastrada.\n\n"
                    + "Por enquanto use:\n"
                    + "🚕 Uber\n"
                    + "🍺 Bebidas");

            return;
        }

        String mesAtual = getMesAtual();

        MesFinanceiro mes = meses.get(mesAtual);

        if (mes == null) {

            mes = new MesFinanceiro();
            meses.put(mesAtual, mes);
        }

        if (categoria.equals("uber")) {

            mes.uber += valor;

        } else {

            mes.bebidas += valor;
        }

        salvarDados();

        double totalCategoria;

        if (categoria.equals("uber")) {

            totalCategoria = mes.uber;

        } else {

            totalCategoria = mes.bebidas;
        }

        responder(update,
                "✅ GASTO REGISTRADO!\n\n"
                + "Categoria: " + categoria.toUpperCase() + "\n"
                + "Valor: " + formatar(valor) + "\n\n"
                + "Total de " + categoria + " neste mês:\n"
                + formatar(totalCategoria));
    }

    private String gerarResumo() {

        MesFinanceiro mes = meses.get(getMesAtual());

        if (mes == null) {
            mes = new MesFinanceiro();
        }

        double gastosFixos =
                ALUGUEL + INTERNET + LUZ;

        double gastosVariaveis =
                mes.uber + mes.bebidas;

        double totalGastos =
                gastosFixos + gastosVariaveis;

        double saldo =
                RENDA_MENSAL - totalGastos;

        return "📊 RESUMO FINANCEIRO\n\n"

                + "💰 Renda mensal: "
                + formatar(RENDA_MENSAL)
                + "\n\n"

                + "📌 GASTOS FIXOS\n"
                + "🏠 Aluguel: "
                + formatar(ALUGUEL)
                + "\n"
                + "🌐 Internet: "
                + formatar(INTERNET)
                + "\n"
                + "💡 Luz: "
                + formatar(LUZ)
                + "\n"
                + "Total fixo: "
                + formatar(gastosFixos)
                + "\n\n"

                + "📌 GASTOS VARIÁVEIS\n"
                + "🚕 Uber: "
                + formatar(mes.uber)
                + "\n"
                + "🍺 Bebidas: "
                + formatar(mes.bebidas)
                + "\n"
                + "Total variável: "
                + formatar(gastosVariaveis)
                + "\n\n"

                + "💸 TOTAL GASTO: "
                + formatar(totalGastos)
                + "\n\n"

                + "💵 SALDO RESTANTE: "
                + formatar(saldo);
    }

    private String gerarGastos() {

        MesFinanceiro mes = meses.get(getMesAtual());

        if (mes == null) {
            mes = new MesFinanceiro();
        }

        double total =
                mes.uber + mes.bebidas;

        return "📋 GASTOS VARIÁVEIS\n\n"
                + "🚕 Uber: "
                + formatar(mes.uber)
                + "\n"
                + "🍺 Bebidas: "
                + formatar(mes.bebidas)
                + "\n\n"
                + "💸 Total: "
                + formatar(total);
    }

    private void carregarDados() {

        File arquivo = new File(ARQUIVO);

        if (!arquivo.exists()) {

            meses = new HashMap<String, MesFinanceiro>();

            return;
        }

        try (FileReader reader = new FileReader(arquivo)) {

            Type tipo =
                    new TypeToken<Map<String, MesFinanceiro>>() {
                    }.getType();

            meses = gson.fromJson(reader, tipo);

            if (meses == null) {
                meses = new HashMap<String, MesFinanceiro>();
            }

        } catch (IOException e) {

            e.printStackTrace();

            meses = new HashMap<String, MesFinanceiro>();
        }
    }

    private void salvarDados() {

        try (FileWriter writer = new FileWriter(ARQUIVO)) {

            gson.toJson(meses, writer);

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    private String getMesAtual() {

        LocalDate data = LocalDate.now();

        return String.format(
                "%04d-%02d",
                data.getYear(),
                data.getMonthValue());
    }

    private String formatar(double valor) {

        return moeda.format(valor);
    }

    private void responder(Update update, String texto) {

        try {

            SendMessage resposta = new SendMessage();

            resposta.setChatId(
                    String.valueOf(
                            update.getMessage().getChatId()));

            resposta.setText(texto);

            execute(resposta);

        } catch (TelegramApiException e) {

            e.printStackTrace();
        }
    }

    public static class MesFinanceiro {

        double uber = 0.0;

        double bebidas = 0.0;
    }
}