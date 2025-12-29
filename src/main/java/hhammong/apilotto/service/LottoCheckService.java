package hhammong.apilotto.service;

import hhammong.apilotto.dto.CheckWinningRequest;
import hhammong.apilotto.dto.CheckWinningResponse;
import hhammong.apilotto.entity.LottoHistory;
import hhammong.apilotto.exception.ResourceNotFoundException;
import hhammong.apilotto.repository.LottoHistoryRepository;
import hhammong.apilotto.util.LottoMatchUtils;
import hhammong.apilotto.util.LottoMatchUtils.MatchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LottoCheckService {

    private final LottoHistoryRepository lottoHistoryRepository;

    /**
     * 특정 회차 당첨 확인
     */
    public CheckWinningResponse checkWinning(CheckWinningRequest request) {
        // 1. 회차 조회 (없으면 최신 회차)
        LottoHistory draw = (request.getDrawNo() != null)
                ? lottoHistoryRepository.findByDrawNoAndDeleteYnAndUseYn(
                        request.getDrawNo(), "N", "Y")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "해당 회차를 찾을 수 없습니다: " + request.getDrawNo()))
                : lottoHistoryRepository.findTopByDeleteYnAndUseYnOrderByDrawNoDesc("N", "Y")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "당첨번호가 존재하지 않습니다"));

        // 2. 당첨번호 리스트 생성
        List<Integer> winningNumbers = Arrays.asList(
                draw.getNumber1().intValue(),
                draw.getNumber2().intValue(),
                draw.getNumber3().intValue(),
                draw.getNumber4().intValue(),
                draw.getNumber5().intValue(),
                draw.getNumber6().intValue()
        );

        // 3. 매칭 계산
        MatchResult result = LottoMatchUtils.calculateMatch(
                request.getMyNumbers(),
                winningNumbers,
                draw.getBonusNumber().intValue()
        );

        // 4. 당첨금 계산
        Long prizeAmount = getPrizeAmount(draw, result.getRank());

        // 5. 응답 생성
        return CheckWinningResponse.builder()
                .drawNo(draw.getDrawNo())
                .myNumbers(request.getMyNumbers())
                .winningNumbers(winningNumbers)
                .bonusNumber(draw.getBonusNumber().intValue())
                .matchCount(result.getMatchCount())
                .hasBonus(result.isHasBonus())
                .rank(result.getRank())
                .rankDescription(result.getRankDescription())
                .prizeAmount(prizeAmount)
                .message(generateMessage(result))
                .build();
    }

    /**
     * 등수별 당첨금 조회
     */
    private Long getPrizeAmount(LottoHistory draw, Integer rank) {
        if (rank == null) return 0L;

        return switch (rank) {
            case 1 -> draw.getPrize1st();
            case 2 -> draw.getPrize2nd();
            case 3 -> draw.getPrize3rd();
            case 4 -> draw.getPrize4th() != null ? draw.getPrize4th().longValue() : 50000L;
            case 5 -> draw.getPrize5th() != null ? draw.getPrize5th().longValue() : 5000L;
            default -> 0L;
        };
    }

    /**
     * 결과 메시지 생성
     */
    private String generateMessage(MatchResult result) {
        if (!result.isWinning()) {
            return "아쉽게도 당첨되지 않았습니다.";
        }

        return String.format("축하합니다! %s에 당첨되었습니다! (일치: %d개%s)",
                result.getRankDescription(),
                result.getMatchCount(),
                result.isHasBonus() ? " + 보너스" : ""
        );
    }
}