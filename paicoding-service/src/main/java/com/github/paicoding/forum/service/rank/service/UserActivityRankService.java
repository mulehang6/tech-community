package com.github.paicoding.forum.service.rank.service;

import com.github.paicoding.forum.api.model.enums.rank.ActivityRankTimeEnum;
import com.github.paicoding.forum.api.model.vo.rank.dto.RankItemDTO;
import com.github.paicoding.forum.service.rank.service.model.ActivityScoreBo;

import java.util.List;

/**
 * 用户活跃排行榜
 *
 * @author YiHui
 * 创建于 2023/8/19
 */
public interface UserActivityRankService {
    /**
     * 添加活跃分
     */
    void addActivityScore(Long userId, ActivityScoreBo activityScore);

    /**
     * 查询用户的活跃信息
     */
    RankItemDTO queryRankInfo(Long userId, ActivityRankTimeEnum time);

    /**
     * 查询活跃度排行榜
     */
    List<RankItemDTO> queryRankList(ActivityRankTimeEnum time, int size);
}
