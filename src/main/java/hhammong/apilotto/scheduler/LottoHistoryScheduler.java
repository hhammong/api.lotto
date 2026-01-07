package hhammong.apilotto.scheduler;

import hhammong.apilotto.dto.DhlotteryApiResponse;
import hhammong.apilotto.dto.LottoHistoryCreateRequest;
import hhammong.apilotto.service.LottoHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class LottoHistoryScheduler {

    private final LottoHistoryService lottoHistoryService;
    private final RestTemplate restTemplate;

    private static final String DHLOTTERY_API_URL = "https://www.dhlottery.co.kr/lt645/selectPstLt645Info.do";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 매주 토요일 밤 10시에 실행 (로또 추첨 후)
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 40 20 * * SAT")
    public void fetchAndSaveLottoHistory() {
        log.info("로또 당첨 번호 수집 스케줄러 시작");

        try {
            // 외부 API 호출
            DhlotteryApiResponse response = restTemplate.getForObject(
                    DHLOTTERY_API_URL,
                    DhlotteryApiResponse.class
            );

            if (response != null && response.getData() != null
                    && response.getData().getList() != null
                    && !response.getData().getList().isEmpty()) {

                // 첫 번째 데이터 가져오기
                DhlotteryApiResponse.LottoInfo lottoInfo = response.getData().getList().get(0);

                // DTO 변환
                LottoHistoryCreateRequest request = convertToRequest(lottoInfo);

                // DB 저장
                lottoHistoryService.createLottoHistory(request);

                log.info("로또 {}회차 당첨 번호 저장 완료", lottoInfo.getLtEpsd());
            } else {
                log.warn("외부 API 응답 데이터가 비어있습니다.");
            }

        } catch (Exception e) {
            log.error("로또 당첨 번호 수집 중 오류 발생", e);
        }
    }

    /**
     * 외부 API 응답을 CreateRequest로 변환
     */
    private LottoHistoryCreateRequest convertToRequest(DhlotteryApiResponse.LottoInfo lottoInfo) {
        // 날짜 변환 (yyyyMMdd -> LocalDate)
        LocalDate drawDate = LocalDate.parse(lottoInfo.getLtRflYmd(), DATE_FORMATTER);

        // numbers 문자열 생성 (선택사항)
        String numbers = String.format("%d,%d,%d,%d,%d,%d",
                lottoInfo.getTm1WnNo(),
                lottoInfo.getTm2WnNo(),
                lottoInfo.getTm3WnNo(),
                lottoInfo.getTm4WnNo(),
                lottoInfo.getTm5WnNo(),
                lottoInfo.getTm6WnNo()
        );

        return LottoHistoryCreateRequest.builder()
                .drawNo(lottoInfo.getLtEpsd())
                .drawDate(drawDate)
                .number1(lottoInfo.getTm1WnNo())
                .number2(lottoInfo.getTm2WnNo())
                .number3(lottoInfo.getTm3WnNo())
                .number4(lottoInfo.getTm4WnNo())
                .number5(lottoInfo.getTm5WnNo())
                .number6(lottoInfo.getTm6WnNo())
                .bonusNumber(lottoInfo.getBnsWnNo())
                .numbers(numbers)
                .prize1st(lottoInfo.getRnk1WnAmt())
                .prize2nd(lottoInfo.getRnk2WnAmt())
                .prize3rd(lottoInfo.getRnk3WnAmt())
                .prize4th(lottoInfo.getRnk4WnAmt())
                .prize5th(lottoInfo.getRnk5WnAmt())
                .build();
    }

    /**
     * 테스트용: 수동 실행 메서드 (필요시 Controller에서 호출 가능)
     */
    public void fetchNow() {
        fetchAndSaveLottoHistory();
    }
}