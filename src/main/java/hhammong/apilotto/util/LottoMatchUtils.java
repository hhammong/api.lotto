package hhammong.apilotto.util;

import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LottoMatchUtils {

    /**
     * 두 번호 배열의 일치 개수 계산
     */
    public static int countMatches(List<Integer> myNumbers, List<Integer> winningNumbers) {
        Set<Integer> winSet = new HashSet<>(winningNumbers);

        return (int) myNumbers.stream()
                .filter(winSet::contains)
                .count();
    }

    /**
     * 일치하는 번호들 반환 (NEW!)
     */
    public static List<Integer> getMatchedNumbers(List<Integer> myNumbers, List<Integer> winningNumbers) {
        Set<Integer> winSet = new HashSet<>(winningNumbers);
        return myNumbers.stream()
                .filter(winSet::contains)
                .sorted()
                .toList();
    }

    /**
     * 보너스 번호 일치 여부
     */
    public static boolean hasBonusMatch(List<Integer> myNumbers, Integer bonusNumber) {
        return myNumbers.contains(bonusNumber);
    }

    /**
     * 등수 판정
     * @param matchCount 일치 개수
     * @param hasBonus 보너스 일치 여부
     * @return 등수 (1~5, null이면 꽝)
     */
    public static Integer getRank(int matchCount, boolean hasBonus) {
        if (matchCount == 6) {
            return 1;  // 1등
        } else if (matchCount == 5 && hasBonus) {
            return 2;  // 2등
        } else if (matchCount == 5) {
            return 3;  // 3등
        } else if (matchCount == 4) {
            return 4;  // 4등
        } else if (matchCount == 3) {
            return 5;  // 5등
        }
        return null;  // 꽝
    }

    /**
     * 매칭 결과 통합 계산
     */
    public static MatchResult calculateMatch(
            List<Integer> myNumbers,
            List<Integer> winningNumbers,
            Integer bonusNumber) {

        int matchCount = countMatches(myNumbers, winningNumbers);
        boolean hasBonus = hasBonusMatch(myNumbers, bonusNumber);
        Integer rank = getRank(matchCount, hasBonus);

        return MatchResult.builder()
                .matchCount(matchCount)
                .hasBonus(hasBonus)
                .rank(rank)
                .build();
    }

    /**
     * 매칭 결과 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class MatchResult {
        private int matchCount;      // 일치 개수
        private boolean hasBonus;    // 보너스 일치 여부
        private Integer rank;        // 등수 (null이면 꽝)

        public boolean isWinning() {
            return rank != null;
        }

        public String getRankDescription() {
            if (rank == null) return "꽝";
            return rank + "등";
        }
    }
}