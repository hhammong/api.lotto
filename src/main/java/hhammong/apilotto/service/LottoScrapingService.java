package hhammong.apilotto.service;

import hhammong.apilotto.dto.LottoScrapingDTO;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 동행복권 홈페이지 스크래핑 서비스
 */
@Service
@Slf4j
public class LottoScrapingService {

    private static final String BASE_URL = "https://dhlottery.co.kr/gameResult.do?method=byWin";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    /**
     * 최신 회차 번호 조회
     */
    public Integer getLatestDrawNo() throws IOException {
        try {
            log.info("최신 회차 조회 시작");

            Document doc = Jsoup.connect(BASE_URL)
                    .userAgent(USER_AGENT)
                    .timeout(10000)
                    .get();

            String drawNoText = doc.select(".win_result h4 strong").text();
            Integer drawNo = Integer.parseInt(drawNoText.replaceAll("[^0-9]", ""));

            log.info("최신 회차: {}", drawNo);
            return drawNo;

        } catch (Exception e) {
            log.error("최신 회차 조회 실패", e);
            throw new IOException("최신 회차 조회 실패", e);
        }
    }

    /**
     * 특정 회차 당첨 정보 스크래핑
     */
    public LottoScrapingDTO scrapDrawResult(Integer drawNo) throws IOException {
        try {
            log.info("{}회차 스크래핑 시작", drawNo);

            String url = BASE_URL + "&drwNo=" + drawNo;
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(10000)
                    .get();

            LottoScrapingDTO result = LottoScrapingDTO.builder()
                    .drawNo(drawNo)
                    .build();

            // 1. 추첨일 파싱
            String drawDateText = doc.select(".win_result .desc").text();
            result.setDrawDate(parseDateFromText(drawDateText));

            // 2. 당첨 번호 파싱
            Elements winBalls = doc.select(".num.win .ball_645");
            if (winBalls.size() >= 6) {
                result.setNumber1(parseNumberFromBall(winBalls.get(0)));
                result.setNumber2(parseNumberFromBall(winBalls.get(1)));
                result.setNumber3(parseNumberFromBall(winBalls.get(2)));
                result.setNumber4(parseNumberFromBall(winBalls.get(3)));
                result.setNumber5(parseNumberFromBall(winBalls.get(4)));
                result.setNumber6(parseNumberFromBall(winBalls.get(5)));
            }

            // 3. 보너스 번호
            Element bonusBall = doc.select(".num.bonus .ball_645").first();
            if (bonusBall != null) {
                result.setBonusNumber(parseNumberFromBall(bonusBall));
            }

            // 4. 당첨금 정보
            Elements rows = doc.select("table.tbl_data tbody tr");
            if (rows.size() >= 5) {
                // 1등: row 0, column 3 (게임당 당첨금)
                result.setPrize1st(parseAmountFromCell(rows.get(0).select("td").get(3)));

                // 2등
                result.setPrize2nd(parseAmountFromCell(rows.get(1).select("td").get(3)));

                // 3등
                result.setPrize3rd(parseAmountFromCell(rows.get(2).select("td").get(3)));

                // 4등
                result.setPrize4th(parseAmountFromCell(rows.get(3).select("td").get(3)).intValue());

                // 5등
                result.setPrize5th(parseAmountFromCell(rows.get(4).select("td").get(3)).intValue());
            }

            log.info("{}회차 스크래핑 완료 - 당첨번호: {},{},{},{},{},{} + {}",
                    drawNo,
                    result.getNumber1(), result.getNumber2(), result.getNumber3(),
                    result.getNumber4(), result.getNumber5(), result.getNumber6(),
                    result.getBonusNumber());

            return result;

        } catch (Exception e) {
            log.error("{}회차 스크래핑 실패", drawNo, e);
            throw new IOException(drawNo + "회차 스크래핑 실패", e);
        }
    }

    // ===== Private 헬퍼 메서드 =====

    /**
     * 날짜 파싱: "(2025년 12월 06일 추첨)" -> LocalDate
     */
    private LocalDate parseDateFromText(String text) {
        try {
            Pattern pattern = Pattern.compile("(\\d{4})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일");
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));
                return LocalDate.of(year, month, day);
            }

            log.warn("날짜 파싱 실패, 현재 날짜 사용: {}", text);
            return LocalDate.now();

        } catch (Exception e) {
            log.error("날짜 파싱 오류: {}", text, e);
            return LocalDate.now();
        }
    }

    /**
     * 공 번호 파싱
     */
    private Short parseNumberFromBall(Element ball) {
        String text = ball.text().trim();
        return Short.parseShort(text);
    }

    /**
     * 테이블 셀에서 금액 파싱
     */
    private Long parseAmountFromCell(Element cell) {
        String text = cell.text();
        return parseAmount(text);
    }

    /**
     * 금액 파싱: "1,414,555,718원" -> 1414555718
     */
    private Long parseAmount(String text) {
        String numbers = text.replaceAll("[^0-9]", "");
        return numbers.isEmpty() ? 0L : Long.parseLong(numbers);
    }

}
