package com.ssafy.partylog.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.partylog.api.Entity.UserEntity;
import com.ssafy.partylog.api.repository.UserRepository;
import com.ssafy.partylog.api.request.UserRequest;
import com.ssafy.partylog.api.response.UserSearchResponse;
import com.ssafy.partylog.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Value("${KAKAO_CLIENT_ID}")
    private String CLIENT_ID;
    @Value("${REDIRECT_URI}")
    private String REDIRECT_URI;
    @Value("${CLIENT_SECRET}")
    private String CLIENT_SECRET;
    @Value("${JWT_SECRETKEY}")
    private String JWT_SECRET_KEY;

    @Override
    @Transactional
    public UserEntity searchKakaoAccessToken(String code) throws Exception {
        String access_Token = "";
        String refresh_Token = "";
        String reqURL = "https://kauth.kakao.com/oauth/token";
        String result = "";
        UserEntity user = null;

        URL url = new URL(reqURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        // POST 요청을 위해 기본값이 false인 setDoOutput을 true로
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        // POST 요청에 필요로 요구하는 파라미터 스트림을 통해 전송
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
        StringBuilder sb = new StringBuilder();
        sb.append("grant_type=authorization_code");
        sb.append("&client_id=" + CLIENT_ID); // REST_API키 본인이 발급받은 key 넣어주기
        sb.append("&redirect_uri=" + REDIRECT_URI); // REDIRECT_URI 본인이 설정한 주소 넣어주기
        sb.append("&client_secret=" + CLIENT_SECRET); // REDIRECT_URI 본인이 설정한 주소 넣어주기
        sb.append("&code=" + code);
        bw.write(sb.toString());
        bw.flush();

        // 요청을 통해 얻은 JSON타입의 Response 메세지 읽어오기
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line = "";
        while ((line = br.readLine()) != null) {
            result += line;
        }
        br.close();
        bw.close();

        // jackson objectmapper 객체 생성
        ObjectMapper objectMapper = new ObjectMapper();
        // JSON String -> Map
        Map<String, Object> jsonMap = objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {
        });

        access_Token = jsonMap.get("access_token").toString();
        refresh_Token = jsonMap.get("refresh_token").toString();

        log.info("카카오 access_token: {}", access_Token);
        log.info("카카오 refresh_token: {}", refresh_Token);

        // 카카오 토큰을 이용하여 카카오 유저 정보 가져오기
        user = searchKakaoUserInfo(access_Token);

        return user;
    }

    @Override
    public UserEntity searchKakaoUserInfo(String access_Token) throws Exception {
        String kakaoId = "";
        String reqURL = "https://kapi.kakao.com/v2/user/me";

        URL url = new URL(reqURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        // 요청에 필요한 Header에 포함될 내용
        conn.setRequestProperty("Authorization", "Bearer " + access_Token);

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        String line = "";
        String result = "";

        while ((line = br.readLine()) != null) {
            result += line;
        }
        br.close();

        // jackson objectmapper 객체 생성
        ObjectMapper objectMapper = new ObjectMapper();
        // JSON String -> Map
        HashMap<String, Object> userInfo = (HashMap<String, Object>) objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {});
        Map<String, Object> properties = (Map<String, Object>) userInfo.get("properties");
        kakaoId = userInfo.get("id").toString();

        // DB에 카카오 아이디가 없다면 저장
        Optional<UserEntity> userEntity = userRepository.findByUserId(kakaoId);
        UserEntity response = null;
        if(!userEntity.isPresent()) {
            UserEntity request = UserEntity.builder()
                    .userId(kakaoId)
                    .userNickname((String) properties.get("nickname"))
                    .userProfile((String) properties.get("profile_image"))
                    .build();
            response = userRepository.save(request);
        } else {
            response = userEntity.get();
        }
        return response;
    }

    @Override
    public boolean join(UserRequest userRequest) throws Exception {
        try {
            userRepository.findByUserNo(userRequest.getUserNo()).ifPresent(item -> {
                item.setUserBirthday(userRequest.getUserBirthday());
                userRepository.save(item);
            });
        } catch (Exception e) {
            log.error("회원가입 실패했습니다.");
            e.printStackTrace();
            return false;
        }
        log.info("회원가입 성공");
        return true;
    }

    @Override
    public String createToken(int userNo, String type) throws Exception {
        long tokenValidTime = 0L;
        if(type.equals("access-token")) {
            tokenValidTime = 30 * 60 * 1000L; // Access 토큰 유효시간 30분
//            tokenValidTime = 2 * 60 * 1000L; // Access 토큰 유효시간 30분
        } else {
            tokenValidTime = 14 * 24 * 60 * 60 * 1000L; // Access 토큰 유효시간 2주
//            tokenValidTime = 5* 60 * 1000L; // Access 토큰 유효시간 2주
        }
        return JwtUtil.createJwt(userNo, type, JWT_SECRET_KEY, tokenValidTime);
    }

    @Override
    public void saveRefreshToken(int userNo, String refreshToken) throws Exception {
        Optional<UserEntity> userEntity = userRepository.findByUserNo(userNo);
        UserEntity user = UserEntity.builder()
                .userNo(userEntity.get().getUserNo())
                .userId(userEntity.get().getUserId())
                .userNickname(userEntity.get().getUserNickname())
                .userBirthday(userEntity.get().getUserBirthday())
                .userProfile(userEntity.get().getUserProfile())
                .Wrefreshtoken(refreshToken)
                .build();
        userRepository.save(user);
    }

    @Override
    public UserEntity searchUserInfoByUserNo(int userNo) throws Exception {
        return userRepository.findByUserNo(userNo).get();
    }

    @Override
    public String searchRefreshToken(String requestToken) throws Exception {
        String userNo = JwtUtil.getUserNo(requestToken, JWT_SECRET_KEY);
        Optional<UserEntity> user = userRepository.findByUserNo(Integer.parseInt(userNo));
        String DBRefreshToken = user.get().getWrefreshtoken();
        if(requestToken.equals(DBRefreshToken)) {
            return createToken(Integer.parseInt(userNo), "access-token");
        }
        return null;
    }

    @Override
    public boolean logout(int userNo) throws Exception {
        try {
            userRepository.findByUserNo(userNo).ifPresent(item -> {
                item.setWrefreshtoken(null);
                userRepository.save(item);
            });
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public List<UserSearchResponse> searchUser(String userNickname, int userNo, int limit, int offset) {
        List<UserSearchResponse> list = userRepository.findUser(userNickname, userNo, limit, offset);
        return list;
    }
}
