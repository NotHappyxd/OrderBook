package me.happy.orderbook;

import lombok.SneakyThrows;
import me.happy.orderbook.lmax.Exchange;
import me.happy.orderbook.server.ExchangeServer;

public class Main {

    @SneakyThrows
    static void main(String[] args) {
        Exchange exchange = new Exchange(4);
        ExchangeServer server = new ExchangeServer(8080);
        server.startServer(exchange);
    }
}
