package com.laptoprepair.controller;

import com.laptoprepair.entity.Request;
import com.laptoprepair.service.RequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PublicController {

    private final RequestService requestService;

    @GetMapping("/login")
    public String login() {
        if (isStaffAuthenticated()) {
            return "redirect:/staff/requests/list";
        }
        return "public/login";
    }

    @GetMapping("/")
    public String index(Model model) {
        if (isStaffAuthenticated()) {
            return "redirect:/staff/requests/list";
        }
        return "public/index";
    }

    @PostMapping("/lookup")
    public String lookupPost(@RequestParam String id,
            RedirectAttributes redirectAttributes) {
        try {
            return "redirect:/public/request/" + UUID.fromString(id.trim());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "ID yêu cầu không hợp lệ. Vui lòng kiểm tra lại.");
            return "redirect:/";
        }
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
            model.addAttribute("request", request);
            return "public/request-submit";
        }

        Request saved = requestService.publicCreate(request);
        redirectAttributes.addFlashAttribute("successMessage", 
                "Yêu cầu đã được gửi thành công! <a href='/public/request/" + saved.getId() + "'>Xem chi tiết tại đây</a>");
        return "redirect:/submit";
    }

    @PostMapping("/recover")
    public String recoverSubmit(@RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {
        requestService.recover(email);
        redirectAttributes.addFlashAttribute("successMessage",
                "Nếu email tồn tại trong hệ thống, chúng tôi đã gửi danh sách yêu cầu!");
        return "redirect:/";
    }

    @GetMapping("/public/request/{id}")
    public String publicRequestDetail(@PathVariable UUID id, Model model) {
        Request request = requestService.findById(id);
        model.addAttribute("request", request);
        model.addAttribute("isStaff", false);
        return "staff/request-detail";
    }

    /**
     * Check if current user is authenticated as staff
     */
    private boolean isStaffAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() &&
                auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));
    }

}