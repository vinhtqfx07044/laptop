package com.laptoprepair.web;

import com.laptoprepair.entity.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.laptoprepair.common.AppConstants;
import org.springframework.ui.Model;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class DefaultRequestFormPopulator implements RequestFormPopulator {

    @Value("${app.upload.max-images-per-request}")
    private int maxImagesPerRequest;

    @Override
    public void populateForCreate(Model model, HttpServletRequest request) {
        model.addAttribute(AppConstants.ATTR_REQUEST, new Request());
        addCommonAttributes(model, request);
    }

    @Override
    public void populateForEdit(Request existing, Model model, HttpServletRequest request) {
        model.addAttribute(AppConstants.ATTR_REQUEST, existing);
        addCommonAttributes(model, request);

        // Add locked status for form controls
        boolean isLocked = existing.getStatus() != null && existing.getStatus().isLocked();
        model.addAttribute("locked", isLocked);
    }

    private void addCommonAttributes(Model model, HttpServletRequest request) {
        model.addAttribute("requestUri", request.getRequestURI());
        model.addAttribute("maxImages", maxImagesPerRequest);
    }
}