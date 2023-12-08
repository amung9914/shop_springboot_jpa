package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * XtoOne(ManyToOne, OneToOne)관계 - 패치조인으로 성능해결가능
 * Order -> Member
 * Order -> Delivery
 */
//주문내역을 조회하는 api
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;
    private final OrderSimpleQueryRepository orderSimpleQueryRepository;

    /**
     * 양방향 연관관계를 엔티티로 던지면 무한루프에 빠짐 오더의멤버의오더의멤버의...
     *     둘 중 하나를 @JsonIgnore 로 끊어준다.
     *     끊어주니 Type definition error 발생(잭슨이 뽑으려고하니 프록시라서 오류남)
     *     implementation 'com.fasterxml.jackson.datatype:jackson-datatype-hibernate5-jakarta'
     *     할 수 있지만 성능 문제 및 엔티티 문제 있음, 유지보수 어려움
     *     엔티티로 직접 노출하지 마시오..! Dto를 권장함.
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1(){
        List<Order> all = orderRepository.findAllByCriteria(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName(); //Lazy 강제 초기화
            order.getDelivery().getAddress(); //Lazy 강제 초기화
        }
        return all;
    }

    /**
     * 엔티티말고 dto로 할 것.
     * 실무에서는 list로 바로 반환하지 말고 result객체로 한 번 감싸주자!
     * 이것도 좋은 방법은 아니다. 성능문제 있음.
     */
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2(){
        // Order가 2개인데 쿼리는 5개가 나가버림~~~~!
        List<Order> orders = orderRepository.findAllByCriteria(new OrderSearch());
        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());
        return result;
    }

    /**
     * 패치조인 : 엔티티 다 찍어오는게 단점
     * 여기까지 오면 대부분의 성능 이슈가 해결된다.
     */
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3(){
        //패치조인 실행(한방쿼리로 다 가져옴)
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());
        return result;
    }

    /**
     * 바로 JPA에서 DTO로 끄집어낸다(v3보다 더 성능 최적화 가능)
     * v3해도 안되면 DTO를 바로 사용하는 것임.
     */
    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> ordersV4(){
        return orderSimpleQueryRepository.findOrderDtos();
    }

    @Data
    static class SimpleOrderDto{
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        // Dto가 엔티티를 파라미터로 받는거는 괜찮음
        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName(); // LAZY 초기화
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress(); // LAZY 초기화
        }
    }
}
