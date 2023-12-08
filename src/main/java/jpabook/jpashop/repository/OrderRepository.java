package jpabook.jpashop.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final EntityManager em;

    public void save(Order order){
        em.persist(order);
    }

    public Order findOne(Long id){
        return em.find(Order.class, id);
    }

    public List<Order> findAllByCriteria(OrderSearch orderSearch) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> o = cq.from(Order.class);
        Join<Order, Object> m = o.join("member", JoinType.INNER); //회원과 조인
        List<Predicate> criteria = new ArrayList<>();
        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            Predicate status = cb.equal(o.get("status"),
                    orderSearch.getOrderStatus());
            criteria.add(status);
        }
        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            Predicate name =
                    cb.like(m.<String>get("name"), "%" +
                            orderSearch.getMemberName() + "%");
            criteria.add(name); }
        cq.where(cb.and(criteria.toArray(new Predicate[criteria.size()])));
        TypedQuery<Order> query = em.createQuery(cq).setMaxResults(1000); //최대 1000건
        return query.getResultList();
    }

    /**
     * v3버전 : 엔티티에 대한 순수성 유지중
     */
    public List<Order> findAllWithMemberDelivery() {
        // 패치조인 : LAZY무시하고 한방쿼리로 다 땡겨온다.
        return em.createQuery(
                "select o from Order o " +
                        "join fetch o.member m " +
                        "join fetch o.delivery d",Order.class
        ).getResultList();
    }


    /**
     * v4버전 : 재사용성 x, 이 dto 쓸때만 사용 가능. Dto로 조회했기때문에 변경하거나 할 수 없다.
     * 장점 : v3보다는 성능 최적화(근데 생각보다 차이 안난다고 함)
     * 근데, repository인데 엔티티를 조회하는게 아니다? api스펙이 그냥 들어온것. 논리적으로 계층 깨진것임.
     * repository 패키지 밑에 order.simplequery패키지로 따로 뽑음.
     * repository 패키지 밑에 order.simplequery패키지로 따로 뽑아서 repository의 순수성을 유지해주자
     */
    public List<OrderSimpleQueryDto> findOrderDtos() {
        // repository.order.simplequery로 코드 옮겼음.
        return null;
    }

    public List<Order> findAllWithItem() {
        // 실무에서는 querydsl로 간단하게 짤 수 있다.
        // 하이버네이트6에서는 패치조인을 하면 자동으로 중복제거를 해준다.(distinct안써도됨)
        // distinct넣어도 DB에서는 적용안됨(값이 완전히 똑같이 않으니까), JPA에서 리스트담을때 엔티티 중복제거하는 용도임.
        return em.createQuery(
                /*"select distinct o from Order o " +*/
                        "select o from Order o " +
                        "join fetch o.member m " +
                        "join fetch o.delivery d " +
                        "join fetch o.orderItems oi " +
                        "join fetch oi.item i",Order.class)
                // firstResult/maxResults specified with collection fetch; applying in memory
                // 다불러와서 페이징을 하겠다는거. outOfMemory 에러 터지기 딱 좋음.
                .setFirstResult(1)
                .setMaxResults(100)
                .getResultList();
    }

    /**
     * v3.1
     *
     * http://localhost:8080/api/v3.1/orders?offset=1&limit=100
     * ToOne관계는 joinfetch와 페이징 가능.
     * & yml파일에 default_batch_fetch_size: 배치크기 넣어주면 최적화 가능! 쿼리 3번만에 끝.
     */
    public List<Order> findAllWithMemberDelivery(int offset, int limit) {
        return em.createQuery(
                "select o from Order o " +
                        "join fetch o.member m " +
                        "join fetch o.delivery d",Order.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }
}
