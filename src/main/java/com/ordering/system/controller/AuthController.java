package com.ordering.system.controller;

import com.ordering.system.service.ReportService;
import com.ordering.system.repository.OrderRepository;
import com.ordering.system.repository.ItemRepository;
import com.ordering.system.repository.UserRepository;
import com.ordering.system.repository.LaborRepository;
import com.ordering.system.entity.Labor;
import com.ordering.system.entity.Order;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AuthController {

    private final ReportService reportService;
    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final LaborRepository laborRepository;

    public AuthController(ReportService reportService, OrderRepository orderRepository, 
                         ItemRepository itemRepository, UserRepository userRepository,
                         LaborRepository laborRepository) {
        this.reportService = reportService;
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.laborRepository = laborRepository;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);
        
        // Get revenue summary
        Map<String, Object> revenueSummary = reportService.getRevenueSummary(thirtyDaysAgo, today);
        model.addAttribute("totalSales", revenueSummary.get("totalSales"));
        model.addAttribute("totalOrders", revenueSummary.get("totalOrders"));
        model.addAttribute("totalItems", itemRepository.findAll().size());
        model.addAttribute("totalStaff", userRepository.findAll().size());
        
        // Labor statistics
        List<Labor> allLabor = laborRepository.findAll();
        long activeLabor = allLabor.stream().filter(l -> "Active".equals(l.getStatus())).count();
        long inactiveLabor = allLabor.stream().filter(l -> "Inactive".equals(l.getStatus())).count();
        double totalPayroll = allLabor.stream()
            .filter(l -> "Active".equals(l.getStatus()))
            .mapToDouble(l -> l.getSalary() != null ? l.getSalary() : 0)
            .sum();
        
        model.addAttribute("activeLabor", activeLabor);
        model.addAttribute("inactiveLabor", inactiveLabor);
        model.addAttribute("totalPayroll", totalPayroll);
        
        // Recent orders
        List<Order> recentOrders = orderRepository.findAll().stream()
            .sorted(Comparator.comparing(Order::getOrderDate).reversed())
            .limit(5)
            .collect(Collectors.toList());
        model.addAttribute("recentOrders", recentOrders);
        
        // Get order status breakdown
        List<Order> allOrders = orderRepository.findAll();
        long completedCount = allOrders.stream().filter(o -> "Completed".equals(o.getStatus())).count();
        long pendingCount = allOrders.stream().filter(o -> "Pending".equals(o.getStatus())).count();
        long cancelledCount = allOrders.stream().filter(o -> "Cancelled".equals(o.getStatus())).count();
        long confirmedCount = allOrders.stream().filter(o -> "Confirmed".equals(o.getStatus())).count();
        
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("cancelledCount", cancelledCount);
        model.addAttribute("confirmedCount", confirmedCount);
        
        // Get top selling items
        List<Order> topSellingItems = allOrders.stream()
            .collect(Collectors.groupingBy(Order::getItemOrdered, 
                Collectors.summingInt(Order::getQuantity)))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .map(e -> {
                Order o = new Order();
                o.setItemOrdered(e.getKey());
                o.setQuantity(e.getValue());
                return o;
            })
            .collect(Collectors.toList());
        model.addAttribute("topSellingItems", topSellingItems);
        
        // Add user info
        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
        }
        model.addAttribute("currentDate", new Date());
        
        return "dashboard";
    }
}