package me.happy.orderbook;

import lombok.SneakyThrows;
import me.happy.orderbook.engine.OrderBook;
import me.happy.orderbook.lmax.Exchange;
import me.happy.orderbook.lmax.OrderEventHandler;
import me.happy.orderbook.order.Side;
import me.happy.orderbook.server.ExchangeServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class Main {

    @SneakyThrows
    public static void main(String[] args) {
        Exchange exchange = new Exchange(4);
        ExchangeServer server = new ExchangeServer(8080);
        server.startServer(exchange);
    }
}
