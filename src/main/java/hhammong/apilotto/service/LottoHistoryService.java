package hhammong.apilotto.service;

import hhammong.apilotto.dto.LottoHistoryResponse;
import hhammong.apilotto.entity.LottoHistory;
import hhammong.apilotto.exception.ResourceNotFoundException;
import hhammong.apilotto.repository.LottoHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LottoHistoryService {

    private final LottoHistoryRepository lottoHistoryRepository;

    /**
     * 최신 회차 당첨번호 조회
     */
    public LottoHistoryResponse getLatestDraw() {
        LottoHistory latestDraw = lottoHistoryRepository
                .findTopByDeleteYnAndUseYnOrderByDrawNoDesc("N", "Y")
                .orElseThrow(() -> new ResourceNotFoundException("당첨번호가 존재하지 않습니다."));

        return LottoHistoryResponse.from(latestDraw);
    }

    /**
     * 특정 회차 당첨번호 조회
     */
    public LottoHistoryResponse getDrawByNumber(Integer drawNo) {
        LottoHistory draw = lottoHistoryRepository
                .findByDrawNoAndDeleteYnAndUseYn(drawNo, "N", "Y")
                .orElseThrow(() -> new ResourceNotFoundException("해당 회차를 찾을 수 없습니다: " + drawNo));

        return LottoHistoryResponse.from(draw);
    }

    /**
     * 전체 회차 목록 조회 (페이징)
     */
    public Page<LottoHistoryResponse> getAllDraws(Pageable pageable) {
        Page<LottoHistory> draws = lottoHistoryRepository
                .findByDeleteYnAndUseYnOrderByDrawNoDesc("N", "Y", pageable);

        return draws.map(LottoHistoryResponse::from);
    }
}
