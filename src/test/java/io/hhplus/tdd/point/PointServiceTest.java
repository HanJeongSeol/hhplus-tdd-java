package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class PointServiceTest {

    // UserPointTable, PointHistoryTable의 Mock 객체 생성
    private final UserPointTable userPointTable = mock(UserPointTable.class);
    private final PointHistoryTable pointHistoryTable = mock(PointHistoryTable.class);

    // PointService 객체 생성. Mock으로 생성한 빈 껍데기 객체를 주입
    private final PointService pointService = new PointService(userPointTable, pointHistoryTable);

    @Test
    @DisplayName("사용자 아이디와 충전 포인트가 전달되지 않을 시 요청 실패.")
    void 사용자_아이디_충전_포인트_누락_실패() {

        // given - 사용자 아이디 및 포인트가 null인 경우
        final Long userId = null;
        final Long amount = null;

        // when & then - IllegalArgumentException 예외 발생 확인
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pointService.chargePoints(userId, amount)
        );
        // 예외 메시지 일치 확인
        assertEquals("사용자 아이디와 충전 포인트는 필수입니다.", exception.getMessage());

    }

    @Test
    @DisplayName("사용자 아이디가 음수인 경우 실패")
    void 사용자_아이디_음수_실패() {
        // given
        final Long userId = -1L;
        final Long amount = 1000L;

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pointService.chargePoints(userId, amount)
        );
        assertEquals("사용자 아이디는 음수가 될 수 없습니다.", exception.getMessage());

    }

    @Test
    @DisplayName("사용자 아이디가 0인 경우 요청 실패")
    void 사용자_아이디_0_실패() {
        // given
        final Long userId = 0L;
        final Long amount = 1000L;

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pointService.chargePoints(userId, amount)
        );
        assertEquals("사용자 아이디는 0일 수 없습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("포인트 충전 요청값이 음수인 경우 요청 실패")
    void 충전_포인트_음수_실패() {
        // given
        final Long userId = 1L;
        final Long amount = -1000L;

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pointService.chargePoints(userId, amount)
        );
        assertEquals("충전 포인트는 음수가 될 수 없습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("포인트 충전 요청값이 0인 경우 요청 실패")
    void 충전_포인트_0_실패() {
        // given
        final Long userId = 1L;
        final Long amount = 0L;

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pointService.chargePoints(userId, amount)
        );
        assertEquals("충전 포인트는 0일 수 없습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("포인트 충전 요청값이 1,000 미만일 경우 요청 실패 ")
    void 충전_포인트_최소값_미만_실패() {
        // given
        final Long userId = 1L;
        final Long amount = 999L;

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pointService.chargePoints(userId, amount)
        );
        assertEquals("충전 포인트는 최소 1000 이상이어야 합니다.", exception.getMessage());
    }

    @Test
    @DisplayName("포인트 충전 요청값이 1,000,000 초과인 경우 요청 실패")
    void 충전_포인트_최대값_초과_실패() {
        // given
        final Long userId = 1L;
        final Long amount = 1000001L;

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pointService.chargePoints(userId, amount)
        );
        assertEquals("충전 포인트는 최대 1000000 이하여야 합니다.", exception.getMessage());
    }

    @Test
    @DisplayName("충전 후 포인트 잔액이 1,000,000 초과인 경우 요청 실패")
    void 충전_포인트_총합_초과_실패() {
        // given
        final Long userId = 1L;
        final Long amount = 200000L;

        // Mock 객체의 메서드가 호출될 때 반환될 값 설정
        given(userPointTable.selectById(userId))
                .willReturn(new UserPoint(userId, 900000L, System.currentTimeMillis()));
        // when & then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> pointService.chargePoints(userId, amount)
        );
        assertEquals("충전 후 포인트 잔액이 1000000을 초과하면 실패한다.", exception.getMessage());
    }

    @Test
    @DisplayName("요청 실패 시 포인트 충전 내역은 저장되지 않는다.")
    void 충전_실패시_내역_저장_금지() {
        // given
        final Long userId = 1L;
        final Long amount = 200_000L;
        
        given(userPointTable.selectById(userId))
                .willReturn(new UserPoint(userId, 900_000L, System.currentTimeMillis()));

        // when
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> pointService.chargePoints(userId, amount)
        );

        // then
        assertEquals("충전 후 포인트 잔액이 1000000을 초과하면 실패한다.", exception.getMessage());
        // 요청 실패 시 내역을 저장하지 않기 때문에 PointHistoryTable.insert() 메서드가 호출되면 안된다.
        verify(pointHistoryTable, times(0)).insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

    }
}
