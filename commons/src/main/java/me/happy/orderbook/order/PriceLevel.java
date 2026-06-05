package me.happy.orderbook.order;

import lombok.Data;

@Data
public class PriceLevel {

    private Order head;
    private Order tail;
    private int totalQuantity = 0;

    public void addOrder(Order order) {
        order.setPriceLevel(this);

        if (this.head == null) {
            this.head = order;
        }else {
            this.tail.setNext(order);
            order.setPrevious(this.tail);
        }

        this.tail = order;

        totalQuantity += order.getQuantity();
    }

    public void removeOrder(Order order) {
        if (order.getPrevious() != null) {
            order.getPrevious().setNext(order.getNext());
        }else {
            head = order.getNext();
        }

        if (order.getNext() != null) {
            order.getNext().setPrevious(order.getPrevious());
        }else {
            tail = order.getPrevious();
        }

        order.setPrevious(null);
        order.setNext(null);
        order.setPriceLevel(null);
    }

    public void reset() {
        this.head = null;
        this.tail = null;

        this.totalQuantity = 0;
    }
}
