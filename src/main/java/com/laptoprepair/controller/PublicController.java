package com.laptoprepair.controller;

import com.laptoprepair.entity.Request;
import com.laptoprepair.service.AuthService;
import com.laptoprepair.service.RequestService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final AuthService authService;

    @GetMapping("/login")
    public String login() {
        if (authService.isStaff()) {
            return "redirect:/staff/requests/list";
        }
        return "public/login";
    }

    @GetMapping("/")
    public String index(Model model) {
        if (authService.isStaff()) {
            return "redirect:/staff/requests/list";
        }
        return "public/index";
    }

    @PostMapping("/lookup")
    public String lookupPost(@RequestParam String id) {
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
            model.addAttribute("request", request);
            return "public/request-submit";
        }

        Request saved = requestService.publicCreate(request);
        redirectAttributes.addFlashAttribute("successMessage",
                "Yêu cầu đã được gửi thành công!");
        redirectAttributes.addFlashAttribute("requestId", saved.getId());
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

}