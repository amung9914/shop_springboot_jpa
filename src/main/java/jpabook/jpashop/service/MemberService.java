package jpabook.jpashop.service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true) // 읽기전용 모드로 최적화 해줌
@RequiredArgsConstructor //final키워드를 이용한 생성자 주입
public class MemberService {

    private final MemberRepository memberRepository;

    //회원 가입
    @Transactional // 디폴트가 읽기전용아님(공통보다 우선적용됨)
    public Long join(Member member){
        validateDuplicateMember(member); // 중복 회원 검증
        memberRepository.save(member);
        return member.getId(); // em.persist하면 영속성 컨텍스트에서 PK를 들고 있음(보장됨)
    }

    private void validateDuplicateMember(Member member) {
        // EXCEPTION, DB에도 member의 name을 유니크 제약조건을 추가핵서 중복 가입이 되지 않도록 한 번 더 검증해야함.
        List<Member> findMembers = memberRepository.findByName(member.getName());
        if(!findMembers.isEmpty()){
            throw new IllegalStateException("이미 존재하는 회원입니다.");
        }
    }

    //회원 전체 조회
    public List<Member> findMembers(){
        return memberRepository.findAll();
    }

    public Member findOne(Long memberId){
        return memberRepository.findOne(memberId);
    }
}
