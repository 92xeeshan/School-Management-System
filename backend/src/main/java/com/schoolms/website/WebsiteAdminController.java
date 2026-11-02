package com.schoolms.website;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.common.api.PagedResponse;
import com.schoolms.website.dto.EnquiryDto;
import com.schoolms.website.dto.PublicSiteAdminDto;
import com.schoolms.website.dto.PublicSiteUpdateRequest;
import com.schoolms.website.dto.UpdateEnquiryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Website admin")
@RequestMapping("/api")
public class WebsiteAdminController {

    private final PublicSiteService publicSiteService;
    private final EnquiryService enquiryService;

    @Operation(summary = "Get public site editor content")
    @PreAuthorize("hasAuthority('PUBLIC_SITE_MANAGE')")
    @GetMapping("/website")
    public ApiResponse<PublicSiteAdminDto> getSite() {
        return ApiResponse.ok(publicSiteService.adminGet());
    }

    @Operation(summary = "Update public site content")
    @PreAuthorize("hasAuthority('PUBLIC_SITE_MANAGE')")
    @PutMapping("/website")
    public ApiResponse<PublicSiteAdminDto> updateSite(@Valid @RequestBody PublicSiteUpdateRequest request) {
        return ApiResponse.ok(publicSiteService.adminUpdate(request), "website.saved");
    }

    @Operation(summary = "List admission enquiries")
    @PreAuthorize("hasAnyAuthority('ENQUIRY_READ','ENQUIRY_MANAGE')")
    @GetMapping("/enquiries")
    public ApiResponse<PagedResponse<EnquiryDto>> listEnquiries(
            @RequestParam(required = false) EnquiryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(enquiryService.list(status, page, size));
    }

    @Operation(summary = "Update an enquiry")
    @PreAuthorize("hasAuthority('ENQUIRY_MANAGE')")
    @PutMapping("/enquiries/{id}")
    public ApiResponse<EnquiryDto> updateEnquiry(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEnquiryRequest request) {
        return ApiResponse.ok(enquiryService.update(id, request), "enquiry.updated");
    }
}
