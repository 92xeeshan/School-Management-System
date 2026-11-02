package com.schoolms.website;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.website.dto.EnquiryDto;
import com.schoolms.website.dto.PublicSiteDto;
import com.schoolms.website.dto.SubmitEnquiryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Public website")
@RequestMapping("/api/public")
public class PublicWebsiteController {

    private final PublicSiteService publicSiteService;
    private final EnquiryService enquiryService;
    private final PublicRateLimiter rateLimiter;

    @Operation(summary = "Public school landing content")
    @GetMapping("/{schoolCode}")
    public ApiResponse<PublicSiteDto> site(
            @PathVariable String schoolCode,
            @RequestParam(required = false) String locale,
            HttpServletRequest request) {
        rateLimiter.enforceGet(clientKey(request, schoolCode));
        return ApiResponse.ok(publicSiteService.publicSite(schoolCode, locale));
    }

    @Operation(summary = "Submit an admission enquiry")
    @PostMapping("/{schoolCode}/enquiries")
    public ApiResponse<EnquiryDto> submitEnquiry(
            @PathVariable String schoolCode,
            @Valid @RequestBody SubmitEnquiryRequest body,
            HttpServletRequest request) {
        rateLimiter.enforcePost(clientKey(request, schoolCode));
        return ApiResponse.ok(enquiryService.submit(schoolCode, body), "enquiry.submitted");
    }

    private String clientKey(HttpServletRequest request, String schoolCode) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr()
                : forwarded.split(",")[0].trim();
        return schoolCode + ":" + ip;
    }
}
