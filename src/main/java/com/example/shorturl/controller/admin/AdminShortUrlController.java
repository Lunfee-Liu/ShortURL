package com.example.shorturl.controller.admin;

import com.example.shorturl.common.PageResult;
import com.example.shorturl.common.Result;
import com.example.shorturl.dto.AdminListShortUrlDTO;
import com.example.shorturl.service.AdminShortUrlService;
import com.example.shorturl.vo.AdminShortUrlVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/short-urls")
@RequiredArgsConstructor
public class AdminShortUrlController {

    private final AdminShortUrlService adminShortUrlService;

    @GetMapping
    public Result<PageResult<AdminShortUrlVO>> list(@Valid AdminListShortUrlDTO dto) {
        return Result.success(adminShortUrlService.listShortUrls(dto));
    }

    @GetMapping("/{id}")
    public Result<AdminShortUrlVO> getById(@PathVariable Long id) {
        return Result.success(adminShortUrlService.getById(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminShortUrlService.deleteById(id);
        return Result.success();
    }
}
