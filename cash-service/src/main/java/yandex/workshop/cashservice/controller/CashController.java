package yandex.workshop.cashservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yandex.workshop.cashservice.api.accounts.model.CashRequest;
import yandex.workshop.cashservice.service.CashService;

@Slf4j
@RestController
@RequestMapping("/cash")
@RequiredArgsConstructor
public class CashController {

    private final CashService cashService;

    @PostMapping
    @PreAuthorize("hasRole('USER') && hasAuthority('cash.write')")
    public String submit(@RequestBody CashRequest request) {

        log.debug("Authorities in CASH: {}", request);
        return cashService.submit(request);
    }
}
