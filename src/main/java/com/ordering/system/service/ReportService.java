package com.ordering.system.service;

import com.ordering.system.entity.Order;
import com.ordering.system.entity.Item;
import com.ordering.system.repository.OrderRepository;
import com.ordering.system.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;

    public ReportService(OrderRepository orderRepository, ItemRepository itemRepository) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
    }

    /**
     * Get revenue summary for dashboard cards
     */
    public Map<String, Object> getRevenueSummary(LocalDate startDate, LocalDate endDate) {
        List<Order> allOrders = orderRepository.findAll();

        List<Order> filteredOrders = allOrders.stream()
            .filter(o -> isWithinDateRange(o.getOrderDate(), startDate, endDate))
            .collect(Collectors.toList());

        double totalSales = filteredOrders.stream()
            .filter(o -> "Completed".equals(o.getStatus()))
            .mapToDouble(Order::getTotalPrice)
            .sum();

        long totalOrders = filteredOrders.size();
        long pendingOrders = filteredOrders.stream()
            .filter(o -> "Pending".equals(o.getStatus()))
            .count();
        long unpaidOrders = filteredOrders.stream()
            .filter(o -> "Completed".equals(o.getStatus()))
            .count();

        double inventoryValue = calculateInventoryValue();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalSales", totalSales);
        summary.put("totalOrders", totalOrders);
        summary.put("pendingOrders", pendingOrders);
        summary.put("unpaidOrders", unpaidOrders);
        summary.put("inventoryValue", inventoryValue);
        return summary;
    }

    /**
     * Get orders filtered by date range
     */
    public List<Order> getOrdersByDateRange(LocalDate startDate, LocalDate endDate) {
        return orderRepository.findAll().stream()
            .filter(o -> isWithinDateRange(o.getOrderDate(), startDate, endDate))
            .sorted(Comparator.comparing(Order::getOrderDate).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Get orders filtered by status and date range
     */
    public List<Order> getOrdersByStatusAndDateRange(String status, LocalDate startDate, LocalDate endDate) {
        return orderRepository.findAll().stream()
            .filter(o -> status == null || "".equals(status) || status.equals(o.getStatus()))
            .filter(o -> isWithinDateRange(o.getOrderDate(), startDate, endDate))
            .sorted(Comparator.comparing(Order::getOrderDate).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Get orders by customer name and date range
     */
    public List<Order> getOrdersByCustomerAndDateRange(String customerName, LocalDate startDate, LocalDate endDate) {
        return orderRepository.findAll().stream()
            .filter(o -> customerName == null || "".equals(customerName) ||
                    o.getCustomerName().toLowerCase().contains(customerName.toLowerCase()))
            .filter(o -> isWithinDateRange(o.getOrderDate(), startDate, endDate))
            .sorted(Comparator.comparing(Order::getOrderDate).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Calculate total inventory value (sum of item price × quantity)
     */
    public double calculateInventoryValue() {
        return itemRepository.findAll().stream()
            .mapToDouble(item -> item.getPrice() * item.getQuantity())
            .sum();
    }

    /**
     * Get inventory summary stats
     */
    public Map<String, Object> getInventorySummary() {
        List<Item> allItems = itemRepository.findAll();

        long totalItems = allItems.size();
        long lowStockItems = allItems.stream()
            .filter(i -> i.getQuantity() < 5)
            .count();
        double inventoryValue = calculateInventoryValue();
        double averagePrice = allItems.isEmpty() ? 0 :
            allItems.stream().mapToDouble(Item::getPrice).average().orElse(0);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalItems", totalItems);
        summary.put("lowStockItems", lowStockItems);
        summary.put("inventoryValue", inventoryValue);
        summary.put("averagePrice", averagePrice);
        return summary;
    }

    /**
     * Get income report breakdown by day
     */
    public Map<LocalDate, Double> getIncomeReportByDay(LocalDate startDate, LocalDate endDate) {
        return orderRepository.findAll().stream()
            .filter(o -> "Completed".equals(o.getStatus()))
            .filter(o -> isWithinDateRange(o.getOrderDate(), startDate, endDate))
            .collect(Collectors.groupingBy(
                o -> o.getOrderDate().toLocalDate(),
                Collectors.summingDouble(Order::getTotalPrice)
            ));
    }

    /**
     * Check if a LocalDateTime falls within a date range
     */
    private boolean isWithinDateRange(LocalDateTime dateTime, LocalDate startDate, LocalDate endDate) {
        if (dateTime == null) return false;
        LocalDate date = dateTime.toLocalDate();

        boolean afterStart = startDate == null || !date.isBefore(startDate);
        boolean beforeEnd = endDate == null || !date.isAfter(endDate);

        return afterStart && beforeEnd;
    }

    /**
     * Get total count of each status
     */
    public Map<String, Long> getStatusCounts(LocalDate startDate, LocalDate endDate) {
        List<Order> filteredOrders = getOrdersByDateRange(startDate, endDate);

        Map<String, Long> statusCounts = new HashMap<>();
        statusCounts.put("Pending", filteredOrders.stream()
            .filter(o -> "Pending".equals(o.getStatus())).count());
        statusCounts.put("Completed", filteredOrders.stream()
            .filter(o -> "Completed".equals(o.getStatus())).count());
        statusCounts.put("Cancelled", filteredOrders.stream()
            .filter(o -> "Cancelled".equals(o.getStatus())).count());

        return statusCounts;
    }
}
