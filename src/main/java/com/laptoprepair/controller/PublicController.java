package com.laptoprepair.controller;

import com.laptoprepair.entity.Request;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.exception.NotFoundException;
import com.laptoprepair.service.RequestService;
import com.laptoprepair.service.SecurityService;
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

    private static final String REQUEST_ATTRIBUTE = "request";
    private static final String REQUEST_SUBMIT_VIEW = "public/request-submit";

    private final RequestService requestService;
    private final ValidationErrorUtil validationErrorUtil;
    private final SecurityService securityService;

    @GetMapping("/login")
    public String login() {
        boolean isStaff = securityService.getCurrentAuthentication()
                .map(auth -> securityService.hasRole(auth, "ROLE_STAFF"))
                .orElse(false);
        if (isStaff) {
            return "redirect:/staff/requests/list";
        }
        return "public/login";
    }

    @GetMapping("/")
    public String index(Model model) {
        boolean isStaff = securityService.getCurrentAuthentication()
                .map(auth -> securityService.hasRole(auth, "ROLE_STAFF"))
                .orElse(false);
        if (isStaff) {
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
        model.addAttribute(REQUEST_ATTRIBUTE, new Request());
        return REQUEST_SUBMIT_VIEW;
    }

    @PostMapping("/submit")
    public String submit(@Valid @ModelAttribute Request request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessages", validationErrorUtil.extractErrorMessages(bindingResult));
            model.addAttribute("fieldHasErrors", validationErrorUtil.getFieldErrorStatus(bindingResult));
            model.addAttribute(REQUEST_ATTRIBUTE, request);
            return REQUEST_SUBMIT_VIEW;
        }

        try {
            Request saved = requestService.publicCreate(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Yêu cầu đã được gửi thành công!");
            redirectAttributes.addFlashAttribute("requestId", saved.getId());
            return "redirect:/submit";
        } catch (ValidationException ex) {
            model.addAttribute(REQUEST_ATTRIBUTE, request);
            model.addAttribute("errorMessage", ex.getMessage());
            return REQUEST_SUBMIT_VIEW;
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
            model.addAttribute(REQUEST_ATTRIBUTE, request);
            model.addAttribute("isStaff", false);
            return "staff/request-detail";
        } catch (NotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/";
        }
    }
}