package com.laptoprepair.controller;

import com.laptoprepair.entity.Request;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.exception.NotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.laptoprepair.service.RequestService;
import com.laptoprepair.utils.ValidationErrorUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;

import java.util.UUID;

/**
 * Controller for handling public-facing requests and views.
 * This includes login, home page, request submission, and public request
 * lookup.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class PublicController {

    private final RequestService requestService;
    private final ValidationErrorUtil validationErrorUtil;

    @GetMapping("/login")
    public String login() {
        if (isStaff()) {
            return "redirect:/staff/requests/list";
        }
        return "public/login";
    }

    @GetMapping("/")
    public String index(Model model) {
        if (isStaff()) {
            return "redirect:/staff/requests/list";
        }
        return "public/index";
    }

    @PostMapping("/lookup")
    public String lookup(@RequestParam String id) {
        return "redirect:/public/request/" + UUID.fromString(id.trim());
    }

    @GetMapping("/about")
    public String about() {
        return "public/about";
    }

    @GetMapping("/submit")
    public String submitForm(Model model) {
        model.addAttribute("request", new Request());
        return "public/request-submit";
    }

    @PostMapping("/submit")
    public String submit(@Valid @ModelAttribute Request request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            // Add centralized error messages for header display
            model.addAttribute("errorMessages", validationErrorUtil.extractErrorMessages(bindingResult));

            // Add field-specific error status for enhanced styling
            model.addAttribute("fieldHasErrors", validationErrorUtil.getFieldErrorStatus(bindingResult));

            model.addAttribute("request", request);
            return "public/request-submit";
        }

        try {
            Request saved = requestService.publicCreate(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Yêu cầu đã được gửi thành công!");
            redirectAttributes.addFlashAttribute("requestId", saved.getId());
            return "redirect:/submit";
        } catch (ValidationException ex) {
            model.addAttribute("request", request);
            model.addAttribute("errorMessage", ex.getMessage());
            return "public/request-submit";
        }
    }

    @PostMapping("/recover")
    public String recover(@RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {
        requestService.recover(email);
        redirectAttributes.addFlashAttribute("successMessage",
                "Nếu email tồn tại trong hệ thống, chúng tôi đã gửi danh sách yêu cầu!");
        return "redirect:/";
    }

    @GetMapping("/public/request/{id}")
    public String viewRequestDetail(@PathVariable UUID id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Request request = requestService.findById(id);
            model.addAttribute("request", request);
            model.addAttribute("isStaff", false);
            return "staff/request-detail";
        } catch (NotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/";
        }
    }

    private boolean isStaff() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() &&
                auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));
    }

}