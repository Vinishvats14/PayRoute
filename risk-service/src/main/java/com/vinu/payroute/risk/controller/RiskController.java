package com.vinu.payroute.risk.controller;


import com.vinu.payroute.risk.dto.DailyTransferRequest;
import com.vinu.payroute.risk.dto.RiskCheckRequest;
import com.vinu.payroute.risk.dto.RiskCheckResponse;
import com.vinu.payroute.risk.service.RiskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/risk")
@RequiredArgsConstructor
public class RiskController {
    private final RiskService riskService ;

    @PostMapping("/check")
    public ResponseEntity<RiskCheckResponse> check(
            @Valid @RequestBody RiskCheckRequest request
    ) {
        return ResponseEntity.ok(
                riskService.check(request)
        );
    }

    @PostMapping("/validate")
    public ResponseEntity<RiskCheckResponse> validate(
            @Valid @RequestBody RiskCheckRequest request
    ) {
        return ResponseEntity.ok(
                riskService.validateTransferCompletion(request)
        );
    }

    @PostMapping("/daily/record")
    public ResponseEntity<Void> recordDailyTransfer(
            @Valid @RequestBody DailyTransferRequest request
    ) {
        riskService.recordCompletedTransfer(
                request.userId(),
                request.amount()
        );

        return ResponseEntity.noContent().build();
    }
}
