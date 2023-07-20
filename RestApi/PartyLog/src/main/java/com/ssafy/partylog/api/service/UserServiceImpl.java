package com.ssafy.partylog.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.partylog.api.Entity.User;
import com.ssafy.partylog.api.request.UserRequest;
import com.ssafy.partylog.api.repository.UserRepository;
import com.ssafy.partylog.api.response.UserResponse;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private static final String CLIENT_ID = "7e939f3c5936dc4bc43aa3fc00ca7717";
    private static final String REDIRECT_URI = "http://localhost:3000/kakaoCallback";
    private static final String CLIENT_SECRET = "ItCyhePKvJ6XBwnMgjWEsM4tiG9QDPqA";

    @Override
    public UserResponse registUser(UserRequest userInfo) throws Exception {
        User user = userInfo.toEntity();
        User userEntity = userRepository.save(user);
        UserResponse userResponse = new UserResponse(userEntity);
        return userResponse;
    }

    @Override
    public String getAccessToken(String code) throws Exception {
        String access_Token = "";
        String refresh_Token = "";
        String reqURL = "https://kauth.kakao.com/oauth/token";
        String result = "";
        try {
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

            // 결과 코드가 200이라면 성공
            int responseCode = conn.getResponseCode();
            System.out.println("responseCode : " + responseCode);

            // 요청을 통해 얻은 JSON타입의 Response 메세지 읽어오기
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = "";
            while ((line = br.readLine()) != null) {
                result += line;
            }
            System.out.println("response body : " + result);

            // jackson objectmapper 객체 생성
            ObjectMapper objectMapper = new ObjectMapper();
            // JSON String -> Map
            Map<String, Object> jsonMap = objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {
            });

            access_Token = jsonMap.get("access_token").toString();
            refresh_Token = jsonMap.get("refresh_token").toString();

            System.out.println("access_token : " + access_Token);
            System.out.println("refresh_token : " + refresh_Token);

            br.close();
            bw.close();

        } catch(IOException e) {
            e.printStackTrace();
        }
        return access_Token;
    }

    @Override
    public HashMap<String, Object> getUserInfo(String access_Token) throws Exception {
        // 요청하는 클라이언트마다 가진 정보가 다를 수 있기에 HashMap타입으로 선언
        HashMap<String, Object> userInfo = new HashMap<String, Object>();
        String reqURL = "https://kapi.kakao.com/v2/user/me";

        try {
            URL url = new URL(reqURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // 요청에 필요한 Header에 포함될 내용
            conn.setRequestProperty("Authorization", "Bearer " + access_Token);

            int responseCode = conn.getResponseCode();
            System.out.println("responseCode : " + responseCode);

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String line = "";
            String result = "";

            while ((line = br.readLine()) != null) {
                result += line;
            }
            System.out.println("response body : " + result);
            System.out.println("result type" + result.getClass().getName()); // java.lang.String

            try {
                // jackson objectmapper 객체 생성
                ObjectMapper objectMapper = new ObjectMapper();
                // JSON String -> Map
                Map<String, Object> jsonMap = objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {
                });

                System.out.println(jsonMap.get("properties"));

                Map<String, Object> properties = (Map<String, Object>) jsonMap.get("properties");
                Map<String, Object> kakao_account = (Map<String, Object>) jsonMap.get("kakao_account");

                // System.out.println(properties.get("nickname"));
                // System.out.println(kakao_account.get("email"));

                String nickname = properties.get("nickname").toString();
                String email = kakao_account.get("email").toString();

                userInfo.put("nickname", nickname);
                userInfo.put("email", email);

            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return userInfo;
    }
}