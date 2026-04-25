package com.ordering.system.controller;

import com.ordering.system.entity.Order;
import com.ordering.system.repository.OrderRepository;
import com.ordering.system.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final OrderRepository orderRepository;

    public ReportController(ReportService reportService, OrderRepository orderRepository) {
        this.reportService = reportService;
        this.orderRepository = orderRepository;
    }

    /**
     * Display Reports & Receipts dashboard
     */
    @GetMapping
    public String showReports(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Model model) {

        // Parse dates (default to show all if not provided)
        LocalDate startDate = null;
        LocalDate endDate = null;

        if (fromDate != null && !fromDate.isBlank()) {
            try {
                startDate = LocalDate.parse(fromDate);
            } catch (Exception e) {
                startDate = null;
            }
        }

        if (toDate != null && !toDate.isBlank()) {
            try {
                endDate = LocalDate.parse(toDate);
            } catch (Exception e) {
                endDate = null;
            }
        }

        // Get filtered orders
        List<Order> orders;

        if (search != null && !search.isBlank()) {
            // Search by customer name
            orders = reportService.getOrdersByCustomerAndDateRange(search, startDate, endDate);
        } else if (status != null && !status.isBlank()) {
            // Filter by status
            orders = reportService.getOrdersByStatusAndDateRange(status, startDate, endDate);
        } else {
            // Get all orders in date range
            orders = reportService.getOrdersByDateRange(startDate, endDate);
        }

        // Get summary stats
        Map<String, Object> revenueSummary = reportService.getRevenueSummary(startDate, endDate);

        // Calculate filtered total
        double filteredTotal = orders.stream()
            .mapToDouble(Order::getTotalPrice)
            .sum();

        // Calculate completed orders total
        double completedTotal = orders.stream()
            .filter(o -> "Completed".equals(o.getStatus()))
            .mapToDouble(Order::getTotalPrice)
            .sum();

        // Add to model
        model.addAttribute("orders", orders);
        model.addAttribute("totalSales", revenueSummary.get("totalSales"));
        model.addAttribute("totalOrders", revenueSummary.get("totalOrders"));
        model.addAttribute("unpaidOrders", revenueSummary.get("unpaidOrders"));
        model.addAttribute("inventoryValue", revenueSummary.get("inventoryValue"));
        model.addAttribute("filteredTotal", filteredTotal);
        model.addAttribute("completedTotal", completedTotal);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedSearch", search);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        return "reports";
    }

    /**
     * View single order receipt
     */
    @GetMapping("/receipt/{orderId}")
    public String viewReceipt(@PathVariable Long orderId, Model model) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));

        model.addAttribute("order", order);
        model.addAttribute("printable", true);

        return "receipt-view";
    }
}
