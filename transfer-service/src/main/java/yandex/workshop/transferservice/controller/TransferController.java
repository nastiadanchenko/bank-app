package yandex.workshop.transferservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yandex.workshop.transfer_service.api.accounts.model.TransferRequest;
import yandex.workshop.transferservice.service.TransferService;

@RestController
@RequestMapping("/transfers")
@RefreshScope
public class TransferController {

    private static final Logger log = LoggerFactory.getLogger(TransferController.class);

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @PreAuthorize("hasRole('USER') && hasAuthority('transfer.write')")
    public String submit(
        @RequestBody TransferRequest request,
        JwtAuthenticationToken authentication
    ) {
        log.info("Submit called by {} with authorities {}", authentication.getName(), authentication.getAuthorities());
        return transferService.submit(request, authentication);
    }
}
