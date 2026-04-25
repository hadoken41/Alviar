package com.ordering.system.controller;

import com.ordering.system.entity.Labor;
import com.ordering.system.repository.LaborRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/labor")
@PreAuthorize("hasRole('ADMIN')")
public class LaborController {

    private final LaborRepository laborRepository;

    public LaborController(LaborRepository laborRepository) {
        this.laborRepository = laborRepository;
    }

    @GetMapping
    public String listLabor(@RequestParam(required = false) String search,
                            @RequestParam(required = false) String role,
                            @RequestParam(required = false) String status,
                            Model model) {

        // Fetch all, copy to mutable list, sort by id DESC
        // Newest staff added will always appear at the top as #1
        List<Labor> laborList = new ArrayList<>(laborRepository.findAll());
        laborList.sort((a, b) -> Long.compare(b.getId(), a.getId()));

        if (search != null && !search.isBlank()) {
            laborList = laborList.stream()
                .filter(l -> l.getName().toLowerCase().contains(search.toLowerCase()))
                .collect(Collectors.toList());
        }
        if (role != null && !role.isBlank()) {
            laborList = laborList.stream()
                .filter(l -> l.getRole().equalsIgnoreCase(role))
                .collect(Collectors.toList());
        }
        if (status != null && !status.isBlank()) {
            laborList = laborList.stream()
                .filter(l -> l.getStatus().equalsIgnoreCase(status))
                .collect(Collectors.toList());
        }

        List<Labor> allStaff = new ArrayList<>(laborRepository.findAll());
        long activeCount   = allStaff.stream().filter(l -> "Active".equalsIgnoreCase(l.getStatus())).count();
        long inactiveCount = allStaff.stream().filter(l -> "Inactive".equalsIgnoreCase(l.getStatus())).count();
        double totalPayroll = allStaff.stream()
            .filter(l -> "Active".equalsIgnoreCase(l.getStatus()))
            .mapToDouble(l -> l.getSalary() != null ? l.getSalary() : 0)
            .sum();

        model.addAttribute("laborList", laborList);
        model.addAttribute("search", search);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("totalStaff", allStaff.size());
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("inactiveCount", inactiveCount);
        model.addAttribute("totalPayroll", totalPayroll);

        return "labor";
    }

    @PostMapping("/add")
    public String addLabor(@RequestParam String name,
                           @RequestParam String role,
                           @RequestParam String shift,
                           @RequestParam String status,
                           @RequestParam Double salary,
                           @RequestParam(required = false) String contact,
                           @RequestParam(required = false) String hireDate,
                           RedirectAttributes ra) {
        Labor labor = new Labor();
        labor.setName(name);
        labor.setRole(role);
        labor.setShift(shift);
        labor.setStatus(status != null ? status : "Active");
        labor.setSalary(salary);
        labor.setContact(contact);
        if (hireDate != null && !hireDate.isBlank()) {
            labor.setHireDate(LocalDate.parse(hireDate));
        }
        laborRepository.save(labor);
        ra.addFlashAttribute("success", "Staff added successfully!");
        return "redirect:/labor";
    }

    @PostMapping("/edit/{id}")
    public String editLabor(@PathVariable Long id,
                            @RequestParam String name,
                            @RequestParam String role,
                            @RequestParam String shift,
                            @RequestParam String status,
                            @RequestParam Double salary,
                            @RequestParam(required = false) String contact,
                            @RequestParam(required = false) String hireDate,
                            RedirectAttributes ra) {
        Labor labor = laborRepository.findById(id).orElseThrow();
        labor.setName(name);
        labor.setRole(role);
        labor.setShift(shift);
        labor.setStatus(status != null ? status : "Active");
        labor.setSalary(salary);
        labor.setContact(contact);
        if (hireDate != null && !hireDate.isBlank()) {
            labor.setHireDate(LocalDate.parse(hireDate));
        } else {
            labor.setHireDate(null);
        }
        laborRepository.save(labor);
        ra.addFlashAttribute("success", "Staff updated successfully!");
        return "redirect:/labor";
    }

    @PostMapping("/delete/{id}")
    public String deleteLabor(@PathVariable Long id, RedirectAttributes ra) {
        laborRepository.deleteById(id);
        ra.addFlashAttribute("success", "Staff removed successfully!");
        return "redirect:/labor";
    }
}