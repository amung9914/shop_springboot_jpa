package jpabook.jpashop.service;

import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    @Transactional
    public void saveItem(Item item){
        itemRepository.save(item);
    }

    // 파라미터가 많으면 service패키지에 Dto클래스 만들어서 파라미터 대신 dto넘겨도 됨
    //updateItem(UpdateItemDto dto) 이런식으로
    @Transactional
    public void updateItem(Long itemId, String name, int price, int stockQUantity){
        Item findItem = itemRepository.findOne(itemId); // 영속상태
        /*
        findItem.change(price,name,stockQuantity)
        findItem.addStock()...
        처럼 의미있는 메소드를 엔티티내에 만들어서 역추척 할 수 있도록 해야함.(setter쓰지말자)
         */

        // 변경된 애로 바꿔준다.(이렇게 하면 JPA가 변경감지 수행)
        findItem.setPrice(price);
        findItem.setName(name);
        findItem.setStockQuantity(stockQUantity);
    }

    public List<Item> findItems(){
        return itemRepository.findAll();
    }

    public Item findOne(Long itemId){
        return itemRepository.findOne(itemId);
    }
}
