package com.swp391.g1.service;

import com.swp391.g1.model.RegistrationForm;

import java.util.List;

public interface IRegistrationFormService {
    List<RegistrationForm> getAll();

    List<RegistrationForm> getOpenForms();

    RegistrationForm getById(int id);

    String save(RegistrationForm form);

    String updateStatus(int id, String status);

    String delete(int id);

    String register(int formId, int studentId, String note);
}