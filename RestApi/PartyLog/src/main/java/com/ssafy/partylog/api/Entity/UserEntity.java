package com.ssafy.partylog.api.Entity;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="user")
@Getter
@ToString
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="user_no")
    private int userNo;
    @Column(name="user_id")
    private String userId;
    @Column(name="user_birthday")
    private Date userBirthday;
    @Column(name="user_nickname")
    private String userNickname;
    @Column(name="user_profile")
    private String userProfile;
    @Column(name="W_refreshtoken")
    private String Wrefreshtoken;
    @Column(name="A_refreshtoken")
    private String Arefreshtoken;

    @Builder
    public UserEntity(int userNo, String userId, Date userBirthday, String userNickname, String userProfile, String Wrefreshtoken, String Arefreshtoken) {
        this.userNo = userNo;
        this.userId = userId;
        this.userBirthday = userBirthday;
        this.userNickname = userNickname;
        this.userProfile = userProfile;
        this. Wrefreshtoken = Wrefreshtoken;
        this.Arefreshtoken = Arefreshtoken;
    }

    // 기본 생성자 없으면 빨간줄 떠서 넣음
    public UserEntity() {}
}
