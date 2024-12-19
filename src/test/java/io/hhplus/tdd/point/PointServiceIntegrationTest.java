package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class PointServiceIntegrationTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointTable userPointTable;

    @Autowired
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void _1번_유저의_초기_포인트_50000_설정() {
        userPointTable.insertOrUpdate(1L, 50_000L); // 1번 유저의 초기 포인트 설정
    }

    @Nested
    @DisplayName("포인트 충전 통합 테스트")
    class ChargePointsTest {

        @Test
        @DisplayName("포인튼 충전 성공")
        void _10000_포인트_충전시_60000_포인트가_된다() {
            // given
            Long userId = 1L;
            Long amount = 10_000L;

            // when
            UserPoint userPoint = pointService.chargePoints(userId, amount);

            // then
            assertThat(userPoint.point()).isEqualTo(60_000L);
        }

        @Test
        @DisplayName("충전 요청 금액이 음수인 경우 포인트 충전 실패")
        void 충전_포인트가_음수인경우_IllegalArgumentException_발생() {
            // given
            Long userId = 1L;
            Long amount = -10_000L;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.chargePoints(userId, amount)
            );

            assertThat(exception.getMessage()).isEqualTo("충전 포인트는 음수가 될 수 없습니다.");
        }

        @Test
        @DisplayName("충전 요청 금액이 0인 경우 포인트 충전 실패")
        void 충전_포인트가_0인_경우_IllegalArgumentException_발생() {
            // given
            final Long userId = 1L;
            final Long amount = 0L;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.chargePoints(userId, amount)
            );
            assertThat(exception.getMessage()).isEqualTo("충전 포인트는 0일 수 없습니다.");
        }

        @Test
        @DisplayName("충전 요청 금액이 1,000 미만인 경우 포인트 충전 실패")
        void 충전_포인트가_1000_미마인_경우_IllegalArgumentException_발생() {
            // given
            long userId = 1L;
            long invalidAmount = 999L;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.chargePoints(userId, invalidAmount)
            );
            assertThat(exception.getMessage()).isEqualTo("충전 포인트는 최소 1000 이상이어야 합니다.");
        }

        @Test
        @DisplayName("충전 요청 금액이 1,000,000 초과인 경우 포인트 충전 실패")
        void 충전_포인트가_1000000_초과인_경우_IllegalArgumentException_발생() {
            // given
            long userId = 1L;
            long invalidAmount = 1_000_001L;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.chargePoints(userId, invalidAmount)
            );
            assertThat(exception.getMessage()).isEqualTo("충전 포인트는 최대 1000000 이하여야 합니다.");
        }

        @Test
        @DisplayName("포인트 충전 후 잔액이 1,000,000 초과한 경우 충전 실패")
        void 충전_후_잔액이_1000000_초과인_경우_RuntimeException_발생() {
            // given
            long userId = 1L;
            long amount = 999_999L;

            // when & then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> pointService.chargePoints(userId, amount)
            );
            assertThat(exception.getMessage()).isEqualTo("충전 후 포인트 잔액이 1000000을 초과하면 실패한다.");
        }
    }

    @Nested
    @DisplayName("포인트 사용 통합 테스트")
    class UsePointsTest {

        @Test
        @DisplayName("포인트 사용 성공")
        void 포인트_사용_성공() {
            // given
            long userId = 1L;
            long amount = 10_000L;

            // when
            UserPoint updatedPoint = pointService.usePoints(userId, amount);

            // then
            assertThat(updatedPoint.point()).isEqualTo(40_000L);
        }

        @Test
        @DisplayName("사용 요청 금액이 음수인 경우 포인트 충전 실패")
        void 사용_포인트가_음수인경우_IllegalArgumentException_발생() {
            // given
            long userId = 1L;
            long amount = -10_000L;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.usePoints(userId, amount)
            );
            assertThat(exception.getMessage()).isEqualTo("사용 포인트는 음수가 될 수 없습니다.");
        }

        @Test
        @DisplayName("사용 요청 금액이 0인 경우 포인트 사용 실패")
        void 사용_포인트가_0인_경우_IllegalArgumentException_발생() {
            // given
            final Long userId = 1L;
            final Long amount = 0L;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.usePoints(userId, amount)
            );
            assertThat(exception.getMessage()).isEqualTo("사용 포인트는 0보다 커야 합니다.");
        }

        @Test
        @DisplayName("사용 요청 금액이 1,000 미만인 경우 포인트 사용 실패")
        void 사용_포인트가_1000_미마인_경우_IllegalArgumentException_발생() {
            // given
            long userId = 1L;
            long amount = 999L;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.usePoints(userId, amount)
            );
            assertThat(exception.getMessage()).isEqualTo("사용 포인트는 최소 1000 이상이어야 합니다.");
        }

        @Test
        @DisplayName("사용 요청 금액이 1,000,000 초과인 경우 포인트 사용 실패")
        void 사용_포인트가_1000000_초과인_경우_IllegalArgumentException_발생() {
            // given
            long userId = 1L;
            long amount = 1_000_001L;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.usePoints(userId, amount)
            );
            assertThat(exception.getMessage()).isEqualTo("사용 포인트는 최대 1000000 이하여야 합니다.");
        }

        @Test
        @DisplayName("포인트 사용 요청값이 보유한 포인트 잔액을 초과하는 경우 요청 실패")
        void 포인트_사용_요청값이_보유_포인트를_초과한_경우_RuntimeException_발생() {
            // given
            long userId = 1L;
            long amount = 600_000L;

            // when & then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> pointService.usePoints(userId, amount)
            );
            assertThat(exception.getMessage()).isEqualTo("사용 포인트가 보유한 잔액을 초과할 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("포인트 조회 통합 테스트")
    class GetPointsTest {
        @Test
        @DisplayName("포인트 조회 성공")
        void 포인트_조회_성공() {
            // given
            long userId = 1L;

            // when
            UserPoint userPoint = pointService.getPoints(userId);

            // then
            assertThat(userPoint.point()).isEqualTo(50_000L);
        }

        @Test
        @DisplayName("사용자 아이디가 null인 경우 포인트 조회 실패")
        void 사용자_아이디로_null값이_전달되면_IllegalArgumentException_발생() {
            // given
            Long userId = null;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.getPoints(userId)
            );
            assertThat(exception.getMessage()).isEqualTo("사용자 아이디는 필수입니다.");
        }

        @Test
        @DisplayName("사용자 아이디가 음수인 경우 포인트 조회 실패")
        void 사용자_아이디로_음수가_전달되면_IllegalArgumentException_발생() {
            // given
            long userId = -1L;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.getPoints(userId)
            );
            assertThat(exception.getMessage()).isEqualTo("사용자 아이디는 음수가 될 수 없습니다.");
        }

        @Test
        @DisplayName("사용자 아이디가 0인 경우 포인트 내역 조회 요청 실패.")
        void 사용자_아이디로_0이_전달되면_IllegalArgumentException_발생() {
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
    @DisplayName("포인트 내역 조회 통합 테스트")
    class GetPointHistoriesTests {
        @Test
        @DisplayName("포인트 내역 조회 성공")
        void 포인트_내역_조회_성공() {
            // given
            long userId = 1L;
            pointService.chargePoints(userId, 10_000L); // 내역 1 생성
            pointService.usePoints(userId, 5_000L);     // 내역 2 생성

            // when
            List<PointHistory> histories = pointService.getPointHistories(userId);

            // then
            assertThat(histories).hasSize(2);
        }

        @Test
        @DisplayName("사용자 아이디가 null인 경우 포인트 내역 조회 실패")
        void 사용자_아이디로_null값이_전달되면_IllegalArgumentException_발생() {
            // given
            Long userId = null;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.getPointHistories(userId)
            );
            assertThat(exception.getMessage()).isEqualTo("사용자 아이디는 필수입니다.");
        }

        @Test
        @DisplayName("사용자 아이디가 음수인 경우 포인트 내역 조회 실패")
        void 사용자_아이디로_음수가_전달되면_IllegalArgumentException_발생() {
            // given
            long userId = -1L;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pointService.getPointHistories(userId)
            );
            assertThat(exception.getMessage()).isEqualTo("사용자 아이디는 음수가 될 수 없습니다.");
        }

        @Test
        @DisplayName("사용자 아이디가 0인 경우 포인트 내역 조회 요청 실패.")
        void 사용자_아이디로_0이_전달되면_IllegalArgumentException_발생() {
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
