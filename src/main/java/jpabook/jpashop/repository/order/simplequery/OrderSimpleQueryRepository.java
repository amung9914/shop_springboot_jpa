package jpabook.jpashop.repository.order.simplequery;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderSimpleQueryRepository {

    private final EntityManager em;

    /**
     * v4버전 : 재사용성 x, 이 dto 쓸때만 사용 가능. Dto로 조회했기때문에 변경하거나 할 수 없다.
     * 장점 : v3보다는 성능 최적화(근데 생각보다 차이 안난다고 함)
     * 근데, repository인데 엔티티를 조회하는게 아니다? api스펙이 그냥 들어온것. 논리적으로 계층 깨진것임.
     * repository 패키지 밑에 order.simplequery패키지로 따로 뽑아서 repository의 순수성을 유지해주자
     *
     */
    public List<OrderSimpleQueryDto> findOrderDtos() {
        // new 명령어를 사용해서 DTO로 즉시 반환
        // 여기선 new 파라미터를(dto생성자 매개변수를) 엔티티로 넘기면 안됨, 엔티티 넘기면 연산자로 인식함
        return em.createQuery(
                        "select new jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto(o.id, m.name, o.orderDate, o.status, d.address) " +
                                "from Order o " +
                                "join o.member m " +
                                "join o.delivery d", OrderSimpleQueryDto.class)
                .getResultList();
    }

}
