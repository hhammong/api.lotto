package hhammong.apilotto.service;

import hhammong.apilotto.dto.LottoHistoryCreateRequest;
import hhammong.apilotto.dto.LottoHistoryResponse;
import hhammong.apilotto.entity.LottoHistory;
import hhammong.apilotto.exception.ResourceNotFoundException;
import hhammong.apilotto.repository.LottoHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LottoHistoryService {

    private final LottoHistoryRepository lottoHistoryRepository;

    /**
     * 최신 회차 당첨번호 조회
     */
    @Transactional(readOnly = true)
    public LottoHistoryResponse getLatestDraw() {
        LottoHistory latestDraw = lottoHistoryRepository
                .findTopByDeleteYnAndUseYnOrderByDrawNoDesc("N", "Y")
                .orElseThrow(() -> new ResourceNotFoundException("당첨번호가 존재하지 않습니다."));

        return LottoHistoryResponse.from(latestDraw);
    }

    /**
     * 특정 회차 당첨번호 조회
     */
    @Transactional(readOnly = true)
    public LottoHistoryResponse getDrawByNumber(Integer drawNo) {
        LottoHistory draw = lottoHistoryRepository
                .findByDrawNoAndDeleteYnAndUseYn(drawNo, "N", "Y")
                .orElseThrow(() -> new ResourceNotFoundException("해당 회차를 찾을 수 없습니다: " + drawNo));

        return LottoHistoryResponse.from(draw);
    }

    /**
     * 전체 회차 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<LottoHistoryResponse> getAllDraws(Pageable pageable) {
        Page<LottoHistory> draws = lottoHistoryRepository
                .findByDeleteYnAndUseYnOrderByDrawNoDesc("N", "Y", pageable);

        return draws.map(LottoHistoryResponse::from);
    }

    /**
     * 최신 회차 당첨 번호 등록
     */
    @Transactional
    public LottoHistoryResponse createLottoHistory(LottoHistoryCreateRequest request) {
        LottoHistory entity = LottoHistory.builder()
                .drawNo(request.getDrawNo())
                .drawDate(request.getDrawDate())
                .number1(request.getNumber1())
                .number2(request.getNumber2())
                .number3(request.getNumber3())
                .number4(request.getNumber4())
                .number5(request.getNumber5())
                .number6(request.getNumber6())
                .bonusNumber(request.getBonusNumber())
                .numbers(request.getNumbers())
                .prize1st(request.getPrize1st())
                .prize2nd(request.getPrize2nd())
                .prize3rd(request.getPrize3rd())
                .prize4th(request.getPrize4th())
                .prize5th(request.getPrize5th())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleteYn("N")
                .useYn("Y")
                .build();

        LottoHistory saved = lottoHistoryRepository.saveAndFlush(entity);
        return LottoHistoryResponse.from(saved);
    }
}
