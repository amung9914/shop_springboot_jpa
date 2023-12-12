package jpabook.jpashop.service.query;

import jpabook.jpashop.api.OrderApiController;
import jpabook.jpashop.domain.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.stream.Collectors.toList;
/*
    트래픽이 많은 api서버(대규모)는 OSIV를 끈다.
    admin같은곳에서는 OSIV를 켠다.
    open-in-view: false 로 해야 커넥션풀이 마르지 않음.
    대신 이런식으로 컨트롤러에서 지연로딩하는 코드를 따로 Query용 QueryService로 만들어주고
    컨트롤러에서는 리턴만 한다.

@Service
@Transactional(readOnly = true)
public class OrderQueryService {

    public List<OrderApiController.OrderDto> ordersV3(){
        List<Order> orders = orderRepository.findAllWithItem();
        List<OrderApiController.OrderDto> collect = orders.stream()
                .map(o -> new OrderApiController.OrderDto(o))
                .collect(toList());
        return collect;
    }
}
*/
