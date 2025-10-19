package org.showpage.rallyserver.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.showpage.rallyserver.RestResponse;
import org.showpage.rallyserver.service.DtoMapper;
import org.showpage.rallyserver.ui.UiMember;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class MemberController {
    private final ServiceCaller serviceCaller;

    /**
     * Return my information.
     */
    @GetMapping("/member/info")
    ResponseEntity<RestResponse<UiMember>> myInfo() {
        return serviceCaller.call((member) -> DtoMapper.toUiMember(member));
    }
}
