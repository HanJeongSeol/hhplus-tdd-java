package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class PointServiceTest {

    // UserPointTable, PointHistoryTable의 Mock 객체 생성
    private final UserPointTable userPointTable = mock(UserPointTable.class);
    private final PointHistoryTable pointHistoryTable = mock(PointHistoryTable.class);

    // PointService 객체 생성. Mock으로 생성한 빈 껍데기 객체를 주입
    private final PointService pointService = new PointService(userPointTable, pointHistoryTable);

    @Nested // 기능별 구역을 구분하기 위한 어노테이션
    @DisplayName("포인트 충전 테스트")
    class ChargePointsTests {

        @Test
        @DisplayName("포인트 충전 성공")
        void _1000_포인트_충전시_포인트_잔액은_3000_포인트가_된다(){
            // given
            final Long userId = 1L;
            final Long amount = 1000L;
            final Long currentTime = System.currentTimeMillis();

            given(userPointTable.selectById(userId))
                    .willReturn(new UserPoint(userId, 2000L, currentTime));

            given(userPointTable.insertOrUpdate(eq(userId), eq(3000L)))
                    .willReturn(new UserPoint(userId, 3000L, System.currentTimeMillis()));

            // when
            UserPoint userPoint = pointService.chargePoints(userId, amount);


            // then
            assertThat(userPoint.point()).isEqualTo(3000L);

            verify(userPointTable, times(1)).insertOrUpdate(eq(userId), eq(3000L));
            verify(pointHistoryTable, times(1)).insert(eq(userId), eq(amount), eq(TransactionType.CHARGE), anyLong());
        }

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

    @Nested
    @DisplayName("포인트 사용 테스트")
    class UsePoints{

        @Test
        @DisplayName("포인트 사용 성공")
        void _1000_포인트_사용시_포인트_잔액은_2000_포인트가_된다(){
            // given
            final Long userId = 1L;
            final Long amount = 1000L;
            final Long currentTime = System.currentTimeMillis();

            given(userPointTable.selectById(userId))
                    .willReturn(new UserPoint(userId, 3000L, currentTime));

            given(userPointTable.insertOrUpdate(eq(userId), eq(2000L)))
                    .willReturn(new UserPoint(userId, 2000L, System.currentTimeMillis()));

            // when
            UserPoint userPoint = pointService.usePoints(userId, amount);

            // then
            assertThat(userPoint.point()).isEqualTo(2000L);

            verify(userPointTable, times(1)).insertOrUpdate(eq(userId), eq(2000L));
            verify(pointHistoryTable, times(1)).insert(eq(userId), eq(amount), eq(TransactionType.USE), anyLong());
        }
        @Test
        @DisplayName("사용자 아이디와 사용 포인트가 전달되지 않을 시 요청 실패.")
        void 사용자_아이디_사용_포인트_누락_실패() {
            // given
            final Long userId = null;
            final Long amount = null;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.usePoints(userId, amount)
            );
            assertEquals("사용자 아이디와 사용 포인트는 필수입니다.", exception.getMessage());
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
        @DisplayName("포인트 사용 요청값이 0인 경우 요청 실패.")
        void 사용_포인트_0_실패() {
            // given
            final Long userId = 1L;
            final Long amount = 0L;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.usePoints(userId, amount)
            );
            assertEquals("사용 포인트는 0보다 커야 합니다.", exception.getMessage());
        }


        @Test
        @DisplayName("포인트 사용 요청값이 1,000 미만일 경우 요청 실패.")
        void 사용_포인트_최소값_미만_실패() {
            // given
            final Long userId = 1L;
            final Long amount = 999L;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.usePoints(userId, amount)
            );
            assertEquals("사용 포인트는 최소 1000 이상이어야 합니다.", exception.getMessage());
        }

        @Test
        @DisplayName("포인트 사용 요청값이 1,000,000 초과인 경우 요청 실패.")
        void 사용_포인트_최대값_초과_실패() {
            // given
            final Long userId = 1L;
            final Long amount = 1_000_001L;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.usePoints(userId, amount)
            );
            assertEquals("사용 포인트는 최대 1000000 이하여야 합니다.", exception.getMessage());
        }

        @Test
        @DisplayName("포인트 사용 요청값이 보유한 포인트 잔액을 초과하는 경우 요청 실패.")
        void 사용_포인트_잔액_초과_실패() {
            // given
            final Long userId = 1L;
            final Long amount = 2000L;

            // Mock 객체의 userPointTable.selectById() 반환값 설정
            given(userPointTable.selectById(userId))
                    .willReturn(new UserPoint(userId, 1000L, System.currentTimeMillis()));

            // when & then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> pointService.usePoints(userId, amount)
            );
            assertEquals("사용 포인트가 보유한 잔액을 초과할 수 없습니다.", exception.getMessage());
        }

        @Test
        @DisplayName("요청 실패시 포인트 사용 내역이 저장되지 않는다.")
        void 사용_실패시_내역_저장_금지() {
            // given
            final Long userId = 1L;
            final Long amount = 2000L;

            // Mock 객체의 메서드 반환값 설정
            given(userPointTable.selectById(userId))
                    .willReturn(new UserPoint(userId, 1000L, System.currentTimeMillis()));

            // when: 요청 실패
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> pointService.usePoints(userId, amount)
            );

            // then: PointHistoryTable.insert()가 호출되지 않아야 함
            assertEquals("사용 포인트가 보유한 잔액을 초과할 수 없습니다.", exception.getMessage());
            verify(pointHistoryTable, times(0)).insert(userId, amount, TransactionType.USE, System.currentTimeMillis());
        }

    }
    @Nested
    @DisplayName("포인트 조회 테스트")
    class GetPoints {

        @Test
        @DisplayName("포인트 조회 성공")
        void 포인트_조회_성공시_2000_포인트를_반환한다() {
            // given
            final Long userId = 1L;

            given(userPointTable.selectById(userId))
                    .willReturn(new UserPoint(userId, 2000L, System.currentTimeMillis()));

            // when
            UserPoint userPoint = pointService.getPoints(userId);

            // then
            assertThat(userPoint.point()).isEqualTo(2000L);

            verify(userPointTable, times(1)).selectById(eq(userId));
        }

        @Test
        @DisplayName("사용자 아이디가 전달되지 않을 시 포인트 조회 요청 실패.")
        void 포인트_조회_아이디_누락_실패() {
            // given
            final Long userId = null;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.getPoints(userId)
            );
            assertEquals("사용자 아이디는 필수입니다.", exception.getMessage());
        }

        @Test
        @DisplayName("사용자 아이디가 음수인 경우 포인트 조회 요청 실패.")
        void 포인트_조회_아이디_음수_실패() {
            // given
            final Long userId = -1L;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.getPoints(userId)
            );
            assertEquals("사용자 아이디는 음수가 될 수 없습니다.", exception.getMessage());
        }

        @Test
        @DisplayName("사용자 아이디가 0인 경우 포인트 조회 요청 실패.")
        void 포인트_조회_아이디_0_실패() {
            // given
            final Long userId = 0L;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.getPoints(userId)
            );
            assertEquals("사용자 아이디는 0일 수 없습니다.", exception.getMessage());
        }
    }
    @Nested
    @DisplayName("포인트 내역 조회 테스트")
    class GetPointHistories {

        @Test
        @DisplayName("포인트 내역 조회 성공")
        void 포인트_내역_조회_성공시_두개의_내역_정보를_반환한다() {
            // given
            final Long userId = 1L;
            final Long currentTime = System.currentTimeMillis();

            given(pointHistoryTable.selectAllByUserId(userId))
                    .willReturn(List.of(
                            new PointHistory(1L, userId, 3000L, TransactionType.CHARGE, currentTime),
                            new PointHistory(2L, userId, 1000L, TransactionType.USE, currentTime)
                    ));

            // when
            List<PointHistory> histories = pointService.getPointHistories(userId);

            // then - 반환 리스트 크기, 내역에 저장된 금액, 내역 타입 검증
            assertThat(histories).hasSize(2);
            assertThat(histories.get(0).amount()).isEqualTo(3000L);
            assertThat(histories.get(0).type()).isEqualTo(TransactionType.CHARGE);
            assertThat(histories.get(1).amount()).isEqualTo(1000L);
            assertThat(histories.get(1).type()).isEqualTo(TransactionType.USE);

            verify(pointHistoryTable, times(1)).selectAllByUserId(eq(userId));
        }

        @Test
        @DisplayName("사용자 아이디가 전달되지 않을 시 포인트 내역 조회 요청 실패.")
        void 포인트_내역_조회_아이디_누락_실패() {
            // given
            final Long userId = null;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.getPointHistories(userId)
            );
            assertEquals("사용자 아이디는 필수입니다.", exception.getMessage());
        }

        @Test
        @DisplayName("사용자 아이디가 음수인 경우 포인트 내역 조회 요청 실패.")
        void 포인트_내역_조회_아이디_음수_실패() {
            // given
            final Long userId = -1L;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.getPointHistories(userId)
            );
            assertEquals("사용자 아이디는 음수가 될 수 없습니다.", exception.getMessage());
        }

        @Test
        @DisplayName("사용자 아이디가 0인 경우 포인트 내역 조회 요청 실패.")
        void 포인트_내역_조회_아이디_0_실패() {
            // given
            final Long userId = 0L;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.getPointHistories(userId)
            );
            assertEquals("사용자 아이디는 0일 수 없습니다.", exception.getMessage());
        }
    }
}
