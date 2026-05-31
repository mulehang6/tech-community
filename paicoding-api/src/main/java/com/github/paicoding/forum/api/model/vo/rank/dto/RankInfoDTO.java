package com.github.paicoding.forum.api.model.vo.rank.dto;

import com.github.paicoding.forum.api.model.enums.rank.ActivityRankTimeEnum;
import lombok.Data;

import java.util.List;

/**
 * 排行榜信息
 *
 * @author YiHui
 * 创建于 2023/8/19
 */
@Data
public class RankInfoDTO {
    private ActivityRankTimeEnum time;
    private List<RankItemDTO> items;
}
