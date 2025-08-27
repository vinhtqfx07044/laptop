package com.laptoprepair.controller;

import com.laptoprepair.entity.Request;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.exception.NotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    /**
     * Handles requests to the login page.
     * Redirects to staff request list if the user is already logged in as staff.
     * 
     * @return The login view or a redirect to staff dashboard.
     */
    @GetMapping("/login")
    public String login() {
        if (isStaff()) {
            return "redirect:/staff/requests/list";
        }
        return "public/login";
    }

    /**
     * Handles requests to the application's home page.
     * Redirects to staff request list if the user is already logged in as staff.
     * 
     * @param model The Model object for view data.
     * @return The index view or a redirect to staff dashboard.
     */
    @GetMapping("/")
    public String index(Model model) {
        if (isStaff()) {
            return "redirect:/staff/requests/list";
        }
        return "public/index";
    }

    /**
     * Handles POST requests for looking up a request by ID.
     * 
     * @param id The ID of the request to lookup.
     * @return A redirect to the public request detail page.
     */
    @PostMapping("/lookup")
    public String lookup(@RequestParam String id) {
        return "redirect:/public/request/" + UUID.fromString(id.trim());
    }

    /**
     * Handles requests to the about page.
     * 
     * @return The about view.
     */
    @GetMapping("/about")
    public String about() {
        return "public/about";
    }

    /**
     * Displays the request submission form.
     * 
     * @param model The Model object for view data.
     * @return The request submission form view.
     */
    @GetMapping("/submit")
    public String submitForm(Model model) {
        model.addAttribute("request", new Request());
        return "public/request-submit";
    }

    /**
     * Handles the submission of a new request.
     * Validates the request and saves it if valid, then redirects with a success
     * message.
     * 
     * @param request            The Request object submitted from the form.
     * @param bindingResult      The BindingResult for validation errors.
     * @param model              The Model object for view data.
     * @param redirectAttributes The RedirectAttributes for flash messages.
     * @return A redirect to the submit page with a success message or back to the
     *         form with errors.
     */
    @PostMapping("/submit")
    public String submit(@Valid @ModelAttribute Request request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
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

    /**
     * Handles requests to recover request information via email.
     * 
     * @param email              The email address to send recovery information to.
     * @param redirectAttributes The RedirectAttributes for flash messages.
     * @return A redirect to the home page with a success message.
     */
    @PostMapping("/recover")
    public String recover(@RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {
        requestService.recover(email);
        redirectAttributes.addFlashAttribute("successMessage",
                "Nếu email tồn tại trong hệ thống, chúng tôi đã gửi danh sách yêu cầu!");
        return "redirect:/";
    }

    /**
     * Displays the public detail page for a specific request.
     * 
     * @param id    The UUID of the request.
     * @param model The Model object for view data.
     * @return The request detail view.
     */
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