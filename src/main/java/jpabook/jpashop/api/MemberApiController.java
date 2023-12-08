package jpabook.jpashop.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    @GetMapping("/api/v1/members") //회원정보 검색 api를 했는데, 주문정보까지 다 나와버리는 참사
    public List<Member> membersV1(){
        return memberService.findMembers();
    }
    @GetMapping("/api/v2/members")
    public Result memberV2(){
        List<Member> findMembers = memberService.findMembers();
        List<MemberDto> collect = findMembers.stream()
                .map(m -> new MemberDto(m.getName()))
                .collect(Collectors.toList());

        return new Result(collect);  //data필드의 값을 Dto로 넣어준다.
                                    // 이렇게하면 나중에 api 반환값 요구사항 새로 추가됐을때
                                    //Result의 필드만 추가하면 구현가능!(ex. count)
        /*return new Result(collect.size(),collect);*/
    }

    @Data
    @AllArgsConstructor // 껍데기클래스 생성
    static class Result<T> {
        /*private int count;*/
        private T data;
    }
    @Data
    @AllArgsConstructor //api스펙이 Dto랑 1:1로 매핑됨. 필요한거만 노출가능.
    static class MemberDto {
        private String name;
    }


    @PostMapping("/api/v1/members") // 회원가입,엔티티로 받으면 엔티티 바뀔때 api스펙도 바껴서 오류가능성높음.
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member){
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request){
        Member member = new Member();
        member.setName(request.getName());

        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    @PutMapping("/api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(@PathVariable("id") Long id,
                                               @RequestBody @Valid UpdateMemberRequest request){
        //커맨드랑 쿼리 분리해서 유지보수성 증대시키는 스타일임.
        memberService.update(id,request.getName());
        Member findMember = memberService.findOne(id);
        return new UpdateMemberResponse(findMember.getId(), findMember.getName());

    }

    @Data //api용 DTO, 이렇게하면 api스펙 까지 않아도 뭐가 넘어오는지 알 수 있음.절대 엔티티로 바로 사용하지 마시오
    static class CreateMemberRequest{
        @NotEmpty //@Valid가 검증해서 api 에러 메세지 전송함.
        private String name;
    }

    @Data
    static class CreateMemberResponse{
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }

    @Data
    static class UpdateMemberRequest {
        private String name;

    }
    @Data
    @AllArgsConstructor
    static class UpdateMemberResponse {
        private Long id;
        private String name;
    }
}
