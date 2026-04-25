package com.ordering.system.controller;

import com.ordering.system.repository.OrderRepository;
import com.ordering.system.repository.ItemRepository;
import com.ordering.system.repository.UserRepository;
import com.ordering.system.repository.LaborRepository;
import com.ordering.system.entity.Labor;
import com.ordering.system.entity.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.security.core.Authentication;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AuthController {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final LaborRepository laborRepository;

    public AuthController(OrderRepository orderRepository,
                         ItemRepository itemRepository,
                         UserRepository userRepository,
                         LaborRepository laborRepository) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.laborRepository = laborRepository;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // Loads instantly - no data fetching
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
        }
        model.addAttribute("currentDate", new Date());
        return "dashboard";
    }

    // API: Summary stats
    @GetMapping("/api/dashboard/summary")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSummary() {
        List<Order> allOrders = orderRepository.findAll();
        double totalSales = allOrders.stream()
            .filter(o -> "Completed".equals(o.getStatus()))
            .mapToDouble(o -> o.getTotalPrice() != null ? o.getTotalPrice() : 0)
            .sum();
        long pendingCount = allOrders.stream().filter(o -> "Pending".equals(o.getStatus())).count();

        Map<String, Object> data = new HashMap<>();
        data.put("totalSales", totalSales);
        data.put("totalOrders", allOrders.size());
        data.put("totalItems", itemRepository.count());
        data.put("totalStaff", userRepository.count());
        data.put("pendingCount", pendingCount);
        return ResponseEntity.ok(data);
    }

    // API: Order status breakdown
    @GetMapping("/api/dashboard/order-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOrderStatus() {
        List<Order> allOrders = orderRepository.findAll();
        Map<String, Object> data = new HashMap<>();
        data.put("completed", allOrders.stream().filter(o -> "Completed".equals(o.getStatus())).count());
        data.put("pending", allOrders.stream().filter(o -> "Pending".equals(o.getStatus())).count());
        data.put("cancelled", allOrders.stream().filter(o -> "Cancelled".equals(o.getStatus())).count());
        data.put("confirmed", allOrders.stream().filter(o -> "Confirmed".equals(o.getStatus())).count());
        return ResponseEntity.ok(data);
    }

    // API: Top selling items
    @GetMapping("/api/dashboard/top-items")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getTopItems() {
        List<Order> allOrders = orderRepository.findAll();
        List<Map<String, Object>> topItems = allOrders.stream()
            .collect(Collectors.groupingBy(Order::getItemOrdered,
                Collectors.summingInt(Order::getQuantity)))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .map(e -> {
                Map<String, Object> item = new HashMap<>();
                item.put("name", e.getKey());
                item.put("quantity", e.getValue());
                return item;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(topItems);
    }

    // API: Recent orders
    @GetMapping("/api/dashboard/recent-orders")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getRecentOrders() {
        List<Order> recentOrders = orderRepository.findAll().stream()
            .sorted(Comparator.comparing(Order::getOrderDate).reversed())
            .limit(5)
            .collect(Collectors.toList());
        List<Map<String, Object>> result = recentOrders.stream().map(o -> {
            Map<String, Object> map = new HashMap<>();
            map.put("customerName", o.getCustomerName());
            map.put("itemOrdered", o.getItemOrdered());
            map.put("status", o.getStatus());
            map.put("totalPrice", o.getTotalPrice());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // API: Labor stats
    @GetMapping("/api/dashboard/labor")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getLaborStats() {
        List<Labor> allLabor = laborRepository.findAll();
        long activeLabor = allLabor.stream().filter(l -> "Active".equals(l.getStatus())).count();
        long inactiveLabor = allLabor.stream().filter(l -> "Inactive".equals(l.getStatus())).count();
        double totalPayroll = allLabor.stream()
            .filter(l -> "Active".equals(l.getStatus()))
            .mapToDouble(l -> l.getSalary() != null ? l.getSalary() : 0)
            .sum();
        Map<String, Object> data = new HashMap<>();
        data.put("activeLabor", activeLabor);
        data.put("inactiveLabor", inactiveLabor);
        data.put("totalPayroll", totalPayroll);
        return ResponseEntity.ok(data);
    }

    // API: Daily sales chart
    @GetMapping("/api/dashboard/daily")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDailySales() {
        List<Order> allOrders = orderRepository.findAll();
        Map<String, Double> dailySales = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate date = java.time.LocalDate.now().minusDays(i);
            String label = date.getMonth().name().substring(0, 3) + " " + date.getDayOfMonth();
            double sales = allOrders.stream()
                .filter(o -> o.getOrderDate() != null &&
                    o.getOrderDate().toLocalDate().equals(date) &&
                    "Completed".equals(o.getStatus()))
                .mapToDouble(o -> o.getTotalPrice() != null ? o.getTotalPrice() : 0)
                .sum();
            dailySales.put(label, sales);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("labels", new ArrayList<>(dailySales.keySet()));
        data.put("values", new ArrayList<>(dailySales.values()));
        return ResponseEntity.ok(data);
    }
}
