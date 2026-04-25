package com.ordering.system.controller;

import com.ordering.system.entity.Order;
import com.ordering.system.entity.Item;
import com.ordering.system.repository.OrderRepository;
import com.ordering.system.repository.ItemRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;

    public OrderController(OrderRepository orderRepository, ItemRepository itemRepository) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
    }

    @GetMapping
    public String listOrders(@RequestParam(required = false) String search,
                             @RequestParam(required = false) String status,
                             Model model) {
        List<Order> orders;
        if (search != null && !search.isBlank()) {
            orders = orderRepository.findByCustomerNameContainingIgnoreCase(search);
        } else if (status != null && !status.isBlank()) {
            orders = orderRepository.findByStatus(status);
        } else {
            orders = orderRepository.findAll();
        }

        double totalRevenue = orders.stream()
            .filter(o -> o.getStatus().equals("Completed"))
            .mapToDouble(Order::getTotalPrice).sum();
        long pendingCount   = orders.stream().filter(o -> o.getStatus().equals("Pending")).count();
        long completedCount = orders.stream().filter(o -> o.getStatus().equals("Completed")).count();
        long cancelledCount = orders.stream().filter(o -> o.getStatus().equals("Cancelled")).count();

        model.addAttribute("orders", orders);
        model.addAttribute("items", itemRepository.findAll());
        model.addAttribute("search", search);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("cancelledCount", cancelledCount);
        return "orders";
    }

    // ── Find item by name, no custom repository method needed ──
    private Item findItemByName(String name) {
        return itemRepository.findAll().stream()
            .filter(i -> i.getName().equalsIgnoreCase(name.trim()))
            .findFirst()
            .orElse(null);
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam String customerName,
                           @RequestParam List<String> itemNames,
                           @RequestParam List<Integer> quantities,
                           @RequestParam List<Double> prices,
                           @RequestParam(required = false) String paymentMethod,
                           RedirectAttributes ra) {

        for (int i = 0; i < itemNames.size(); i++) {
            String itemName   = itemNames.get(i);
            int    orderedQty = quantities.get(i);

            Item item = findItemByName(itemName);
            if (item != null) {
                item.setQuantity(Math.max(item.getQuantity() - orderedQty, 0));
                item.setUpdatedAt(LocalDateTime.now());
                itemRepository.save(item);
            }

            Order order = new Order();
            order.setCustomerName(customerName);
            order.setPaymentMethod(paymentMethod);
            order.setItemOrdered(itemName);
            order.setQuantity(orderedQty);
            order.setTotalPrice(prices.get(i));
            order.setStatus("Pending");
            order.setOrderDate(LocalDateTime.now());
            orderRepository.save(order);
        }

        ra.addFlashAttribute("success", "Order placed successfully for " + customerName + "!");
        return "redirect:/orders";
    }

    @PostMapping("/edit/{id}")
    public String editOrder(@PathVariable Long id,
                            @RequestParam String customerName,
                            @RequestParam String itemOrdered,
                            @RequestParam Integer quantity,
                            @RequestParam Double totalPrice,
                            @RequestParam String status,
                            @RequestParam(required = false) String notes,
                            RedirectAttributes ra) {

        Order order = orderRepository.findById(id).orElseThrow();
        String previousStatus = order.getStatus();

        // ── If Pending → Cancelled: restore the stock ──
        if ("Pending".equals(previousStatus) && "Cancelled".equals(status)) {
            Item item = findItemByName(order.getItemOrdered());
            if (item != null) {
                item.setQuantity(item.getQuantity() + order.getQuantity());
                item.setUpdatedAt(LocalDateTime.now());
                itemRepository.save(item);
            }
        }

        order.setCustomerName(customerName);
        order.setItemOrdered(itemOrdered);
        order.setQuantity(quantity);
        order.setTotalPrice(totalPrice);
        order.setStatus(status);
        order.setNotes(notes);
        orderRepository.save(order);

        ra.addFlashAttribute("success", "Order updated successfully!");
        return "redirect:/orders";
    }

    @PostMapping("/paid/{id}")
    public String markAsPaid(@PathVariable Long id, RedirectAttributes ra) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order != null) {
            order.setStatus("Completed");
            orderRepository.save(order);
            ra.addFlashAttribute("success", "Order marked as Paid!");
        }
        return "redirect:/orders";
    }

    @PostMapping("/delete/{id}")
    public String deleteOrder(@PathVariable Long id, RedirectAttributes ra) {
        // ── If a Pending order is deleted: restore the stock ──
        Order order = orderRepository.findById(id).orElse(null);
        if (order != null && "Pending".equals(order.getStatus())) {
            Item item = findItemByName(order.getItemOrdered());
            if (item != null) {
                item.setQuantity(item.getQuantity() + order.getQuantity());
                item.setUpdatedAt(LocalDateTime.now());
                itemRepository.save(item);
            }
        }

        orderRepository.deleteById(id);
        ra.addFlashAttribute("success", "Order deleted successfully!");
        return "redirect:/orders";
    }
}