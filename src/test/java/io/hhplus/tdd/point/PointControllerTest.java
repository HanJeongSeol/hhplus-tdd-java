package io.hhplus.tdd.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointController.class)
public class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PointService pointService; // PointService를 Mock으로 등록

    @Test
    @DisplayName("포인트 충전 성공")
    void 포인트_충전_성공() throws Exception{
        // given
        Long userId = 1L;
        Long amount = 1500L;

        // when
        given(pointService.chargePoints(userId, amount))
                .willReturn(new UserPoint(userId, amount, System.currentTimeMillis()));

        // then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(amount));

    }

    @Test
    @DisplayName("포인트 충전 요청값이 음수인 경우 실패")
    void 포인트_충전_음수_실패() throws Exception {
        // given
        Long userId = 1L;
        Long amount = -1000L;

        // when
        given(pointService.chargePoints(userId, amount))
                .willReturn(new UserPoint(userId, amount, System.currentTimeMillis()));

        // when & then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(amount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("충전 포인트는 음수가 될 수 없습니다."));
    }
}
