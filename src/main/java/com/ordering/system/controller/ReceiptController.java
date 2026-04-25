package com.ordering.system.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/receipt-view")
public class ReceiptController {

    /**
     * Redirect to reports page (Report & Receipts are together)
     */
    @GetMapping
    public String viewReceipts() {
        return "redirect:/reports";
    }
}
