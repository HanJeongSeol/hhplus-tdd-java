package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public class PointServiceConcurrencyTest {

    // 동시성 테스트를 위해 Mock이 아닌 구현된 Bean 사용
    private final UserPointTable userPointTable = new UserPointTable();
    private final PointHistoryTable pointHistoryTable = new PointHistoryTable();
    private final PointService pointService = new PointService(userPointTable, pointHistoryTable);

    final long USER_ID = 1L;

    @BeforeEach
    void 초기_충전() {
        pointService.chargePoints(USER_ID, 50_000L);
    }

    @Nested
    @DisplayName("동시성 문제 테스트")
    class ConcurrencyTest{
        @Test
        @DisplayName("1000포인트를 10번 동시 충전하면 최종 포인트는 60,000포인트가 되어야 한다.")
        void 동시_1000원_10번_충전_최종_60000원() throws InterruptedException {

            // given - 초기 상태 포인트 조회
            UserPoint prevUserPoint = pointService.getPoints(USER_ID);

            // 동시 실행할 쓰레드 수 설정
            int threadCount = 10;

            // 동시 작업 처리를 위한 쓰레드 풀 생성
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

            // 모든 쓰레드의 작업 완료를 대기하기 위한 CountDownLatch
            CountDownLatch latch = new CountDownLatch(threadCount);

            // when - 10개의 쓰레드에서 1000포인트 충전 요청
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        pointService.chargePoints(USER_ID, 1000L);
                    } finally {
                        latch.countDown(); // 쓰레드 작업 완료 후 카운트 다운
                    }
                });
            }

            // 모든 쓰레드가 작업을 마칠 때까지 대기한다.
            latch.await();
            executorService.shutdown();

            // then - 최종 포인트가 예상한 포인트와 일치하는지 검증
            UserPoint currentUserPoint = pointService.getPoints(USER_ID);
            assertThat(currentUserPoint.point()).isEqualTo(prevUserPoint.point() + 1000 * threadCount);
        }

        @Test
        @DisplayName("1000포인트를 10번 동시 사용하면 최종 포인트는 40,000포인트가 되어야 한다.")
        void 동시_1000원_10번_사용_최종_40000원() throws InterruptedException {
            // given
            UserPoint prevUserPoint = pointService.getPoints(USER_ID);

            int threadCount = 10;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            // when
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        pointService.usePoints(USER_ID, 1000L);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            // then
            UserPoint currentUserPoint = pointService.getPoints(USER_ID);
            assertThat(currentUserPoint.point()).isEqualTo(prevUserPoint.point() - 1000L * threadCount);
        }
    }

    @Test
    @DisplayName("1000포인트 10번, 2000포인트 사용을 1번 동시 요청하면 최종 포인트는 58,000원이 되어야 한다.")
    void 동시_충전_1000원_10번_사용_2000원_1번_최종_58000원() throws InterruptedException {
        // given
        UserPoint prevUserPoint = pointService.getPoints(USER_ID);

        int chargeThreadCount = 10; // 충전 요청 쓰레드 수
        int useThreadCount = 1;    // 사용 요청 쓰레드 수
        ExecutorService executorService = Executors.newFixedThreadPool(chargeThreadCount + useThreadCount);
        CountDownLatch latch = new CountDownLatch(chargeThreadCount + useThreadCount);

        // when
        for (int i = 0; i < chargeThreadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargePoints(USER_ID, 1000L);
                } finally {
                    latch.countDown();
                }
            });
        }

        executorService.submit(() -> {
            try {
                pointService.usePoints(USER_ID, 2000L);
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        executorService.shutdown();

        // then
        UserPoint currentUserPoint = pointService.getPoints(USER_ID);

        // 예상되는 최종 포인트 계산
        long expectedFinalPoint = prevUserPoint.point() + (1000 * chargeThreadCount) - 2000;
        assertThat(currentUserPoint.point()).isEqualTo(expectedFinalPoint);

    }
}
