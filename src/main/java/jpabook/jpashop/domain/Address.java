package jpabook.jpashop.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@AllArgsConstructor

public class Address {

    private String city;
    private String street;
    private String zipcode;

    protected Address() {
    }
}
