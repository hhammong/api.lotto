package hhammong.apilotto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DhlotteryApiResponse {
    private String resultCode;
    private String resultMessage;
    private Data data;

    @Getter
    @Setter
    public static class Data {
        private List<LottoInfo> list;
    }

    @Getter
    @Setter
    public static class LottoInfo {
        @JsonProperty("ltEpsd")
        private Integer ltEpsd;           // 회차

        @JsonProperty("tm1WnNo")
        private Short tm1WnNo;

        @JsonProperty("tm2WnNo")
        private Short tm2WnNo;

        @JsonProperty("tm3WnNo")
        private Short tm3WnNo;

        @JsonProperty("tm4WnNo")
        private Short tm4WnNo;

        @JsonProperty("tm5WnNo")
        private Short tm5WnNo;

        @JsonProperty("tm6WnNo")
        private Short tm6WnNo;

        @JsonProperty("bnsWnNo")
        private Short bnsWnNo;            // 보너스번호

        @JsonProperty("ltRflYmd")
        private String ltRflYmd;          // 추첨날짜 (yyyyMMdd)

        @JsonProperty("rnk1WnAmt")
        private Long rnk1WnAmt;           // 1등 1게임당 당첨금

        @JsonProperty("rnk2WnAmt")
        private Long rnk2WnAmt;           // 2등 1게임당 당첨금

        @JsonProperty("rnk3WnAmt")
        private Long rnk3WnAmt;           // 3등 1게임당 당첨금

        @JsonProperty("rnk4WnAmt")
        private Integer rnk4WnAmt;        // 4등 1게임당 당첨금

        @JsonProperty("rnk5WnAmt")
        private Integer rnk5WnAmt;        // 5등 1게임당 당첨금
    }
}