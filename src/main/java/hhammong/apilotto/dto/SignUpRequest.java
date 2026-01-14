package hhammong.apilotto.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpRequest {

    private String userUid;
    private String password;
    private String name;
    private String nickname;

}
