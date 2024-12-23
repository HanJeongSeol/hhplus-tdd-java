package io.hhplus.tdd.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc   // MockMvc 객체 자동 구성
public class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PointService pointService; // PointService를 Mock 객체로 주입

    @Autowired
    private ObjectMapper objectMapper; // JSON 변환용 ObjectMapper, 데이터 전달 테스트 시 사용

    @Test
    @DisplayName("GET /point/{id} 요청 시 포인트 정보를 반환해야 한다.")
    void 포인트_조회_성공() throws Exception {
        // given
        long userId = 1L;
        long point = 1000L;
        long updateMillis = System.currentTimeMillis();

        // when
        given(pointService.getPoints(userId))
                .willReturn(new UserPoint(userId, point, updateMillis));

        // then
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk()) // 상태 코드 200 검증
                .andExpect(jsonPath("$.id").value(userId)) // JSON 응답의 id 검증
                .andExpect(jsonPath("$.point").value(point)) // JSON 응답의 point 검증
                .andExpect(jsonPath("$.updateMillis").value(updateMillis)); // JSON 응답의 updateMillis 검증
    }

    @Test
    @DisplayName("PATCH /point/{id}/charge 요청 시 포인트 충전 성공")
    void 포인트_충전_성공() throws Exception {
        // given
        long userId = 1L;
        long chargeAmount = 1000L;
        long newPoint = 2000L;
        long updateMillis = System.currentTimeMillis();

        UserPoint updatedUserPoint = new UserPoint(userId, newPoint, updateMillis);

        // when
        given(pointService.chargePoints(userId, chargeAmount))
                .willReturn(updatedUserPoint);

        // then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chargeAmount))) // 요청 본문에 JSON 객체로 데이터 전달
                .andExpect(status().isOk()) // 상태 코드 200 검증
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(newPoint));

    }

    @Test
    @DisplayName("PATCH /point/{id}/use 요청 시 포인트 사용 성공")
    void 포인트_사용_성공() throws Exception {
        // given
        long userId = 1L;
        long useAmount = 500L;
        long newPoint = 1500L;
        long updateMillis = System.currentTimeMillis();

        UserPoint updatedUserPoint = new UserPoint(userId, newPoint, updateMillis);

        // when
        given(pointService.usePoints(userId, useAmount))
                .willReturn(updatedUserPoint);

        // then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(useAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(newPoint))
                .andExpect(jsonPath("$.updateMillis").value(updateMillis));
    }

    @Test
    @DisplayName("GET /point/{id}/histories 요청 시 포인트 내역을 반환해야 한다.")
    void 포인트_내역_조회_성공() throws Exception {
        // given
        long userId = 1L;
        long currentTime = System.currentTimeMillis();

        PointHistory history1 = new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, currentTime);
        PointHistory history2 = new PointHistory(2L, userId, 2000L, TransactionType.USE, currentTime);

        // when
        given(pointService.getPointHistories(userId))
                .willReturn(List.of(history1, history2));

        // then
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk()) // 상태 코드 200 검증
                .andExpect(jsonPath("$.size()").value(2)) // JSON 응답의 내역 개수 검증
                .andExpect(jsonPath("$[0].amount").value(1000L)) // 첫 번째 내역의 금액 검증
                .andExpect(jsonPath("$[0].type").value(TransactionType.CHARGE.name())) // 첫 번째 내역의 타입 검증
                .andExpect(jsonPath("$[1].amount").value(2000L)) // 두 번째 내역의 금액 검증
                .andExpect(jsonPath("$[1].type").value(TransactionType.USE.name())); // 두 번째 내역의 타입 검증
    }
}