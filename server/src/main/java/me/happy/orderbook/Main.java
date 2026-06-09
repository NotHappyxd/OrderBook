package me.happy.orderbook;

import lombok.SneakyThrows;
import me.happy.orderbook.lmax.Exchange;
import me.happy.orderbook.server.ExchangeServer;

import java.io.File;

public class Main {

    @SneakyThrows
    static void main(String[] args) {
        File file = new File("logs");
        if (!file.exists())
            file.mkdir();

        Exchange exchange = new Exchange(4);
        ExchangeServer server = new ExchangeServer(8080);
        server.startServer(exchange);
    }
}
