package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.query.OrderFlatDto;
import jpabook.jpashop.repository.order.query.OrderItemQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.*;


//컬렉션 조회 최적화해보기
// 엔티티 조회방식을 우선 권장합니다. 배치 사이즈로도 안되면 사실 캐시(레이드같은거) 하는게 맞음.
// 참고로 엔티티는 캐시에 올리면 안됩니다. 무조건 DTO변환해서 DTO를 캐시해야함.
// 요즘 네트워크 성능좋아서 엔티티랑 DTO방식이랑 별 차이 안남.
@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;
// 양방향은 @JsonIgnore 꼭 해주기

    /**
     * 엔티티 직접 노출
     */
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1(){
        List<Order> all = orderRepository.findAllByCriteria(new OrderSearch());
        for (Order order : all) {
            // 객체 그래프 초기화 (Hibernate5JakartaModule 때문에 프록시출력하려면 초기화해야됨.)
            order.getMember().getName();
            order.getDelivery().getAddress();
            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName());
        }
        return all;
    }

    /**
     * 엔티티를 DTO로 변환
     * 기존에는 order를 Dto로 변환해도 OrderItems가 엔티티라서 그냥 가져오면 null나옴(프록시)
     * dto안에 엔티티 있어도 안됨. 아예 의존을 끊어야함. (address같은 값타입은 괜찮음)
     */
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2(){
        List<Order> orders = orderRepository.findAllByCriteria(new OrderSearch());
        List<OrderDto> collect = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return collect;
    }

    /**
     * 하이버네이트6에서는 패치조인을 하면 자동으로 중복제거를 해준다.(distinct안써도됨)
     * 어마어마한 단점 : 컬렉션 패치 조인은 페이징이 불가능하다. 1:다 관계는 하지마시오.
     * outofMemory터집니다
     *
     * 페이징 안쓰고 data 얼마 없을때는 이렇게 해도 상관은 없다. 빠르다.
     */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3(){
       List<Order> orders = orderRepository.findAllWithItem();
        List<OrderDto> collect = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return collect;
    }

    /**
     * 페이징 써야할 경우 이 방법 선호.
     * ToOne관계는 joinfetch와 페이징 가능.
     * ToMany관계는 지연로딩 + @BatchSize(size=1000) 또는 글로벌 세팅
     *  yml파일에 default_batch_fetch_size: 100~1000 사이를 권장합니다.
     *  그런데 1000개 하면 DB,애플리케이션 순간 부하가 확 올 수 있다.
     *  was,DB 순간부하 걱정되면 100~500으로 쓰기
     *
     *  90%이상의 성능최적화는 이 level에서 해결된다.
     */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(
            @RequestParam(value = "offset",defaultValue = "0") int offset,
            @RequestParam(value = "limit",defaultValue = "100") int limit)
    {
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset,limit);

        List<OrderDto> collect = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return collect;
    }

    /**
     * DTO 직접 조회
     * toOne은 조인
     * toMany는 조인하지않고 루프돌면서 직접 채운다. (n+1문제 발생)
     * 특정 주문 단건 조회시에는 유용하다.
     * 예를들어 Order데이터가 1건이면 OrderItems도 1번만 조회한다.
     */
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4(){
        return orderQueryRepository.findOrderQueryDtos();
    }

    /**
     * in Query로 쿼리 두방 끝.(정규화된 데이터 가지고 옴)
     * 데이터를 한꺼번에 처리할 때 많이 사용하는 방식
     * 장점 : data select 양이 줄어듬, 성능 효과 좋음, 페이징도 가능,
     * 단점 : 코딩양이 많음.
     * 근데 이게 뭐 배치사이즈 적용하는거랑 다를바 없다.
     */
    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5(){
        return orderQueryRepository.findAllByDto_optimization();
    }

    /**
     * 쿼리 진짜 한번만 나감 대신 네트워크 전송량이 많음.
     * 데이터가 엄청 큰 경우 상황에 따라 V5보다 더 느릴 수 도 있다.
     * 대신 페이징 불가능, 애플리케이션 추가 작업 필요하다.
     * 이 부분은 난이도가 좀 있으니 우선 이런게 있구나 정도 알아두시고, 향후 성능 최적화의 필요성이 느껴질 때 다시 참고하시면 됩니다.
     */
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6(){
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();
        //개발자가 지지고 볶아서 한줄로 나온걸 개발자가 직접 분해하고 조립하면 된다.
        return flats.stream()
                .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(), o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        mapping(o -> new OrderItemQueryDto(o.getOrderId(), o.getItemName(), o.getOrderPrice(), o.getCount()), toList())
                )).entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(), e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(), e.getKey().getAddress(), e.getValue()))
                .collect(toList());
    }


    @Getter
    static class OrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        /*private List<OrderItem> orderItems; 이것조차 dto로 바꿔야함.*/
        private List<OrderItemDto> orderItems;


        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            /*order.getOrderItems().stream().forEach(o -> o.getItem().getName());*/
            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(toList());

        }
    }
    @Getter
    static class OrderItemDto{
        // 필요한 스펙 넣어주기
        private String itemName; // 상품명
        private int orderPrice; // 주문 가격
        private int count; //주문 수량
        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }

}
