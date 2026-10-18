package com.thalison.botfinanceiro;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {

    public static void main(String[] args) {

        try {

            TelegramBotsApi botsApi =
                    new TelegramBotsApi(DefaultBotSession.class);

            botsApi.registerBot(new BotFinanceiro());

            System.out.println("=================================");
            System.out.println("BOT FINANCEIRO INICIADO!");
            System.out.println("Aguardando mensagens...");
            System.out.println("=================================");

        } catch (TelegramApiException e) {

            System.out.println("ERRO AO INICIAR O BOT:");
            e.printStackTrace();
        }
    }
}