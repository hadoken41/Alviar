package com.ordering.system.controller;

import com.ordering.system.entity.Item;
import com.ordering.system.repository.ItemRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/items")
public class ItemController {

    private final ItemRepository itemRepository;

    public ItemController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    /**
     * Calculate inventory summary stats
     */
    private Map<String, Object> getInventorySummary(List<Item> items) {
        long totalItems = items.size();
        long lowStockItems = items.stream()
            .filter(i -> i.getQuantity() < 5)
            .count();
        double inventoryValue = items.stream()
            .mapToDouble(item -> item.getPrice() * item.getQuantity())
            .sum();
        double averagePrice = items.isEmpty() ? 0 :
            items.stream().mapToDouble(Item::getPrice).average().orElse(0);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalItems", totalItems);
        summary.put("lowStockItems", lowStockItems);
        summary.put("inventoryValue", inventoryValue);
        summary.put("averagePrice", averagePrice);
        return summary;
    }

    @GetMapping
    public String listItems(@RequestParam(required = false) String search,
                            @RequestParam(required = false) String category,
                            Model model) {
        List<Item> items;
        if (search != null && !search.isBlank()) {
            items = itemRepository.findByNameContainingIgnoreCase(search);
        } else if (category != null && !category.isBlank()) {
            items = itemRepository.findByCategory(category);
        } else {
            items = itemRepository.findAll();
        }

        // Add inventory summary
        Map<String, Object> summary = getInventorySummary(items);

        model.addAttribute("items", items);
        model.addAttribute("search", search);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("inventorySummary", summary);
        return "items";
    }

    @PostMapping("/add")
    public String addItem(@RequestParam String name,
                          @RequestParam String sku,
                          @RequestParam String category,
                          @RequestParam Double price,
                          @RequestParam Integer quantity,
                          @RequestParam(required = false) String description,
                          Authentication authentication,
                          RedirectAttributes ra) {
        // Check if user has permission (only STAFF and ADMIN can add items)
        boolean hasPermission = authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_STAFF") || auth.getAuthority().equals("ROLE_ADMIN"));

        if (!hasPermission) {
            ra.addFlashAttribute("error", "You don't have permission to add items!");
            return "redirect:/items";
        }

        // Check if SKU already exists
        if (itemRepository.findBySku(sku).isPresent()) {
            ra.addFlashAttribute("error", "SKU already exists!");
            return "redirect:/items";
        }

        Item item = new Item();
        item.setName(name);
        item.setSku(sku);
        item.setCategory(category);
        item.setPrice(price);
        item.setQuantity(quantity);
        item.setDescription(description);
        item.setUpdatedAt(LocalDateTime.now());
        itemRepository.save(item);
        ra.addFlashAttribute("success", "Item added successfully!");
        return "redirect:/items";
    }

    @PostMapping("/edit/{id}")
    public String editItem(@PathVariable Long id,
                           @RequestParam String name,
                           @RequestParam String sku,
                           @RequestParam String category,
                           @RequestParam Double price,
                           @RequestParam Integer quantity,
                           @RequestParam(required = false) String description,
                           Authentication authentication,
                           RedirectAttributes ra) {
        // Check if user has permission (only STAFF and ADMIN can edit items)
        boolean hasPermission = authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_STAFF") || auth.getAuthority().equals("ROLE_ADMIN"));

        if (!hasPermission) {
            ra.addFlashAttribute("error", "You don't have permission to edit items!");
            return "redirect:/items";
        }

        Item item = itemRepository.findById(id).orElseThrow();

        // Check if SKU already exists (but allow if it's the same item's SKU)
        if (!item.getSku().equals(sku)) {
            if (itemRepository.findBySku(sku).isPresent()) {
                ra.addFlashAttribute("error", "SKU already exists!");
                return "redirect:/items";
            }
        }

        item.setName(name);
        item.setSku(sku);
        item.setCategory(category);
        item.setPrice(price);
        item.setQuantity(quantity);
        item.setDescription(description);
        item.setUpdatedAt(LocalDateTime.now());
        itemRepository.save(item);
        ra.addFlashAttribute("success", "Item updated successfully!");
        return "redirect:/items";
    }

    @PostMapping("/delete/{id}")
    public String deleteItem(@PathVariable Long id,
                            Authentication authentication,
                            RedirectAttributes ra) {
        // Check if user has permission (only STAFF and ADMIN can delete items)
        boolean hasPermission = authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_STAFF") || auth.getAuthority().equals("ROLE_ADMIN"));

        if (!hasPermission) {
            ra.addFlashAttribute("error", "You don't have permission to delete items!");
            return "redirect:/items";
        }

        itemRepository.deleteById(id);
        ra.addFlashAttribute("success", "Item deleted successfully!");
        return "redirect:/items";
    }
}