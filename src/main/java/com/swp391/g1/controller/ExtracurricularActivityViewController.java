package com.swp391.g1.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/extracurricular-activities")
public class ExtracurricularActivityViewController {

    @GetMapping
    public String page() {
        return "extracurricular-activities/index";
    }
}
