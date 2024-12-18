package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private static final long MAX_POINTS = 1_000_000L;  // 포인트 최대 한도
    private static final long MIN_AMOUNT = 1_000L;   // 최소 충전, 사용 포인트
    private static final long MAX_AMOUNT = 1_000_000L;  // 최대 충전, 사용 포인트

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;


    /**
     * 포인트 충전 비즈니스 로직
     * @param userId    사용자 아이디
     * @param amount    충전 포인트
     * @return  UserPoint
     */
    public UserPoint chargePoints(Long userId, Long amount){

        // 1. 아이디 및 충전 포인트 누락 확인
        if (userId == null || amount == null){
            throw new IllegalArgumentException("사용자 아이디와 충전 포인트는 필수입니다.");
        }

        // 2. 사용자 아이디가 음수인 경우
        if (userId < 0) {
            throw new IllegalArgumentException("사용자 아이디는 음수가 될 수 없습니다.");
        }

        // 3. 사용자 아이디가 0인 경우
        if (userId == 0) {
            throw new IllegalArgumentException("사용자 아이디는 0일 수 없습니다.");
        }

        // 4. 충전 포인트가 음수인 경우
        if (amount < 0){
            throw new IllegalArgumentException("충전 포인트는 음수가 될 수 없습니다.");
        }
        // 5. 충전 포인트가 0인 경우
        if (amount == 0){
            throw new IllegalArgumentException("충전 포인트는 0일 수 없습니다.");
        }

        // 6. 충전 요청값이 1,000 미만인 경우
        if (amount < MIN_AMOUNT) {
            throw new IllegalArgumentException("충전 포인트는 최소 " + MIN_AMOUNT + " 이상이어야 합니다.");
        }

        // 7. 충전 요청값이 1,000,000인 경우
        if (amount > MAX_AMOUNT) {
            throw new IllegalArgumentException("충전 포인트는 최대 " + MAX_AMOUNT + " 이하여야 합니다.");
        }

        // 기존 사용자 포인트 조회
        UserPoint currentPoint = userPointTable.selectById(userId);

        // 충전 후 포인트 계산
        long resultPoint = currentPoint.point() + amount;

        // 8. 충전 후 포인트 잔액이 최대치를 초과한 경우
        if(resultPoint > MAX_POINTS){
            throw new RuntimeException("충전 후 포인트 잔액이 "+MAX_POINTS+ "을 초과하면 실패한다.");
        }

        // 포인트 업데이트
        UserPoint updateUserPoint = userPointTable.insertOrUpdate(userId, resultPoint);

        // 충전 내역 저장
        pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

        return updateUserPoint;
    }
}
