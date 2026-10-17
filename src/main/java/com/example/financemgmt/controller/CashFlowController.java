package com.example.financemgmt.controller;

import com.example.financemgmt.dto.CashFlowResponse;
import com.example.financemgmt.dto.TransferRequest;
import com.example.financemgmt.model.CashTransfer;
import com.example.financemgmt.service.CashFlowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cash-flow")
public class CashFlowController {
    private final CashFlowService service;
    public CashFlowController(CashFlowService service) { this.service = service; }

    @GetMapping
    public CashFlowResponse get() { return CashFlowResponse.from(service.get()); }
    @GetMapping("/transfers")
    public List<TransferResponse> transferHistory() {
        return service.transferHistory().stream().map(TransferResponse::from).toList();
    }
    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
        return TransferResponse.from(service.transfer(request));
    }
    public record TransferResponse(Long id, String source, java.math.BigDecimal amount, java.time.LocalDate date) {
        static TransferResponse from(CashTransfer transfer) {
            return new TransferResponse(transfer.getId(), transfer.getSource().name(), transfer.getAmount(), transfer.getTransferDate());
        }
    }
}
