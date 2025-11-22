package com.lq.travel.controller;

import com.lq.travel.AI.core.model.dto.ResponseDTO;
import com.lq.travel.common.DeleteRequest;
import com.lq.travel.common.ResponseUtils;
import com.lq.travel.model.entity.ScenicSpot;
import com.lq.travel.service.ScenicSpotService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@RestController
@RequestMapping("/scenic")
public class ScenicController {

    @Resource
    private ScenicSpotService scenicSpotService;

    /**
     * 获取所有景点列表
     */
    @GetMapping("/list")
    public ResponseDTO<List<ScenicSpot>> listSpots() {
        try {
            List<ScenicSpot> spots = scenicSpotService.getAllSpots();
            return ResponseUtils.success(spots != null ? spots : List.of());
        } catch (Exception e) {
            log.error("获取景点列表失败", e);
            return ResponseUtils.error(500, "获取景点列表失败");
        }
    }

    /**
     * 获取景点详情
     */
    @GetMapping("/{id}")
    public ResponseDTO<ScenicSpot> getScenicDetail(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseUtils.error(400, "景点ID无效");
        }
        
        try {
            ScenicSpot spot = scenicSpotService.getById(id);
            if (spot == null || spot.getIsDelete() == 1) {
                return ResponseUtils.error(404, "景点不存在");
            }
            return ResponseUtils.success(spot);
        } catch (Exception e) {
            log.error("获取景点详情失败", e);
            return ResponseUtils.error(500, "获取景点详情失败");
        }
    }

    /**
     * 新增景点
     */
    @PostMapping("/add")
    public ResponseDTO<Long> addScenicSpot(@RequestBody ScenicSpot scenicSpot) {
        if (scenicSpot == null || isBlank(scenicSpot.getName())) {
            return ResponseUtils.error(400, "景点名称不能为空");
        }

        if (scenicSpot.getIsDelete() == null) {
            scenicSpot.setIsDelete(0);
        }

        try {
            boolean saved = scenicSpotService.save(scenicSpot);
            if (!saved) {
                return ResponseUtils.error(500, "新增景点失败");
            }
            return ResponseUtils.success(scenicSpot.getId());
        } catch (Exception e) {
            log.error("新增景点失败", e);
            return ResponseUtils.error(500, "新增景点失败");
        }
    }

    /**
     * 更新景点信息（支持部分字段）
     */
    @PostMapping("/update")
    public ResponseDTO<Boolean> updateScenicSpot(@RequestBody ScenicSpot scenicSpot) {
        if (scenicSpot == null || scenicSpot.getId() == null || scenicSpot.getId() <= 0) {
            return ResponseUtils.error(400, "景点ID无效");
        }

        try {
            boolean updated = scenicSpotService.updateById(scenicSpot);
            return updated
                    ? ResponseUtils.success(true)
                    : ResponseUtils.error(404, "景点不存在或更新失败");
        } catch (Exception e) {
            log.error("更新景点失败，id={}", scenicSpot.getId(), e);
            return ResponseUtils.error(500, "更新景点失败");
        }
    }

    /**
     * 删除景点（软删除）
     */
    @PostMapping("/delete")
    public ResponseDTO<Boolean> deleteScenicSpot(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            return ResponseUtils.error(400, "景点ID无效");
        }

        try {
            ScenicSpot update = new ScenicSpot();
            update.setId(deleteRequest.getId());
            update.setIsDelete(1);
            boolean removed = scenicSpotService.updateById(update);
            return removed
                    ? ResponseUtils.success(true)
                    : ResponseUtils.error(404, "景点不存在或删除失败");
        } catch (Exception e) {
            log.error("删除景点失败，id={}", deleteRequest.getId(), e);
            return ResponseUtils.error(500, "删除景点失败");
        }
    }
}

