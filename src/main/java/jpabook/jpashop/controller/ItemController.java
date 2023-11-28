package jpabook.jpashop.controller;

import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/items/new")
    public String createForm(Model model){
        model.addAttribute("form",new BookForm());
        return "items/createItemForm";
    }
    @PostMapping("/items/new")
    public String create(BookForm form){
        Book book = new Book();
        // setter말고 생성자메서드 createBook으로 설계하는게 더 나은 설계이다.
        book.setName(form.getName());
        book.setPrice(form.getPrice());
        book.setStockQuantity(form.getStockQuantity());
        book.setAuthor(form.getAuthor());
        book.setIsbn(form.getIsbn());

        itemService.saveItem(book);
        return "redirect:/";
    }

    @GetMapping("/items")
    public String list(Model model){
        List<Item> items = itemService.findItems();
        model.addAttribute("items",items);
        return "items/itemList";
    }

    // http://localhost:8080/items/1/edit PathVariable로 바인딩 해줌
    @GetMapping("/items/{itemId}/edit")
    public String updateItemForm(@PathVariable("itemId") Long itemId, Model model){
        Book item = (Book) itemService.findOne(itemId);

        BookForm form = new BookForm();
        form.setId(item.getId());
        form.setName(item.getName());
        form.setPrice(item.getPrice());
        form.setStockQuantity(item.getStockQuantity());
        form.setAuthor(item.getAuthor());
        form.setIsbn(item.getIsbn());

        model.addAttribute("form",form);
        return "items/updateItemForm";
    }

    @PostMapping("/items/{itemId}/edit")
    public String updateItem(@PathVariable("itemId") Long itemId, @ModelAttribute("form") BookForm form){

        /*
         new로 생성하긴 했으나 실제 DB에 갔다 온 애라서 식별자(id)가 있음. 준영속 상태.
         임의로 만들어내도 기존 식별자가 있는 경우에는 준영속 엔티티이다. JPA가 관리안해줌.
         변경하는 방법2가지 1)변경감지이용(권장) 2)merge(쓰지마세요)
         컨트롤러에서 어설프게 엔티티 만들지 마세요.
        */

/*
        Book book = new Book();
        book.setId(form.getId());
        book.setName(form.getName());
        book.setPrice(form.getPrice());
        book.setStockQuantity(form.getStockQuantity());
        book.setAuthor(form.getAuthor());
        book.setIsbn(form.getIsbn());
        itemService.saveItem(book);
*/
        //더 나은 설계 버전
        itemService.updateItem(itemId, form.getName(), form.getPrice(), form.getStockQuantity());



        // 이때 id값으로 넘어오면 누군가 조작할 수 있기때문에 취약점 존재.
        // 서버쪽에서 이 유저가 이 아이템에 대해 권한이 있는지 체크해주어야한다.
        return "redirect:/items";
    }
}
