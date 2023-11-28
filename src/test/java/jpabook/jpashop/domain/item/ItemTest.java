package jpabook.jpashop.domain.item;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.Assert.*;
@SpringBootTest
public class ItemTest {

    @Test
    public void 상품수량(){

        Item book = new Book();
        book.setStockQuantity(2);
        book.removeStock(3);
    }



}