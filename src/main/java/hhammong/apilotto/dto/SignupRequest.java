package hhammong.apilotto.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    private String userUid;
    private String password;
    private String name;
    private String nickname;

}
