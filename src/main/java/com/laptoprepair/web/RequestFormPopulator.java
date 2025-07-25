package com.laptoprepair.web;

import com.laptoprepair.entity.Request;
import org.springframework.ui.Model;

import jakarta.servlet.http.HttpServletRequest;

public interface RequestFormPopulator {
    void populateForCreate(Model model, HttpServletRequest request);
    void populateForEdit(Request existing, Model model, HttpServletRequest request);
}